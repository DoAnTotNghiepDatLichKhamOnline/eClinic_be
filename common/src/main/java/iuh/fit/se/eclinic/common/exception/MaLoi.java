package iuh.fit.se.eclinic.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Mã lỗi dùng chung. HTTP status của response lấy từ đây (xem XuLyLoiHandler).
 * Thêm mã mới ở đây khi viết nghiệp vụ / API; tên mã chính là giá trị "maLoi" trả về cho frontend.
 */
@Getter
@RequiredArgsConstructor
public enum MaLoi {

    // Chung
    KHONG_TIM_THAY(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu"),
    DU_LIEU_KHONG_HOP_LE(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ"),
    CHUA_DANG_NHAP(HttpStatus.UNAUTHORIZED, "Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn"),
    KHONG_CO_QUYEN(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này"),
    PHUONG_THUC_KHONG_HO_TRO(HttpStatus.METHOD_NOT_ALLOWED, "Phương thức HTTP không được hỗ trợ"),
    XUNG_DOT_DU_LIEU(HttpStatus.CONFLICT, "Dữ liệu bị trùng hoặc vi phạm ràng buộc"),
    DU_LIEU_DA_THAY_DOI(HttpStatus.CONFLICT, "Dữ liệu đã bị người khác thay đổi, vui lòng tải lại"),
    LOI_HE_THONG(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống, vui lòng thử lại sau"),

    // Tài khoản / xác thực
    OTP_KHONG_DUNG(HttpStatus.BAD_REQUEST, "Mã OTP không đúng"),
    OTP_HET_HAN(HttpStatus.BAD_REQUEST, "Mã OTP đã hết hạn hoặc không tồn tại"),
    OTP_NHAP_SAI_QUA_NHIEU(HttpStatus.TOO_MANY_REQUESTS, "Nhập sai OTP quá nhiều lần"),

    // Danh mục
    TEN_CHUYEN_KHOA_DA_TON_TAI(HttpStatus.CONFLICT, "Tên chuyên khoa đã tồn tại"),
    CHUYEN_KHOA_DANG_DUOC_SU_DUNG(HttpStatus.CONFLICT, "Chuyên khoa đang được sử dụng, không thể xoá"),

    // Lịch làm việc / đặt lịch
    TRUNG_LICH_LAM_VIEC(HttpStatus.CONFLICT, "Lịch làm việc bị trùng giờ"),
    KHUNG_GIO_KHONG_CON_TRONG(HttpStatus.CONFLICT, "Khung giờ không còn trống");

    private final HttpStatus trangThaiHttp;
    private final String thongDiepMacDinh;

}
