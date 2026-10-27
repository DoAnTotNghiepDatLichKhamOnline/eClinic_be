package iuh.fit.se.eclinic.booking.service;

import java.util.List;

import iuh.fit.se.eclinic.booking.dto.request.GuiYeuCauDoiLichRequest;
import iuh.fit.se.eclinic.booking.dto.request.XinNghiNhieuCaRequest;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaXuLyNhomResponse;
import iuh.fit.se.eclinic.booking.dto.response.YeuCauDoiLichResponse;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.enums.TrangThaiYeuCau;

/** Bác sĩ xin đổi ca, xin nghỉ, nhường ca; quản trị viên duyệt hoặc từ chối. */
public interface YeuCauDoiLichService {

    /** Bác sĩ gửi yêu cầu cho 1 ca của mình khi ca còn đủ xa giờ bắt đầu. */
    YeuCauDoiLichResponse gui(Long idTaiKhoanBacSi, GuiYeuCauDoiLichRequest request);

    /** Xin nghỉ nhiều ca 1 lần: 1 ca không gửi được thì không lưu gì. */
    List<YeuCauDoiLichResponse> xinNghiNhieuCa(Long idTaiKhoanBacSi, XinNghiNhieuCaRequest request);

    /** Yêu cầu của bác sĩ đang đăng nhập, mới gửi nhất trước; {@code trangThai} null = mọi trạng thái. */
    TrangDuLieu<YeuCauDoiLichResponse> cuaToi(Long idTaiKhoanBacSi, TrangThaiYeuCau trangThai, int trang,
            int kichThuoc);

    /** Bác sĩ rút yêu cầu còn chờ của mình. Ném KHONG_TIM_THAY, YEU_CAU_DA_XU_LY. */
    YeuCauDoiLichResponse rut(Long idTaiKhoanBacSi, Long idYeuCau);

    /** Bác sĩ rút mọi yêu cầu còn chờ của 1 nhóm xin nghỉ nhiều ca của mình. */
    List<YeuCauDoiLichResponse> rutNhom(Long idTaiKhoanBacSi, String maNhom);

    // ----- Bác sĩ nhận ca của yêu cầu nhường ca -----

    /** Yêu cầu nhường ca gửi cho bác sĩ đang đăng nhập, mới gửi nhất trước; {@code trangThai} null = mọi trạng thái. */
    TrangDuLieu<YeuCauDoiLichResponse> guiChoToi(Long idTaiKhoanBacSi, TrangThaiYeuCau trangThai, int trang,
            int kichThuoc);

    /** Bác sĩ nhận đồng ý nhận ca: yêu cầu thành CHO_DUYET, quản trị viên và bác sĩ gửi được báo. */
    YeuCauDoiLichResponse dongY(Long idTaiKhoanBacSi, Long idYeuCau);

    /** Bác sĩ nhận từ chối: DONG_NGHIEP_TU_CHOI, bác sĩ gửi được báo. */
    YeuCauDoiLichResponse tuChoiNhan(Long idTaiKhoanBacSi, Long idYeuCau, String ghiChu);

    // ----- Quản trị viên -----

    /** Yêu cầu của mọi bác sĩ cho quản trị viên, gửi sớm nhất trước; {@code trangThai} null = mọi trạng thái. */
    TrangDuLieu<YeuCauDoiLichResponse> danhSach(TrangThaiYeuCau trangThai, int trang, int kichThuoc);

    /** Duyệt: XIN_NGHI hủy ca, DOI_CA đổi phòng hoặc xếp ca mới, NHUONG_CA chuyển ca sang bác sĩ nhận. */
    YeuCauDoiLichResponse duyet(Long idTaiKhoanQuanTri, Long idYeuCau, String ghiChu);

    /** Từ chối, bắt buộc có ghi chú. Ném YEU_CAU_DA_XU_LY, YEU_CAU_CHO_DONG_NGHIEP. */
    YeuCauDoiLichResponse tuChoi(Long idTaiKhoanQuanTri, Long idYeuCau, String ghiChu);

    /** Duyệt mọi yêu cầu còn chờ duyệt của 1 nhóm xin nghỉ nhiều ca (hủy từng ca). */
    KetQuaXuLyNhomResponse duyetNhom(Long idTaiKhoanQuanTri, String maNhom, String ghiChu);

    /** Từ chối mọi yêu cầu còn chờ duyệt của 1 nhóm, bắt buộc có ghi chú; bác sĩ được báo 1 lần. */
    KetQuaXuLyNhomResponse tuChoiNhom(Long idTaiKhoanQuanTri, String maNhom, String ghiChu);

}
