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
 */
public record CapNhatHoSoBacSiRequest(

        @Size(max = 100, message = "Học vị tối đa 100 ký tự")
        String hocVi,

        @Size(max = 150, message = "Chức vụ tối đa 150 ký tự")
        String chucVu,

        @Min(value = 0, message = "Số năm kinh nghiệm không được âm")
        @Max(value = 80, message = "Số năm kinh nghiệm tối đa 80")
        Integer soNamKinhNghiem,

        @Size(max = 300, message = "Giới thiệu ngắn tối đa 300 ký tự")
        String gioiThieuNgan,

        @Size(max = 5000, message = "Tiểu sử tối đa 5000 ký tự")
        String tieuSu,

        @Size(max = SO_Y_TOI_DA, message = "Quá trình đào tạo tối đa 20 ý")
        List<@NotBlank(message = MOI_Y_KHONG_TRONG) @Size(max = DO_DAI_Y, message = MOI_Y_TOI_DA) @Pattern(regexp = MOT_DONG, message = MOI_Y_MOT_DONG) String> quaTrinhDaoTao,

        @Size(max = SO_Y_TOI_DA, message = "Quá trình công tác tối đa 20 ý")
        List<@NotBlank(message = MOI_Y_KHONG_TRONG) @Size(max = DO_DAI_Y, message = MOI_Y_TOI_DA) @Pattern(regexp = MOT_DONG, message = MOI_Y_MOT_DONG) String> quaTrinhCongTac,

        @Size(max = SO_Y_TOI_DA, message = "Lĩnh vực khám chữa tối đa 20 ý")
        List<@NotBlank(message = MOI_Y_KHONG_TRONG) @Size(max = DO_DAI_Y, message = MOI_Y_TOI_DA) @Pattern(regexp = MOT_DONG, message = MOI_Y_MOT_DONG) String> linhVucKhamChua) {

    private static final int SO_Y_TOI_DA = 20;
    private static final int DO_DAI_Y = 300;
    private static final String MOT_DONG = "[^\r\n]*";
    private static final String MOI_Y_KHONG_TRONG = "Mỗi ý không được để trống";
    private static final String MOI_Y_TOI_DA = "Mỗi ý tối đa 300 ký tự";
    private static final String MOI_Y_MOT_DONG = "Mỗi ý không được chứa ký tự xuống dòng";
}
