package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import iuh.fit.se.eclinic.common.enums.LoaiNguoiThucHien;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.enums.SuKienLichHen;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;

/** Chi tiết 1 lịch hẹn cho quản trị viên. */
public record LichHenQuanTriChiTietResponse(
        LichHenTrongCaResponse lichHen,
        String emailLienHe,
        TaiKhoanDat taiKhoanDat,
        NgonNgu ngonNgu,
        boolean quanTriHuyDoiDuoc,
        List<MatXich> chuoiDoiLich,
        List<SuKien> lichSu) {

    public record TaiKhoanDat(Long id, String hoTen, String email) {
    }

    /** @param laLichNay true ở lịch hẹn đang xem */
    public record MatXich(Long id, String maTraCuu, TrangThaiLichHen trangThai, LocalDateTime gioKhamDuKien,
            boolean laLichNay) {
    }

    public record SuKien(LocalDateTime thoiDiem, String maTraCuu, SuKienLichHen suKien, NguoiLam nguoiThucHien,
            String lyDo, String maTraCuuLienQuan) {
    }

    /** @param idTaiKhoan null khi {@code loai} = KHACH */
    public record NguoiLam(LoaiNguoiThucHien loai, Long idTaiKhoan, String hoTen) {
    }
}
