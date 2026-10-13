package iuh.fit.se.eclinic.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu thêm / sửa phòng khám.
 *
 * @param tang       vd "Tầng 2"; có thể để trống
 * @param tenPhongEn tên tiếng Anh, vd "Room 203"; bỏ trống thì người xem tiếng Anh thấy tên tiếng Việt
 * @param tangEn     vd "Floor 2"
 */
public record PhongKhamRequest(
        @NotBlank(message = "{kiemTra.tenPhong.batBuoc}")
        @Size(max = 100, message = "{kiemTra.tenPhong.toiDa}")
        String tenPhong,

        @Size(max = 100, message = "{kiemTra.tenPhongEn.toiDa}")
        String tenPhongEn,
        @Size(max = 20, message = "{kiemTra.tang.toiDa}")
        String tang,

        @Size(max = 20, message = "{kiemTra.tangEn.toiDa}")
        String tangEn,
        @NotNull(message = "{kiemTra.chuyenKhoa.chuaChon}")
        Long idChuyenKhoa) {
}
