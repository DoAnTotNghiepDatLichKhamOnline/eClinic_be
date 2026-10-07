package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.request.GuiYeuCauDoiLichRequest;
import iuh.fit.se.eclinic.booking.dto.response.YeuCauDoiLichResponse;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.enums.TrangThaiYeuCau;

/**
 * SCHED-02, SCHED-03: bác sĩ xin đổi ca / xin nghỉ 1 ca của mình, quản trị viên duyệt hoặc từ chối.
 * <p>
 * Vòng đời: CHO_DUYET -> DA_DUYET / TU_CHOI (quản trị viên) hoặc DA_RUT (bác sĩ tự rút). Mỗi ca tối đa 1 yêu cầu
 * CHO_DUYET (UNIQUE trong DB).
 */
public interface YeuCauDoiLichService {

    /**
     * Bác sĩ gửi yêu cầu cho 1 ca còn hoạt động của mình, khi ca còn cách giờ bắt đầu ít nhất
     * {@code app.lich-lam-viec.gui-yeu-cau-truoc-toi-thieu}. Ném KHONG_TIM_THAY (ca không có / của bác sĩ khác),
     * CA_KHONG_SUA_DUOC, QUA_HAN_GUI_YEU_CAU, CA_DA_CO_YEU_CAU_CHO_DUYET, DU_LIEU_KHONG_HOP_LE.
     */
    YeuCauDoiLichResponse gui(Long idTaiKhoanBacSi, GuiYeuCauDoiLichRequest request);

    /** Yêu cầu của bác sĩ đang đăng nhập, mới gửi nhất trước; {@code trangThai} null = mọi trạng thái. */
    TrangDuLieu<YeuCauDoiLichResponse> cuaToi(Long idTaiKhoanBacSi, TrangThaiYeuCau trangThai, int trang,
            int kichThuoc);

    /** Bác sĩ rút yêu cầu còn CHO_DUYET của mình. Ném KHONG_TIM_THAY, YEU_CAU_DA_XU_LY. */
    YeuCauDoiLichResponse rut(Long idTaiKhoanBacSi, Long idYeuCau);

    /** Yêu cầu của mọi bác sĩ cho quản trị viên, gửi sớm nhất trước; {@code trangThai} null = mọi trạng thái. */
    TrangDuLieu<YeuCauDoiLichResponse> danhSach(TrangThaiYeuCau trangThai, int trang, int kichThuoc);

    /**
     * Duyệt: XIN_NGHI thì hủy ca; DOI_CA chỉ khác phòng thì đổi phòng của ca, còn lại thì hủy ca cũ và xếp ca mới theo
     * ngày / giờ / phòng mong muốn (giữ sức chứa của ca cũ). Lịch hẹn của ca bị hủy được đánh dấu cần đổi lịch. Ca mới
     * trùng giờ thì ném TRUNG_LICH_LAM_VIEC và không có gì thay đổi. Ném YEU_CAU_DA_XU_LY, CA_KHONG_SUA_DUOC.
     */
    YeuCauDoiLichResponse duyet(Long idTaiKhoanQuanTri, Long idYeuCau, String ghiChu);

    /** Từ chối, bắt buộc có ghi chú. Ném YEU_CAU_DA_XU_LY. */
    YeuCauDoiLichResponse tuChoi(Long idTaiKhoanQuanTri, Long idYeuCau, String ghiChu);

}
