package iuh.fit.se.eclinic.identity.service;

import java.time.Duration;
import java.util.Optional;

import iuh.fit.se.eclinic.identity.enums.MucDichLienKet;

/** Token của liên kết gửi qua email (kích hoạt tài khoản, đặt lại mật khẩu, đổi email), lưu ở Redis. */
public interface TokenLienKetService {

    /** Token đã dùng. */
    record LienKetDaDung(Long idTaiKhoan, String duLieu) {
    }

    /** Phát token mới (token cũ cùng mục đích hết hiệu lực) và trả về token gốc để đặt vào liên kết. */
    String tao(Long idTaiKhoan, MucDichLienKet mucDich, Duration thoiHan);

    /** Như {@link #tao(Long, MucDichLienKet, Duration)}, kèm dữ liệu sẽ nhận lại ở {@link #suDungKemDuLieu}. */
    String tao(Long idTaiKhoan, MucDichLienKet mucDich, Duration thoiHan, String duLieu);

    /** Dùng token (xoá luôn, nên chỉ dùng được 1 lần) và trả về id tài khoản. */
    Long suDung(String token, MucDichLienKet mucDich);

    /** Như {@link #suDung}, trả thêm dữ liệu đã kèm lúc phát token. */
    LienKetDaDung suDungKemDuLieu(String token, MucDichLienKet mucDich);

    /** Dữ liệu kèm của token đang hiệu lực của tài khoản; chỉ đọc, không dùng token. */
    Optional<String> xemDuLieu(Long idTaiKhoan, MucDichLienKet mucDich);

    /** Giữ khoá chờ; false nếu lần gửi trước chưa hết thời gian chờ. */
    boolean giuCho(Long idTaiKhoan, MucDichLienKet mucDich, Duration thoiGianCho);

    /** Huỷ token đang hiệu lực (nếu có) của tài khoản. */
    void huy(Long idTaiKhoan, MucDichLienKet mucDich);

}
