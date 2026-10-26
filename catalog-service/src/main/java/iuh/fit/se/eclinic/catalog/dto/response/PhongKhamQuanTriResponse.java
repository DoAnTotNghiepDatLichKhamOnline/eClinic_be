package iuh.fit.se.eclinic.catalog.dto.response;

import iuh.fit.se.eclinic.common.enums.TrangThaiPhongKham;

/** 1 phòng khám trong danh mục của quản trị viên, kể cả phòng đã ngừng hoạt động. */
public record PhongKhamQuanTriResponse(
        Long id,
        String tenPhong,
        String tenPhongEn,
        String tang,
        String tangEn,
        Long idChuyenKhoa,
        String tenChuyenKhoa,
        TrangThaiPhongKham trangThai,
        long soCaSapToi) {
}
