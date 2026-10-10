package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.request.SuaHoSoBenhNhanRequest;
import iuh.fit.se.eclinic.booking.dto.response.HoSoKhamResponse;

/**
 * Hồ sơ khám của bệnh nhân cho bác sĩ. Mọi thao tác chỉ với lịch hẹn của CHÍNH bác sĩ đang đăng nhập: lịch hẹn không có
 * hoặc của bác sĩ khác đều ném KHONG_TIM_THAY với cùng 1 thông điệp. Lỗi tài khoản như
 * {@link TaiKhoanService#layBacSiDangHoatDong}.
 */
public interface HoSoKhamService {

    /** Mở từ danh sách (bấm vào lịch hẹn / số thứ tự). */
    HoSoKhamResponse theoLichHen(Long idTaiKhoanBacSi, Long idLichHen);

    /** Mở bằng mã gõ tay hoặc quét QR; {@code ma} như {@link LichHenService#timTheoMa}. */
    HoSoKhamResponse theoMa(Long idTaiKhoanBacSi, String ma);

    /**
     * Sửa hồ sơ bệnh nhân của lịch hẹn sau khi đối chiếu giấy tờ, rồi trả hồ sơ khám mới. Ném thêm các mã của
     * {@link SuaHoSoBenhNhanService#sua}.
     */
    HoSoKhamResponse suaBenhNhan(Long idTaiKhoanBacSi, Long idLichHen, SuaHoSoBenhNhanRequest request);

    /** Đã đối chiếu giấy tờ: bỏ đánh dấu cần đối chiếu của lịch hẹn. Gọi lại nhiều lần không đổi gì. */
    void daDoiChieu(Long idTaiKhoanBacSi, Long idLichHen);

}
