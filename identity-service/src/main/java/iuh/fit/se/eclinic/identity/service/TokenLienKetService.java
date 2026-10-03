package iuh.fit.se.eclinic.identity.service;

import java.time.Duration;
import java.util.Optional;

import iuh.fit.se.eclinic.identity.enums.MucDichLienKet;

/**
 * Token của liên kết gửi qua email (kích hoạt tài khoản, đặt lại mật khẩu, đổi email), lưu ở Redis:
 * <ul>
 * <li>{@code lien-ket:<MUC_DICH>:<sha256>} -> {@code <id tài khoản>} hoặc {@code <id tài khoản>:<dữ liệu kèm>}, có TTL.
 * Chỉ lưu bản băm, không lưu token gốc.</li>
 * <li>{@code lien-ket:tai-khoan:<MUC_DICH>:<id>} -> bản băm đang hiệu lực, để phát token mới thì huỷ token cũ.</li>
 * <li>{@code lien-ket:cho:<MUC_DICH>:<id>} -> khoá chờ giữa 2 lần gửi.</li>
 * </ul>
 * Dữ liệu kèm (ví dụ email mới của liên kết đổi email) nằm chung 1 giá trị với id tài khoản, nên 1 lần đọc-xoá nguyên tử
 * trả về đúng dữ liệu đã phát cùng token đó: liên kết cũ không bao giờ áp dụng được dữ liệu của yêu cầu mới hơn.
 * <p>
 * <b>Quy ước:</b> chỉ gọi {@link #tao} sau khi {@link #giuCho} trả về true. {@code tao} không nguyên tử;
 * khoá chờ đảm bảo không có 2 lần {@code tao} chạy song song cho cùng tài khoản + mục đích.
 */
public interface TokenLienKetService {

    /**
     * Token đã dùng.
     *
     * @param duLieu dữ liệu kèm lúc phát token; null nếu token không kèm dữ liệu
     */
    record LienKetDaDung(Long idTaiKhoan, String duLieu) {
    }

    /** Phát token mới (token cũ cùng mục đích của tài khoản hết hiệu lực) và trả về token gốc để đặt vào liên kết. */
    String tao(Long idTaiKhoan, MucDichLienKet mucDich, Duration thoiHan);

    /** Như {@link #tao(Long, MucDichLienKet, Duration)}, kèm dữ liệu sẽ nhận lại ở {@link #suDungKemDuLieu}. */
    String tao(Long idTaiKhoan, MucDichLienKet mucDich, Duration thoiHan, String duLieu);

    /**
     * Dùng token (xoá luôn, nên chỉ dùng được 1 lần kể cả khi bấm đồng thời) và trả về id tài khoản.
     * Ném LIEN_KET_KHONG_HOP_LE nếu token sai, hết hạn, đã dùng hoặc thuộc mục đích khác.
     */
    Long suDung(String token, MucDichLienKet mucDich);

    /** Như {@link #suDung}, trả thêm dữ liệu đã kèm lúc phát token. */
    LienKetDaDung suDungKemDuLieu(String token, MucDichLienKet mucDich);

    /**
     * Dữ liệu kèm của token đang hiệu lực của tài khoản; chỉ đọc, không dùng token. Rỗng nếu tài khoản không có token
     * đang hiệu lực (chưa phát, đã dùng, đã huỷ, hết hạn) hoặc token không kèm dữ liệu.
     */
    Optional<String> xemDuLieu(Long idTaiKhoan, MucDichLienKet mucDich);

    /** Giữ khoá chờ; false nếu lần gửi trước chưa hết thời gian chờ. */
    boolean giuCho(Long idTaiKhoan, MucDichLienKet mucDich, Duration thoiGianCho);

    /** Huỷ token đang hiệu lực (nếu có) của tài khoản. */
    void huy(Long idTaiKhoan, MucDichLienKet mucDich);

}
