package iuh.fit.se.eclinic.report.service;

import java.time.LocalDate;

import iuh.fit.se.eclinic.report.dto.request.KyBaoCao;
import iuh.fit.se.eclinic.report.dto.response.BaoCaoThongKeResponse;

/** Báo cáo thống kê của quản trị viên, tính trực tiếp từ DB chung, không ghi gì. */
public interface ThongKeService {

    BaoCaoThongKeResponse lap(KyBaoCao ky, LocalDate ngay, LocalDate tuNgay, LocalDate denNgay, Long idChuyenKhoa);

}
