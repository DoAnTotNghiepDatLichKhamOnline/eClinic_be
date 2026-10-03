package iuh.fit.se.eclinic.identity.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import iuh.fit.se.eclinic.common.entity.identity.RefreshToken;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;

/**
 * Quản lý phiên đăng nhập. Token gốc chỉ nằm ở client; DB chỉ giữ SHA-256 của token.
 */
public interface RefreshTokenService {

    /** SHA-256 hex của refresh token gốc. */
    String bamToken(String rawToken);

    /**
     * Phiên vừa tạo: token gốc để gửi cho client (chỉ lần này) và mã phiên để đưa vào access token.
     */
    record PhienMoi(String refreshToken, String maPhien) {

        /** Không in token (tránh lộ khi bị log). */
        @Override
        public String toString() {
            return "PhienMoi[maPhien=" + maPhien + "]";
        }

    }

    /** Tạo phiên đăng nhập mới (mã phiên mới, thời điểm đăng nhập = bây giờ), lưu hash của token. */
    PhienMoi tao(TaiKhoan taiKhoan, String thongTinThietBi, Duration thoiHan);

    /**
     * Tạo dòng tiếp theo của 1 phiên đang có (làm mới phiên): giữ nguyên mã phiên và thời điểm đăng nhập.
     * Người gọi PHẢI đã thu hồi dòng cũ. Trả về token gốc mới.
     */
    String taoTiepTheo(TaiKhoan taiKhoan, String maPhien, LocalDateTime ngayDangNhap, String thongTinThietBi,
            Duration thoiHan);

    /** Phiên tương ứng với token gốc, bất kể đã thu hồi hay hết hạn. */
    Optional<RefreshToken> timTheoToken(String rawToken);

    /** Phiên còn hiệu lực (chưa thu hồi, chưa hết hạn) tương ứng với token gốc. */
    Optional<RefreshToken> timPhienConHanTheoToken(String rawToken);

    /** Danh sách thiết bị đang đăng nhập của tài khoản. */
    List<RefreshToken> layPhienDangHoatDong(Long taiKhoanId);

    void thuHoi(Long refreshTokenId);

    /** Thu hồi nguyên tử: true nếu lời gọi này là lời gọi thu hồi phiên (phiên còn hiệu lực trước đó). */
    boolean thuHoiNeuConHieuLuc(Long refreshTokenId);

    /** Đăng xuất khỏi tất cả thiết bị (AUTH-03 sau khi đặt lại mật khẩu, ADM-03 khi vô hiệu hóa tài khoản). */
    int thuHoiTatCaCuaTaiKhoan(Long taiKhoanId);

    /**
     * Đăng xuất 1 phiên của tài khoản theo mã phiên. false nếu tài khoản không có phiên nào còn hiệu lực với mã đó
     * (mã sai, phiên của tài khoản khác, đã đăng xuất hoặc đã hết hạn).
     */
    boolean thuHoiPhien(Long taiKhoanId, String maPhien);

    /**
     * Đăng xuất mọi phiên của tài khoản trừ phiên {@code maPhienGiuLai}; null thì đăng xuất tất cả.
     * Trả về số phiên đã thu hồi.
     */
    int thuHoiCacPhienKhac(Long taiKhoanId, String maPhienGiuLai);

    /** Xoá các phiên đã hết hạn; trả về số dòng đã xoá. */
    int xoaPhienHetHan();

}
