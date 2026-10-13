package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.common.thongbao.ThamSoThongBao;

/**
 * Lý do hủy 1 ca làm việc: câu quản trị viên gõ (giữ nguyên văn ở mọi ngôn ngữ) hoặc câu do hệ thống viết (lưu khoá
 * thông điệp, người đọc thấy câu của ngôn ngữ mình).
 *
 * @param chu  câu quản trị viên gõ, đã có dấu kết câu; null khi là câu của hệ thống
 * @param khoa khoá {@code heThong.<...>}; null khi là câu quản trị viên gõ
 */
public record LyDoHuyCa(String chu, String khoa) {

    public static final LyDoHuyCa BAC_SI_NGHI_CA = new LyDoHuyCa(null, "heThong.bacSiNghiCa");
    public static final LyDoHuyCa BAC_SI_DOI_CA = new LyDoHuyCa(null, "heThong.bacSiDoiCa");

    /** Câu quản trị viên gõ; thêm dấu chấm nếu chưa có dấu kết câu (câu này nằm giữa thông báo). */
    public static LyDoHuyCa nguoiDungGo(String cau) {
        String gon = cau.trim();
        return new LyDoHuyCa(gon.endsWith(".") || gon.endsWith("!") || gon.endsWith("?") ? gon : gon + ".", null);
    }

    /** Thêm lý do làm tham số kế tiếp của 1 mẫu thông báo. */
    public ThamSoThongBao themVao(ThamSoThongBao thamSo) {
        return khoa == null ? thamSo.chu(chu) : thamSo.khoa(khoa);
    }
}
