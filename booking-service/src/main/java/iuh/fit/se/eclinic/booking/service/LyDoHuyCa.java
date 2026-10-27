package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.common.thongbao.ThamSoThongBao;

/** Lý do hủy ca: câu quản trị viên gõ (giữ nguyên văn) hoặc khoá thông điệp do hệ thống ghi. */
public record LyDoHuyCa(String chu, String chuEn, String khoa) {

    public static final LyDoHuyCa BAC_SI_NGHI_CA = new LyDoHuyCa(null, null, "heThong.bacSiNghiCa");
    public static final LyDoHuyCa BAC_SI_DOI_CA = new LyDoHuyCa(null, null, "heThong.bacSiDoiCa");

    /** Câu quản trị viên gõ; thêm dấu chấm nếu chưa có dấu kết câu (câu này nằm giữa thông báo). */
    public static LyDoHuyCa nguoiDungGo(String cau) {
        return new LyDoHuyCa(ketCau(cau), null, null);
    }

    /** Lý do của ngày bệnh viện nghỉ: tiếng Việt bắt buộc, tiếng Anh có thể null (khi đó đọc câu tiếng Việt). */
    public static LyDoHuyCa benhVienNghi(String lyDo, String lyDoEn) {
        return new LyDoHuyCa(ketCau(lyDo), lyDoEn == null ? null : ketCau(lyDoEn), null);
    }

    /** Thêm lý do làm tham số kế tiếp của 1 mẫu thông báo. */
    public ThamSoThongBao themVao(ThamSoThongBao thamSo) {
        if (khoa != null) {
            return thamSo.khoa(khoa);
        }
        return chuEn == null ? thamSo.chu(chu) : thamSo.haiNgonNgu(chu, chuEn);
    }

    private static String ketCau(String cau) {
        String gon = cau.trim();
        return gon.endsWith(".") || gon.endsWith("!") || gon.endsWith("?") ? gon : gon + ".";
    }
}
