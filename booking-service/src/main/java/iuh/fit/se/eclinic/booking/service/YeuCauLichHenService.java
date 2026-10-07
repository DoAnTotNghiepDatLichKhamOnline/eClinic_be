package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;

/**
 * Bác sĩ xác nhận / từ chối lịch hẹn đang chờ xác nhận của CHÍNH MÌNH. Lịch hẹn không có hoặc của bác sĩ khác đều ném
 * KHONG_TIM_THAY với cùng 1 thông điệp. Lỗi tài khoản như {@link TaiKhoanService#layBacSiDangHoatDong}.
 * <p>
 * Chỉ xác nhận / từ chối được lịch CHO_XAC_NHAN mà lượt khám chưa bắt đầu: ném LICH_HEN_KHONG_CHO_XAC_NHAN (đã xác
 * nhận, bị từ chối, đã khám, đã hủy; kể cả khi gọi lặp lại) hoặc LICH_HEN_DA_QUA_GIO.
 */
public interface YeuCauLichHenService {

    /** Lịch CHO_XAC_NHAN của bác sĩ mà lượt khám chưa bắt đầu, giờ khám sớm nhất trước. */
    TrangDuLieu<LichHenTrongCaResponse> danhSach(Long idTaiKhoanBacSi, int trang, int kichThuoc);

    /** CHO_XAC_NHAN -> DA_XAC_NHAN. */
    LichHenTrongCaResponse xacNhan(Long idTaiKhoanBacSi, Long idLichHen);

    /** CHO_XAC_NHAN -> BI_TU_CHOI, ghi lý do và trả lượt khám về CON_TRONG để người khác đặt được. */
    LichHenTrongCaResponse tuChoi(Long idTaiKhoanBacSi, Long idLichHen, String lyDo);

}
