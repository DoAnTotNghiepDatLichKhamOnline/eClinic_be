package iuh.fit.fe.be_websatlichkham.notification.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.fe.be_websatlichkham.common.entity.notification.ThongBao;
import iuh.fit.fe.be_websatlichkham.notification.repository.ThongBaoRepository;
import iuh.fit.fe.be_websatlichkham.notification.service.ThongBaoService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThongBaoServiceImpl implements ThongBaoService {

    private final ThongBaoRepository thongBaoRepository;

    @Override
    public List<ThongBao> findByTaiKhoan(Long taiKhoanId) {
        return thongBaoRepository.findByTaiKhoanIdOrderByNgayTaoDesc(taiKhoanId);
    }

    @Override
    public long countChuaDoc(Long taiKhoanId) {
        return thongBaoRepository.countByTaiKhoanIdAndDaDocFalse(taiKhoanId);
    }

}
