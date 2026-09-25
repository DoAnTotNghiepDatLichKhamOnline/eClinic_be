package iuh.fit.fe.be_websatlichkham.catalog.service.impl;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.fe.be_websatlichkham.catalog.repository.ChuyenKhoaRepository;
import iuh.fit.fe.be_websatlichkham.catalog.service.ChuyenKhoaService;
import iuh.fit.fe.be_websatlichkham.common.entity.catalog.ChuyenKhoa;
import iuh.fit.fe.be_websatlichkham.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChuyenKhoaServiceImpl implements ChuyenKhoaService {

    private final ChuyenKhoaRepository chuyenKhoaRepository;

    @Override
    public ChuyenKhoa getById(Long id) {
        return chuyenKhoaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("ChuyenKhoa", id));
    }

    @Override
    public List<ChuyenKhoa> findAll() {
        return chuyenKhoaRepository.findAll(Sort.by("tenChuyenKhoa"));
    }

}
