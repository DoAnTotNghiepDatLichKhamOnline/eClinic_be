package iuh.fit.se.eclinic.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Mã lỗi dùng chung. HTTP status của response lấy từ đây (xem XuLyLoiHandler).
 * Thêm mã mới ở đây khi viết nghiệp vụ / API; tên mã chính là giá trị "maLoi" trả về cho frontend.
 * Thông điệp của mã nằm ở thong-diep/chung.properties (tiếng Việt) và chung_en.properties (tiếng Anh), khoá loi.&lt;MA_LOI&gt;:
 * thêm mã mới thì thêm 1 dòng vào mỗi file.
 */
@Getter
@RequiredArgsConstructor
public enum MaLoi {

    // Chung
    KHONG_TIM_THAY(HttpStatus.NOT_FOUND),
    DU_LIEU_KHONG_HOP_LE(HttpStatus.BAD_REQUEST),
    CHUA_DANG_NHAP(HttpStatus.UNAUTHORIZED),
    KHONG_CO_QUYEN(HttpStatus.FORBIDDEN),
    PHUONG_THUC_KHONG_HO_TRO(HttpStatus.METHOD_NOT_ALLOWED),
    XUNG_DOT_DU_LIEU(HttpStatus.CONFLICT),
    DU_LIEU_DA_THAY_DOI(HttpStatus.CONFLICT),
    TEP_QUA_LON(HttpStatus.CONTENT_TOO_LARGE),
    LOI_HE_THONG(HttpStatus.INTERNAL_SERVER_ERROR),

    // Tài khoản / xác thực
    EMAIL_DA_TON_TAI(HttpStatus.CONFLICT),
    SO_DIEN_THOAI_DA_TON_TAI(HttpStatus.CONFLICT),
    LIEN_KET_KHONG_HOP_LE(HttpStatus.GONE),
    GUI_LAI_QUA_NHANH(HttpStatus.TOO_MANY_REQUESTS),
    TAI_KHOAN_BI_VO_HIEU_HOA(HttpStatus.FORBIDDEN),
    SAI_THONG_TIN_DANG_NHAP(HttpStatus.UNAUTHORIZED),
    TAI_KHOAN_CHUA_XAC_THUC(HttpStatus.FORBIDDEN),
    DANG_NHAP_SAI_QUA_NHIEU(HttpStatus.TOO_MANY_REQUESTS),
    PHIEN_DANG_NHAP_KHONG_HOP_LE(HttpStatus.UNAUTHORIZED),
    GOOGLE_TOKEN_KHONG_HOP_LE(HttpStatus.UNAUTHORIZED),
    DANG_NHAP_GOOGLE_KHONG_KHA_DUNG(HttpStatus.SERVICE_UNAVAILABLE),
    DANG_NHAP_GOOGLE_KHONG_HO_TRO(HttpStatus.FORBIDDEN),
    EMAIL_DA_DANG_KY_MAT_KHAU(HttpStatus.CONFLICT),
    // Đổi mật khẩu khi đang đăng nhập: 400 chứ không 401, để frontend không hiểu nhầm là hết phiên rồi đi làm mới token
    MAT_KHAU_CU_KHONG_DUNG(HttpStatus.BAD_REQUEST),
    MAT_KHAU_MOI_TRUNG_MAT_KHAU_CU(HttpStatus.BAD_REQUEST),
    TAI_KHOAN_CHUA_CO_MAT_KHAU(HttpStatus.CONFLICT),
    SAI_MAT_KHAU_QUA_NHIEU(HttpStatus.TOO_MANY_REQUESTS),
    EMAIL_MOI_TRUNG_EMAIL_CU(HttpStatus.BAD_REQUEST),
    ANH_KHONG_HOP_LE(HttpStatus.BAD_REQUEST),
    LUU_TRU_ANH_KHONG_KHA_DUNG(HttpStatus.SERVICE_UNAVAILABLE),
    // Quản trị viên quản lý tài khoản
    TAI_KHOAN_QUAN_TRI_DUOC_BAO_VE(HttpStatus.FORBIDDEN),
    TRANG_THAI_TAI_KHOAN_KHONG_HOP_LE(HttpStatus.CONFLICT),
    BAC_SI_CON_LICH_HEN(HttpStatus.CONFLICT),
    TAI_KHOAN_DANG_DUOC_SU_DUNG(HttpStatus.CONFLICT),

    // Danh mục
    TEN_CHUYEN_KHOA_DA_TON_TAI(HttpStatus.CONFLICT),
    CHUYEN_KHOA_DANG_DUOC_SU_DUNG(HttpStatus.CONFLICT),
    VUOT_SO_ANH_BAC_SI(HttpStatus.CONFLICT),

    // Lịch làm việc / đặt lịch
    TRUNG_LICH_LAM_VIEC(HttpStatus.CONFLICT),
    /** Khung 1 giờ đã đủ số lượt (BOOK-04, BOOK-12). */
    KHUNG_GIO_KHONG_CON_TRONG(HttpStatus.CONFLICT),
    /** Ca bị hủy, bác sĩ ngừng làm việc, khung giờ đã qua hạn đặt hoặc nằm ngoài số ngày được đặt trước. */
    KHUNG_GIO_KHONG_KHA_DUNG(HttpStatus.CONFLICT),
    LICH_HEN_TRUNG_GIO(HttpStatus.CONFLICT),
    /**
     * Đặt cho bản thân với số CCCD khác hồ sơ bệnh nhân của tài khoản. Họ tên / ngày sinh nhập khác hồ sơ của 1 số CCCD
     * KHÔNG còn bị từ chối: lịch hẹn được đánh dấu cần đối chiếu.
     */
    THONG_TIN_BENH_NHAN_KHONG_KHOP(HttpStatus.CONFLICT),
    THIEU_NGUOI_GIAM_HO(HttpStatus.BAD_REQUEST),
    NGUOI_GIAM_HO_KHONG_HOP_LE(HttpStatus.BAD_REQUEST),
    VUOT_GIOI_HAN_DAT_LICH(HttpStatus.CONFLICT),

    // Bác sĩ hủy lịch hẹn
    /** Lượt khám của lịch hẹn đã bắt đầu: bác sĩ không hủy được nữa. */
    LICH_HEN_DA_QUA_GIO(HttpStatus.CONFLICT),

    // Bệnh nhân hủy / đổi lịch hẹn
    /** Lịch hẹn đã khám xong, đã hủy, đã đổi sang lịch khác hoặc bác sĩ đã hủy. */
    LICH_HEN_KHONG_HUY_DOI_DUOC(HttpStatus.CONFLICT),
    /** Còn cách giờ khám ít hơn app.dat-lich.huy-doi-truoc-toi-thieu. */
    QUA_HAN_HUY_DOI_LICH(HttpStatus.CONFLICT),
    VUOT_SO_LAN_DOI_LICH(HttpStatus.CONFLICT),
    /** Hủy / đổi bằng link phiếu khám: SĐT nhập vào không phải SĐT liên hệ của lượt khám. */
    SO_DIEN_THOAI_KHONG_KHOP(HttpStatus.FORBIDDEN),

    // Hồ sơ bệnh nhân của tài khoản
    /** Tài khoản chưa có hồ sơ mà số CCCD nhập vào đã có hồ sơ: gắn hồ sơ đã có vào tài khoản phải qua xác minh. */
    CCCD_DA_CO_HO_SO(HttpStatus.CONFLICT),
    HO_SO_CHO_XAC_MINH(HttpStatus.CONFLICT),

    // Khám bệnh
    /** Bác sĩ chỉ ghi nhận kết quả khám từ ngày khám của lịch hẹn trở đi. */
    CHUA_DEN_NGAY_KHAM(HttpStatus.CONFLICT),
    HET_HAN_SUA_BENH_AN(HttpStatus.CONFLICT),
    /** Lịch hẹn đã khám xong, đã hủy hoặc bị từ chối. */
    LICH_HEN_KHONG_KHAM_DUOC(HttpStatus.CONFLICT),

    // Hàng đợi khám của bác sĩ
    /** Bác sĩ đang khám 1 bệnh nhân khác chưa ghi nhận kết quả: chỉ 1 bệnh nhân đang khám mỗi lúc. */
    DANG_KHAM_BENH_NHAN_KHAC(HttpStatus.CONFLICT),
    /** Ca không còn bệnh nhân nào chờ gọi. */
    HANG_DOI_TRONG(HttpStatus.CONFLICT),
    /** Chỉ gọi / đánh dấu vắng mặt được trong ngày khám của lịch hẹn. */
    KHONG_PHAI_NGAY_KHAM(HttpStatus.CONFLICT),

    // Ca làm việc, yêu cầu đổi ca / xin nghỉ
    CA_KHONG_SUA_DUOC(HttpStatus.CONFLICT),
    CA_CON_LICH_HEN(HttpStatus.CONFLICT),
    CA_DA_CO_YEU_CAU_CHO_DUYET(HttpStatus.CONFLICT),
    QUA_HAN_GUI_YEU_CAU(HttpStatus.CONFLICT),
    YEU_CAU_DA_XU_LY(HttpStatus.CONFLICT),
    LICH_HEN_CAN_DOI_LICH(HttpStatus.CONFLICT),

    // Quản lý bác sĩ
    PHAI_DOI_MAT_KHAU(HttpStatus.FORBIDDEN),
    SO_GIAY_PHEP_DA_TON_TAI(HttpStatus.CONFLICT),
    BAC_SI_CON_CA_LAM_VIEC(HttpStatus.CONFLICT),
    DICH_VU_NOI_BO_LOI(HttpStatus.SERVICE_UNAVAILABLE),

    // Danh mục phòng khám, thuốc
    TEN_PHONG_DA_TON_TAI(HttpStatus.CONFLICT),
    PHONG_CON_CA_LAM_VIEC(HttpStatus.CONFLICT),
    TEN_THUOC_DA_TON_TAI(HttpStatus.CONFLICT),
    THUOC_DA_DUOC_KE(HttpStatus.CONFLICT),
    THUOC_NGUNG_DUNG(HttpStatus.CONFLICT),
    THUOC_CHUA_XAC_MINH(HttpStatus.CONFLICT),
    THUOC_THIEU_THONG_TIN(HttpStatus.CONFLICT),

    // Đánh giá lượt khám
    LICH_HEN_CHUA_KHAM_XONG(HttpStatus.CONFLICT),
    DA_DANH_GIA(HttpStatus.CONFLICT),
    HET_HAN_SUA_DANH_GIA(HttpStatus.CONFLICT),

    // Thông báo
    KHONG_TIM_THAY_THONG_BAO(HttpStatus.NOT_FOUND),

    // Chatbot
    /** Vượt giới hạn số tin nhắn / số phiên chat (đếm trên Redis). */
    CHAT_QUA_NHIEU(HttpStatus.TOO_MANY_REQUESTS),
    /** Chưa đặt khoá API của mô hình ngôn ngữ, hoặc mô hình không trả lời (quá thời gian chờ, lỗi, trả lời rỗng). */
    CHATBOT_KHONG_KHA_DUNG(HttpStatus.SERVICE_UNAVAILABLE);

    private final HttpStatus trangThaiHttp;

    /** Khoá thông điệp mặc định của mã lỗi trong thong-diep/chung*.properties. */
    public String khoa() {
        return "loi." + name();
    }

}
