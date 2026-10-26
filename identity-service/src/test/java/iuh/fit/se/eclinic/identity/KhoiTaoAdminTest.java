package iuh.fit.se.eclinic.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.CapDoQuyen;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.testsupport.MySqlTestcontainersConfiguration;
import iuh.fit.se.eclinic.common.testsupport.RedisTestcontainersConfiguration;
import iuh.fit.se.eclinic.identity.config.KhoiTaoAdminRunner;
import iuh.fit.se.eclinic.identity.repository.QuanTriVienRepository;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;

/**
 * Khởi động identity-service trên DB trống phải tạo đúng 1 admin mặc định, chạy lại không tạo thêm.
 */
@SpringBootTest
@Import({ MySqlTestcontainersConfiguration.class, RedisTestcontainersConfiguration.class })
class KhoiTaoAdminTest {

    @Autowired KhoiTaoAdminRunner khoiTaoAdminRunner;
    @Autowired TaiKhoanRepository taiKhoanRepository;
    @Autowired QuanTriVienRepository quanTriVienRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void taoDungMotAdminMacDinh() {
        assertThat(soAdmin()).isEqualTo(1);

        TaiKhoan admin = taiKhoanRepository.findByEmail("admin@eclinic.local").orElseThrow();
        assertThat(admin.getTrangThai()).isEqualTo(TrangThaiTaiKhoan.DA_KICH_HOAT);
        assertThat(passwordEncoder.matches("Admin@123", admin.getMatKhauHash())).isTrue();
        assertThat(quanTriVienRepository.findByTaiKhoanId(admin.getId()).orElseThrow().getCapDoQuyen())
                .isEqualTo(CapDoQuyen.TOAN_QUYEN);

        khoiTaoAdminRunner.run(null);
        assertThat(soAdmin()).isEqualTo(1);
    }

    private long soAdmin() {
        return taiKhoanRepository.findAll().stream().filter(tk -> tk.getVaiTro() == VaiTro.QUAN_TRI_VIEN).count();
    }

}
