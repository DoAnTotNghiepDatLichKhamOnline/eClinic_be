package iuh.fit.se.eclinic.medical.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.entity.medical.Thuoc;
import iuh.fit.se.eclinic.common.enums.TrangThaiThuoc;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.util.MauTimKiem;
import iuh.fit.se.eclinic.medical.dto.request.ThuocRequest;
import iuh.fit.se.eclinic.medical.dto.response.ThuocQuanTriResponse;
import iuh.fit.se.eclinic.medical.repository.ThuocRepository;
import iuh.fit.se.eclinic.medical.repository.ThuocRepository.SoLanKeTheoThuoc;
import iuh.fit.se.eclinic.medical.service.QuanLyThuocService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuanLyThuocServiceImpl implements QuanLyThuocService {

    private final ThuocRepository thuocRepository;

    @Override
    public TrangDuLieu<ThuocQuanTriResponse> danhSach(String tuKhoa, Boolean daXacMinh, TrangThaiThuoc trangThai,
            int trang, int kichThuoc) {
        String mau = tuKhoa == null || tuKhoa.isBlank() ? null : MauTimKiem.chua(ThuocServiceImpl.normalize(tuKhoa));
        Page<Thuoc> ketQua = thuocRepository.timChoQuanTri(mau, daXacMinh, trangThai,
                PageRequest.of(trang, kichThuoc));
        List<Long> idCacThuoc = ketQua.getContent().stream().map(Thuoc::getId).toList();
        Map<Long, Long> soLanKe = idCacThuoc.isEmpty() ? Map.of()
                : thuocRepository.demSoLanKe(idCacThuoc).stream()
                        .collect(Collectors.toMap(SoLanKeTheoThuoc::getIdThuoc, SoLanKeTheoThuoc::getSoLanKe));
        return TrangDuLieu.tu(ketQua.map(thuoc -> toResponse(thuoc, soLanKe.getOrDefault(thuoc.getId(), 0L))));
    }

    @Override
    @Transactional
    public ThuocQuanTriResponse them(ThuocRequest request) {
        String tenChuanHoa = ThuocServiceImpl.normalize(request.tenThuoc());
        String hamLuongChuanHoa = ThuocServiceImpl.normalize(request.hamLuong());
        if (thuocRepository.existsByTenChuanHoaAndHamLuongChuanHoa(tenChuanHoa, hamLuongChuanHoa)) {
            throw new LoiNghiepVu(MaLoi.TEN_THUOC_DA_TON_TAI);
        }
        Thuoc thuoc = new Thuoc();
        thuoc.setTenChuanHoa(tenChuanHoa);
        ganNoiDung(thuoc, request);
        thuoc.setDaXacMinh(true);
        thuoc.setTrangThai(TrangThaiThuoc.DANG_DUNG);
        // Flush ngay: trùng tên và hàm lượng thì vi phạm UNIQUE -> 409 XUNG_DOT_DU_LIEU
        thuocRepository.saveAndFlush(thuoc);
        log.info("Thêm thuốc id={} vào danh mục", thuoc.getId());
        return toResponse(thuoc, 0);
    }

    @Override
    @Transactional
    public ThuocQuanTriResponse sua(Long id, ThuocRequest request) {
        Thuoc thuoc = khoa(id);
        long soLanKe = thuocRepository.demSoLanKe(id);
        if (soLanKe > 0 && doiNhanDang(thuoc, request)) {
            throw new LoiNghiepVu(MaLoi.THUOC_DA_DUOC_KE);
        }
        String tenChuanHoa = ThuocServiceImpl.normalize(request.tenThuoc());
        String hamLuongChuanHoa = ThuocServiceImpl.normalize(request.hamLuong());
        // Kiểm tra trước khi sửa entity đang được quản lý: query sau khi sửa sẽ flush UPDATE trước
        if ((!tenChuanHoa.equals(thuoc.getTenChuanHoa()) || !hamLuongChuanHoa.equals(thuoc.getHamLuongChuanHoa()))
                && thuocRepository.existsByTenChuanHoaAndHamLuongChuanHoaAndIdNot(tenChuanHoa, hamLuongChuanHoa, id)) {
            throw new LoiNghiepVu(MaLoi.TEN_THUOC_DA_TON_TAI);
        }
        thuoc.setTenChuanHoa(tenChuanHoa);
        ganNoiDung(thuoc, request);
        thuocRepository.saveAndFlush(thuoc);
        log.info("Sửa thuốc id={}", id);
        return toResponse(thuoc, soLanKe);
    }

    @Override
    @Transactional
    public ThuocQuanTriResponse xacMinh(Long id) {
        return doi(id, thuoc -> {
            // Thuốc chỉ có tên phải có hoạt chất và hàm lượng mới kê được (Thông tư 26/2025/TT-BYT)
            if (thuoc.getHoatChat() == null || thuoc.getHamLuong() == null) {
                throw new LoiNghiepVu(MaLoi.THUOC_THIEU_THONG_TIN);
            }
            thuoc.setDaXacMinh(true);
        }, "xác minh");
    }

    @Override
    @Transactional
    public ThuocQuanTriResponse ngungDung(Long id) {
        return doi(id, thuoc -> thuoc.setTrangThai(TrangThaiThuoc.NGUNG_DUNG), "ngừng dùng");
    }

    @Override
    @Transactional
    public ThuocQuanTriResponse dungLai(Long id) {
        return doi(id, thuoc -> thuoc.setTrangThai(TrangThaiThuoc.DANG_DUNG), "dùng lại");
    }

    /** Gọi lại khi thuốc đã ở trạng thái đích thì không đổi gì. */
    private ThuocQuanTriResponse doi(Long id, Consumer<Thuoc> thayDoi, String viec) {
        Thuoc thuoc = khoa(id);
        thayDoi.accept(thuoc);
        log.info("Thuốc id={}: {}", id, viec);
        return toResponse(thuoc, thuocRepository.demSoLanKe(id));
    }

    private Thuoc khoa(Long id) {
        return thuocRepository.findByIdForUpdate(id).orElseThrow(() -> new LoiKhongTimThay("Thuoc", id));
    }

    /** Tên (trừ hoa thường, khoảng trắng), hoạt chất, hàm lượng, dạng bào chế, đường dùng. */
    private static boolean doiNhanDang(Thuoc thuoc, ThuocRequest request) {
        return !ThuocServiceImpl.normalize(request.tenThuoc()).equals(thuoc.getTenChuanHoa())
                || khacGiaTriCu(thuoc.getHoatChat(), request.hoatChat())
                || khacGiaTriCu(thuoc.getHamLuong(), request.hamLuong())
                || khacGiaTriCu(thuoc.getDangBaoChe(), request.dangBaoChe())
                || (thuoc.getDuongDung() != null && thuoc.getDuongDung() != request.duongDung());
    }

    private static boolean khacGiaTriCu(String cu, String moi) {
        return cu != null && !Objects.equals(ThuocServiceImpl.normalize(cu),
                moi == null || moi.isBlank() ? null : ThuocServiceImpl.normalize(moi));
    }

    /** Mọi trường quản trị viên nhập, trừ {@code tenChuanHoa}. */
    private static void ganNoiDung(Thuoc thuoc, ThuocRequest request) {
        thuoc.setTenThuoc(ThuocServiceImpl.collapseWhitespace(request.tenThuoc()));
        thuoc.setHoatChat(ThuocServiceImpl.collapseWhitespace(request.hoatChat()));
        thuoc.setHamLuong(ThuocServiceImpl.collapseWhitespace(request.hamLuong()));
        thuoc.setHamLuongChuanHoa(ThuocServiceImpl.normalize(request.hamLuong()));
        thuoc.setDangBaoChe(rongThanhNull(request.dangBaoChe()));
        thuoc.setDangBaoCheEn(rongThanhNull(request.dangBaoCheEn()));
        thuoc.setDuongDung(request.duongDung());
        thuoc.setDonVi(rongThanhNull(request.donVi()));
        thuoc.setDonViEn(rongThanhNull(request.donViEn()));
        thuoc.setMoTa(rongThanhNull(request.moTa()));
        thuoc.setMoTaEn(rongThanhNull(request.moTaEn()));
    }

    private static ThuocQuanTriResponse toResponse(Thuoc thuoc, long soLanKe) {
        return new ThuocQuanTriResponse(thuoc.getId(), thuoc.getTenThuoc(), thuoc.getHoatChat(), thuoc.getHamLuong(),
                thuoc.getDangBaoChe(), thuoc.getDangBaoCheEn(), thuoc.getDuongDung(), thuoc.getDonVi(), thuoc.getDonViEn(),
                thuoc.getMoTa(), thuoc.getMoTaEn(), thuoc.isDaXacMinh(), thuoc.getTrangThai(),
                thuoc.getBacSiTao() == null ? null : thuoc.getBacSiTao().getTaiKhoan().getHoTen(), soLanKe);
    }

    private static String rongThanhNull(String chuoi) {
        return chuoi == null || chuoi.isBlank() ? null : chuoi.trim();
    }
}
