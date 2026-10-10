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

    /**
     * BOOK-07: lịch hẹn tài khoản được xem: lịch tài khoản đặt khi đã đăng nhập, lịch của hồ sơ bệnh nhân đã liên kết
     * của tài khoản (bất kể ai đặt) và lịch mà chủ tài khoản là người giám hộ. {@code phamVi} tách "của tôi" (người khám
     * là chủ tài khoản) và "của người khác". SAP_TOI xếp gần nhất trước, TAT_CA và LICH_SU xếp giờ khám muộn nhất trước.
     * <p>
     * Ném các mã của {@link TaiKhoanService#layBenhNhanDangHoatDong}.
     */
    TrangDuLieu<LichHenCuaToiResponse> lichHenCuaToi(Long idTaiKhoan, LocLichHen loc, PhamViLichHen phamVi, int trang,
            int kichThuoc);

    /**
     * Lịch hẹn (mọi trạng thái) tài khoản được xem có ngày khám trong [tuNgay, denNgay], theo giờ khám, không phân
     * trang: cho màn hình lịch của bệnh nhân. Ném DU_LIEU_KHONG_HOP_LE nếu denNgay trước tuNgay hoặc khoảng dài hơn 42
     * ngày, và các mã của {@link TaiKhoanService#layBenhNhanDangHoatDong}.
     */
    List<LichHenCuaToiResponse> lichCuaToiTrongKhoang(Long idTaiKhoan, LocalDate tuNgay, LocalDate denNgay,
            PhamViLichHen phamVi);

    /**
     * Chi tiết 1 lịch hẹn theo mã phiếu khám, chỉ với lịch tài khoản được xem (như {@link #lichHenCuaToi}). Ném
     * KHONG_TIM_THAY nếu mã sai hoặc lịch không thuộc phạm vi của tài khoản, và các mã của
     * {@link TaiKhoanService#layBenhNhanDangHoatDong}.
     */
    LichHenChiTietCuaToiResponse chiTietCuaToi(Long idTaiKhoan, String maPhieuKham);

    /**
     * Tìm 1 lịch hẹn theo mã in trên phiếu khám, kèm mọi thứ danh sách của bác sĩ / quản trị viên hiển thị. {@code ma}
     * là mã tra cứu ngắn (ECL-..., không phân biệt hoa thường), mã phiếu khám (khớp từng ký tự), hoặc cả link phiếu
     * khám mà máy quét QR trả về (lấy đoạn cuối của đường dẫn). Không đúng dạng nào thì trả rỗng.
     */
    Optional<LichHen> timTheoMa(String ma);

    /**
     * Phòng khám đã đối chiếu giấy tờ của bệnh nhân: bỏ đánh dấu "cần đối chiếu" của lịch hẹn. Lịch hẹn không có đánh
     * dấu thì không đổi gì. Ném KHONG_TIM_THAY nếu không có lịch hẹn.
     */
    void danhDauDaDoiChieu(Long idLichHen);

}
