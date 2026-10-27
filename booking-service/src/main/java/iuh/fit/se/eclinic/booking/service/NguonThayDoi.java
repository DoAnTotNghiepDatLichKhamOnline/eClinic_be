package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.common.enums.NguonThayDoiCa;

/** Thay đổi ca làm việc đến từ đâu: ghi vào lịch sử ca (lich_su_ca_lam_viec.nguon, id_yeu_cau). */
public record NguonThayDoi(NguonThayDoiCa nguon, Long idYeuCau) {

    public static final NguonThayDoi QUAN_TRI = new NguonThayDoi(NguonThayDoiCa.QUAN_TRI, null);
    public static final NguonThayDoi NGUNG_CONG_TAC = new NguonThayDoi(NguonThayDoiCa.NGUNG_CONG_TAC, null);
    public static final NguonThayDoi SAO_CHEP = new NguonThayDoi(NguonThayDoiCa.SAO_CHEP, null);
    public static final NguonThayDoi NHAP_EXCEL = new NguonThayDoi(NguonThayDoiCa.NHAP_EXCEL, null);
    public static final NguonThayDoi NGAY_NGHI = new NguonThayDoi(NguonThayDoiCa.NGAY_NGHI, null);

    public static NguonThayDoi duyetYeuCau(Long idYeuCau) {
        return new NguonThayDoi(NguonThayDoiCa.DUYET_YEU_CAU, idYeuCau);
    }
}
