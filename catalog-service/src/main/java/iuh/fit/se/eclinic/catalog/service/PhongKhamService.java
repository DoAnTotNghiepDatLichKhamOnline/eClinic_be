package iuh.fit.se.eclinic.catalog.service;

import java.util.List;

import iuh.fit.se.eclinic.common.entity.catalog.PhongKham;

public interface PhongKhamService {

    PhongKham layTheoId(Long id);

    /** Các phòng khám đang hoạt động. */
    List<PhongKham> layDangHoatDong();

    /** Các phòng khám đang hoạt động của 1 chuyên khoa. */
    List<PhongKham> layDangHoatDongTheoChuyenKhoa(Long chuyenKhoaId);

}
