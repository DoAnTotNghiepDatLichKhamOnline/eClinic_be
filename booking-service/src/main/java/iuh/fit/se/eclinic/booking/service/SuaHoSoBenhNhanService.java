package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.request.SuaHoSoBenhNhanRequest;
import iuh.fit.se.eclinic.booking.dto.response.HoSoBenhNhanQuanTriResponse;

/** Quản trị viên hoặc bác sĩ có lịch hẹn xem, sửa hồ sơ bệnh nhân sau khi đối chiếu giấy tờ. */
public interface SuaHoSoBenhNhanService {

    /** Ném KHONG_TIM_THAY nếu không có hồ sơ. */
    HoSoBenhNhanQuanTriResponse xem(Long id);

    /** Sửa họ tên, ngày sinh, giới tính, SĐT, địa chỉ, số bảo hiểm y tế; điền số CCCD cho hồ sơ chưa có. */
    HoSoBenhNhanQuanTriResponse sua(Long id, SuaHoSoBenhNhanRequest request, Long idNguoiSua);

}
