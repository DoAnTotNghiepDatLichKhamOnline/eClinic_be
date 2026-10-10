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
            "Chức năng tải ảnh lên tạm thời không khả dụng, vui lòng thử lại sau"),
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
    VUOT_SO_ANH_BAC_SI(HttpStatus.CONFLICT, "Bác sĩ đã có đủ số ảnh giới thiệu tối đa, hãy xoá bớt ảnh trước khi thêm"),

    // Lịch làm việc / đặt lịch
    TRUNG_LICH_LAM_VIEC(HttpStatus.CONFLICT, "Lịch làm việc bị trùng giờ"),
    /** Khung 1 giờ đã đủ số lượt (BOOK-04, BOOK-12). */
    KHUNG_GIO_KHONG_CON_TRONG(HttpStatus.CONFLICT, "Khung giờ đã đủ số lượt khám, vui lòng chọn khung giờ khác"),
    /** Ca bị hủy, bác sĩ ngừng làm việc, khung giờ đã qua hạn đặt hoặc nằm ngoài số ngày được đặt trước. */
    KHUNG_GIO_KHONG_KHA_DUNG(HttpStatus.CONFLICT, "Khung giờ này không còn nhận đặt lịch, vui lòng chọn khung giờ khác"),
    LICH_HEN_TRUNG_GIO(HttpStatus.CONFLICT, "Bệnh nhân đã có lịch hẹn còn hiệu lực trong khung giờ này"),
    /**
     * Đặt cho bản thân với số CCCD khác hồ sơ bệnh nhân của tài khoản. Họ tên / ngày sinh nhập khác hồ sơ của 1 số CCCD
     * KHÔNG còn bị từ chối: lịch hẹn được đánh dấu cần đối chiếu.
     */
    THONG_TIN_BENH_NHAN_KHONG_KHOP(HttpStatus.CONFLICT,
            "Số CCCD không trùng với hồ sơ bệnh nhân của tài khoản"),
    THIEU_NGUOI_GIAM_HO(HttpStatus.BAD_REQUEST, "Bệnh nhân dưới 18 tuổi phải có thông tin người giám hộ"),
    NGUOI_GIAM_HO_KHONG_HOP_LE(HttpStatus.BAD_REQUEST, "Thông tin người giám hộ không hợp lệ"),
    VUOT_GIOI_HAN_DAT_LICH(HttpStatus.CONFLICT, "Đã đạt số lịch hẹn tối đa có thể đặt cùng lúc"),

    // Bác sĩ xác nhận / từ chối lịch hẹn
    /** Lịch hẹn đã được xác nhận, bị từ chối, đã khám xong hoặc đã hủy. */
    LICH_HEN_KHONG_CHO_XAC_NHAN(HttpStatus.CONFLICT, "Lịch hẹn không còn ở trạng thái chờ xác nhận"),
    /** Lượt khám của lịch hẹn đã bắt đầu: không xác nhận / từ chối được nữa. */
    LICH_HEN_DA_QUA_GIO(HttpStatus.CONFLICT, "Đã qua giờ khám của lịch hẹn này"),

    // Bệnh nhân hủy / đổi lịch hẹn
    /** Lịch hẹn đã khám xong, đã hủy, đã đổi sang lịch khác hoặc bị từ chối. */
    LICH_HEN_KHONG_HUY_DOI_DUOC(HttpStatus.CONFLICT, "Lịch hẹn đã khám xong, đã hủy hoặc bị từ chối nên không hủy / đổi được"),
    /** Còn cách giờ khám ít hơn app.dat-lich.huy-doi-truoc-toi-thieu. */
    QUA_HAN_HUY_DOI_LICH(HttpStatus.CONFLICT,
            "Đã quá hạn hủy / đổi lịch hẹn này trên hệ thống, vui lòng liên hệ phòng khám"),
    VUOT_SO_LAN_DOI_LICH(HttpStatus.CONFLICT, "Lịch hẹn này đã đổi đủ số lần cho phép"),
    /** Hủy / đổi bằng link phiếu khám: SĐT nhập vào không phải SĐT liên hệ của lượt khám. */
    SO_DIEN_THOAI_KHONG_KHOP(HttpStatus.FORBIDDEN, "Số điện thoại không khớp với số đã dùng khi đặt lịch"),

    // Hồ sơ bệnh nhân của tài khoản
    /** Tài khoản chưa có hồ sơ mà số CCCD nhập vào đã có hồ sơ: gắn hồ sơ đã có vào tài khoản phải qua xác minh. */
    CCCD_DA_CO_HO_SO(HttpStatus.CONFLICT,
            "Số CCCD này đã có hồ sơ bệnh nhân, chưa thể gắn vào tài khoản của bạn; vui lòng liên hệ phòng khám để xác minh"),
    HO_SO_CHO_XAC_MINH(HttpStatus.CONFLICT, "Hồ sơ bệnh nhân của tài khoản đang chờ phòng khám xác minh"),

    // Khám bệnh
    /** Bác sĩ chỉ ghi nhận kết quả khám từ ngày khám của lịch hẹn trở đi. */
    CHUA_DEN_NGAY_KHAM(HttpStatus.CONFLICT, "Chưa đến ngày khám của lịch hẹn này"),
    /** Lịch hẹn đã khám xong, đã hủy hoặc bị từ chối. */
    LICH_HEN_KHONG_KHAM_DUOC(HttpStatus.CONFLICT, "Lịch hẹn đã khám xong hoặc đã hủy, không ghi nhận kết quả khám được"),

    // Ca làm việc, yêu cầu đổi ca / xin nghỉ
    CA_KHONG_SUA_DUOC(HttpStatus.CONFLICT, "Ca làm việc đã bắt đầu hoặc đã hủy, không sửa / hủy / gửi yêu cầu được"),
    CA_CON_LICH_HEN(HttpStatus.CONFLICT,
            "Thay đổi này làm mất lượt khám đã có người đặt; hãy giữ các lượt đó hoặc hủy cả ca"),
    CA_DA_CO_YEU_CAU_CHO_DUYET(HttpStatus.CONFLICT, "Ca làm việc này đã có 1 yêu cầu đang chờ duyệt"),
    QUA_HAN_GUI_YEU_CAU(HttpStatus.CONFLICT, "Đã quá hạn gửi yêu cầu đổi ca / xin nghỉ cho ca này"),
    YEU_CAU_DA_XU_LY(HttpStatus.CONFLICT, "Yêu cầu đã được xử lý hoặc đã rút"),
    LICH_HEN_CAN_DOI_LICH(HttpStatus.CONFLICT,
            "Ca khám của lịch hẹn này đã bị hủy, đang chờ bệnh nhân đổi sang khung giờ khác"),

    // Quản lý bác sĩ
    PHAI_DOI_MAT_KHAU(HttpStatus.FORBIDDEN,
            "Tài khoản đang dùng mật khẩu mặc định, hãy đặt mật khẩu mới trước khi đăng nhập"),
    SO_GIAY_PHEP_DA_TON_TAI(HttpStatus.CONFLICT, "Số giấy phép hành nghề đã thuộc về bác sĩ khác"),
    BAC_SI_CON_CA_LAM_VIEC(HttpStatus.CONFLICT,
            "Bác sĩ còn ca làm việc sắp tới nên không đổi chuyên khoa được; hãy hủy các ca đó trước"),
    DICH_VU_NOI_BO_LOI(HttpStatus.SERVICE_UNAVAILABLE,
            "Một dịch vụ của hệ thống đang không phản hồi, thao tác chưa hoàn tất; vui lòng thử lại sau ít phút"),

    // Danh mục phòng khám, thuốc
    TEN_PHONG_DA_TON_TAI(HttpStatus.CONFLICT, "Tên phòng khám đã tồn tại"),
    PHONG_CON_CA_LAM_VIEC(HttpStatus.CONFLICT,
            "Phòng khám còn ca làm việc sắp tới; hãy chuyển các ca đó sang phòng khác trước"),
    TEN_THUOC_DA_TON_TAI(HttpStatus.CONFLICT, "Tên thuốc đã có trong danh mục"),
    THUOC_DA_DUOC_KE(HttpStatus.CONFLICT,
            "Thuốc đã có trong đơn thuốc nên không đổi sang tên khác được (chỉ sửa được cách viết hoa, khoảng trắng);"
                    + " hãy thêm thuốc mới và cho thuốc này ngừng dùng"),
    THUOC_NGUNG_DUNG(HttpStatus.CONFLICT, "Thuốc đã ngừng dùng trong danh mục, không kê mới được"),

    // Đánh giá lượt khám
    LICH_HEN_CHUA_KHAM_XONG(HttpStatus.CONFLICT, "Chỉ đánh giá được lượt khám đã hoàn thành"),
    DA_DANH_GIA(HttpStatus.CONFLICT, "Lượt khám này đã được đánh giá"),
    HET_HAN_SUA_DANH_GIA(HttpStatus.CONFLICT, "Đã quá thời hạn sửa đánh giá"),

    // Thông báo
    KHONG_TIM_THAY_THONG_BAO(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo");

    private final HttpStatus trangThaiHttp;
    private final String thongDiepMacDinh;

}
