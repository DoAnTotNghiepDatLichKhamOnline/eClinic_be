package iuh.fit.fe.be_websatlichkham.notification.service;

import java.util.List;

import iuh.fit.fe.be_websatlichkham.common.entity.notification.ThongBao;

public interface ThongBaoService {

    List<ThongBao> findByTaiKhoan(Long taiKhoanId);

    /** Số thông báo chưa đọc (badge trên biểu tượng chuông). */
    long countChuaDoc(Long taiKhoanId);

}
