package iuh.fit.se.eclinic.report.service;

import iuh.fit.se.eclinic.report.dto.response.BaoCaoThongKeResponse;

/** Tệp Excel của báo cáo thống kê (use case "Xuất báo cáo thống kê"), chữ theo ngôn ngữ của request. */
public interface ExcelBaoCaoService {

    /** 5 trang: tổng quan, theo chuyên khoa, theo bác sĩ, theo thời gian, ghi chú (cách tính). */
    byte[] xuat(BaoCaoThongKeResponse baoCao);

}
