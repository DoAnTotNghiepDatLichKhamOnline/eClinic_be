package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.response.HoSoBenhNhanDongResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;

/**
 * Màn hình "Patient Management" của quản trị viên: danh sách / tìm kiếm hồ sơ bệnh nhân và lịch hẹn của 1 hồ sơ. Xem /
 * sửa 1 hồ sơ ở {@link SuaHoSoBenhNhanService}.
 */
public interface QuanLyHoSoBenhNhanService {

    /**
     * Hồ sơ mới tạo đứng trước, kể cả hồ sơ của khách đặt lịch không có tài khoản.
     *
     * @param tuKhoa           so với họ tên, số điện thoại, số bảo hiểm y tế (chứa từ khoá); đúng 12 chữ số thì còn so
     *                         khớp hoàn toàn với số CCCD. Rỗng = không lọc
     * @param trangThaiLienKet null = mọi trạng thái
     */
    TrangDuLieu<HoSoBenhNhanDongResponse> danhSach(String tuKhoa, TrangThaiLienKet trangThaiLienKet, int trang,
            int kichThuoc);

    /**
     * Lịch hẹn của 1 hồ sơ, mọi trạng thái, giờ khám muộn nhất trước. Không có chẩn đoán / đơn thuốc.
     * <p>
     * Ném KHONG_TIM_THAY nếu không có hồ sơ.
     */
    TrangDuLieu<LichHenTrongCaResponse> lichHen(Long idHoSoBenhNhan, int trang, int kichThuoc);
}
