package iuh.fit.se.eclinic.booking.service;

import java.util.List;

import iuh.fit.se.eclinic.booking.dto.request.CapLaiLienKetRequest;
import iuh.fit.se.eclinic.booking.dto.request.KhachCapLaiLienKetRequest;
import iuh.fit.se.eclinic.booking.dto.response.CapLaiLienKetResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;

/** Cấp lại liên kết phiếu khám (và sổ khám bệnh nếu đã khám) cho người đã mất link. */
public interface CapLaiLienKetService {

    /** Quản trị viên cấp lại cho 1 lịch hẹn bất kỳ. */
    CapLaiLienKetResponse quanTri(Long idTaiKhoan, Long idLichHen, CapLaiLienKetRequest request);

    /** Bác sĩ cấp lại cho 1 lịch hẹn của chính mình; lịch hẹn của bác sĩ khác coi như không có (404). */
    CapLaiLienKetResponse bacSi(Long idTaiKhoan, Long idLichHen, CapLaiLienKetRequest request);

    /** Khách tự xin: đếm giới hạn trước, chỉ gửi email khi mã có thật, SĐT khớp và lịch hẹn có email. */
    void khach(KhachCapLaiLienKetRequest request, String diaChiIp);

    /** Lịch hẹn của bác sĩ đang đăng nhập mà người khám / người giám hộ có số CCCD này (tối đa 50, mới nhất trước). */
    List<LichHenTrongCaResponse> bacSiTimTheoCccd(Long idTaiKhoan, String cccd);

}
