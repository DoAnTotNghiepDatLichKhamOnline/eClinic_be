package iuh.fit.se.eclinic.booking.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.config.LichLamViecProperties;
import iuh.fit.se.eclinic.booking.dto.request.GuiYeuCauDoiLichRequest;
import iuh.fit.se.eclinic.booking.dto.response.CaLamViecResponse;
import iuh.fit.se.eclinic.booking.dto.response.YeuCauDoiLichResponse;
import iuh.fit.se.eclinic.booking.mapper.CaKhamMapper;
import iuh.fit.se.eclinic.booking.mapper.LichLamViecMapper;
import iuh.fit.se.eclinic.booking.repository.LichLamViecRepository;
import iuh.fit.se.eclinic.booking.repository.LichLamViecRepository.SoLuotCuaCa;
import iuh.fit.se.eclinic.booking.repository.PhongKhamChiDocRepository;
import iuh.fit.se.eclinic.booking.repository.YeuCauDoiLichRepository;
import iuh.fit.se.eclinic.booking.service.QuanLyCaService;
import iuh.fit.se.eclinic.booking.service.TaiKhoanService;
import iuh.fit.se.eclinic.booking.service.ThongBaoLichLamViecService;
import iuh.fit.se.eclinic.booking.service.YeuCauDoiLichService;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.catalog.PhongKham;
import iuh.fit.se.eclinic.common.entity.identity.QuanTriVien;
import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;
import iuh.fit.se.eclinic.common.entity.scheduling.YeuCauDoiLich;
import iuh.fit.se.eclinic.common.enums.LoaiYeuCauDoiLich;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichLamViec;
import iuh.fit.se.eclinic.common.enums.TrangThaiPhongKham;
import iuh.fit.se.eclinic.common.enums.TrangThaiYeuCau;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Thứ tự khoá khi duyệt / từ chối: dòng ca trước, dòng yêu cầu sau (giống hủy ca trực tiếp, nơi ca được khoá rồi yêu cầu
 * đang chờ của ca mới bị đóng). Bác sĩ rút yêu cầu chỉ khoá dòng yêu cầu.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YeuCauDoiLichServiceImpl implements YeuCauDoiLichService {

    private final YeuCauDoiLichRepository yeuCauDoiLichRepository;
    private final LichLamViecRepository lichLamViecRepository;
    private final PhongKhamChiDocRepository phongKhamChiDocRepository;
    private final TaiKhoanService taiKhoanService;
    private final QuanLyCaService quanLyCaService;
    private final ThongBaoLichLamViecService thongBaoLichLamViecService;
    private final LichLamViecProperties lichLamViecProperties;
    private final LichLamViecMapper lichLamViecMapper;
    private final CaKhamMapper caKhamMapper;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public YeuCauDoiLichResponse gui(Long idTaiKhoanBacSi, GuiYeuCauDoiLichRequest request) {
        BacSi bacSi = taiKhoanService.layBacSiDangHoatDong(idTaiKhoanBacSi);
        // Ca không có và ca của bác sĩ khác trả cùng 1 lỗi
        LichLamViec ca = lichLamViecRepository.timTheoIdKemBacSiVaPhongKham(request.idLichLamViec())
                .filter(timThay -> timThay.getBacSi().getId().equals(bacSi.getId()))
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.KHONG_TIM_THAY, "Không tìm thấy ca làm việc"));
        if (ca.getTrangThai() != TrangThaiLichLamViec.HOAT_DONG) {
            throw new LoiNghiepVu(MaLoi.CA_KHONG_SUA_DUOC);
        }
        LocalDateTime hanChot = LocalDateTime.now().plus(lichLamViecProperties.guiYeuCauTruocToiThieu());
        if (ca.getNgayLamViec().atTime(ca.getGioBatDau()).isBefore(hanChot)) {
            throw new LoiNghiepVu(MaLoi.QUA_HAN_GUI_YEU_CAU, "Chỉ gửi được yêu cầu trước giờ bắt đầu ca ít nhất "
                    + lichLamViecProperties.guiYeuCauTruocToiThieu().toHours() + " giờ");
        }

        YeuCauDoiLich yeuCau = new YeuCauDoiLich();
        yeuCau.setBacSi(bacSi);
        yeuCau.setLichLamViec(ca);
        yeuCau.setLoaiYeuCau(request.loaiYeuCau());
        yeuCau.setLyDo(request.lyDo().trim());
        if (request.loaiYeuCau() == LoaiYeuCauDoiLich.DOI_CA) {
            dienMongMuon(yeuCau, ca, request, hanChot);
        }
        if (yeuCauDoiLichRepository.existsByLichLamViecIdAndTrangThai(ca.getId(), TrangThaiYeuCau.CHO_DUYET)) {
            throw new LoiNghiepVu(MaLoi.CA_DA_CO_YEU_CAU_CHO_DUYET);
        }
        try {
            yeuCauDoiLichRepository.saveAndFlush(yeuCau);
        } catch (DataIntegrityViolationException ex) {
            // 2 lần gửi cùng lúc: UNIQUE uk_yeu_cau_doi_lich_cho_duyet chặn lần sau
            throw new LoiNghiepVu(MaLoi.CA_DA_CO_YEU_CAU_CHO_DUYET);
        }
        thongBaoLichLamViecService.yeuCauMoi(yeuCau);
        log.info("Bác sĩ id={} gửi yêu cầu {} id={} cho ca id={}", bacSi.getId(), yeuCau.getLoaiYeuCau(),
                yeuCau.getId(), ca.getId());
        return toResponse(yeuCau);
    }

    private void dienMongMuon(YeuCauDoiLich yeuCau, LichLamViec ca, GuiYeuCauDoiLichRequest request,
            LocalDateTime hanChot) {
        if (request.ngayMongMuon() == null || request.gioBatDauMongMuon() == null
                || request.gioKetThucMongMuon() == null) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE,
                    "Yêu cầu đổi ca cần ngayMongMuon, gioBatDauMongMuon, gioKetThucMongMuon");
        }
        if (!request.gioKetThucMongMuon().isAfter(request.gioBatDauMongMuon())) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "Giờ kết thúc mong muốn phải sau giờ bắt đầu");
        }
        if (request.ngayMongMuon().atTime(request.gioBatDauMongMuon()).isBefore(hanChot)) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "Ca mong muốn phải cách hiện tại ít nhất "
                    + lichLamViecProperties.guiYeuCauTruocToiThieu().toHours() + " giờ");
        }
        if (request.ngayMongMuon().isAfter(LocalDateTime.now().toLocalDate()
                .plusDays(lichLamViecProperties.soNgayXepTruocToiDa()))) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "Chỉ xếp được ca trong "
                    + lichLamViecProperties.soNgayXepTruocToiDa() + " ngày tới");
        }
        PhongKham phongMongMuon = null;
        Long idPhong = request.idPhongKhamMongMuon();
        if (idPhong != null && !idPhong.equals(ca.getPhongKham().getId())) {
            phongMongMuon = phongKhamChiDocRepository.findById(idPhong)
                    .filter(phong -> phong.getTrangThai() == TrangThaiPhongKham.HOAT_DONG
                            && phong.getChuyenKhoa().getId().equals(ca.getBacSi().getChuyenKhoa().getId()))
                    .orElseThrow(() -> new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE,
                            "Phòng khám mong muốn không có, đã ngừng hoạt động hoặc không thuộc chuyên khoa của bạn"));
        }
        boolean khongDoiGi = phongMongMuon == null && request.ngayMongMuon().equals(ca.getNgayLamViec())
                && request.gioBatDauMongMuon().equals(ca.getGioBatDau())
                && request.gioKetThucMongMuon().equals(ca.getGioKetThuc());
        if (khongDoiGi) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "Ca mong muốn phải khác ca hiện tại");
        }
        yeuCau.setNgayMongMuon(request.ngayMongMuon());
        yeuCau.setGioBatDauMongMuon(request.gioBatDauMongMuon());
        yeuCau.setGioKetThucMongMuon(request.gioKetThucMongMuon());
        yeuCau.setPhongKhamMongMuon(phongMongMuon);
    }

    @Override
    @Transactional(readOnly = true)
    public TrangDuLieu<YeuCauDoiLichResponse> cuaToi(Long idTaiKhoanBacSi, TrangThaiYeuCau trangThai, int trang,
            int kichThuoc) {
        BacSi bacSi = taiKhoanService.layBacSiDangHoatDong(idTaiKhoanBacSi);
        return toTrang(yeuCauDoiLichRepository.timCuaBacSi(bacSi.getId(), trangThai, PageRequest.of(trang, kichThuoc)));
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public YeuCauDoiLichResponse rut(Long idTaiKhoanBacSi, Long idYeuCau) {
        Long idBacSi = taiKhoanService.layBacSiDangHoatDong(idTaiKhoanBacSi).getId();
        YeuCauDoiLich yeuCau = yeuCauDoiLichRepository.findByIdForUpdate(idYeuCau)
                .filter(timThay -> timThay.getBacSi().getId().equals(idBacSi))
                .orElseThrow(YeuCauDoiLichServiceImpl::khongTimThay);
        if (yeuCau.getTrangThai() != TrangThaiYeuCau.CHO_DUYET) {
            throw new LoiNghiepVu(MaLoi.YEU_CAU_DA_XU_LY);
        }
        yeuCau.setTrangThai(TrangThaiYeuCau.DA_RUT);
        yeuCau.setNgayXuLy(LocalDateTime.now());
        log.info("Bác sĩ id={} rút yêu cầu id={}", idBacSi, idYeuCau);
        return toResponse(yeuCau);
    }

    @Override
    @Transactional(readOnly = true)
    public TrangDuLieu<YeuCauDoiLichResponse> danhSach(TrangThaiYeuCau trangThai, int trang, int kichThuoc) {
        return toTrang(yeuCauDoiLichRepository.timTatCa(trangThai, PageRequest.of(trang, kichThuoc)));
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public YeuCauDoiLichResponse duyet(Long idTaiKhoanQuanTri, Long idYeuCau, String ghiChu) {
        QuanTriVien quanTri = quanLyCaService.layQuanTriVien(idTaiKhoanQuanTri);
        Long idCa = idCaCuaYeuCau(idYeuCau);
        // Khoá ca trước, yêu cầu sau. Yêu cầu đã xử lý thì báo YEU_CAU_DA_XU_LY chứ không báo lỗi của ca
        LichLamViec ca = lichLamViecRepository.findByIdForUpdate(idCa).orElseThrow(YeuCauDoiLichServiceImpl::khongTimThay);
        YeuCauDoiLich yeuCau = khoaYeuCauChoDuyet(idYeuCau);
        quanLyCaService.khoaCaConSuaDuoc(idCa);

        if (yeuCau.getLoaiYeuCau() == LoaiYeuCauDoiLich.XIN_NGHI) {
            quanLyCaService.huyCaDaKhoa(ca, quanTri, "Bác sĩ nghỉ ca này.", idYeuCau);
        } else {
            Long idPhongMoi = yeuCau.getPhongKhamMongMuon() == null ? ca.getPhongKham().getId()
                    : yeuCau.getPhongKhamMongMuon().getId();
            boolean chiDoiPhong = yeuCau.getNgayMongMuon().equals(ca.getNgayLamViec())
                    && yeuCau.getGioBatDauMongMuon().equals(ca.getGioBatDau())
                    && yeuCau.getGioKetThucMongMuon().equals(ca.getGioKetThuc());
            if (chiDoiPhong) {
                quanLyCaService.suaCaDaKhoa(ca, idPhongMoi, ca.getGioBatDau(), ca.getGioKetThuc(),
                        ca.getSoLuotToiDaMoiGio(), ca.getThoiLuongLuotPhut());
            } else {
                quanLyCaService.huyCaDaKhoa(ca, quanTri, "Bác sĩ đổi sang ca khác.", idYeuCau);
                // Ca cũ đã DA_HUY nên không tự trùng với ca mới (kiểm tra trùng chỉ xét ca còn hoạt động)
                lichLamViecRepository.flush();
                quanLyCaService.taoCaMoi(ca.getBacSi().getId(), idPhongMoi, quanTri, yeuCau.getNgayMongMuon(),
                        yeuCau.getGioBatDauMongMuon(), yeuCau.getGioKetThucMongMuon(), ca.getSoLuotToiDaMoiGio(),
                        ca.getThoiLuongLuotPhut());
            }
        }
        dong(yeuCau, TrangThaiYeuCau.DA_DUYET, quanTri, ghiChu);
        log.info("Quản trị viên id={} duyệt yêu cầu {} id={} của ca id={}", quanTri.getId(), yeuCau.getLoaiYeuCau(),
                idYeuCau, idCa);
        return toResponse(yeuCau);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public YeuCauDoiLichResponse tuChoi(Long idTaiKhoanQuanTri, Long idYeuCau, String ghiChu) {
        QuanTriVien quanTri = quanLyCaService.layQuanTriVien(idTaiKhoanQuanTri);
        if (ghiChu == null || ghiChu.isBlank()) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "Từ chối yêu cầu phải có ghi chú");
        }
        Long idCa = idCaCuaYeuCau(idYeuCau);
        lichLamViecRepository.findByIdForUpdate(idCa).orElseThrow(YeuCauDoiLichServiceImpl::khongTimThay);
        YeuCauDoiLich yeuCau = khoaYeuCauChoDuyet(idYeuCau);
        dong(yeuCau, TrangThaiYeuCau.TU_CHOI, quanTri, ghiChu);
        log.info("Quản trị viên id={} từ chối yêu cầu id={}", quanTri.getId(), idYeuCau);
        return toResponse(yeuCau);
    }

    /** Đọc không khoá, chỉ để biết phải khoá ca nào trước. */
    private Long idCaCuaYeuCau(Long idYeuCau) {
        return yeuCauDoiLichRepository.timIdLichLamViec(idYeuCau).orElseThrow(YeuCauDoiLichServiceImpl::khongTimThay);
    }

    private YeuCauDoiLich khoaYeuCauChoDuyet(Long idYeuCau) {
        YeuCauDoiLich yeuCau = yeuCauDoiLichRepository.findByIdForUpdate(idYeuCau)
                .orElseThrow(YeuCauDoiLichServiceImpl::khongTimThay);
        if (yeuCau.getTrangThai() != TrangThaiYeuCau.CHO_DUYET) {
            throw new LoiNghiepVu(MaLoi.YEU_CAU_DA_XU_LY);
        }
        return yeuCau;
    }

    private void dong(YeuCauDoiLich yeuCau, TrangThaiYeuCau trangThai, QuanTriVien quanTri, String ghiChu) {
        yeuCau.setTrangThai(trangThai);
        yeuCau.setAdminXuLy(quanTri);
        yeuCau.setGhiChuXuLy(ghiChu == null || ghiChu.isBlank() ? null : ghiChu.trim());
        yeuCau.setNgayXuLy(LocalDateTime.now());
        thongBaoLichLamViecService.ketQuaYeuCau(yeuCau);
    }

    private static LoiNghiepVu khongTimThay() {
        return new LoiNghiepVu(MaLoi.KHONG_TIM_THAY, "Không tìm thấy yêu cầu");
    }

    private YeuCauDoiLichResponse toResponse(YeuCauDoiLich yeuCau) {
        yeuCauDoiLichRepository.flush();
        return toResponse(yeuCau, soLuotTheoCa(List.of(yeuCau)));
    }

    private TrangDuLieu<YeuCauDoiLichResponse> toTrang(Page<YeuCauDoiLich> trang) {
        Map<Long, SoLuotCuaCa> soLuot = soLuotTheoCa(trang.getContent());
        return TrangDuLieu.tu(trang.map(yeuCau -> toResponse(yeuCau, soLuot)));
    }

    private Map<Long, SoLuotCuaCa> soLuotTheoCa(List<YeuCauDoiLich> cacYeuCau) {
        if (cacYeuCau.isEmpty()) {
            return Map.of();
        }
        return lichLamViecRepository
                .demLuotTheoCa(cacYeuCau.stream().map(yeuCau -> yeuCau.getLichLamViec().getId()).distinct().toList())
                .stream().collect(Collectors.toMap(SoLuotCuaCa::getIdLichLamViec, Function.identity()));
    }

    private YeuCauDoiLichResponse toResponse(YeuCauDoiLich yeuCau, Map<Long, SoLuotCuaCa> soLuot) {
        LichLamViec ca = yeuCau.getLichLamViec();
        CaLamViecResponse caResponse = lichLamViecMapper.toResponse(ca, soLuot.get(ca.getId()));
        return new YeuCauDoiLichResponse(yeuCau.getId(), yeuCau.getLoaiYeuCau(), yeuCau.getTrangThai(),
                yeuCau.getLyDo(), caResponse, yeuCau.getNgayMongMuon(), yeuCau.getGioBatDauMongMuon(),
                yeuCau.getGioKetThucMongMuon(),
                yeuCau.getPhongKhamMongMuon() == null ? null
                        : caKhamMapper.toPhongKhamTomTat(yeuCau.getPhongKhamMongMuon()),
                yeuCau.getGhiChuXuLy(), yeuCau.getNgayGui(), yeuCau.getNgayXuLy());
    }

}
