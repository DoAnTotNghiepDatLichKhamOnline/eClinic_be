package iuh.fit.se.eclinic.medical.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
import iuh.fit.se.eclinic.medical.service.ThuocService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThuocServiceImpl implements ThuocService {

    private static final int SO_GOI_Y_TOI_DA = 20;

    private final ThuocRepository thuocRepository;

    @Override
    public Map<Long, Thuoc> layDeKe(List<Long> idCacThuoc, Set<Long> idThuocDaCoTrongDon) {
        if (idCacThuoc.isEmpty()) {
            return Map.of();
        }
        // Không khoá: quản trị viên cho ngừng dùng đúng lúc bác sĩ đang lưu thì đơn đó vẫn giữ thuốc, như 1 đơn cũ
        Map<Long, Thuoc> theoId = thuocRepository.findAllById(idCacThuoc).stream()
                .collect(Collectors.toMap(Thuoc::getId, Function.identity()));
        for (Long id : idCacThuoc) {
            Thuoc thuoc = theoId.get(id);
            if (thuoc == null) {
                throw new LoiNghiepVu(MaLoi.KHONG_TIM_THAY, "loi.KHONG_TIM_THAY.thuoc", id);
            }
            if (thuoc.keDuoc() || idThuocDaCoTrongDon.contains(id)) {
                continue;
            }
            if (thuoc.getTrangThai() == TrangThaiThuoc.NGUNG_DUNG) {
                throw new LoiNghiepVu(MaLoi.THUOC_NGUNG_DUNG, "loi.THUOC_NGUNG_DUNG.ten", thuoc.getTenThuoc());
            }
            throw new LoiNghiepVu(MaLoi.THUOC_CHUA_XAC_MINH, "loi.THUOC_CHUA_XAC_MINH.ten", thuoc.getTenThuoc());
        }
        return theoId;
    }

    @Override
    public List<Thuoc> timKiem(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        return thuocRepository.goiYKeDon(MauTimKiem.chua(normalize(keyword)), PageRequest.of(0, SO_GOI_Y_TOI_DA));
    }

    static String normalize(String ten) {
        return collapseWhitespace(ten).toLowerCase(Locale.ROOT);
    }

    static String collapseWhitespace(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    @Override
    public TrangDuLieu<ThuocQuanTriResponse> danhSach(String tuKhoa, Boolean daXacMinh, TrangThaiThuoc trangThai,
            int trang, int kichThuoc) {
        String mau = tuKhoa == null || tuKhoa.isBlank() ? null : MauTimKiem.chua(normalize(tuKhoa));
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
        String tenChuanHoa = normalize(request.tenThuoc());
        String hamLuongChuanHoa = normalize(request.hamLuong());
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
        String tenChuanHoa = normalize(request.tenThuoc());
        String hamLuongChuanHoa = normalize(request.hamLuong());
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
        return !normalize(request.tenThuoc()).equals(thuoc.getTenChuanHoa())
                || khacGiaTriCu(thuoc.getHoatChat(), request.hoatChat())
                || khacGiaTriCu(thuoc.getHamLuong(), request.hamLuong())
                || khacGiaTriCu(thuoc.getDangBaoChe(), request.dangBaoChe())
                || (thuoc.getDuongDung() != null && thuoc.getDuongDung() != request.duongDung());
    }

    private static boolean khacGiaTriCu(String cu, String moi) {
        return cu != null && !Objects.equals(normalize(cu),
                moi == null || moi.isBlank() ? null : normalize(moi));
    }

    /** Mọi trường quản trị viên nhập, trừ {@code tenChuanHoa}. */
    private static void ganNoiDung(Thuoc thuoc, ThuocRequest request) {
        thuoc.setTenThuoc(collapseWhitespace(request.tenThuoc()));
        thuoc.setHoatChat(collapseWhitespace(request.hoatChat()));
        thuoc.setHamLuong(collapseWhitespace(request.hamLuong()));
        thuoc.setHamLuongChuanHoa(normalize(request.hamLuong()));
        thuoc.setDangBaoChe(rongThanhNull(request.dangBaoChe()));
        thuoc.setDangBaoCheEn(rongThanhNull(request.dangBaoCheEn()));
        thuoc.setDuongDung(request.duongDung());
        thuoc.setDonVi(rongThanhNull(request.donVi()));
        thuoc.setDonViEn(rongThanhNull(request.donViEn()));
        thuoc.setMoTa(rongThanhNull(request.moTa()));
        thuoc.setMoTaEn(rongThanhNull(request.moTaEn()));
        thuoc.setCoBaoHiem(request.coBaoHiem());
        // Đã kiểm tra không có phần thập phân: 1500.0 -> 1500, để phản hồi không trả "1500.0"
        thuoc.setDonGia(request.donGia().setScale(0));
    }

    private static ThuocQuanTriResponse toResponse(Thuoc thuoc, long soLanKe) {
        return new ThuocQuanTriResponse(thuoc.getId(), thuoc.getTenThuoc(), thuoc.getHoatChat(), thuoc.getHamLuong(),
                thuoc.getDangBaoChe(), thuoc.getDangBaoCheEn(), thuoc.getDuongDung(), thuoc.getDonVi(), thuoc.getDonViEn(),
                thuoc.getMoTa(), thuoc.getMoTaEn(), thuoc.isDaXacMinh(), thuoc.getTrangThai(),
                thuoc.isCoBaoHiem(), thuoc.getDonGia(),
                thuoc.getBacSiTao() == null ? null : thuoc.getBacSiTao().getTaiKhoan().getHoTen(), soLanKe);
    }

    private static String rongThanhNull(String chuoi) {
        return chuoi == null || chuoi.isBlank() ? null : chuoi.trim();
    }

}
