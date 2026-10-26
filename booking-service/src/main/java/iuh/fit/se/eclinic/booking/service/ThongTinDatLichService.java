package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.response.ThongTinDatLichResponse;

public interface ThongTinDatLichService {

    /**
     * Thông tin điền sẵn form đặt lịch của tài khoản bệnh nhân đang đăng nhập: hồ sơ của mình, người thân đã lưu,
     * chuyên khoa và bác sĩ của lần đặt gần nhất. Lỗi tài khoản như {@link TaiKhoanService#layBenhNhanDangHoatDong}.
     */
    ThongTinDatLichResponse cuaToi(Long idTaiKhoan);

}
