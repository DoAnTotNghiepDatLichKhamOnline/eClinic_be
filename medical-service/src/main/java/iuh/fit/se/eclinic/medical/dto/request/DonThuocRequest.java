package iuh.fit.se.eclinic.medical.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 1 dòng đơn thuốc. */
public record DonThuocRequest(
        @NotNull(message = "{kiemTra.idThuoc.batBuoc}")
        Long idThuoc,

        @Size(max = 100, message = "{kiemTra.lieuDung.toiDa}")
        String lieuDung,

        @Min(value = 1, message = "{kiemTra.soLanMoiNgay.toiThieu}")
        Integer soLanMoiNgay,

        @NotNull(message = "{kiemTra.soNgayDung.batBuoc}")
        @Min(value = 1, message = "{kiemTra.soNgayDung.toiThieu}")
        @Max(value = 90, message = "{kiemTra.soNgayDung.toiDa}")
        Integer soNgayDung,

        @NotNull(message = "{kiemTra.soLuong.batBuoc}")
        @Min(value = 1, message = "{kiemTra.soLuong.khoang}")
        @Max(value = 9999, message = "{kiemTra.soLuong.khoang}")
        Integer soLuong,

        @Size(max = 255, message = "{kiemTra.ghiChuSuDung.toiDa}")
        String ghiChuSuDung) {
}
