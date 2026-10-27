package iuh.fit.se.eclinic.medical.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Kết quả 1 lượt khám do bác sĩ ghi. */
public record BenhAnRequest(
        @Size(max = 2000, message = "{kiemTra.lyDoKham.toiDa}")
        String lyDoKham,

        @Valid
        SinhHieuRequest sinhHieu,

        @Size(max = 5000, message = "{kiemTra.khamLamSang.toiDa}")
        String khamLamSang,

        @NotBlank(message = "{kiemTra.chanDoan.batBuoc}")
        @Size(max = 5000, message = "{kiemTra.chanDoan.toiDa}")
        String chanDoan,

        @Size(max = 10, message = "{kiemTra.maIcdChinh.toiDa}")
        String maIcdChinh,

        @Size(max = 2000, message = "{kiemTra.chanDoanKemTheo.toiDa}")
        String chanDoanKemTheo,

        @Size(max = 5000, message = "{kiemTra.ghiChu.toiDa}")
        String ghiChu,

        LocalDate ngayTaiKhamDeXuat,

        @Valid
        @Size(max = 30, message = "{kiemTra.donThuoc.toiDa}")
        List<DonThuocRequest> donThuoc,

        @NotBlank(groups = BenhAnRequest.KhiSua.class, message = "{kiemTra.lyDoSua.batBuoc}")
        @Size(max = 500, groups = BenhAnRequest.KhiSua.class, message = "{kiemTra.lyDoSua.toiDa}")
        String lyDoSua,

        @NotNull(groups = BenhAnRequest.KhiSua.class, message = "{kiemTra.phienBan.batBuoc}")
        Integer phienBan) {

    /** Nhóm kiểm tra chỉ dùng khi sửa hồ sơ bệnh án (PUT). */
    public interface KhiSua {
    }
}
