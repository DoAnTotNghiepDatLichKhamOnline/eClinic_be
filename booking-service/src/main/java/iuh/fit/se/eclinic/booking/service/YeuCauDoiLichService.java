package iuh.fit.se.eclinic.booking.service;

import java.util.List;

import iuh.fit.se.eclinic.booking.dto.request.GuiYeuCauDoiLichRequest;
import iuh.fit.se.eclinic.booking.dto.request.XinNghiNhieuCaRequest;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaXuLyNhomResponse;
import iuh.fit.se.eclinic.booking.dto.response.YeuCauDoiLichResponse;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.enums.TrangThaiYeuCau;

/**
 * SCHED-02, SCHED-03: bác sĩ xin đổi ca / xin nghỉ / nhường cho đồng nghiệp 1 ca của mình, quản trị viên duyệt hoặc từ
 * chối.
 * <p>
 * Vòng đời: CHO_DUYET -> DA_DUYET / TU_CHOI (quản trị viên) hoặc DA_RUT (bác sĩ tự rút). Nhường ca bắt đầu ở CHO_DONG_Y:
 * bác sĩ nhận đồng ý thì thành CHO_DUYET, từ chối thì thành DONG_NGHIEP_TU_CHOI. Mỗi ca tối đa 1 yêu cầu còn chờ
 * (CHO_DONG_Y / CHO_DUYET; UNIQUE trong DB). Xin nghỉ nhiều ca: mỗi ca 1 yêu cầu XIN_NGHI, chung 1 mã nhóm; xử lý được
 * từng yêu cầu hoặc cả nhóm.
 */
public interface YeuCauDoiLichService {

    /**
     * Bác sĩ gửi yêu cầu cho 1 ca còn hoạt động của mình, khi ca còn cách giờ bắt đầu ít nhất
     * {@code app.lich-lam-viec.gui-yeu-cau-truoc-toi-thieu}. Nhường ca: bác sĩ nhận phải khác mình, cùng chuyên khoa, đang
     * công tác, và (kiểm tra sớm) không trùng giờ / không vượt giờ làm trong tuần; yêu cầu chờ bác sĩ nhận đồng ý. Ném
     * KHONG_TIM_THAY (ca không có / của bác sĩ khác), CA_KHONG_SUA_DUOC, QUA_HAN_GUI_YEU_CAU, CA_DA_CO_YEU_CAU_CHO_DUYET,
     * DU_LIEU_KHONG_HOP_LE, TRUNG_LICH_LAM_VIEC, VUOT_GIO_LAM_TUAN.
     */
    YeuCauDoiLichResponse gui(Long idTaiKhoanBacSi, GuiYeuCauDoiLichRequest request);

    /**
     * Xin nghỉ nhiều ca trong 1 lần: mỗi ca phải gửi được như {@link #gui}; 1 ca không gửi được thì không lưu gì và lỗi
     * nêu ca đó. Quản trị viên được báo 1 lần.
     *
     * @return các yêu cầu vừa tạo, theo ngày giờ của ca
     */
    List<YeuCauDoiLichResponse> xinNghiNhieuCa(Long idTaiKhoanBacSi, XinNghiNhieuCaRequest request);

    /** Yêu cầu của bác sĩ đang đăng nhập, mới gửi nhất trước; {@code trangThai} null = mọi trạng thái. */
    TrangDuLieu<YeuCauDoiLichResponse> cuaToi(Long idTaiKhoanBacSi, TrangThaiYeuCau trangThai, int trang,
            int kichThuoc);

    /** Bác sĩ rút yêu cầu còn chờ của mình. Ném KHONG_TIM_THAY, YEU_CAU_DA_XU_LY. */
    YeuCauDoiLichResponse rut(Long idTaiKhoanBacSi, Long idYeuCau);

    /**
     * Bác sĩ rút mọi yêu cầu còn chờ của 1 nhóm xin nghỉ nhiều ca của mình. Ném KHONG_TIM_THAY, YEU_CAU_DA_XU_LY (không
     * còn yêu cầu nào chờ).
     */
    List<YeuCauDoiLichResponse> rutNhom(Long idTaiKhoanBacSi, String maNhom);

    // ----- Bác sĩ nhận ca của yêu cầu nhường ca -----

    /** Yêu cầu nhường ca gửi cho bác sĩ đang đăng nhập, mới gửi nhất trước; {@code trangThai} null = mọi trạng thái. */
    TrangDuLieu<YeuCauDoiLichResponse> guiChoToi(Long idTaiKhoanBacSi, TrangThaiYeuCau trangThai, int trang,
            int kichThuoc);

    /**
     * Bác sĩ nhận đồng ý nhận ca: yêu cầu thành CHO_DUYET, quản trị viên và bác sĩ gửi được báo. Kiểm tra sớm trùng giờ /
     * giờ làm trong tuần của bác sĩ nhận. Ném KHONG_TIM_THAY (không phải yêu cầu gửi cho mình), YEU_CAU_DA_XU_LY,
     * CA_KHONG_SUA_DUOC, TRUNG_LICH_LAM_VIEC, VUOT_GIO_LAM_TUAN.
     */
    YeuCauDoiLichResponse dongY(Long idTaiKhoanBacSi, Long idYeuCau);

    /** Bác sĩ nhận từ chối (ghi chú tùy chọn): DONG_NGHIEP_TU_CHOI, bác sĩ gửi được báo. Ném KHONG_TIM_THAY, YEU_CAU_DA_XU_LY. */
    YeuCauDoiLichResponse tuChoiNhan(Long idTaiKhoanBacSi, Long idYeuCau, String ghiChu);

    // ----- Quản trị viên -----

    /** Yêu cầu của mọi bác sĩ cho quản trị viên, gửi sớm nhất trước; {@code trangThai} null = mọi trạng thái. */
    TrangDuLieu<YeuCauDoiLichResponse> danhSach(TrangThaiYeuCau trangThai, int trang, int kichThuoc);

    /**
     * Duyệt: XIN_NGHI thì hủy ca; DOI_CA chỉ khác phòng thì đổi phòng của ca, còn lại thì hủy ca cũ và xếp ca mới theo
     * ngày / giờ / phòng mong muốn (giữ sức chứa của ca cũ); NHUONG_CA thì chuyển ca (cùng lịch hẹn) sang bác sĩ nhận. Lịch
     * hẹn của ca bị hủy được đánh dấu cần đổi lịch. Ca mới / bác sĩ nhận trùng giờ hoặc vượt giờ làm trong tuần thì ném
     * TRUNG_LICH_LAM_VIEC / VUOT_GIO_LAM_TUAN và không có gì thay đổi. Ném YEU_CAU_DA_XU_LY, YEU_CAU_CHO_DONG_NGHIEP,
     * CA_KHONG_SUA_DUOC.
     */
    YeuCauDoiLichResponse duyet(Long idTaiKhoanQuanTri, Long idYeuCau, String ghiChu);

    /** Từ chối, bắt buộc có ghi chú. Ném YEU_CAU_DA_XU_LY, YEU_CAU_CHO_DONG_NGHIEP. */
    YeuCauDoiLichResponse tuChoi(Long idTaiKhoanQuanTri, Long idYeuCau, String ghiChu);

    /**
     * Duyệt mọi yêu cầu còn chờ duyệt của 1 nhóm xin nghỉ nhiều ca (hủy từng ca). Ca đã bắt đầu thì bỏ qua (yêu cầu vẫn
     * chờ) và liệt kê; bác sĩ được báo 1 lần. Ném KHONG_TIM_THAY, YEU_CAU_DA_XU_LY (không còn yêu cầu nào chờ duyệt),
     * CA_KHONG_SUA_DUOC (mọi ca còn chờ đều đã bắt đầu).
     */
    KetQuaXuLyNhomResponse duyetNhom(Long idTaiKhoanQuanTri, String maNhom, String ghiChu);

    /** Từ chối mọi yêu cầu còn chờ duyệt của 1 nhóm, bắt buộc có ghi chú; bác sĩ được báo 1 lần. */
    KetQuaXuLyNhomResponse tuChoiNhom(Long idTaiKhoanQuanTri, String maNhom, String ghiChu);

}
