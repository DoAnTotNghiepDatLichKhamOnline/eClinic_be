package iuh.fit.se.eclinic.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.CapDoQuyen;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.identity.config.KhoiTaoAdminRunner;
import iuh.fit.se.eclinic.identity.repository.QuanTriVienRepository;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;

/**
 * Khởi động identity-service trên DB trống phải tạo admin mặc định, chạy lại không tạo thêm.
 * Context dùng chung với các test khác (có test tự tạo quản trị viên), nên không đếm "đúng 1" trên cả DB.
 */
class KhoiTaoAdminTest extends KiemThuTichHop {

    @Autowired KhoiTaoAdminRunner khoiTaoAdminRunner;
    @Autowired TaiKhoanRepository taiKhoanRepository;
    @Autowired QuanTriVienRepository quanTriVienRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void taoDungMotAdminMacDinh() {
        long soAdminTruoc = soAdmin();

        TaiKhoan admin = taiKhoanRepository.findByEmail("admin@eclinic.local").orElseThrow();
        assertThat(admin.getTrangThai()).isEqualTo(TrangThaiTaiKhoan.DA_KICH_HOAT);
        assertThat(passwordEncoder.matches("Admin@123", admin.getMatKhauHash())).isTrue();
        assertThat(quanTriVienRepository.findByTaiKhoanId(admin.getId()).orElseThrow().getCapDoQuyen())
                .isEqualTo(CapDoQuyen.TOAN_QUYEN);

        khoiTaoAdminRunner.run(null);
        assertThat(soAdmin()).isEqualTo(soAdminTruoc);
    }

    private long soAdmin() {
        return taiKhoanRepository.findAll().stream().filter(tk -> tk.getVaiTro() == VaiTro.QUAN_TRI_VIEN).count();
    }

}
