package iuh.fit.se.eclinic.report.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.report.dto.request.KyBaoCao;
import iuh.fit.se.eclinic.report.dto.response.DonViThoiGian;

/**
 * Khoảng ngày của 1 báo cáo (tính cả 2 đầu) và đơn vị gom của chuỗi theo thời gian.
 */
public record KhoangBaoCao(KyBaoCao ky, LocalDate tuNgay, LocalDate denNgay, DonViThoiGian donVi) {

    /** Khoảng tuỳ chọn dài nhất: đủ 1 năm nhuận. */
    public static final int SO_NGAY_TOI_DA = 366;
    private static final int SO_NGAY_THEO_NGAY_TOI_DA = 31;
    private static final int SO_NGAY_THEO_TUAN_TOI_DA = 184;

    /**
     * Khoảng của kỳ chứa ngày mốc ({@code ngay}, mặc định hôm nay). Kỳ TUY_CHON cần cả {@code tuNgay} và
     * {@code denNgay}; các kỳ khác bỏ qua 2 giá trị này.
     */
    public static KhoangBaoCao tu(KyBaoCao ky, LocalDate ngay, LocalDate tuNgay, LocalDate denNgay) {
        KyBaoCao kyDung = ky == null ? KyBaoCao.THANG : ky;
        LocalDate moc = ngay == null ? LocalDate.now() : ngay;
        return switch (kyDung) {
            case NGAY -> tao(kyDung, moc, moc);
            case TUAN -> tao(kyDung, moc.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                    moc.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
            case THANG -> tao(kyDung, moc.withDayOfMonth(1), moc.with(TemporalAdjusters.lastDayOfMonth()));
            case NAM -> tao(kyDung, moc.withDayOfYear(1), moc.with(TemporalAdjusters.lastDayOfYear()));
            case TUY_CHON -> tuyChon(tuNgay, denNgay);
        };
    }

    private static KhoangBaoCao tuyChon(LocalDate tuNgay, LocalDate denNgay) {
        if (tuNgay == null || denNgay == null) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.kyTuyChon");
        }
        if (denNgay.isBefore(tuNgay) || ChronoUnit.DAYS.between(tuNgay, denNgay) + 1 > SO_NGAY_TOI_DA) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.khoangBaoCao",
                    SO_NGAY_TOI_DA);
        }
        return tao(KyBaoCao.TUY_CHON, tuNgay, denNgay);
    }

    private static KhoangBaoCao tao(KyBaoCao ky, LocalDate tuNgay, LocalDate denNgay) {
        long soNgay = ChronoUnit.DAYS.between(tuNgay, denNgay) + 1;
        DonViThoiGian donVi = soNgay <= SO_NGAY_THEO_NGAY_TOI_DA ? DonViThoiGian.NGAY
                : soNgay <= SO_NGAY_THEO_TUAN_TOI_DA ? DonViThoiGian.TUAN : DonViThoiGian.THANG;
        return new KhoangBaoCao(ky, tuNgay, denNgay, donVi);
    }

    /** Ngày đầu của điểm (theo đơn vị gom) chứa ngày này; điểm đầu tiên có thể bắt đầu trước {@code tuNgay}. */
    public LocalDate dauDiem(LocalDate ngay) {
        return switch (donVi) {
            case NGAY -> ngay;
            case TUAN -> ngay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case THANG -> ngay.withDayOfMonth(1);
        };
    }

    /** Ngày đầu của điểm kế tiếp. */
    public LocalDate dauDiemSau(LocalDate dauDiem) {
        return switch (donVi) {
            case NGAY -> dauDiem.plusDays(1);
            case TUAN -> dauDiem.plusWeeks(1);
            case THANG -> dauDiem.plusMonths(1);
        };
    }

}
