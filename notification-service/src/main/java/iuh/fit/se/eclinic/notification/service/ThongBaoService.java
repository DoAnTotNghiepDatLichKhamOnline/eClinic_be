package iuh.fit.se.eclinic.notification.service;

import java.util.List;

import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.notification.dto.request.TaoThongBaoRequest;
import iuh.fit.se.eclinic.notification.dto.response.DanhDauDaDocResponse;
import iuh.fit.se.eclinic.notification.dto.response.KetQuaNhanThongBaoResponse;
import iuh.fit.se.eclinic.notification.dto.response.SoChuaDocResponse;
import iuh.fit.se.eclinic.notification.dto.response.ThongBaoResponse;

/**
 * NOTI-01: thông báo trong ứng dụng. Mỗi tài khoản chỉ đọc và đánh dấu thông báo của chính mình; thông báo của tài khoản
 * khác coi như không có.
 */
public interface ThongBaoService {

    /** Thông báo của tài khoản, mới nhất trước. */
    TrangDuLieu<ThongBaoResponse> danhSach(Long idTaiKhoan, VaiTro vaiTro, boolean chiChuaDoc, int trang,
            int kichThuoc);

    /** Số thông báo chưa đọc (số trên biểu tượng chuông). */
    SoChuaDocResponse demChuaDoc(Long idTaiKhoan);

    /** Đánh dấu 1 thông báo đã đọc; đã đọc rồi thì giữ nguyên. */
    ThongBaoResponse danhDauDaDoc(Long idTaiKhoan, VaiTro vaiTro, Long id);

    DanhDauDaDocResponse danhDauDaDocTatCa(Long idTaiKhoan);

    /**
     * Nhận thông báo do service khác gửi sang. Gửi lại cùng {@code maNguon} không tạo thông báo thứ hai; tài khoản nhận
     * không còn tồn tại thì bỏ qua dòng đó (không báo lỗi, để service nguồn không gửi lại mãi).
     */
    KetQuaNhanThongBaoResponse nhan(List<TaoThongBaoRequest> danhSach);

}
