package iuh.fit.se.eclinic.identity.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import iuh.fit.se.eclinic.common.entity.identity.RefreshToken;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;

/** Quản lý phiên đăng nhập. */
public interface RefreshTokenService {

    /** SHA-256 hex của refresh token gốc. */
    String bamToken(String rawToken);

    /** Phiên vừa tạo: token gốc để gửi cho client (chỉ lần này) và mã phiên cho access token. */
    record PhienMoi(String refreshToken, String maPhien) {

        /** Không in token (tránh lộ khi bị log). */
        @Override
        public String toString() {
            return "PhienMoi[maPhien=" + maPhien + "]";
        }

    }

    /** Tạo phiên đăng nhập mới (mã phiên mới, thời điểm đăng nhập = bây giờ), lưu hash của token. */
    PhienMoi tao(TaiKhoan taiKhoan, String thongTinThietBi, Duration thoiHan);

    /** Tạo dòng tiếp theo của 1 phiên (làm mới): giữ nguyên mã phiên và thời điểm đăng nhập. */
    String taoTiepTheo(TaiKhoan taiKhoan, String maPhien, LocalDateTime ngayDangNhap, String thongTinThietBi,
            Duration thoiHan);

    /** Phiên tương ứng với token gốc, bất kể đã thu hồi hay hết hạn. */
    Optional<RefreshToken> timTheoToken(String rawToken);

    /** Phiên còn hiệu lực (chưa thu hồi, chưa hết hạn) tương ứng với token gốc. */
    Optional<RefreshToken> timPhienConHanTheoToken(String rawToken);

    /** Danh sách thiết bị đang đăng nhập của tài khoản. */
    List<RefreshToken> layPhienDangHoatDong(Long taiKhoanId);

    void thuHoi(Long refreshTokenId);

    /** Thu hồi nguyên tử: true nếu lời gọi này là lời gọi thu hồi phiên. */
    boolean thuHoiNeuConHieuLuc(Long refreshTokenId);

    /** Đăng xuất khỏi tất cả thiết bị. */
    int thuHoiTatCaCuaTaiKhoan(Long taiKhoanId);

    /** Đăng xuất 1 phiên của tài khoản theo mã; false nếu không có phiên nào còn hiệu lực với mã đó. */
    boolean thuHoiPhien(Long taiKhoanId, String maPhien);

    /** Đăng xuất mọi phiên của tài khoản trừ phiên {@code maPhienGiuLai}; null thì đăng xuất tất cả. */
    int thuHoiCacPhienKhac(Long taiKhoanId, String maPhienGiuLai);

    /** Xoá các phiên đã hết hạn; trả về số dòng đã xoá. */
    int xoaPhienHetHan();

}
