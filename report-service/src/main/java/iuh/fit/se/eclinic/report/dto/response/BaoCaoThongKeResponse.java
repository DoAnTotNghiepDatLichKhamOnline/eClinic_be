package iuh.fit.se.eclinic.report.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import iuh.fit.se.eclinic.report.dto.request.KyBaoCao;

/**
 * Báo cáo thống kê của quản trị viên (DASH-01..04, ADM-05).
 * <p>
 * "Lịch hẹn" = lịch hẹn có lượt khám bắt đầu trong kỳ, trừ lịch cũ của 1 lần đổi lịch (DA_HUY_DO_DOI_LICH): 1 lần đổi
 * lịch chỉ tính 1 lịch. "Lượt khám" = lịch đã khám xong (DA_HOAN_THANH). "Đã hủy" = bệnh nhân hủy + bác sĩ hủy + quản trị
 * viên hủy. Tỷ lệ là phần trăm, 1 chữ số thập phân, null khi mẫu số bằng 0.
 *
 * @param ky             kỳ đã chọn
 * @param tuNgay         ngày đầu của kỳ (tính cả ngày này)
 * @param denNgay        ngày cuối của kỳ (tính cả ngày này)
 * @param idChuyenKhoa   chuyên khoa đã lọc; null = toàn viện
 * @param donViThoiGian  đơn vị gom của {@code theoThoiGian}
 * @param tongQuan       các số tổng (DASH-01, DASH-04)
 * @param trangThai      lịch hẹn theo trạng thái
 * @param kenhDat        lịch hẹn theo kênh đặt
 * @param thoiGianDatTruoc lịch hẹn theo thời gian đặt trước
 * @param theoChuyenKhoa mọi chuyên khoa (DASH-03), kể cả chuyên khoa không có lịch nào trong kỳ
 * @param theoBacSi      bác sĩ có ca hoặc có lịch hẹn trong kỳ, nhiều lịch hẹn trước
 * @param theoThoiGian   chuỗi theo thời gian, đủ mọi điểm của kỳ (điểm không có lịch là 0)
 * @param tinhLuc        thời điểm tính
 */
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

    /**
     * @param bacSiDangCongTac    bác sĩ đang công tác có tài khoản đã kích hoạt, hiện tại (không theo kỳ; cùng nghĩa với
     *                            Dashboard)
     * @param bacSiCoCa           bác sĩ có ít nhất 1 ca hoạt động trong kỳ
     * @param taiKhoanBenhNhanMoi tài khoản bệnh nhân tạo trong kỳ, đã xác thực email (không lọc theo chuyên khoa)
     * @param benhNhanDaKham      bệnh nhân duy nhất đã khám trong kỳ (theo hồ sơ bệnh nhân, duy nhất theo CCCD)
     * @param luotKham            lịch hẹn đã khám xong
     * @param lichHen             lịch hẹn trong kỳ
     * @param luotDatMoi          lịch hẹn được TẠO trong kỳ, trừ lịch cũ của 1 lần đổi lịch (cùng quy tắc với ô "lượt
     *                            đặt trong tháng" của Dashboard)
     * @param tyLeHuy             đã hủy / lịch hẹn
     * @param tyLeKhongDen        không đến / (đã khám + không đến): chỉ tính lịch đã có kết quả đến hay không
     * @param luotDaDat           lượt khám đã đặt của các ca hoạt động trong kỳ (bệnh nhân không đến vẫn tính)
     * @param tongLuot            lượt khám (còn trống + đã đặt) của các ca hoạt động trong kỳ
     * @param tyLeLapDay          luotDaDat / tongLuot
     */
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

    /**
     * @param chuaKham    đã xác nhận, chưa khám (lượt sắp tới, hoặc ca bị hủy còn chờ đổi lịch)
     * @param daKham      đã khám xong
     * @param khongDen    bệnh nhân không đến
     * @param benhNhanHuy bệnh nhân hủy (DA_HUY)
     * @param bacSiHuy    bác sĩ hủy (BI_TU_CHOI)
     * @param quanTriHuy  quản trị viên hủy (QUAN_TRI_HUY)
     * @param canDoiLich  trong số chưa khám: ca bị hủy, còn chờ bệnh nhân đổi lịch
     */
    public record TrangThai(long chuaKham, long daKham, long khongDen, long benhNhanHuy, long bacSiHuy,
            long quanTriHuy, long canDoiLich) {
    }

    /**
     * @param khach    khách đặt không đăng nhập
     * @param taiKhoan tài khoản bệnh nhân đặt
     * @param hotline  quản trị viên đặt thay người gọi tổng đài / đến quầy
     */
    public record KenhDat(long khach, long taiKhoan, long hotline) {
    }

    /**
     * Thời gian đặt trước = giờ bắt đầu lượt khám - lúc tạo lịch hẹn; lịch đổi tính từ lúc đổi.
     *
     * @param trungBinhGio trung bình, giờ (1 chữ số thập phân); null khi không có lịch hẹn
     */
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

    /**
     * Các số của 1 chuyên khoa / 1 bác sĩ, cùng định nghĩa với {@link TongQuan}.
     *
     * @param benhNhanDaKham bệnh nhân duy nhất đã khám (trong nhóm này)
     * @param daHuy          bệnh nhân + bác sĩ + quản trị viên hủy
     */
    public record ChiSo(long lichHen, long luotKham, long benhNhanDaKham, long daHuy, long khongDen,
            BigDecimal tyLeHuy, BigDecimal tyLeKhongDen, long luotDaDat, long tongLuot, BigDecimal tyLeLapDay) {
    }

    /**
     * 1 điểm của chuỗi theo thời gian.
     *
     * @param tuNgay  ngày đầu của điểm (điểm đầu / cuối cắt theo kỳ)
     * @param denNgay ngày cuối của điểm
     */
    public record DiemThoiGian(LocalDate tuNgay, LocalDate denNgay, long lichHen, long luotKham, long daHuy,
            long khongDen) {
    }

}
