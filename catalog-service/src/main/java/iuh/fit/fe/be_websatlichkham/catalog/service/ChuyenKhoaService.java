package iuh.fit.fe.be_websatlichkham.catalog.service;

import java.util.List;

import iuh.fit.fe.be_websatlichkham.common.entity.catalog.ChuyenKhoa;

public interface ChuyenKhoaService {

    ChuyenKhoa getById(Long id);

    List<ChuyenKhoa> findAll();

}
