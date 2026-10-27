package iuh.fit.se.eclinic.gateway.exception;

/** Body JSON lỗi của gateway, cùng các khoá với PhanHoiApi (gateway không phụ thuộc module common). */
public record PhanHoiLoiGateway(boolean thanhCong, String maLoi, String thongDiep) {

    public static PhanHoiLoiGateway loi(String maLoi, String thongDiep) {
        return new PhanHoiLoiGateway(false, maLoi, thongDiep);
    }

}
