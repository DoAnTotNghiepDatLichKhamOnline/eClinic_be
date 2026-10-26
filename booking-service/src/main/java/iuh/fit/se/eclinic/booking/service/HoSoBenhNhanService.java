package iuh.fit.se.eclinic.booking.service;

import java.util.Optional;

import iuh.fit.se.eclinic.booking.dto.request.HoSoCuaToiRequest;
import iuh.fit.se.eclinic.booking.dto.response.HoSoCuaToiResponse;
import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;

public interface HoSoBenhNhanService {

    HoSoBenhNhan layTheoId(Long id);

    Optional<HoSoBenhNhan> timTheoTaiKhoanId(Long taiKhoanId);

    Optional<HoSoBenhNhan> timTheoCccd(String cccd);

    /** Hồ sơ bệnh nhân ĐÃ LIÊN KẾT của tài khoản. */
    Optional<HoSoBenhNhan> timDaLienKetCuaTaiKhoan(Long idTaiKhoan);

    /** Hồ sơ bệnh nhân của tài khoản đang đăng nhập. */
    HoSoCuaToiResponse xemCuaToi(Long idTaiKhoan);

    /** Tạo hồ sơ gắn vào tài khoản nếu chưa có (CCCD bắt buộc, chưa dùng), có rồi thì sửa thông tin. */
    HoSoCuaToiResponse luuCuaToi(Long idTaiKhoan, HoSoCuaToiRequest request);

}
