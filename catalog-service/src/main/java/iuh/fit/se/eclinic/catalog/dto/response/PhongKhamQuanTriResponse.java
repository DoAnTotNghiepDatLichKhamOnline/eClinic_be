package iuh.fit.se.eclinic.catalog.dto.response;

import iuh.fit.se.eclinic.common.enums.TrangThaiPhongKham;

/**
 * 1 phòng khám trong danh mục của quản trị viên, kể cả phòng đã ngừng hoạt động. Tên phòng, tầng là chữ đã lưu của cả 2
 * ngôn ngữ (không đổi theo ngôn ngữ của request); tên chuyên khoa theo ngôn ngữ của request.
 *
 * @param soCaSapToi số ca làm việc còn hoạt động chưa bắt đầu đang xếp ở phòng này; còn ca thì phòng chưa ngừng hoạt
 *                   động được và chưa đổi chuyên khoa được
 */
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
