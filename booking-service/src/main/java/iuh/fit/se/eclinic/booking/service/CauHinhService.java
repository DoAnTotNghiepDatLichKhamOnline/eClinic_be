package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.request.CapNhatCauHinhRequest;
import iuh.fit.se.eclinic.booking.dto.response.CauHinhQuanTriResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichSuCauHinhResponse;
import iuh.fit.se.eclinic.booking.dto.response.QuyTacDatLichResponse;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;

/** Quy tắc đặt lịch và xếp ca do quản trị viên sửa, lưu ở dòng duy nhất của {@code cau_hinh_he_thong}. */
public interface CauHinhService {

    /** Giá trị đang dùng, đọc từ bộ nhớ đệm. */
    CauHinhHienTai hienTai();

    /** Giá trị đang lưu kèm phiên bản và người sửa gần nhất, cho form của quản trị viên. */
    CauHinhQuanTriResponse layChoQuanTri();

    /** Lưu cả 10 quy tắc. */
    CauHinhQuanTriResponse capNhat(Long idTaiKhoanQuanTri, CapNhatCauHinhRequest request);

    /** Lịch sử sửa cấu hình, mới nhất trước. */
    TrangDuLieu<LichSuCauHinhResponse> lichSu(int trang, int kichThuoc);

    /** Quy tắc bệnh nhân cần biết khi đặt / hủy / đổi lịch. */
    QuyTacDatLichResponse quyTacCongKhai();

    /** Lúc khởi động: chưa có cấu hình thì ghi từ biến môi trường, có rồi thì cảnh báo giá trị khác. */
    void khoiTao();
}
