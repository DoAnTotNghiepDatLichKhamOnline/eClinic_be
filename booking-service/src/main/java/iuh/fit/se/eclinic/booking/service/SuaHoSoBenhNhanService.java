package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.request.SuaHoSoBenhNhanRequest;
import iuh.fit.se.eclinic.booking.dto.response.HoSoBenhNhanQuanTriResponse;

/**
 * Phòng khám xem / sửa hồ sơ bệnh nhân (đã liên kết tài khoản hay chưa) sau khi đối chiếu giấy tờ: quản trị viên với hồ
 * sơ bất kỳ, bác sĩ với hồ sơ của bệnh nhân có lịch hẹn với mình (bên gọi kiểm tra quyền). Hồ sơ chưa liên kết tài
 * khoản chỉ sửa được ở đây: việc đặt lịch không sửa hồ sơ đã có.
 */
public interface SuaHoSoBenhNhanService {

    /** Ném KHONG_TIM_THAY nếu không có hồ sơ. */
    HoSoBenhNhanQuanTriResponse xem(Long id);

    /**
     * Sửa họ tên, ngày sinh, giới tính, SĐT, địa chỉ, số bảo hiểm y tế; điền số CCCD cho hồ sơ chưa có. Hồ sơ chưa có
     * CCCD thì khoá nhận diện được tính lại theo họ tên / ngày sinh mới.
     * <p>
     * Ném KHONG_TIM_THAY, DU_LIEU_KHONG_HOP_LE (đổi số CCCD đã có); số CCCD hoặc khoá nhận diện trùng hồ sơ khác vi
     * phạm UNIQUE -> 409 XUNG_DOT_DU_LIEU.
     *
     * @param idNguoiSua tài khoản quản trị viên / bác sĩ thực hiện, để ghi log
     */
    HoSoBenhNhanQuanTriResponse sua(Long id, SuaHoSoBenhNhanRequest request, Long idNguoiSua);

}
