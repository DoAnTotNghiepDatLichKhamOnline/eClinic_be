package iuh.fit.se.eclinic.identity.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.identity.client.ThongTinGoogle;
import iuh.fit.se.eclinic.identity.client.XacMinhTokenGoogle;
import iuh.fit.se.eclinic.identity.dto.response.DangNhapResponse;
import iuh.fit.se.eclinic.identity.enums.MucDichLienKet;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.service.DangNhapGoogleService;
import iuh.fit.se.eclinic.identity.service.DangNhapService;
import iuh.fit.se.eclinic.identity.service.TokenLienKetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DangNhapGoogleServiceImpl implements DangNhapGoogleService {

    private static final int DO_DAI_HO_TEN_TOI_DA = 150;
    private static final int DO_DAI_ANH_TOI_DA = 500;

    private final XacMinhTokenGoogle xacMinhTokenGoogle;
    private final TaiKhoanRepository taiKhoanRepository;
    private final TokenLienKetService tokenLienKetService;
    private final DangNhapService dangNhapService;

    /**
     * 2 request đăng nhập lần đầu cùng lúc: request sau vi phạm UNIQUE (google_id / email) khi saveAndFlush
     * -> 409 XUNG_DOT_DU_LIEU (XuLyLoiHandler), thử lại sẽ tìm thấy theo google_id.
     */
    @Override
    @Transactional
    public DangNhapResponse dangNhap(String idToken, String thongTinThietBi) {
        ThongTinGoogle google = xacMinhTokenGoogle.xacMinh(idToken);

        TaiKhoan taiKhoan = taiKhoanRepository.findByGoogleId(google.sub()).orElse(null);
        if (taiKhoan != null) {
            // Không đồng bộ email từ Google: email mới có thể trùng tài khoản khác
            kiemTraDuocDangNhap(taiKhoan);
            if (taiKhoan.getTrangThai() == TrangThaiTaiKhoan.CHO_XAC_NHAN) {
                // Liên kết Google luôn kích hoạt tài khoản, nên không thể xảy ra
                throw new LoiNghiepVu(MaLoi.TAI_KHOAN_CHUA_XAC_THUC);
            }
        } else {
            taiKhoan = timHoacTaoTheoEmail(google);
        }
        log.info("Đăng nhập Google id={}", taiKhoan.getId());
        return dangNhapService.capPhien(taiKhoan, thongTinThietBi);
    }

    private TaiKhoan timHoacTaoTheoEmail(ThongTinGoogle google) {
        TaiKhoan taiKhoan = taiKhoanRepository.findByEmail(google.email()).orElse(null);
        if (taiKhoan == null) {
            return taoMoi(google);
        }
        // Kiểm tra trước vai trò / trạng thái: người Google không đảm bảo là chủ email không được biết thêm gì
        // ngoài "email đã đăng ký" (điều mà đăng ký cũng đã cho biết)
        if (taiKhoan.getGoogleId() != null || !google.laGoogleXacNhan()) {
            log.info("Từ chối liên kết Google với tài khoản id={}: {}", taiKhoan.getId(),
                    taiKhoan.getGoogleId() != null ? "đã liên kết tài khoản Google khác" : "Google không đảm bảo chủ email");
            throw new LoiNghiepVu(MaLoi.EMAIL_DA_DANG_KY_MAT_KHAU);
        }
        kiemTraDuocDangNhap(taiKhoan);
        taiKhoan.setGoogleId(google.sub());
        if (taiKhoan.getTrangThai() == TrangThaiTaiKhoan.CHO_XAC_NHAN) {
            // Người đăng ký trước có thể không phải chủ email: bỏ mật khẩu, số điện thoại (chưa xác minh)
            // và liên kết kích hoạt của họ; chủ email thật (Google đảm bảo) nhận tài khoản
            taiKhoan.setTrangThai(TrangThaiTaiKhoan.DA_KICH_HOAT);
            taiKhoan.setMatKhauHash(null);
            taiKhoan.setSoDienThoai(null);
            ganHoSoGoogle(taiKhoan, google);
            tokenLienKetService.huy(taiKhoan.getId(), MucDichLienKet.XAC_THUC_EMAIL);
            log.info("Kích hoạt tài khoản id={} bằng Google, bỏ mật khẩu và số điện thoại chưa xác minh",
                    taiKhoan.getId());
        } else {
            // Đã kích hoạt: liên kết, giữ nguyên họ tên / mật khẩu. Ảnh: chỉ lấy ảnh Google khi tài khoản chưa có ảnh,
            // và chỉ ở lần liên kết này (các lần đăng nhập Google sau không đụng tới ảnh, nên ảnh đã bỏ không quay lại)
            if (taiKhoan.getAnhDaiDien() == null) {
                taiKhoan.setAnhDaiDien(anhGoogle(google));
            }
            log.info("Liên kết Google cho tài khoản id={}", taiKhoan.getId());
        }
        return taiKhoanRepository.saveAndFlush(taiKhoan);
    }

    private TaiKhoan taoMoi(ThongTinGoogle google) {
        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setEmail(google.email());
        taiKhoan.setGoogleId(google.sub());
        taiKhoan.setVaiTro(VaiTro.BENH_NHAN);
        taiKhoan.setTrangThai(TrangThaiTaiKhoan.DA_KICH_HOAT);
        ganHoSoGoogle(taiKhoan, google);
        taiKhoan = taiKhoanRepository.saveAndFlush(taiKhoan);
        log.info("Tạo tài khoản từ Google id={}", taiKhoan.getId());
        return taiKhoan;
    }

    /** Tài khoản đã liên kết: chỉ bệnh nhân, không bị vô hiệu hoá. */
    private static void kiemTraDuocDangNhap(TaiKhoan taiKhoan) {
        if (taiKhoan.getVaiTro() != VaiTro.BENH_NHAN) {
            log.info("Từ chối đăng nhập Google id={}: vai trò {}", taiKhoan.getId(), taiKhoan.getVaiTro());
            throw new LoiNghiepVu(MaLoi.DANG_NHAP_GOOGLE_KHONG_HO_TRO);
        }
        if (taiKhoan.getTrangThai() == TrangThaiTaiKhoan.VO_HIEU_HOA) {
            log.info("Từ chối đăng nhập Google id={}: tài khoản bị vô hiệu hoá", taiKhoan.getId());
            throw new LoiNghiepVu(MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA);
        }
    }

    /** Họ tên lấy từ Google (không có thì lấy phần trước @ của email); ảnh quá dài thì bỏ qua. */
    private static void ganHoSoGoogle(TaiKhoan taiKhoan, ThongTinGoogle google) {
        int viTriA = google.email().indexOf('@');
        String hoTen = google.ten() != null && !google.ten().isBlank()
                ? google.ten().trim()
                : (viTriA > 0 ? google.email().substring(0, viTriA) : google.email());
        taiKhoan.setHoTen(hoTen.length() > DO_DAI_HO_TEN_TOI_DA ? hoTen.substring(0, DO_DAI_HO_TEN_TOI_DA) : hoTen);
        taiKhoan.setAnhDaiDien(anhGoogle(google));
    }

    /**
     * Ảnh Google được lưu nguyên là URL của Google, không chép vào kho ảnh của mình (đăng nhập không phụ thuộc kho ảnh);
     * URL quá dài so với cột thì bỏ qua.
     */
    private static String anhGoogle(ThongTinGoogle google) {
        return google.anh() != null && google.anh().length() <= DO_DAI_ANH_TOI_DA ? google.anh() : null;
    }

}
