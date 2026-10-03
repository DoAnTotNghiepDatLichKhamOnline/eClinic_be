package iuh.fit.se.eclinic.booking.service;

import java.util.List;

import iuh.fit.se.eclinic.booking.dto.request.LocLichHen;
import iuh.fit.se.eclinic.booking.dto.response.LichHenCuaToiResponse;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;

public interface LichHenService {

    LichHen layTheoId(Long id);

    List<LichHen> timTheoHoSoBenhNhan(Long hoSoBenhNhanId);

    List<LichHen> timTheoBacSiVaTrangThai(Long bacSiId, TrangThaiLichHen trangThai);

    /**
     * BOOK-07: lịch hẹn do tài khoản này đặt khi đã đăng nhập, cho bản thân hoặc người thân. Lịch đặt lúc chưa đăng nhập
     * không nằm trong đây, kể cả khi dùng đúng CCCD của chủ tài khoản (quy tắc #3). SAP_TOI xếp gần nhất trước, TAT_CA
     * và LICH_SU xếp giờ khám muộn nhất trước.
     * <p>
     * Ném các mã của {@link TaiKhoanService#layBenhNhanDangHoatDong}.
     */
    TrangDuLieu<LichHenCuaToiResponse> lichHenCuaToi(Long idTaiKhoan, LocLichHen loc, int trang, int kichThuoc);

}
