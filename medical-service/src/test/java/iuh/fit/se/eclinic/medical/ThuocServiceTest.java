package iuh.fit.se.eclinic.medical;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import iuh.fit.se.eclinic.common.entity.medical.Thuoc;
import iuh.fit.se.eclinic.common.enums.TrangThaiThuoc;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.testsupport.MySqlTestcontainersConfiguration;
import iuh.fit.se.eclinic.medical.repository.ThuocRepository;
import iuh.fit.se.eclinic.medical.service.ThuocService;

@SpringBootTest
@Import(MySqlTestcontainersConfiguration.class)
class ThuocServiceTest {

    @Autowired ThuocService thuocService;
    @Autowired ThuocRepository thuocRepository;

    @Test
    void prescribableMedicinesAreReturnedAndOthersRefused() {
        Thuoc keDuoc = luu("Paracetamol", true, TrangThaiThuoc.DANG_DUNG);
        Thuoc ngungDung = luu("Ibuprofen", true, TrangThaiThuoc.NGUNG_DUNG);
        Thuoc chuaXacMinh = luu("Thuoc la", false, TrangThaiThuoc.DANG_DUNG);

        assertThat(thuocService.layDeKe(List.of(keDuoc.getId()), Set.of())).containsOnlyKeys(keDuoc.getId());
        assertThatThrownBy(() -> thuocService.layDeKe(List.of(keDuoc.getId(), ngungDung.getId()), Set.of()))
                .isInstanceOfSatisfying(LoiNghiepVu.class, loi -> assertThat(loi.getMaLoi()).isEqualTo(MaLoi.THUOC_NGUNG_DUNG));
        assertThatThrownBy(() -> thuocService.layDeKe(List.of(chuaXacMinh.getId()), Set.of()))
                .isInstanceOfSatisfying(LoiNghiepVu.class, loi -> assertThat(loi.getMaLoi()).isEqualTo(MaLoi.THUOC_CHUA_XAC_MINH));
        assertThatThrownBy(() -> thuocService.layDeKe(List.of(Long.MAX_VALUE), Set.of()))
                .isInstanceOfSatisfying(LoiNghiepVu.class, loi -> assertThat(loi.getMaLoi()).isEqualTo(MaLoi.KHONG_TIM_THAY));
    }

    @Test
    void medicinesAlreadyOnTheRecordAreKept() {
        Thuoc ngungDung = luu("Cefuroxim", true, TrangThaiThuoc.NGUNG_DUNG);
        Thuoc chuaXacMinh = luu("Thuoc cu", false, TrangThaiThuoc.DANG_DUNG);

        assertThat(thuocService.layDeKe(List.of(ngungDung.getId(), chuaXacMinh.getId()),
                Set.of(ngungDung.getId(), chuaXacMinh.getId())))
                .containsOnlyKeys(ngungDung.getId(), chuaXacMinh.getId());
    }

    @Test
    void suggestionsMatchNameOrActiveIngredientAndOnlyPrescribable() {
        String hoatChat = "hoatchat" + UUID.randomUUID().toString().substring(0, 8);
        Thuoc keDuoc = luu("Hapacol", hoatChat, true, TrangThaiThuoc.DANG_DUNG);
        luu("Hapacol chua duyet", hoatChat, false, TrangThaiThuoc.DANG_DUNG);
        luu("Hapacol ngung", hoatChat, true, TrangThaiThuoc.NGUNG_DUNG);

        assertThat(thuocService.timKiem(hoatChat.toUpperCase())).extracting(Thuoc::getId).containsExactly(keDuoc.getId());
        assertThat(thuocService.timKiem(keDuoc.getTenThuoc().substring(0, 12))).extracting(Thuoc::getId)
                .contains(keDuoc.getId());
    }

    /** Tên và hoạt chất có hậu tố ngẫu nhiên: các test dùng chung 1 DB. */
    private Thuoc luu(String ten, boolean daXacMinh, TrangThaiThuoc trangThai) {
        return luu(ten, "hoatchat" + UUID.randomUUID().toString().substring(0, 8), daXacMinh, trangThai);
    }

    private Thuoc luu(String ten, String hoatChat, boolean daXacMinh, TrangThaiThuoc trangThai) {
        String hauTo = UUID.randomUUID().toString().substring(0, 8);
        Thuoc thuoc = new Thuoc();
        thuoc.setTenThuoc(ten + " " + hauTo);
        thuoc.setTenChuanHoa((ten + " " + hauTo).toLowerCase());
        thuoc.setHoatChat(hoatChat);
        thuoc.setHamLuong("500mg");
        thuoc.setHamLuongChuanHoa("500mg");
        thuoc.setDaXacMinh(daXacMinh);
        thuoc.setTrangThai(trangThai);
        return thuocRepository.save(thuoc);
    }

}
