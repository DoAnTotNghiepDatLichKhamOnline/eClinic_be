package iuh.fit.se.eclinic.notification.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.entity.notification.ThongBao;
import iuh.fit.se.eclinic.notification.repository.ThongBaoRepository;
import iuh.fit.se.eclinic.notification.service.ThongBaoService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThongBaoServiceImpl implements ThongBaoService {

    private final ThongBaoRepository thongBaoRepository;

    @Override
    public List<ThongBao> timTheoTaiKhoan(Long taiKhoanId) {
        return thongBaoRepository.findByTaiKhoanIdOrderByNgayTaoDesc(taiKhoanId);
    }

    @Override
    public long demChuaDoc(Long taiKhoanId) {
        return thongBaoRepository.countByTaiKhoanIdAndDaDocFalse(taiKhoanId);
    }

}
