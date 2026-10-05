package iuh.fit.se.eclinic.booking.service.impl;

import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import iuh.fit.se.eclinic.booking.dto.response.HoSoChoXacMinhResponse;
import iuh.fit.se.eclinic.booking.repository.HoSoBenhNhanRepository;
import iuh.fit.se.eclinic.booking.repository.LichHenRepository;
import iuh.fit.se.eclinic.booking.repository.LienKetHoSoBiTuChoiRepository;
import iuh.fit.se.eclinic.booking.repository.NguoiThanDaLuuRepository;
import iuh.fit.se.eclinic.booking.repository.TaiKhoanChiDocRepository;
import iuh.fit.se.eclinic.booking.service.LienKetHoSoService;
import iuh.fit.se.eclinic.booking.util.ChuanHoaTen;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.entity.booking.LienKetHoSoBiTuChoi;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import lombok.extern.slf4j.Slf4j;

/**
 * Không đặt {@code @Transactional} ở lớp: {@link #thuLienKet} tự mở transaction riêng bằng {@link TransactionTemplate}
 * để bắt được cả lỗi lúc commit; {@link #lienKet} chạy trong transaction của bên gọi.
 */
@Slf4j
@Service
public class LienKetHoSoServiceImpl implements LienKetHoSoService {

    private final HoSoBenhNhanRepository hoSoBenhNhanRepository;
    private final TaiKhoanChiDocRepository taiKhoanChiDocRepository;
    private final LienKetHoSoBiTuChoiRepository lienKetHoSoBiTuChoiRepository;
    private final NguoiThanDaLuuRepository nguoiThanDaLuuRepository;
    private final LichHenRepository lichHenRepository;
    private final TransactionTemplate giaoDichRieng;

    public LienKetHoSoServiceImpl(HoSoBenhNhanRepository hoSoBenhNhanRepository,
            TaiKhoanChiDocRepository taiKhoanChiDocRepository,
            LienKetHoSoBiTuChoiRepository lienKetHoSoBiTuChoiRepository,
            NguoiThanDaLuuRepository nguoiThanDaLuuRepository, LichHenRepository lichHenRepository,
            PlatformTransactionManager transactionManager) {
        this.hoSoBenhNhanRepository = hoSoBenhNhanRepository;
        this.taiKhoanChiDocRepository = taiKhoanChiDocRepository;
        this.lienKetHoSoBiTuChoiRepository = lienKetHoSoBiTuChoiRepository;
        this.nguoiThanDaLuuRepository = nguoiThanDaLuuRepository;
        this.lichHenRepository = lichHenRepository;
        this.giaoDichRieng = new TransactionTemplate(transactionManager);
        this.giaoDichRieng.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void thuLienKet(Long idTaiKhoan) {
        try {
            giaoDichRieng.executeWithoutResult(trangThai -> thuLienKetTrongGiaoDich(idTaiKhoan));
        } catch (RuntimeException e) {
            // 2 request cùng lúc: request sau vi phạm UNIQUE id_tai_khoan; lần đọc kế tiếp thấy kết quả của request trước
            log.warn("Không liên kết được hồ sơ bệnh nhân cho tài khoản id={}: {}", idTaiKhoan, e.toString());
        }
    }

    private void thuLienKetTrongGiaoDich(Long idTaiKhoan) {
        TaiKhoan taiKhoan = taiKhoanChiDocRepository.findById(idTaiKhoan).orElse(null);
        if (taiKhoan == null || taiKhoan.getVaiTro() != VaiTro.BENH_NHAN
                || taiKhoan.getTrangThai() != TrangThaiTaiKhoan.DA_KICH_HOAT || taiKhoan.getCccdDangKy() == null) {
            return;
        }
        if (hoSoBenhNhanRepository.findByTaiKhoanId(idTaiKhoan).isPresent()) {
            return;
        }
        Optional<HoSoBenhNhan> timThay = hoSoBenhNhanRepository.findByCccdForUpdate(taiKhoan.getCccdDangKy());
        if (timThay.isEmpty()) {
            return;
        }
        HoSoBenhNhan hoSo = timThay.get();
        if (hoSo.getTaiKhoan() != null || daBiTuChoi(idTaiKhoan, hoSo.getId())) {
            return;
        }
        // Lúc này chỉ có họ tên và SĐT của tài khoản để đối chiếu (ngày sinh chỉ có ở form hồ sơ của tôi)
        boolean khop = taiKhoan.getHoTen() != null && ChuanHoaTen.giongNhau(taiKhoan.getHoTen(), hoSo.getHoTen())
                && taiKhoan.getSoDienThoai() != null && taiKhoan.getSoDienThoai().equals(hoSo.getSoDienThoai());
        lienKet(taiKhoan, hoSo, khop);
    }

    @Override
    public void lienKet(TaiKhoan taiKhoan, HoSoBenhNhan hoSo, boolean khop) {
        hoSo.setTaiKhoan(taiKhoan);
        hoSo.setTrangThaiLienKet(khop ? TrangThaiLienKet.DA_LIEN_KET : TrangThaiLienKet.CHO_XAC_MINH);
        hoSoBenhNhanRepository.saveAndFlush(hoSo);
        if (khop) {
            boNguoiThanDaLuu(taiKhoan.getId(), hoSo.getId());
        }
        log.info("Gắn hồ sơ bệnh nhân id={} vào tài khoản id={}: {}", hoSo.getId(), taiKhoan.getId(),
                hoSo.getTrangThaiLienKet());
    }

    @Override
    public boolean daBiTuChoi(Long idTaiKhoan, Long idHoSoBenhNhan) {
        return lienKetHoSoBiTuChoiRepository.existsByTaiKhoanIdAndHoSoBenhNhanId(idTaiKhoan, idHoSoBenhNhan);
    }

    @Override
    @Transactional(readOnly = true)
    public TrangDuLieu<HoSoChoXacMinhResponse> choXacMinh(int trang, int kichThuoc) {
        return TrangDuLieu.tu(hoSoBenhNhanRepository
                .timTheoTrangThaiLienKet(TrangThaiLienKet.CHO_XAC_MINH, PageRequest.of(trang, kichThuoc))
                .map(this::toChoXacMinh));
    }

    @Override
    @Transactional
    public void duyet(Long idHoSoBenhNhan) {
        HoSoBenhNhan hoSo = layDangChoXacMinh(idHoSoBenhNhan);
        hoSo.setTrangThaiLienKet(TrangThaiLienKet.DA_LIEN_KET);
        boNguoiThanDaLuu(hoSo.getTaiKhoan().getId(), hoSo.getId());
        log.info("Duyệt liên kết hồ sơ bệnh nhân id={} với tài khoản id={}", hoSo.getId(),
                hoSo.getTaiKhoan().getId());
    }

    @Override
    @Transactional
    public void tuChoi(Long idHoSoBenhNhan, String lyDo) {
        HoSoBenhNhan hoSo = layDangChoXacMinh(idHoSoBenhNhan);
        TaiKhoan taiKhoan = hoSo.getTaiKhoan();
        // Ghi nhớ để lần đọc kế tiếp của tài khoản này không đưa hồ sơ trở lại hàng chờ
        LienKetHoSoBiTuChoi biTuChoi = new LienKetHoSoBiTuChoi();
        biTuChoi.setTaiKhoan(taiKhoan);
        biTuChoi.setHoSoBenhNhan(hoSo);
        biTuChoi.setLyDo(lyDo == null || lyDo.isBlank() ? null : lyDo.trim());
        lienKetHoSoBiTuChoiRepository.save(biTuChoi);
        hoSo.setTaiKhoan(null);
        hoSo.setTrangThaiLienKet(TrangThaiLienKet.CHUA_LIEN_KET);
        log.info("Từ chối liên kết hồ sơ bệnh nhân id={} với tài khoản id={}", hoSo.getId(), taiKhoan.getId());
    }

    private HoSoBenhNhan layDangChoXacMinh(Long idHoSoBenhNhan) {
        HoSoBenhNhan hoSo = hoSoBenhNhanRepository.findByIdForUpdate(idHoSoBenhNhan)
                .orElseThrow(() -> new LoiKhongTimThay("HoSoBenhNhan", idHoSoBenhNhan));
        if (hoSo.getTrangThaiLienKet() != TrangThaiLienKet.CHO_XAC_MINH) {
            throw new LoiNghiepVu(MaLoi.XUNG_DOT_DU_LIEU, "Hồ sơ bệnh nhân không ở trạng thái chờ xác minh");
        }
        return hoSo;
    }

    /** Hồ sơ đã là của chính tài khoản thì không còn là "người thân đã lưu" của tài khoản đó. */
    private void boNguoiThanDaLuu(Long idTaiKhoan, Long idHoSoBenhNhan) {
        nguoiThanDaLuuRepository.findByTaiKhoanIdAndHoSoBenhNhanId(idTaiKhoan, idHoSoBenhNhan)
                .ifPresent(nguoiThanDaLuuRepository::delete);
    }

    private HoSoChoXacMinhResponse toChoXacMinh(HoSoBenhNhan hoSo) {
        TaiKhoan taiKhoan = hoSo.getTaiKhoan();
        return new HoSoChoXacMinhResponse(
                new HoSoChoXacMinhResponse.HoSo(hoSo.getId(), hoSo.getHoTen(), hoSo.getNgaySinh(), hoSo.getCccd(),
                        hoSo.getSoDienThoai(), lichHenRepository.countByHoSoBenhNhanId(hoSo.getId())),
                new HoSoChoXacMinhResponse.TaiKhoan(taiKhoan.getId(), taiKhoan.getHoTen(), taiKhoan.getEmail(),
                        taiKhoan.getSoDienThoai()),
                taiKhoan.getHoTen() != null && ChuanHoaTen.giongNhau(taiKhoan.getHoTen(), hoSo.getHoTen()),
                taiKhoan.getSoDienThoai() != null && taiKhoan.getSoDienThoai().equals(hoSo.getSoDienThoai()));
    }

}
