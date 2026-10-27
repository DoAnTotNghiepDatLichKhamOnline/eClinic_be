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

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Hồ sơ giới thiệu của bác sĩ do quản trị viên sửa. */
public record CapNhatHoSoBacSiRequest(

        @NotNull(message = "{kiemTra.phienBan.batBuoc}")
        Integer phienBan,

        @Size(max = 100, message = "{kiemTra.hocVi.toiDa}")
        String hocVi,

        @Size(max = 150, message = "{kiemTra.chucVu.toiDa}")
        String chucVu,

        @Size(max = 150, message = "{kiemTra.chucVuEn.toiDa}")
        String chucVuEn,

        @Min(value = 1950, message = "{kiemTra.namBatDauHanhNghe.toiThieu}")
        Integer namBatDauHanhNghe,

        @PastOrPresent(message = "{kiemTra.ngayCapGiayPhep.khongTuongLai}")
        LocalDate ngayCapGiayPhep,

        @Size(max = 200, message = "{kiemTra.noiCapGiayPhep.toiDa}")
        String noiCapGiayPhep,

        @Size(max = 200, message = "{kiemTra.noiCapGiayPhepEn.toiDa}")
        String noiCapGiayPhepEn,

        @Size(max = 500, message = "{kiemTra.phamViHanhNghe.toiDa}")
        String phamViHanhNghe,

        @Size(max = 500, message = "{kiemTra.phamViHanhNgheEn.toiDa}")
        String phamViHanhNgheEn,

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
        List<@NotBlank(message = MOI_Y_KHONG_TRONG) @Size(max = DO_DAI_Y, message = MOI_Y_TOI_DA) @Pattern(regexp = MOT_DONG, message = MOI_Y_MOT_DONG) String> linhVucKhamChuaEn,

        @Size(max = SO_Y_TOI_DA, message = "{kiemTra.giaiThuong.toiDa}")
        List<@NotBlank(message = MOI_Y_KHONG_TRONG) @Size(max = DO_DAI_Y, message = MOI_Y_TOI_DA) @Pattern(regexp = MOT_DONG, message = MOI_Y_MOT_DONG) String> giaiThuong,

        @Size(max = SO_Y_TOI_DA, message = "{kiemTra.giaiThuongEn.toiDa}")
        List<@NotBlank(message = MOI_Y_KHONG_TRONG) @Size(max = DO_DAI_Y, message = MOI_Y_TOI_DA) @Pattern(regexp = MOT_DONG, message = MOI_Y_MOT_DONG) String> giaiThuongEn,

        @Size(max = SO_Y_TOI_DA, message = "{kiemTra.congTrinhNghienCuu.toiDa}")
        List<@NotBlank(message = MOI_Y_KHONG_TRONG) @Size(max = DO_DAI_Y, message = MOI_Y_TOI_DA) @Pattern(regexp = MOT_DONG, message = MOI_Y_MOT_DONG) String> congTrinhNghienCuu,

        @Size(max = SO_Y_TOI_DA, message = "{kiemTra.congTrinhNghienCuuEn.toiDa}")
        List<@NotBlank(message = MOI_Y_KHONG_TRONG) @Size(max = DO_DAI_Y, message = MOI_Y_TOI_DA) @Pattern(regexp = MOT_DONG, message = MOI_Y_MOT_DONG) String> congTrinhNghienCuuEn) {
}
