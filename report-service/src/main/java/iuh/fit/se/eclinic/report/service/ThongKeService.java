package iuh.fit.se.eclinic.report.service;

import java.time.LocalDate;

import iuh.fit.se.eclinic.report.dto.request.KyBaoCao;
import iuh.fit.se.eclinic.report.dto.response.BaoCaoThongKeResponse;

/** Báo cáo thống kê của quản trị viên (DASH-01..04, ADM-05), tính trực tiếp từ DB chung, không ghi gì. */
public interface ThongKeService {

    /**
     * @param ky           kỳ (null = THANG)
     * @param ngay         ngày mốc của kỳ NGAY / TUAN / THANG / NAM (null = hôm nay)
     * @param tuNgay       ngày đầu, chỉ dùng cho kỳ TUY_CHON
     * @param denNgay      ngày cuối, chỉ dùng cho kỳ TUY_CHON
     * @param idChuyenKhoa chỉ tính 1 chuyên khoa (null = toàn viện)
     */
    BaoCaoThongKeResponse lap(KyBaoCao ky, LocalDate ngay, LocalDate tuNgay, LocalDate denNgay, Long idChuyenKhoa);

}
