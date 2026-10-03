package iuh.fit.se.eclinic.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;

import iuh.fit.se.eclinic.catalog.dto.request.ChuyenKhoaRequest;
import iuh.fit.se.eclinic.catalog.dto.response.ChuyenKhoaResponse;
import iuh.fit.se.eclinic.catalog.service.ChuyenKhoaService;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.testsupport.MySqlTestcontainersConfiguration;
import iuh.fit.se.eclinic.common.testsupport.TestFixtures;
import jakarta.persistence.EntityManager;

/**
 * MẪU test service: chạy với MySQL thật (Testcontainers) cho các quy tắc do DB quyết định
 * (collation khi so trùng tên / tìm kiếm, đếm dữ liệu liên quan, khoá ngoại).
 * Dữ liệu mỗi test có hậu tố UUID để không đụng nhau trong container dùng chung.
 */
@SpringBootTest
@Import(MySqlTestcontainersConfiguration.class)
class ChuyenKhoaServiceTest {

    @Autowired ChuyenKhoaService chuyenKhoaService;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactionManager;

    /** Các test bên dưới dựa vào collation giống docker-compose.yml (MySqlTestcontainersConfiguration). */
    @Test
    void cotTenDungCollationGiongDockerCompose() {
        Object collation = entityManager.createNativeQuery("SELECT COLLATION_NAME FROM information_schema.COLUMNS"
                + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'chuyen_khoa' AND COLUMN_NAME = 'ten_chuyen_khoa'")
                .getSingleResult();
        assertThat(collation).isEqualTo("utf8mb4_unicode_ci");
    }

    @Test
    void trungTenKhongPhanBietHoaThuongVaDau() {
        String hauTo = hauTo();
        chuyenKhoaService.tao(new ChuyenKhoaRequest("Nội tiết " + hauTo, null));

        assertThatThrownBy(() -> chuyenKhoaService.tao(new ChuyenKhoaRequest("  noi TIET " + hauTo + " ", null)))
                .isInstanceOf(LoiNghiepVu.class)
                .extracting(e -> ((LoiNghiepVu) e).getMaLoi())
                .isEqualTo(MaLoi.TEN_CHUYEN_KHOA_DA_TON_TAI);
    }

    @Test
    void capNhatGiuTenCuDuocNhungTrungTenKhacThiLoi() {
        String hauTo = hauTo();
        ChuyenKhoaResponse timMach = chuyenKhoaService.tao(new ChuyenKhoaRequest("Tim mạch " + hauTo, null));
        chuyenKhoaService.tao(new ChuyenKhoaRequest("Thần kinh " + hauTo, null));

        ChuyenKhoaResponse daSua = chuyenKhoaService.capNhat(timMach.id(),
                new ChuyenKhoaRequest("Tim mạch " + hauTo, "Bệnh tim, huyết áp"));
        assertThat(daSua.moTa()).isEqualTo("Bệnh tim, huyết áp");

        assertThatThrownBy(() -> chuyenKhoaService.capNhat(timMach.id(),
                new ChuyenKhoaRequest("THAN KINH " + hauTo, null)))
                .isInstanceOf(LoiNghiepVu.class)
                .extracting(e -> ((LoiNghiepVu) e).getMaLoi())
                .isEqualTo(MaLoi.TEN_CHUYEN_KHOA_DA_TON_TAI);
    }

    @Test
    void timKiemKhongDauVanThay() {
        String hauTo = hauTo();
        chuyenKhoaService.tao(new ChuyenKhoaRequest("Nội tiết " + hauTo, null));

        TrangDuLieu<ChuyenKhoaResponse> ketQua = chuyenKhoaService.timKiem("noi tiet " + hauTo, 0, 20);

        assertThat(ketQua.tongSoPhanTu()).isEqualTo(1);
        assertThat(ketQua.noiDung().get(0).tenChuyenKhoa()).isEqualTo("Nội tiết " + hauTo);
    }

    @Test
    void xoaChuyenKhoaChuaDungThiXoaDuoc() {
        ChuyenKhoaResponse daTao = chuyenKhoaService.tao(new ChuyenKhoaRequest("Da liễu " + hauTo(), null));

        chuyenKhoaService.xoa(daTao.id());

        assertThatThrownBy(() -> chuyenKhoaService.layTheoId(daTao.id())).isInstanceOf(LoiKhongTimThay.class);
    }

    @Test
    void xoaChuyenKhoaDangDungThiBao409() {
        Long idChuyenKhoa = new TestFixtures(entityManager, transactionManager).taoCaLamViec()
                .bacSi().getChuyenKhoa().getId();

        assertThatThrownBy(() -> chuyenKhoaService.xoa(idChuyenKhoa))
                .isInstanceOf(LoiNghiepVu.class)
                .hasMessageContaining("1 phòng khám và 1 bác sĩ")
                .extracting(e -> ((LoiNghiepVu) e).getMaLoi())
                .isEqualTo(MaLoi.CHUYEN_KHOA_DANG_DUOC_SU_DUNG);
    }

    private static String hauTo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

}
