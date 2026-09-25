package iuh.fit.fe.be_websatlichkham.catalog.service;

import java.util.List;

import iuh.fit.fe.be_websatlichkham.common.entity.catalog.PhongKham;

public interface PhongKhamService {

    PhongKham getById(Long id);

    /** Các phòng khám đang hoạt động. */
    List<PhongKham> findActive();

    /** Các phòng khám đang hoạt động của 1 chuyên khoa. */
    List<PhongKham> findActiveByChuyenKhoa(Long chuyenKhoaId);

}
