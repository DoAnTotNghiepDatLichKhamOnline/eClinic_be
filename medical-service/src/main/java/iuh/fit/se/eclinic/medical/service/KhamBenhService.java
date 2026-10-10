package iuh.fit.se.eclinic.medical.service;

import iuh.fit.se.eclinic.medical.dto.request.BenhAnRequest;
import iuh.fit.se.eclinic.medical.dto.response.BenhAnResponse;

/**
 * Bác sĩ ghi nhận kết quả khám (EXAM-01, EXAM-02). Mọi thao tác chỉ với lịch hẹn của CHÍNH bác sĩ đang đăng nhập: lịch
 * hẹn không có hoặc của bác sĩ khác đều ném KHONG_TIM_THAY với cùng 1 thông điệp. Lỗi tài khoản như
 * {@link BacSiDangNhapService#layBacSiDangHoatDong}.
 */
public interface KhamBenhService {

    /**
     * Lưu hồ sơ bệnh án kèm đơn thuốc và chuyển lịch hẹn sang DA_HOAN_THANH, trong 1 transaction.
     * <p>
     * Ném LICH_HEN_KHONG_KHAM_DUOC nếu lịch hẹn không còn ở CHO_XAC_NHAN / DA_XAC_NHAN (đã khám, đã hủy, bị từ chối);
     * CHUA_DEN_NGAY_KHAM nếu ngày khám của lịch hẹn sau hôm nay.
     */
    BenhAnResponse ghiNhan(Long idTaiKhoanBacSi, Long idLichHen, BenhAnRequest request);

    /**
     * Sửa hồ sơ bệnh án đã ghi: thay chẩn đoán, ghi chú, ngày tái khám và toàn bộ đơn thuốc. Lịch hẹn giữ nguyên trạng
     * thái. Ném KHONG_TIM_THAY nếu lịch hẹn chưa có hồ sơ bệnh án.
     */
    BenhAnResponse sua(Long idTaiKhoanBacSi, Long idLichHen, BenhAnRequest request);

    /** Hồ sơ bệnh án của 1 lịch hẹn. Ném KHONG_TIM_THAY nếu lịch hẹn chưa có hồ sơ bệnh án. */
    BenhAnResponse xem(Long idTaiKhoanBacSi, Long idLichHen);

}
