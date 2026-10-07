package iuh.fit.se.eclinic.notification.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.entity.notification.ThongBao;
import iuh.fit.se.eclinic.common.entity.scheduling.YeuCauDoiLich;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.notification.dto.request.TaoThongBaoRequest;
import iuh.fit.se.eclinic.notification.dto.response.DanhDauDaDocResponse;
import iuh.fit.se.eclinic.notification.dto.response.KetQuaNhanThongBaoResponse;
import iuh.fit.se.eclinic.notification.dto.response.SoChuaDocResponse;
import iuh.fit.se.eclinic.notification.dto.response.ThongBaoResponse;
import iuh.fit.se.eclinic.notification.mapper.ThongBaoMapper;
import iuh.fit.se.eclinic.notification.repository.LichHenChiDocRepository;
import iuh.fit.se.eclinic.notification.repository.TaiKhoanChiDocRepository;
import iuh.fit.se.eclinic.notification.repository.ThongBaoRepository;
import iuh.fit.se.eclinic.notification.service.ThongBaoService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThongBaoServiceImpl implements ThongBaoService {

    private final ThongBaoRepository thongBaoRepository;
    private final TaiKhoanChiDocRepository taiKhoanChiDocRepository;
    private final LichHenChiDocRepository lichHenChiDocRepository;
    private final ThongBaoMapper thongBaoMapper;
    private final EntityManager entityManager;

    @Override
    public TrangDuLieu<ThongBaoResponse> danhSach(Long idTaiKhoan, VaiTro vaiTro, boolean chiChuaDoc, int trang,
            int kichThuoc) {
        boolean kemMaPhieuKham = vaiTro == VaiTro.BENH_NHAN;
        return TrangDuLieu.tu(thongBaoRepository
                .timCuaTaiKhoan(idTaiKhoan, chiChuaDoc, PageRequest.of(trang, kichThuoc))
                .map(thongBao -> thongBaoMapper.toResponse(thongBao, kemMaPhieuKham)));
    }

    @Override
    public SoChuaDocResponse demChuaDoc(Long idTaiKhoan) {
        return new SoChuaDocResponse(thongBaoRepository.countByTaiKhoanIdAndDaDocFalse(idTaiKhoan));
    }

    @Override
    @Transactional
    public ThongBaoResponse danhDauDaDoc(Long idTaiKhoan, VaiTro vaiTro, Long id) {
        // Thông báo không có và thông báo của tài khoản khác trả cùng 1 lỗi
        ThongBao thongBao = thongBaoRepository.timCuaTaiKhoanTheoId(id, idTaiKhoan)
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.KHONG_TIM_THAY_THONG_BAO));
        thongBao.setDaDoc(true);
        return thongBaoMapper.toResponse(thongBao, vaiTro == VaiTro.BENH_NHAN);
    }

    @Override
    @Transactional
    public DanhDauDaDocResponse danhDauDaDocTatCa(Long idTaiKhoan) {
        return new DanhDauDaDocResponse(thongBaoRepository.danhDauDaDocTatCa(idTaiKhoan));
    }

    /**
     * 2 lô chứa cùng {@code maNguon} đến cùng lúc: lô commit sau vi phạm UNIQUE uk_thong_bao_ma_nguon, cả lô đó rollback
     * và trả lỗi; service nguồn gửi lại, lần đó các dòng đã có được bỏ qua.
     */
    @Override
    @Transactional
    public KetQuaNhanThongBaoResponse nhan(List<TaoThongBaoRequest> danhSach) {
        int daTao = 0;
        Set<String> trongLo = new HashSet<>();
        for (TaoThongBaoRequest request : danhSach) {
            if (!trongLo.add(request.maNguon()) || thongBaoRepository.existsByMaNguon(request.maNguon())) {
                continue;
            }
            if (!taiKhoanChiDocRepository.existsById(request.idTaiKhoan())) {
                log.warn("Bỏ qua thông báo {}: không còn tài khoản id={}", request.maNguon(), request.idTaiKhoan());
                continue;
            }
            ThongBao thongBao = new ThongBao();
            thongBao.setMaNguon(request.maNguon());
            thongBao.setLoai(request.loai());
            thongBao.setNoiDung(request.noiDung());
            thongBao.setTaiKhoan(entityManager.getReference(TaiKhoan.class, request.idTaiKhoan()));
            if (request.idLichHen() != null && lichHenChiDocRepository.existsById(request.idLichHen())) {
                thongBao.setLichHen(entityManager.getReference(LichHen.class, request.idLichHen()));
            }
            if (request.idYeuCau() != null && entityManager.find(YeuCauDoiLich.class, request.idYeuCau()) != null) {
                thongBao.setYeuCauDoiLich(entityManager.getReference(YeuCauDoiLich.class, request.idYeuCau()));
            }
            thongBaoRepository.save(thongBao);
            daTao++;
        }
        return new KetQuaNhanThongBaoResponse(daTao, danhSach.size() - daTao);
    }

}
