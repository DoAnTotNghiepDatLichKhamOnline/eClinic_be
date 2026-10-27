package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.response.HoSoBenhNhanDongResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;

/** Màn hình "Patient Management" của quản trị viên: danh sách / tìm kiếm hồ sơ bệnh nhân và lịch hẹn của 1 hồ sơ. */
public interface QuanLyHoSoBenhNhanService {

    /** Hồ sơ mới tạo đứng trước, kể cả hồ sơ của khách đặt lịch không có tài khoản. */
    TrangDuLieu<HoSoBenhNhanDongResponse> danhSach(String tuKhoa, TrangThaiLienKet trangThaiLienKet, int trang,
            int kichThuoc);

    /** Lịch hẹn của 1 hồ sơ, mọi trạng thái, giờ khám muộn nhất trước. */
    TrangDuLieu<LichHenTrongCaResponse> lichHen(Long idHoSoBenhNhan, int trang, int kichThuoc);
}
