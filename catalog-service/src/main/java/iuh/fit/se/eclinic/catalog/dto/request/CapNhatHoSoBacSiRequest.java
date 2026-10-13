package iuh.fit.se.eclinic.catalog.dto.request;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Hồ sơ giới thiệu của bác sĩ do quản trị viên sửa (DOC-01). Gửi đủ mọi trường: trường bỏ trống / null bị xoá trắng.
 * Chuyên khoa, số giấy phép, trạng thái không sửa ở đây.
 * <p>
 * 3 mục danh sách: mỗi phần tử 1 ý, không chứa ký tự xuống dòng (lưu mỗi dòng 1 ý).
 * <p>
 * Các trường {@code ...En} là bản tiếng Anh, cùng giới hạn với bản tiếng Việt; bỏ trống thì người xem tiếng Anh thấy bản
 * tiếng Việt của trường đó. Học vị (chữ viết tắt) không có bản tiếng Anh.
 */
public record CapNhatHoSoBacSiRequest(

        @Size(max = 100, message = "{kiemTra.hocVi.toiDa}")
        String hocVi,

        @Size(max = 150, message = "{kiemTra.chucVu.toiDa}")
        String chucVu,

        @Size(max = 150, message = "{kiemTra.chucVuEn.toiDa}")
        String chucVuEn,

        @Min(value = 0, message = "{kiemTra.soNamKinhNghiem.khongAm}")
        @Max(value = 80, message = "{kiemTra.soNamKinhNghiem.toiDa}")
        Integer soNamKinhNghiem,

        @Size(max = 300, message = "{kiemTra.gioiThieuNgan.toiDa}")
        String gioiThieuNgan,

        @Size(max = 300, message = "{kiemTra.gioiThieuNganEn.toiDa}")
        String gioiThieuNganEn,

        @Size(max = 5000, message = "{kiemTra.tieuSu.toiDa}")
        String tieuSu,

        @Size(max = 5000, message = "{kiemTra.tieuSuEn.toiDa}")
        String tieuSuEn,

        @Size(max = SO_Y_TOI_DA, message = "{kiemTra.quaTrinhDaoTao.toiDa}")
        List<@NotBlank(message = MOI_Y_KHONG_TRONG) @Size(max = DO_DAI_Y, message = MOI_Y_TOI_DA) @Pattern(regexp = MOT_DONG, message = MOI_Y_MOT_DONG) String> quaTrinhDaoTao,

        @Size(max = SO_Y_TOI_DA, message = "{kiemTra.quaTrinhDaoTaoEn.toiDa}")
        List<@NotBlank(message = MOI_Y_KHONG_TRONG) @Size(max = DO_DAI_Y, message = MOI_Y_TOI_DA) @Pattern(regexp = MOT_DONG, message = MOI_Y_MOT_DONG) String> quaTrinhDaoTaoEn,

        @Size(max = SO_Y_TOI_DA, message = "{kiemTra.quaTrinhCongTac.toiDa}")
        List<@NotBlank(message = MOI_Y_KHONG_TRONG) @Size(max = DO_DAI_Y, message = MOI_Y_TOI_DA) @Pattern(regexp = MOT_DONG, message = MOI_Y_MOT_DONG) String> quaTrinhCongTac,

        @Size(max = SO_Y_TOI_DA, message = "{kiemTra.quaTrinhCongTacEn.toiDa}")
        List<@NotBlank(message = MOI_Y_KHONG_TRONG) @Size(max = DO_DAI_Y, message = MOI_Y_TOI_DA) @Pattern(regexp = MOT_DONG, message = MOI_Y_MOT_DONG) String> quaTrinhCongTacEn,

        @Size(max = SO_Y_TOI_DA, message = "{kiemTra.linhVucKhamChua.toiDa}")
        List<@NotBlank(message = MOI_Y_KHONG_TRONG) @Size(max = DO_DAI_Y, message = MOI_Y_TOI_DA) @Pattern(regexp = MOT_DONG, message = MOI_Y_MOT_DONG) String> linhVucKhamChua,

        @Size(max = SO_Y_TOI_DA, message = "{kiemTra.linhVucKhamChuaEn.toiDa}")
        List<@NotBlank(message = MOI_Y_KHONG_TRONG) @Size(max = DO_DAI_Y, message = MOI_Y_TOI_DA) @Pattern(regexp = MOT_DONG, message = MOI_Y_MOT_DONG) String> linhVucKhamChuaEn) {

    private static final int SO_Y_TOI_DA = 20;
    private static final int DO_DAI_Y = 300;
    private static final String MOT_DONG = "[^\r\n]*";
    private static final String MOI_Y_KHONG_TRONG = "{kiemTra.moiY.batBuoc}";
    private static final String MOI_Y_TOI_DA = "{kiemTra.moiY.toiDa}";
    private static final String MOI_Y_MOT_DONG = "{kiemTra.moiY.motDong}";
}
