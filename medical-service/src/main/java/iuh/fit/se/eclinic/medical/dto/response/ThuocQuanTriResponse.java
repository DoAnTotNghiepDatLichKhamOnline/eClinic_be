package iuh.fit.se.eclinic.medical.dto.response;

import iuh.fit.se.eclinic.common.enums.DuongDungThuoc;
import iuh.fit.se.eclinic.common.enums.TrangThaiThuoc;

/** 1 thuốc trong danh mục của quản trị viên. */
public record ThuocQuanTriResponse(
        Long id,
        String tenThuoc,
        String hoatChat,
        String hamLuong,
        String dangBaoChe,
        String dangBaoCheEn,
        DuongDungThuoc duongDung,
        String donVi,
        String donViEn,
        String moTa,
        String moTaEn,
        boolean daXacMinh,
        TrangThaiThuoc trangThai,
        String tenBacSiTao,
        long soLanKe) {
}
