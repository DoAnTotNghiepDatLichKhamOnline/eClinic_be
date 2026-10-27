package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import iuh.fit.se.eclinic.common.enums.GioiTinh;
import iuh.fit.se.eclinic.common.enums.KenhDatLich;
import iuh.fit.se.eclinic.common.enums.QuanHeGiamHo;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;

/** 1 lịch hẹn trong danh sách của bác sĩ (lịch hẹn trong ngày) hoặc của quản trị viên (lịch hẹn của 1 ca). */
public record LichHenTrongCaResponse(
        Long id,
        String maTraCuu,
        Long idLichLamViec,
        TrangThaiLichHen trangThai,
        Integer soThuTu,
        LocalDateTime gioKhamDuKien,
        LocalDateTime gioBatDauKhung,
        LocalDateTime gioKetThucKhung,
        BacSiTomTatResponse bacSi,
        PhongKhamTomTatResponse phongKham,
        String lyDoKham,
        LocalDateTime ngayDat,
        Long idHoSoBenhNhan,
        BenhNhan benhNhan,
        NguoiGiamHo nguoiGiamHo,
        DoiChieu doiChieu,
        String lyDoHuy,
        boolean canDoiLich,
        KenhDatLich kenhDat,
        ChuyenKhoa chuyenKhoa) {

    /** @param ten tên theo ngôn ngữ của request */
    public record ChuyenKhoa(Long id, String ten) {
    }

    public record BenhNhan(String hoTen, LocalDate ngaySinh, Integer tuoi, GioiTinh gioiTinh, String soDienThoai) {
    }

    public record NguoiGiamHo(String hoTen, QuanHeGiamHo quanHe, String soDienThoai) {
    }

    public record DoiChieu(boolean canDoiChieu, String hoTenDaNhap, LocalDate ngaySinhDaNhap, GioiTinh gioiTinhDaNhap,
            String hoTenGiamHoDaNhap) {
    }
}
