package iuh.fit.se.eclinic.booking.dto.request;

import org.hibernate.validator.constraints.Range;

import jakarta.validation.constraints.NotNull;

/** Quản trị viên lưu quy tắc đặt lịch / xếp ca. */
public record CapNhatCauHinhRequest(
        @NotNull @Range(min = 0, max = 1440, message = "{kiemTra.cauHinh.datTruocToiThieuPhut}")
        Integer datTruocToiThieuPhut,

        @NotNull @Range(min = 1, max = 180, message = "{kiemTra.cauHinh.soNgayDatTruocToiDa}")
        Integer soNgayDatTruocToiDa,

        @NotNull @Range(min = 1, max = 20, message = "{kiemTra.cauHinh.soLichHieuLucToiDaMoiHoSo}")
        Integer soLichHieuLucToiDaMoiHoSo,

        @NotNull @Range(min = 1, max = 50, message = "{kiemTra.cauHinh.soLichHieuLucToiDaMoiSoDienThoai}")
        Integer soLichHieuLucToiDaMoiSoDienThoai,

        @NotNull @Range(min = 0, max = 10080, message = "{kiemTra.cauHinh.huyDoiTruocToiThieuPhut}")
        Integer huyDoiTruocToiThieuPhut,

        @NotNull @Range(min = 0, max = 10, message = "{kiemTra.cauHinh.soLanDoiLichToiDa}")
        Integer soLanDoiLichToiDa,

        @NotNull @Range(min = 0, max = 168, message = "{kiemTra.cauHinh.guiYeuCauTruocToiThieuGio}")
        Integer guiYeuCauTruocToiThieuGio,

        @NotNull @Range(min = 7, max = 365, message = "{kiemTra.cauHinh.soNgayXepTruocToiDa}")
        Integer soNgayXepTruocToiDa,

        @NotNull @Range(min = 1, max = 168, message = "{kiemTra.cauHinh.soGioToiDaMoiTuan}")
        Integer soGioToiDaMoiTuan,

        @NotNull @Range(min = 0, max = 60, message = "{kiemTra.cauHinh.soNgayDuocSuaDanhGia}")
        Integer soNgayDuocSuaDanhGia,

        @NotNull(message = "{kiemTra.cauHinh.phienBan.batBuoc}")
        Integer phienBan) {
}
