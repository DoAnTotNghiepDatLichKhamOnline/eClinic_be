package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.request.GuiLichRequest;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaGuiLichResponse;

/**
 * Quản trị viên gửi lịch làm việc cho bác sĩ: mỗi bác sĩ có ca còn hoạt động (từ hôm nay) trong khoảng ngày nhận 1
 * email liệt kê các ca và 1 thông báo trong ứng dụng; bản in PDF bác sĩ tự tải ở trang Lịch làm việc. Gửi lại cùng
 * khoảng thì gửi thêm lần nữa.
 */
public interface GuiLichLamViecService {

    /**
     * Ném DU_LIEU_KHONG_HOP_LE nếu khoảng ngày sai / dài hơn 42 ngày, hoặc không có ca nào để gửi
     * ({@code guiLichKhongCoCa}).
     */
    KetQuaGuiLichResponse gui(Long idTaiKhoanQuanTri, GuiLichRequest request);

}
