package iuh.fit.se.eclinic.common.security;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Giới hạn số lượt BCrypt chạy cùng lúc (mặc định = số CPU), các lượt sau xếp hàng theo thứ tự đến.
 * <p>
 * BCrypt tốn CPU có chủ đích. Khi nhiều người đăng nhập cùng lúc trên máy ít CPU, để tất cả chạy song song thì lượt nào
 * cũng chậm như nhau và đa số vượt 30 giây của gateway; luồng đang giữ kết nối DB cũng không được CPU nên pool cạn (đã đo,
 * DOANTOTNGH-25 phase 4). Xếp hàng thì mỗi lượt chạy nhanh, luồng chờ không giữ kết nối DB và không tốn CPU.
 * Chờ quá {@code thoiGianCho} (ngắn hơn timeout của gateway) thì bỏ, để không băm cho request mà client đã bỏ đi.
 */
public class MaHoaMatKhauGioiHan implements PasswordEncoder {

    private final PasswordEncoder goc;
    private final Semaphore luot;
    private final long thoiGianChoMs;

    public MaHoaMatKhauGioiHan(PasswordEncoder goc, int soLuotDongThoi, Duration thoiGianCho) {
        this.goc = goc;
        this.luot = new Semaphore(soLuotDongThoi, true);
        this.thoiGianChoMs = thoiGianCho.toMillis();
    }

    @Override
    public String encode(CharSequence matKhau) {
        layLuot();
        try {
            return goc.encode(matKhau);
        } finally {
            luot.release();
        }
    }

    @Override
    public boolean matches(CharSequence matKhau, String hash) {
        layLuot();
        try {
            return goc.matches(matKhau, hash);
        } finally {
            luot.release();
        }
    }

    @Override
    public boolean upgradeEncoding(String hash) {
        return goc.upgradeEncoding(hash);
    }

    private void layLuot() {
        try {
            if (!luot.tryAcquire(thoiGianChoMs, TimeUnit.MILLISECONDS)) {
                throw new QuaTaiMaHoaException();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new QuaTaiMaHoaException();
        }
    }

    /** Ra 500 LOI_HE_THONG như khi pool DB hết hạn chờ. Không kèm stack trace để log không phình khi quá tải. */
    static class QuaTaiMaHoaException extends RuntimeException {
        QuaTaiMaHoaException() {
            super("Quá tải mã hoá mật khẩu: chờ quá lâu", null, false, false);
        }
    }

}
