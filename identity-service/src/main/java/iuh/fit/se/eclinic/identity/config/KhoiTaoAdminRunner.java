package iuh.fit.se.eclinic.identity.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.entity.identity.QuanTriVien;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.CapDoQuyen;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.identity.repository.QuanTriVienRepository;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.util.ChuanHoa;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Khi khởi động, nếu DB chưa có tài khoản QUAN_TRI_VIEN nào thì tạo 1 admin toàn quyền từ
 * {@link AdminMacDinhProperties}. Cần có admin vì tài khoản bác sĩ chỉ do admin cấp (AUTH-04).
 * Chạy lại nhiều lần không tạo thêm.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KhoiTaoAdminRunner implements ApplicationRunner {

    /** PHẢI trùng giá trị mặc định của app.admin-mac-dinh.mat-khau trong application.yml. */
    static final String MAT_KHAU_MAC_DINH = "Admin@123";

    private final TaiKhoanRepository taiKhoanRepository;
    private final QuanTriVienRepository quanTriVienRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminMacDinhProperties adminMacDinhProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (taiKhoanRepository.existsByVaiTro(VaiTro.QUAN_TRI_VIEN)) {
            return;
        }

        // Chuẩn hoá như lúc đăng ký để đăng nhập tìm được (đăng nhập cũng chuẩn hoá email)
        String email = ChuanHoa.email(adminMacDinhProperties.email());
        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setHoTen("Quản trị viên");
        taiKhoan.setEmail(email);
        taiKhoan.setMatKhauHash(passwordEncoder.encode(adminMacDinhProperties.matKhau()));
        taiKhoan.setVaiTro(VaiTro.QUAN_TRI_VIEN);
        taiKhoan.setTrangThai(TrangThaiTaiKhoan.DA_KICH_HOAT);
        taiKhoanRepository.save(taiKhoan);

        QuanTriVien quanTriVien = new QuanTriVien();
        quanTriVien.setTaiKhoan(taiKhoan);
        quanTriVien.setCapDoQuyen(CapDoQuyen.TOAN_QUYEN);
        quanTriVienRepository.save(quanTriVien);

        log.info("Đã tạo tài khoản quản trị mặc định: {}", email);
        if (MAT_KHAU_MAC_DINH.equals(adminMacDinhProperties.matKhau())) {
            log.warn("Tài khoản quản trị dùng mật khẩu mặc định (xem README): đặt ADMIN_PASSWORD khi triển khai thật.");
        }
    }

}
