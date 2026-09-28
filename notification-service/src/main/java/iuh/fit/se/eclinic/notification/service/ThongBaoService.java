package iuh.fit.se.eclinic.notification.service;

import java.util.List;

import iuh.fit.se.eclinic.common.entity.notification.ThongBao;

public interface ThongBaoService {

    List<ThongBao> timTheoTaiKhoan(Long taiKhoanId);

    /** Số thông báo chưa đọc (badge trên biểu tượng chuông). */
    long demChuaDoc(Long taiKhoanId);

}
