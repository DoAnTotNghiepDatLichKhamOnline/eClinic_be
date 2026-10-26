package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.request.CapNhatCauHinhRequest;
import iuh.fit.se.eclinic.booking.dto.response.CauHinhQuanTriResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichSuCauHinhResponse;
import iuh.fit.se.eclinic.booking.dto.response.QuyTacDatLichResponse;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;

/**
 * Quy tắc đặt lịch / xếp ca do quản trị viên sửa (SCHED-05, quy tắc #13), lưu ở dòng duy nhất của
 * {@code cau_hinh_he_thong}. Biến môi trường (app.dat-lich, app.lich-lam-viec, app.danh-gia) chỉ dùng để ghi dòng đầu
 * tiên; sau đó DB là nguồn. Giá trị mới chỉ áp dụng cho thao tác mới: ca, lịch hẹn, đánh giá đã có không bị đổi.
 */
public interface CauHinhService {

    /**
     * Giá trị đang dùng, đọc từ bộ nhớ đệm. Bộ nhớ đệm bị xoá khi quản trị viên lưu xong và đọc lại khi quá 60 giây (DB
     * bị sửa tay, nhiều instance). Chưa có dòng cấu hình thì ghi từ biến môi trường. Gọi 1 lần ở đầu mỗi thao tác.
     */
    CauHinhHienTai hienTai();

    /** Giá trị đang lưu kèm phiên bản và người sửa gần nhất, cho form của quản trị viên. */
    CauHinhQuanTriResponse layChoQuanTri();

    /**
     * Lưu cả 10 quy tắc. Mỗi trường đổi giá trị thêm 1 dòng lịch sử; không trường nào đổi thì không ghi gì, phiên bản
     * giữ nguyên.
     * <p>
     * Ném KHONG_CO_QUYEN (tài khoản không phải quản trị viên), DU_LIEU_KHONG_HOP_LE (số lịch mỗi SĐT nhỏ hơn số lịch mỗi
     * hồ sơ), DU_LIEU_DA_THAY_DOI (409, {@code phienBan} đã cũ).
     */
    CauHinhQuanTriResponse capNhat(Long idTaiKhoanQuanTri, CapNhatCauHinhRequest request);

    /** Lịch sử sửa cấu hình, mới nhất trước. */
    TrangDuLieu<LichSuCauHinhResponse> lichSu(int trang, int kichThuoc);

    /** Quy tắc bệnh nhân cần biết khi đặt / hủy / đổi lịch. */
    QuyTacDatLichResponse quyTacCongKhai();

    /**
     * Lúc khởi động: chưa có dòng cấu hình thì ghi từ biến môi trường; có rồi thì ghi cảnh báo cho mỗi biến môi trường
     * khác giá trị đang lưu (giá trị đang lưu vẫn được dùng).
     */
    void khoiTao();
}
