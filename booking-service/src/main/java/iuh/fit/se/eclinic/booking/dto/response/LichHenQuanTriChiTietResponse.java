package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import iuh.fit.se.eclinic.common.enums.LoaiNguoiThucHien;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.enums.SuKienLichHen;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;

/**
 * ADM-04: chi tiết 1 lịch hẹn cho quản trị viên. Không có mã phiếu khám (link phiếu khám chỉ gửi cho người đặt).
 *
 * @param lichHen           dòng lịch hẹn như danh sách của quản trị viên
 * @param emailLienHe       email người đặt nhập; null nếu để trống
 * @param taiKhoanDat       tài khoản đã đặt; null = khách đặt
 * @param ngonNgu           ngôn ngữ email gửi cho khách đặt; null = lịch hẹn tạo trước khi lưu ngôn ngữ
 * @param quanTriHuyDoiDuoc quản trị viên còn hủy / đổi được lịch này không
 * @param chuoiDoiLich      các lịch hẹn của cùng 1 chuỗi đổi lịch (gồm lịch này), cũ nhất trước; 1 phần tử nếu chưa đổi
 * @param lichSu            nhật ký thao tác của cả chuỗi, cũ nhất trước; chỉ có thao tác từ khi có nhật ký (V35)
 */
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

    /**
     * @param maTraCuu         lịch hẹn của thao tác (trong chuỗi)
     * @param lyDo             theo ngôn ngữ của request khi là câu của hệ thống; câu người dùng gõ giữ nguyên
     * @param maTraCuuLienQuan DOI_LICH: lịch hẹn mới; null ở các thao tác khác
     */
    public record SuKien(LocalDateTime thoiDiem, String maTraCuu, SuKienLichHen suKien, NguoiLam nguoiThucHien,
            String lyDo, String maTraCuuLienQuan) {
    }

    /** @param idTaiKhoan null khi {@code loai} = KHACH */
    public record NguoiLam(LoaiNguoiThucHien loai, Long idTaiKhoan, String hoTen) {
    }
}
