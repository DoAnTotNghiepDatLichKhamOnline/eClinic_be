package iuh.fit.se.eclinic.gateway.exception;

/**
 * Body JSON lỗi do chính gateway trả về, cùng các khoá với PhanHoiApi của các service
 * để frontend chỉ phải xử lý 1 dạng lỗi. (Gateway không phụ thuộc module common.)
 */
public record PhanHoiLoiGateway(boolean thanhCong, String maLoi, String thongDiep) {

    public static PhanHoiLoiGateway loi(String maLoi, String thongDiep) {
        return new PhanHoiLoiGateway(false, maLoi, thongDiep);
    }

}
