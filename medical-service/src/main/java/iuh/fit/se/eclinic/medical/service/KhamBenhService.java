package iuh.fit.se.eclinic.medical.service;

import iuh.fit.se.eclinic.medical.dto.request.BenhAnRequest;
import iuh.fit.se.eclinic.medical.dto.response.BenhAnResponse;

/** Bác sĩ ghi nhận kết quả khám. */
public interface KhamBenhService {

    /** Lưu hồ sơ bệnh án kèm đơn thuốc và chuyển lịch hẹn sang DA_HOAN_THANH, trong 1 transaction. */
    BenhAnResponse ghiNhan(Long idTaiKhoanBacSi, Long idLichHen, BenhAnRequest request);

    /** Sửa hồ sơ bệnh án đã ghi: tăng phiên bản và thêm 1 dòng lịch sử sửa. */
    BenhAnResponse sua(Long idTaiKhoanBacSi, Long idLichHen, BenhAnRequest request);

    /** Hồ sơ bệnh án của 1 lịch hẹn. Ném KHONG_TIM_THAY nếu lịch hẹn chưa có hồ sơ bệnh án. */
    BenhAnResponse xem(Long idTaiKhoanBacSi, Long idLichHen);

}
