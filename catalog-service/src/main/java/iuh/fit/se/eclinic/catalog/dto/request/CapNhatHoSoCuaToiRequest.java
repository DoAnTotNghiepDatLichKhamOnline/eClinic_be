package iuh.fit.se.eclinic.catalog.dto.request;

import static iuh.fit.se.eclinic.catalog.dto.request.GioiHanHoSoBacSi.DO_DAI_Y;
import static iuh.fit.se.eclinic.catalog.dto.request.GioiHanHoSoBacSi.MA_NGON_NGU;
import static iuh.fit.se.eclinic.catalog.dto.request.GioiHanHoSoBacSi.MA_NGON_NGU_KHONG_HOP_LE;
import static iuh.fit.se.eclinic.catalog.dto.request.GioiHanHoSoBacSi.MOI_Y_KHONG_TRONG;
import static iuh.fit.se.eclinic.catalog.dto.request.GioiHanHoSoBacSi.MOI_Y_MOT_DONG;
import static iuh.fit.se.eclinic.catalog.dto.request.GioiHanHoSoBacSi.MOI_Y_TOI_DA;
import static iuh.fit.se.eclinic.catalog.dto.request.GioiHanHoSoBacSi.MOT_DONG;
import static iuh.fit.se.eclinic.catalog.dto.request.GioiHanHoSoBacSi.SO_NGON_NGU_TOI_DA;
import static iuh.fit.se.eclinic.catalog.dto.request.GioiHanHoSoBacSi.SO_Y_TOI_DA;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Phần hồ sơ giới thiệu bác sĩ tự sửa, cả bản tiếng Anh. */
public record CapNhatHoSoCuaToiRequest(

        @NotNull(message = "{kiemTra.phienBan.batBuoc}")
        Integer phienBan,

        @Size(max = 300, message = "{kiemTra.gioiThieuNgan.toiDa}")
        String gioiThieuNgan,

        @Size(max = 300, message = "{kiemTra.gioiThieuNganEn.toiDa}")
        String gioiThieuNganEn,

        @Size(max = 5000, message = "{kiemTra.tieuSu.toiDa}")
        String tieuSu,

        @Size(max = 5000, message = "{kiemTra.tieuSuEn.toiDa}")
        String tieuSuEn,

        @Size(max = SO_NGON_NGU_TOI_DA, message = "{kiemTra.ngonNguKham.toiDa}")
        List<@NotBlank(message = MA_NGON_NGU_KHONG_HOP_LE) @Pattern(regexp = MA_NGON_NGU, message = MA_NGON_NGU_KHONG_HOP_LE) String> ngonNguKham,

        @Size(max = SO_Y_TOI_DA, message = "{kiemTra.giaiThuong.toiDa}")
        List<@NotBlank(message = MOI_Y_KHONG_TRONG) @Size(max = DO_DAI_Y, message = MOI_Y_TOI_DA) @Pattern(regexp = MOT_DONG, message = MOI_Y_MOT_DONG) String> giaiThuong,

        @Size(max = SO_Y_TOI_DA, message = "{kiemTra.giaiThuongEn.toiDa}")
        List<@NotBlank(message = MOI_Y_KHONG_TRONG) @Size(max = DO_DAI_Y, message = MOI_Y_TOI_DA) @Pattern(regexp = MOT_DONG, message = MOI_Y_MOT_DONG) String> giaiThuongEn,

        @Size(max = SO_Y_TOI_DA, message = "{kiemTra.congTrinhNghienCuu.toiDa}")
        List<@NotBlank(message = MOI_Y_KHONG_TRONG) @Size(max = DO_DAI_Y, message = MOI_Y_TOI_DA) @Pattern(regexp = MOT_DONG, message = MOI_Y_MOT_DONG) String> congTrinhNghienCuu,

        @Size(max = SO_Y_TOI_DA, message = "{kiemTra.congTrinhNghienCuuEn.toiDa}")
        List<@NotBlank(message = MOI_Y_KHONG_TRONG) @Size(max = DO_DAI_Y, message = MOI_Y_TOI_DA) @Pattern(regexp = MOT_DONG, message = MOI_Y_MOT_DONG) String> congTrinhNghienCuuEn) {
}
