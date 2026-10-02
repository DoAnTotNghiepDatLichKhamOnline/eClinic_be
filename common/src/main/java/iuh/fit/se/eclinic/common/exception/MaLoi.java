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
    TEP_QUA_LON(HttpStatus.CONTENT_TOO_LARGE, "Tệp tải lên vượt quá dung lượng cho phép"),
    LOI_HE_THONG(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống, vui lòng thử lại sau"),

    // Tài khoản / xác thực
    EMAIL_DA_TON_TAI(HttpStatus.CONFLICT, "Email đã được đăng ký"),
    SO_DIEN_THOAI_DA_TON_TAI(HttpStatus.CONFLICT, "Số điện thoại đã được đăng ký"),
    LIEN_KET_KHONG_HOP_LE(HttpStatus.GONE, "Liên kết đã hết hạn hoặc không hợp lệ"),
    GUI_LAI_QUA_NHANH(HttpStatus.TOO_MANY_REQUESTS, "Bạn thao tác quá nhanh, vui lòng thử lại sau ít phút"),
    TAI_KHOAN_BI_VO_HIEU_HOA(HttpStatus.FORBIDDEN, "Tài khoản đã bị vô hiệu hoá"),
    SAI_THONG_TIN_DANG_NHAP(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng"),
    TAI_KHOAN_CHUA_XAC_THUC(HttpStatus.FORBIDDEN, "Tài khoản chưa được kích hoạt, vui lòng kiểm tra email"),
    DANG_NHAP_SAI_QUA_NHIEU(HttpStatus.TOO_MANY_REQUESTS, "Đăng nhập sai quá nhiều lần, vui lòng thử lại sau 15 phút"),
    PHIEN_DANG_NHAP_KHONG_HOP_LE(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại"),
    GOOGLE_TOKEN_KHONG_HOP_LE(HttpStatus.UNAUTHORIZED, "Đăng nhập Google không hợp lệ hoặc đã hết hạn, vui lòng thử lại"),
    DANG_NHAP_GOOGLE_KHONG_KHA_DUNG(HttpStatus.SERVICE_UNAVAILABLE,
            "Đăng nhập Google tạm thời không khả dụng, vui lòng thử lại sau"),
    DANG_NHAP_GOOGLE_KHONG_HO_TRO(HttpStatus.FORBIDDEN, "Tài khoản này không hỗ trợ đăng nhập bằng Google"),
    EMAIL_DA_DANG_KY_MAT_KHAU(HttpStatus.CONFLICT, "Email đã được đăng ký, vui lòng đăng nhập bằng mật khẩu"),
    // Đổi mật khẩu khi đang đăng nhập: 400 chứ không 401, để frontend không hiểu nhầm là hết phiên rồi đi làm mới token
    MAT_KHAU_CU_KHONG_DUNG(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không đúng"),
    MAT_KHAU_MOI_TRUNG_MAT_KHAU_CU(HttpStatus.BAD_REQUEST, "Mật khẩu mới phải khác mật khẩu hiện tại"),
    TAI_KHOAN_CHUA_CO_MAT_KHAU(HttpStatus.CONFLICT,
            "Tài khoản đăng nhập bằng Google chưa có mật khẩu, vui lòng dùng chức năng Quên mật khẩu để đặt mật khẩu"),
    SAI_MAT_KHAU_QUA_NHIEU(HttpStatus.TOO_MANY_REQUESTS,
            "Bạn nhập sai mật khẩu hiện tại quá nhiều lần, vui lòng thử lại sau 15 phút"),
    EMAIL_MOI_TRUNG_EMAIL_CU(HttpStatus.BAD_REQUEST, "Email mới phải khác email hiện tại"),
    ANH_KHONG_HOP_LE(HttpStatus.BAD_REQUEST, "Ảnh phải là tệp JPEG, PNG hoặc WebP"),
    LUU_TRU_ANH_KHONG_KHA_DUNG(HttpStatus.SERVICE_UNAVAILABLE,
            "Chức năng ảnh đại diện tạm thời không khả dụng, vui lòng thử lại sau"),
    // Quản trị viên quản lý tài khoản
    TAI_KHOAN_QUAN_TRI_DUOC_BAO_VE(HttpStatus.FORBIDDEN,
            "Không thể vô hiệu hoá hoặc xoá tài khoản quản trị viên"),
    TRANG_THAI_TAI_KHOAN_KHONG_HOP_LE(HttpStatus.CONFLICT, "Không thể chuyển tài khoản sang trạng thái này"),
    BAC_SI_CON_LICH_HEN(HttpStatus.CONFLICT,
            "Bác sĩ còn lịch hẹn sắp tới, hãy chuyển hoặc huỷ các lịch hẹn này trước khi vô hiệu hoá tài khoản"),
    TAI_KHOAN_DANG_DUOC_SU_DUNG(HttpStatus.CONFLICT,
            "Tài khoản đang có dữ liệu liên quan nên không thể xoá, hãy vô hiệu hoá tài khoản thay cho xoá"),

    // Danh mục
    TEN_CHUYEN_KHOA_DA_TON_TAI(HttpStatus.CONFLICT, "Tên chuyên khoa đã tồn tại"),
    CHUYEN_KHOA_DANG_DUOC_SU_DUNG(HttpStatus.CONFLICT, "Chuyên khoa đang được sử dụng, không thể xoá"),

    // Lịch làm việc / đặt lịch
    TRUNG_LICH_LAM_VIEC(HttpStatus.CONFLICT, "Lịch làm việc bị trùng giờ"),
    KHUNG_GIO_KHONG_CON_TRONG(HttpStatus.CONFLICT, "Khung giờ không còn trống");

    private final HttpStatus trangThaiHttp;
    private final String thongDiepMacDinh;

}
