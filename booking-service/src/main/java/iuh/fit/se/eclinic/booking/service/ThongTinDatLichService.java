package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.response.ThongTinDatLichResponse;

public interface ThongTinDatLichService {

    /** Thông tin điền sẵn form đặt lịch: hồ sơ của mình, người thân đã lưu, lần đặt gần nhất. */
    ThongTinDatLichResponse cuaToi(Long idTaiKhoan);

}
