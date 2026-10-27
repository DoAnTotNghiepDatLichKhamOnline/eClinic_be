package iuh.fit.se.eclinic.report.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import iuh.fit.se.eclinic.report.dto.request.KyBaoCao;

/** Báo cáo thống kê của quản trị viên. */
public record BaoCaoThongKeResponse(
        KyBaoCao ky,
        LocalDate tuNgay,
        LocalDate denNgay,
        Long idChuyenKhoa,
        DonViThoiGian donViThoiGian,
        TongQuan tongQuan,
        TrangThai trangThai,
        KenhDat kenhDat,
        ThoiGianDatTruoc thoiGianDatTruoc,
        List<DongChuyenKhoa> theoChuyenKhoa,
        List<DongBacSi> theoBacSi,
        List<DiemThoiGian> theoThoiGian,
        LocalDateTime tinhLuc) {

    public record TongQuan(
            long bacSiDangCongTac,
            long bacSiCoCa,
            long taiKhoanBenhNhanMoi,
            long benhNhanDaKham,
            long luotKham,
            long lichHen,
            long luotDatMoi,
            BigDecimal tyLeHuy,
            BigDecimal tyLeKhongDen,
            long luotDaDat,
            long tongLuot,
            BigDecimal tyLeLapDay) {
    }

    public record TrangThai(long chuaKham, long daKham, long khongDen, long benhNhanHuy, long bacSiHuy,
            long quanTriHuy, long canDoiLich) {
    }

    public record KenhDat(long khach, long taiKhoan, long hotline) {
    }

    /** Thời gian đặt trước = giờ bắt đầu lượt khám - lúc tạo lịch hẹn; lịch đổi tính từ lúc đổi. */
    public record ThoiGianDatTruoc(BigDecimal trungBinhGio, long duoi1Ngay, long tu1Den3Ngay, long tu4Den7Ngay,
            long tu8Den14Ngay, long tren14Ngay) {
    }

    /** 1 chuyên khoa: các số như {@link ChiSo}. */
    public record DongChuyenKhoa(Long idChuyenKhoa, String tenChuyenKhoa, ChiSo chiSo) {
    }

    /** 1 bác sĩ; chuyên khoa là chuyên khoa hiện tại của bác sĩ. */
    public record DongBacSi(Long idBacSi, String maBacSi, String hoTen, Long idChuyenKhoa, String tenChuyenKhoa,
            ChiSo chiSo) {
    }

    /** Các số của 1 chuyên khoa / 1 bác sĩ, cùng định nghĩa với {@link TongQuan}. */
    public record ChiSo(long lichHen, long luotKham, long benhNhanDaKham, long daHuy, long khongDen,
            BigDecimal tyLeHuy, BigDecimal tyLeKhongDen, long luotDaDat, long tongLuot, BigDecimal tyLeLapDay) {
    }

    /** 1 điểm của chuỗi theo thời gian. */
    public record DiemThoiGian(LocalDate tuNgay, LocalDate denNgay, long lichHen, long luotKham, long daHuy,
            long khongDen) {
    }

}
