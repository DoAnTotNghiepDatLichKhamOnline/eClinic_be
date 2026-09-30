package iuh.fit.se.eclinic.identity.service;

import java.time.Duration;

import iuh.fit.se.eclinic.identity.enums.MucDichLienKet;

/**
 * Token của liên kết gửi qua email (kích hoạt tài khoản, đặt lại mật khẩu), lưu ở Redis:
 * <ul>
 * <li>{@code lien-ket:<MUC_DICH>:<sha256>} -> id tài khoản, có TTL. Chỉ lưu bản băm, không lưu token gốc.</li>
 * <li>{@code lien-ket:tai-khoan:<MUC_DICH>:<id>} -> bản băm đang hiệu lực, để phát token mới thì huỷ token cũ.</li>
 * <li>{@code lien-ket:cho:<MUC_DICH>:<id>} -> khoá chờ giữa 2 lần gửi.</li>
 * </ul>
 * <b>Quy ước:</b> chỉ gọi {@link #tao} sau khi {@link #giuCho} trả về true. {@code tao} không nguyên tử;
 * khoá chờ đảm bảo không có 2 lần {@code tao} chạy song song cho cùng tài khoản + mục đích.
 */
public interface TokenLienKetService {

    /** Phát token mới (token cũ cùng mục đích của tài khoản hết hiệu lực) và trả về token gốc để đặt vào liên kết. */
    String tao(Long idTaiKhoan, MucDichLienKet mucDich, Duration thoiHan);

    /**
     * Dùng token (xoá luôn, nên chỉ dùng được 1 lần kể cả khi bấm đồng thời) và trả về id tài khoản.
     * Ném LIEN_KET_KHONG_HOP_LE nếu token sai, hết hạn, đã dùng hoặc thuộc mục đích khác.
     */
    Long suDung(String token, MucDichLienKet mucDich);

    /** Giữ khoá chờ; false nếu lần gửi trước chưa hết thời gian chờ. */
    boolean giuCho(Long idTaiKhoan, MucDichLienKet mucDich, Duration thoiGianCho);

    /** Huỷ token đang hiệu lực (nếu có) của tài khoản. */
    void huy(Long idTaiKhoan, MucDichLienKet mucDich);

}
