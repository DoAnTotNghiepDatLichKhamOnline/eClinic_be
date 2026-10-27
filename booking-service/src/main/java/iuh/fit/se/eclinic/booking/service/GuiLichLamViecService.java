package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.request.GuiLichRequest;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaGuiLichResponse;

/** Quản trị viên gửi lịch làm việc: mỗi bác sĩ có ca nhận 1 email và 1 thông báo trong ứng dụng. */
public interface GuiLichLamViecService {

    /** Gửi lịch trong khoảng ngày (tối đa 42 ngày); không có ca nào thì báo lỗi. */
    KetQuaGuiLichResponse gui(Long idTaiKhoanQuanTri, GuiLichRequest request);

}
