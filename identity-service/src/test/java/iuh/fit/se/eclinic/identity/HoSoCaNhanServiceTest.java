package iuh.fit.se.eclinic.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.testsupport.TestFixtures;
import iuh.fit.se.eclinic.identity.dto.request.CapNhatHoSoRequest;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.service.HoSoCaNhanService;
import jakarta.persistence.EntityManager;

/** Hồ sơ cá nhân với MySQL thật: đọc hồ sơ của miền khác, quy tắc sửa họ tên và số điện thoại. */
class HoSoCaNhanServiceTest extends KiemThuTichHop {

    @Autowired HoSoCaNhanService hoSoCaNhanService;
    @Autowired TaiKhoanRepository taiKhoanRepository;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactionManager;

    private TestFixtures fixtures;

    @BeforeEach
    void khoiTao() {
        fixtures = new TestFixtures(entityManager, transactionManager);
    }

    @Test
    void soDienThoaiCuaTaiKhoanKhacTraVe409VaKhongSuaGi() {
        TaiKhoan taiKhoan = taoTaiKhoan(VaiTro.BENH_NHAN);
        TaiKhoan nguoiKhac = taoTaiKhoan(VaiTro.BENH_NHAN);

        assertMaLoi(() -> hoSoCaNhanService.capNhat(taiKhoan.getId(),
                new CapNhatHoSoRequest("Tên Mới", nguoiKhac.getSoDienThoai())), MaLoi.SO_DIEN_THOAI_DA_TON_TAI);

        TaiKhoan daLuu = taiKhoanRepository.findById(taiKhoan.getId()).orElseThrow();
        assertThat(daLuu.getHoTen()).isEqualTo(taiKhoan.getHoTen());
        assertThat(daLuu.getSoDienThoai()).isEqualTo(taiKhoan.getSoDienThoai());
    }

    private TaiKhoan taoTaiKhoan(VaiTro vaiTro) {
        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setHoTen("Người dùng " + hauTo());
        taiKhoan.setEmail("hs-" + hauTo() + "@example.com");
        taiKhoan.setMatKhauHash("x");
        taiKhoan.setSoDienThoai(soDienThoaiMoi());
        taiKhoan.setVaiTro(vaiTro);
        taiKhoan.setTrangThai(TrangThaiTaiKhoan.DA_KICH_HOAT);
        return taiKhoanRepository.save(taiKhoan);
    }

    private static void assertMaLoi(Runnable hanhDong, MaLoi maLoi) {
        assertThatThrownBy(hanhDong::run)
                .isInstanceOf(LoiNghiepVu.class)
                .extracting(e -> ((LoiNghiepVu) e).getMaLoi())
                .isEqualTo(maLoi);
    }

    private static String hauTo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /** Số điện thoại hợp lệ ngẫu nhiên (cột so_dien_thoai là UNIQUE). */
    private static String soDienThoaiMoi() {
        return "09" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    }

}
