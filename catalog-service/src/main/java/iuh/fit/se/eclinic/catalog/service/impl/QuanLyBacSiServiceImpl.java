package iuh.fit.se.eclinic.catalog.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import iuh.fit.se.eclinic.catalog.client.LichLamViecClient;
import iuh.fit.se.eclinic.catalog.client.LichLamViecClient.KetQuaHuyCa;
import iuh.fit.se.eclinic.catalog.client.TaiKhoanClient;
import iuh.fit.se.eclinic.catalog.dto.request.SuaBacSiRequest;
import iuh.fit.se.eclinic.catalog.dto.request.ThemBacSiRequest;
import iuh.fit.se.eclinic.catalog.dto.response.AnhHuongNgungCongTacResponse;
import iuh.fit.se.eclinic.catalog.dto.response.BacSiQuanTriResponse;
import iuh.fit.se.eclinic.catalog.dto.response.HoSoBacSiQuanTriResponse;
import iuh.fit.se.eclinic.catalog.dto.response.KetQuaNgungCongTacResponse;
import iuh.fit.se.eclinic.catalog.mapper.BacSiMapper;
import iuh.fit.se.eclinic.catalog.repository.BacSiRepository;
import iuh.fit.se.eclinic.catalog.repository.ChuyenKhoaRepository;
import iuh.fit.se.eclinic.catalog.repository.LichHenChiDocRepository;
import iuh.fit.se.eclinic.catalog.repository.LichHenChiDocRepository.SoLuotTheoBacSi;
import iuh.fit.se.eclinic.catalog.repository.LichLamViecChiDocRepository;
import iuh.fit.se.eclinic.catalog.service.HoSoBacSiService;
import iuh.fit.se.eclinic.catalog.service.QuanLyBacSiService;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.catalog.ChuyenKhoa;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiBacSi;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Không đặt {@code @Transactional} ở method: mỗi method gọi sang service khác (HTTP) nên phần ghi bảng bac_si chạy trong
 * transaction ngắn của {@link TransactionTemplate}, không giữ khoá dòng trong lúc chờ service kia.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuanLyBacSiServiceImpl implements QuanLyBacSiService {

    private final BacSiRepository bacSiRepository;
    private final ChuyenKhoaRepository chuyenKhoaRepository;
    private final LichHenChiDocRepository lichHenChiDocRepository;
    private final LichLamViecChiDocRepository lichLamViecChiDocRepository;
    private final TaiKhoanClient taiKhoanClient;
    private final LichLamViecClient lichLamViecClient;
    private final HoSoBacSiService hoSoBacSiService;
    private final BacSiMapper bacSiMapper;
    private final TransactionTemplate giaoDich;
    private final EntityManager entityManager;

    @Override
    public TrangDuLieu<BacSiQuanTriResponse> danhSach(String tuKhoa, Long idChuyenKhoa, TrangThaiBacSi trangThai,
            int trang, int kichThuoc) {
        Page<BacSi> ketQua = bacSiRepository.timChoQuanTri(BacSiServiceImpl.mauTen(tuKhoa), idChuyenKhoa, trangThai,
                PageRequest.of(trang, kichThuoc));
        List<Long> idCacBacSi = ketQua.getContent().stream().map(BacSi::getId).toList();
        Map<Long, Long> soLuotDaKham = idCacBacSi.isEmpty() ? Map.of()
                : lichHenChiDocRepository.demDaKham(idCacBacSi).stream()
                        .collect(Collectors.toMap(SoLuotTheoBacSi::getIdBacSi, SoLuotTheoBacSi::getSoLuot));
        return TrangDuLieu.tu(ketQua.map(bacSi -> bacSiMapper.toDongQuanTri(bacSi,
                soLuotDaKham.getOrDefault(bacSi.getId(), 0L))));
    }

    @Override
    public HoSoBacSiQuanTriResponse them(ThemBacSiRequest request) {
        Long idChuyenKhoa = request.idChuyenKhoa();
        String soGiayPhep = bacSiMapper.chuanHoa(request.soGiayPhep());
        if (!chuyenKhoaRepository.existsById(idChuyenKhoa)) {
            throw new LoiKhongTimThay("ChuyenKhoa", idChuyenKhoa);
        }
        if (soGiayPhep != null && bacSiRepository.existsBySoGiayPhep(soGiayPhep)) {
            throw new LoiNghiepVu(MaLoi.SO_GIAY_PHEP_DA_TON_TAI);
        }
        // Tài khoản trước (identity-service kiểm tra email, số điện thoại), hồ sơ bác sĩ sau
        Long idTaiKhoan = taiKhoanClient.taoTaiKhoanBacSi(request.hoTen().trim(), request.email().trim(),
                bacSiMapper.chuanHoa(request.soDienThoai()));
        Long idBacSi;
        try {
            idBacSi = giaoDich.execute(trangThai -> {
                BacSi bacSi = new BacSi();
                bacSi.setTaiKhoan(entityManager.getReference(TaiKhoan.class, idTaiKhoan));
                bacSi.setChuyenKhoa(entityManager.getReference(ChuyenKhoa.class, idChuyenKhoa));
                bacSi.setSoGiayPhep(soGiayPhep);
                bacSi.setHocVi(bacSiMapper.chuanHoa(request.hocVi()));
                bacSi.setChucVu(bacSiMapper.chuanHoa(request.chucVu()));
                bacSi.setSoNamKinhNghiem(request.soNamKinhNghiem());
                return bacSiRepository.saveAndFlush(bacSi).getId();
            });
        } catch (RuntimeException ex) {
            xoaTaiKhoanVuaTao(idTaiKhoan);
            // Chuyên khoa vừa bị xoá hoặc số giấy phép vừa bị người khác dùng: khoá của bảng chặn
            if (ex instanceof DataIntegrityViolationException) {
                throw new LoiNghiepVu(MaLoi.XUNG_DOT_DU_LIEU,
                        "Chuyên khoa hoặc số giấy phép vừa thay đổi, vui lòng tải lại và thử lại");
            }
            throw ex;
        }
        log.info("Thêm bác sĩ id={} (tài khoản id={})", idBacSi, idTaiKhoan);
        return hoSoBacSiService.layHoSo(idBacSi);
    }

    /** Không để lại tài khoản bác sĩ không có hồ sơ. Xoá không được thì chỉ ghi log: lỗi gốc mới là lỗi cần báo. */
    private void xoaTaiKhoanVuaTao(Long idTaiKhoan) {
        try {
            taiKhoanClient.xoaTaiKhoanBacSi(idTaiKhoan);
        } catch (RuntimeException ex) {
            log.error("Không tạo được hồ sơ bác sĩ và cũng không xoá được tài khoản id={} vừa tạo; cần xoá tay",
                    idTaiKhoan, ex);
        }
    }

    @Override
    public HoSoBacSiQuanTriResponse sua(Long idBacSi, SuaBacSiRequest request) {
        BacSi bacSi = timBacSi(idBacSi);
        TaiKhoan taiKhoan = bacSi.getTaiKhoan();
        Long idChuyenKhoa = request.idChuyenKhoa();
        String hoTen = request.hoTen().trim();
        String soDienThoai = bacSiMapper.chuanHoa(request.soDienThoai());
        String soGiayPhep = bacSiMapper.chuanHoa(request.soGiayPhep());
        boolean doiChuyenKhoa = !bacSi.getChuyenKhoa().getId().equals(idChuyenKhoa);

        // Kiểm tra hết trước khi gọi identity-service, để lỗi thường gặp không làm thay đổi dở dang
        if (doiChuyenKhoa) {
            if (!chuyenKhoaRepository.existsById(idChuyenKhoa)) {
                throw new LoiKhongTimThay("ChuyenKhoa", idChuyenKhoa);
            }
            kiemTraKhongConCaSapToi(idBacSi);
        }
        if (soGiayPhep != null && bacSiRepository.existsBySoGiayPhepAndIdNot(soGiayPhep, idBacSi)) {
            throw new LoiNghiepVu(MaLoi.SO_GIAY_PHEP_DA_TON_TAI);
        }
        if (!hoTen.equals(taiKhoan.getHoTen()) || !Objects.equals(soDienThoai, taiKhoan.getSoDienThoai())) {
            taiKhoanClient.suaThongTin(taiKhoan.getId(), hoTen, soDienThoai);
        }
        try {
            giaoDich.executeWithoutResult(trangThai -> {
                // booking-service khoá cùng dòng này khi xếp ca, nên kiểm tra lại "không còn ca" sau khi giữ khoá
                BacSi dangSua = khoaBacSi(idBacSi);
                if (doiChuyenKhoa) {
                    kiemTraKhongConCaSapToi(idBacSi);
                    dangSua.setChuyenKhoa(entityManager.getReference(ChuyenKhoa.class, idChuyenKhoa));
                }
                dangSua.setSoGiayPhep(soGiayPhep);
                bacSiRepository.flush();
            });
        } catch (DataIntegrityViolationException ex) {
            throw new LoiNghiepVu(MaLoi.XUNG_DOT_DU_LIEU,
                    "Chuyên khoa hoặc số giấy phép vừa thay đổi, vui lòng tải lại và thử lại");
        }
        log.info("Sửa thông tin cơ bản của bác sĩ id={}", idBacSi);
        return hoSoBacSiService.layHoSo(idBacSi);
    }

    @Override
    public AnhHuongNgungCongTacResponse anhHuongNgungCongTac(Long idBacSi) {
        if (!bacSiRepository.existsById(idBacSi)) {
            throw new LoiKhongTimThay("BacSi", idBacSi);
        }
        LocalDateTime bayGio = LocalDateTime.now();
        return new AnhHuongNgungCongTacResponse(
                lichLamViecChiDocRepository.demCaSapToi(idBacSi, bayGio.toLocalDate(), bayGio.toLocalTime()),
                lichHenChiDocRepository.demBiAnhHuongNeuNgungCongTac(idBacSi, bayGio.toLocalDate(),
                        bayGio.toLocalTime()));
    }

    @Override
    public KetQuaNgungCongTacResponse ngungCongTac(Long idTaiKhoanQuanTri, Long idBacSi, String lyDoNhap) {
        String lyDo = lyDoNhap.trim();
        // Bước 1 commit trước: từ đây booking-service không xếp ca mới, không nhận lịch hẹn mới cho bác sĩ này
        Long idTaiKhoan = giaoDich.execute(trangThai -> {
            BacSi bacSi = khoaBacSi(idBacSi);
            bacSi.setTrangThai(TrangThaiBacSi.NGUNG_CONG_TAC);
            return bacSi.getTaiKhoan().getId();
        });
        KetQuaHuyCa ketQua = lichLamViecClient.huyCaSapToi(idBacSi, idTaiKhoanQuanTri, lyDo);
        taiKhoanClient.voHieuHoa(idTaiKhoan, lyDo);
        log.info("Bác sĩ id={} ngừng công tác: {} ca bị hủy, {} lịch hẹn cần đổi lịch, tài khoản id={} bị vô hiệu hoá",
                idBacSi, ketQua.soCaDaHuy(), ketQua.soLichHenCanDoi(), idTaiKhoan);
        return new KetQuaNgungCongTacResponse(hoSoBacSiService.layHoSo(idBacSi), ketQua.soCaDaHuy(),
                ketQua.soLichHenCanDoi());
    }

    @Override
    public HoSoBacSiQuanTriResponse congTacLai(Long idBacSi) {
        Long idTaiKhoan = timBacSi(idBacSi).getTaiKhoan().getId();
        // Tài khoản trước: identity-service lỗi thì bác sĩ vẫn ở trạng thái cũ, không có bác sĩ "đang công tác" mà
        // không đăng nhập được
        taiKhoanClient.kichHoatLai(idTaiKhoan);
        giaoDich.executeWithoutResult(trangThai -> khoaBacSi(idBacSi).setTrangThai(TrangThaiBacSi.DANG_CONG_TAC));
        log.info("Bác sĩ id={} công tác lại, tài khoản id={} được kích hoạt lại", idBacSi, idTaiKhoan);
        return hoSoBacSiService.layHoSo(idBacSi);
    }

    private void kiemTraKhongConCaSapToi(Long idBacSi) {
        LocalDateTime bayGio = LocalDateTime.now();
        if (lichLamViecChiDocRepository.demCaSapToi(idBacSi, bayGio.toLocalDate(), bayGio.toLocalTime()) > 0) {
            throw new LoiNghiepVu(MaLoi.BAC_SI_CON_CA_LAM_VIEC);
        }
    }

    private BacSi timBacSi(Long idBacSi) {
        return bacSiRepository.timKemTaiKhoanVaChuyenKhoa(idBacSi)
                .orElseThrow(() -> new LoiKhongTimThay("BacSi", idBacSi));
    }

    /** Phải gọi trong transaction. */
    private BacSi khoaBacSi(Long idBacSi) {
        return bacSiRepository.findByIdForUpdate(idBacSi).orElseThrow(() -> new LoiKhongTimThay("BacSi", idBacSi));
    }

}
