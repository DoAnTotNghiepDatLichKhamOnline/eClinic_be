package iuh.fit.se.eclinic.common.enums;

/** Thay đổi ca làm việc đến từ đâu (lich_su_ca_lam_viec.nguon). */
public enum NguonThayDoiCa {
    /** Quản trị viên xếp / sửa / hủy trực tiếp. */
    QUAN_TRI,
    /** Quản trị viên duyệt yêu cầu đổi ca / xin nghỉ của bác sĩ (dòng lịch sử mang id yêu cầu). */
    DUYET_YEU_CAU,
    /** Hủy hàng loạt khi quản trị viên cho bác sĩ ngừng công tác. */
    NGUNG_CONG_TAC,
    /** Quản trị viên sao chép các ca của 1 khoảng ngày sang khoảng khác. */
    SAO_CHEP,
    /** Quản trị viên nhập các ca từ tệp Excel (.xlsx). */
    NHAP_EXCEL,
    /** Hủy hàng loạt khi quản trị viên khai báo ngày bệnh viện nghỉ. */
    NGAY_NGHI,
    /** Dữ liệu mẫu (SEED_DATA) của identity-service. */
    DU_LIEU_MAU,
    /** Ca có trước khi có bảng lịch sử: ảnh ca là giá trị lúc chạy migration. */
    DU_LIEU_CU
}
