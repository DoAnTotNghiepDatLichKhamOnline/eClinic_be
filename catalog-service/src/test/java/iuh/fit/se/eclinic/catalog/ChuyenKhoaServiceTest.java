package iuh.fit.se.eclinic.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;

import iuh.fit.se.eclinic.catalog.dto.request.ChuyenKhoaRequest;
import iuh.fit.se.eclinic.catalog.service.ChuyenKhoaService;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.testsupport.MySqlTestcontainersConfiguration;
import iuh.fit.se.eclinic.common.testsupport.TestFixtures;
import jakarta.persistence.EntityManager;

/** Test service với MySQL thật (Testcontainers) cho các quy tắc do DB quyết định. */
// Giữ 3 annotation giống hệt KhungDungChungTest để hai lớp dùng chung một Spring context (một lần khởi động MySQL)
@SpringBootTest(properties = "app.bao-mat.duong-dan-cong-khai=GET /api/mau/cong-khai")
@AutoConfigureMockMvc
@Import(MySqlTestcontainersConfiguration.class)
class ChuyenKhoaServiceTest {

    @Autowired ChuyenKhoaService chuyenKhoaService;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void trungTenKhongPhanBietHoaThuongVaDau() {
        String hauTo = hauTo();
        chuyenKhoaService.tao(new ChuyenKhoaRequest("Nội tiết " + hauTo, null, null, null, null, null));

        assertThatThrownBy(() -> chuyenKhoaService.tao(new ChuyenKhoaRequest("  noi TIET " + hauTo + " ", null, null, null, null, null)))
                .isInstanceOf(LoiNghiepVu.class)
                .extracting(e -> ((LoiNghiepVu) e).getMaLoi())
                .isEqualTo(MaLoi.TEN_CHUYEN_KHOA_DA_TON_TAI);
    }

    @Test
    void xoaChuyenKhoaDangDungThiBao409() {
        Long idChuyenKhoa = new TestFixtures(entityManager, transactionManager).taoCaLamViec()
                .bacSi().getChuyenKhoa().getId();

        assertThatThrownBy(() -> chuyenKhoaService.xoa(idChuyenKhoa))
                .isInstanceOf(LoiNghiepVu.class)
                // Lỗi mang khoá thông điệp + tham số (số phòng khám, số bác sĩ); câu chữ do XuLyLoiHandler soạn
                .hasMessage("loi.CHUYEN_KHOA_DANG_DUOC_SU_DUNG.soLuong")
                .satisfies(e -> assertThat(((LoiNghiepVu) e).getThamSo()).containsExactly("1", "1"))
                .extracting(e -> ((LoiNghiepVu) e).getMaLoi())
                .isEqualTo(MaLoi.CHUYEN_KHOA_DANG_DUOC_SU_DUNG);
    }

    private static String hauTo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

}
