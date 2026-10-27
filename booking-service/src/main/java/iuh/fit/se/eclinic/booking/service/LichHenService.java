package iuh.fit.se.eclinic.booking.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import iuh.fit.se.eclinic.booking.dto.request.LocLichHen;
import iuh.fit.se.eclinic.booking.dto.request.PhamViLichHen;
import iuh.fit.se.eclinic.booking.dto.response.LichHenChiTietCuaToiResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenCuaToiResponse;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;

public interface LichHenService {

    LichHen layTheoId(Long id);

    List<LichHen> timTheoHoSoBenhNhan(Long hoSoBenhNhanId);

    List<LichHen> timTheoBacSiVaTrangThai(Long bacSiId, TrangThaiLichHen trangThai);

    /** Lịch hẹn tài khoản được xem: tự đặt, của hồ sơ đã liên kết, hoặc mình là người giám hộ. */
    TrangDuLieu<LichHenCuaToiResponse> lichHenCuaToi(Long idTaiKhoan, LocLichHen loc, PhamViLichHen phamVi, int trang,
            int kichThuoc);

    /** Lịch hẹn tài khoản được xem trong khoảng ngày, theo giờ khám, cho màn hình lịch của bệnh nhân. */
    List<LichHenCuaToiResponse> lichCuaToiTrongKhoang(Long idTaiKhoan, LocalDate tuNgay, LocalDate denNgay,
            PhamViLichHen phamVi);

    /** Chi tiết 1 lịch hẹn theo mã phiếu khám, chỉ với lịch tài khoản được xem (như {@link #lichHenCuaToi}). */
    LichHenChiTietCuaToiResponse chiTietCuaToi(Long idTaiKhoan, String maPhieuKham);

    /** Tìm 1 lịch hẹn theo mã in trên phiếu khám, kèm mọi thứ danh sách của bác sĩ / quản trị viên hiển thị. */
    Optional<LichHen> timTheoMa(String ma);

    /** Phòng khám đã đối chiếu giấy tờ của bệnh nhân: bỏ đánh dấu "cần đối chiếu" của lịch hẹn. */
    void danhDauDaDoiChieu(Long idLichHen);

}
