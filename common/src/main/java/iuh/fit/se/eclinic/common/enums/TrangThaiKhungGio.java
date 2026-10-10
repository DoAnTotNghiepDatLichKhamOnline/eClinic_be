package iuh.fit.se.eclinic.common.enums;

/**
 * Trạng thái khung giờ khám. Mỗi dòng khung giờ là 1 lượt khám, nhận 1 bệnh nhân.
 */
public enum TrangThaiKhungGio {
    CON_TRONG,
    DA_DAT,
    /**
     * Lượt khám thuộc ca đã bị hủy, hoặc bị bỏ khi quản trị viên sửa giờ / sức chứa của ca: không đặt được nữa. Dòng
     * không bị xoá vì lịch hẹn cũ còn trỏ tới; sửa ca cho lượt đó có lại thì dòng trở về CON_TRONG.
     */
    DA_HUY
}
