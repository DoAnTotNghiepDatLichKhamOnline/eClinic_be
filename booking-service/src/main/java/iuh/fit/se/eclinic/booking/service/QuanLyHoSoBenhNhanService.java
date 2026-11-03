package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.request.SuaHoSoBenhNhanRequest;
import iuh.fit.se.eclinic.booking.dto.response.HoSoBenhNhanDongResponse;
import iuh.fit.se.eclinic.booking.dto.response.HoSoBenhNhanQuanTriResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;

/**
 * Màn hình "Patient Management" của quản trị viên: danh sách / tìm kiếm hồ sơ bệnh nhân và lịch hẹn của 1 hồ sơ. Quản
 * trị viên hoặc bác sĩ có lịch hẹn xem, sửa hồ sơ bệnh nhân sau khi đối chiếu giấy tờ.
 */
public interface QuanLyHoSoBenhNhanService {

    /** Hồ sơ mới tạo đứng trước, kể cả hồ sơ của khách đặt lịch không có tài khoản. */
    TrangDuLieu<HoSoBenhNhanDongResponse> danhSach(String tuKhoa, TrangThaiLienKet trangThaiLienKet, int trang,
            int kichThuoc);

    /** Lịch hẹn của 1 hồ sơ, mọi trạng thái, giờ khám muộn nhất trước. */
    TrangDuLieu<LichHenTrongCaResponse> lichHen(Long idHoSoBenhNhan, int trang, int kichThuoc);

    /** Ném KHONG_TIM_THAY nếu không có hồ sơ. */
    HoSoBenhNhanQuanTriResponse xem(Long id);

    /** Sửa họ tên, ngày sinh, giới tính, SĐT, địa chỉ, số bảo hiểm y tế; điền số CCCD cho hồ sơ chưa có. */
    HoSoBenhNhanQuanTriResponse sua(Long id, SuaHoSoBenhNhanRequest request, Long idNguoiSua);

}
