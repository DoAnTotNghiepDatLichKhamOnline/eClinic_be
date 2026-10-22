package iuh.fit.se.eclinic.booking.service;

import java.util.List;

import iuh.fit.se.eclinic.booking.dto.request.CapLaiLienKetRequest;
import iuh.fit.se.eclinic.booking.dto.request.KhachCapLaiLienKetRequest;
import iuh.fit.se.eclinic.booking.dto.response.CapLaiLienKetResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;

/**
 * PAT-03: cấp lại liên kết phiếu khám (và sổ khám bệnh nếu lịch hẹn đã khám) cho người đã mất link. Luôn dùng lại mã
 * cũ, không tạo mã mới (link người bệnh đang giữ vẫn dùng được), và luôn cho lịch hẹn hiện hành của chuỗi đổi lịch: chọn
 * lịch cũ đã đổi thì được phiếu của lịch mới nhất. Mỗi lần cấp lại thành công ghi nhật ký lịch hẹn
 * {@code CAP_LAI_LIEN_KET} (ai làm, gửi bằng cách nào).
 * <p>
 * Nhân viên: phải nhập số CCCD trên giấy tờ người đến xuất trình (CCCD của hồ sơ bệnh nhân hoặc của người giám hộ ghi
 * trên lịch hẹn), rồi gửi email tới địa chỉ của lịch hẹn hoặc nhận mã + link để mở mã QR trên màn hình. Khách: mã tra
 * cứu + đầy đủ SĐT liên hệ, link chỉ gửi qua email, phản hồi luôn như nhau.
 */
public interface CapLaiLienKetService {

    /**
     * Quản trị viên cấp lại cho 1 lịch hẹn bất kỳ. Lỗi: KHONG_TIM_THAY; CCCD_KHONG_KHOP (403); KHONG_CO_EMAIL_NHAN (409,
     * chỉ khi gửi EMAIL).
     */
    CapLaiLienKetResponse quanTri(Long idTaiKhoan, Long idLichHen, CapLaiLienKetRequest request);

    /** Bác sĩ cấp lại cho 1 lịch hẹn của chính mình; lịch hẹn của bác sĩ khác coi như không có (404). */
    CapLaiLienKetResponse bacSi(Long idTaiKhoan, Long idLichHen, CapLaiLienKetRequest request);

    /**
     * Khách tự xin: đếm giới hạn trước (vượt thì 429), rồi chỉ khi mã có thật, SĐT khớp và lịch hẹn có email mới ghi
     * email. Mọi trường hợp khác kết thúc lặng lẽ để phản hồi không cho biết mã nào có thật.
     */
    void khach(KhachCapLaiLienKetRequest request, String diaChiIp);

    /** Lịch hẹn của bác sĩ đang đăng nhập mà người khám / người giám hộ có số CCCD này (tối đa 50, mới nhất trước). */
    List<LichHenTrongCaResponse> bacSiTimTheoCccd(Long idTaiKhoan, String cccd);

}
