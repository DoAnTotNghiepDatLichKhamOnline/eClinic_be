package iuh.fit.se.eclinic.booking.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.config.LichLamViecProperties;
import iuh.fit.se.eclinic.booking.dto.request.GuiYeuCauDoiLichRequest;
import iuh.fit.se.eclinic.booking.dto.request.XinNghiNhieuCaRequest;
import iuh.fit.se.eclinic.booking.dto.response.CaLamViecResponse;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaXuLyNhomResponse;
import iuh.fit.se.eclinic.booking.dto.response.YeuCauDoiLichResponse;
import iuh.fit.se.eclinic.booking.mapper.CaKhamMapper;
import iuh.fit.se.eclinic.booking.mapper.LichLamViecMapper;
import iuh.fit.se.eclinic.booking.repository.BacSiChiDocRepository;
import iuh.fit.se.eclinic.booking.repository.LichLamViecRepository;
import iuh.fit.se.eclinic.booking.repository.LichLamViecRepository.SoLuotCuaCa;
import iuh.fit.se.eclinic.booking.repository.PhongKhamChiDocRepository;
import iuh.fit.se.eclinic.booking.repository.YeuCauDoiLichRepository;
import iuh.fit.se.eclinic.booking.repository.YeuCauDoiLichRepository.ThanhVienNhom;
import iuh.fit.se.eclinic.booking.service.LyDoHuyCa;
import iuh.fit.se.eclinic.booking.service.NguonThayDoi;
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
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.enums.TrangThaiBacSi;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichLamViec;
import iuh.fit.se.eclinic.common.enums.TrangThaiPhongKham;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiYeuCau;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.thongbao.VanBanHeThong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Thứ tự khoá khi duyệt / từ chối / bác sĩ nhận đồng ý: dòng ca trước, dòng yêu cầu sau (giống hủy ca trực tiếp, nơi ca
 * được khoá rồi yêu cầu đang chờ của ca mới bị đóng). Cả nhóm: mọi ca theo id tăng dần, rồi mọi yêu cầu cùng thứ tự đó.
 * Bác sĩ rút yêu cầu, bác sĩ nhận từ chối chỉ khoá dòng yêu cầu.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YeuCauDoiLichServiceImpl implements YeuCauDoiLichService {

    private static final DateTimeFormatter NGAY_GIO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final YeuCauDoiLichRepository yeuCauDoiLichRepository;
    private final LichLamViecRepository lichLamViecRepository;
    private final PhongKhamChiDocRepository phongKhamChiDocRepository;
    private final BacSiChiDocRepository bacSiChiDocRepository;
    private final TaiKhoanService taiKhoanService;
    private final QuanLyCaService quanLyCaService;
    private final ThongBaoLichLamViecService thongBaoLichLamViecService;
    private final LichLamViecProperties lichLamViecProperties;
    private final LichLamViecMapper lichLamViecMapper;
    private final CaKhamMapper caKhamMapper;
    private final MessageSource messageSource;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public YeuCauDoiLichResponse gui(Long idTaiKhoanBacSi, GuiYeuCauDoiLichRequest request) {
        BacSi bacSi = taiKhoanService.layBacSiDangHoatDong(idTaiKhoanBacSi);
        LocalDateTime hanChot = hanChotGui();
        LichLamViec ca = timCaCuaToi(bacSi, request.idLichLamViec());
        kiemTraCaGuiDuoc(ca, hanChot);

        YeuCauDoiLich yeuCau = yeuCauMoi(bacSi, ca, request.loaiYeuCau(), request.lyDo());
        if (request.loaiYeuCau() == LoaiYeuCauDoiLich.DOI_CA) {
            dienMongMuon(yeuCau, ca, request, hanChot);
        } else if (request.loaiYeuCau() == LoaiYeuCauDoiLich.NHUONG_CA) {
            BacSi bacSiNhan = bacSiNhanHopLe(bacSi, ca, request.idBacSiNhan());
            // Báo sớm; lúc quản trị viên duyệt mới kiểm tra thật (sau khi khoá bác sĩ nhận)
            quanLyCaService.kiemTraNhanCa(bacSiNhan.getId(), ca);
            yeuCau.setBacSiNhan(bacSiNhan);
            yeuCau.setTrangThai(TrangThaiYeuCau.CHO_DONG_Y);
        }
        luu(yeuCau);
        if (yeuCau.getLoaiYeuCau() == LoaiYeuCauDoiLich.NHUONG_CA) {
            thongBaoLichLamViecService.nhuongCaMoi(yeuCau);
        } else {
            thongBaoLichLamViecService.yeuCauMoi(yeuCau);
        }
        log.info("Bác sĩ id={} gửi yêu cầu {} id={} cho ca id={}", bacSi.getId(), yeuCau.getLoaiYeuCau(),
                yeuCau.getId(), ca.getId());
        return toResponse(yeuCau);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<YeuCauDoiLichResponse> xinNghiNhieuCa(Long idTaiKhoanBacSi, XinNghiNhieuCaRequest request) {
        BacSi bacSi = taiKhoanService.layBacSiDangHoatDong(idTaiKhoanBacSi);
        LocalDateTime hanChot = hanChotGui();
        List<LichLamViec> cacCa = new ArrayList<>();
        for (Long idCa : new LinkedHashSet<>(request.cacIdLichLamViec())) {
            LichLamViec ca = null;
            try {
                ca = timCaCuaToi(bacSi, idCa);
                kiemTraCaGuiDuoc(ca, hanChot);
            } catch (LoiNghiepVu loi) {
                // Giữ mã lỗi, câu lỗi nêu ca nào (ca không tìm thấy thì nêu id)
                String tenCa = ca == null ? "#" + idCa : NGAY_GIO.format(ca.getNgayLamViec().atTime(ca.getGioBatDau()));
                throw new LoiNghiepVu(loi.getMaLoi(), "loi.yeuCauNhieuCa.ca", tenCa, messageSource.getMessage(
                        loi.getKhoaThongDiep(), loi.getThamSo(), loi.getKhoaThongDiep(), NgonNgu.hienTai().locale()));
            }
            cacCa.add(ca);
        }
        cacCa.sort(Comparator.comparing(LichLamViec::getNgayLamViec).thenComparing(LichLamViec::getGioBatDau));

        String maNhom = UUID.randomUUID().toString();
        List<YeuCauDoiLich> cacYeuCau = new ArrayList<>();
        for (LichLamViec ca : cacCa) {
            YeuCauDoiLich yeuCau = yeuCauMoi(bacSi, ca, LoaiYeuCauDoiLich.XIN_NGHI, request.lyDo());
            yeuCau.setMaNhom(maNhom);
            luu(yeuCau);
            cacYeuCau.add(yeuCau);
        }
        thongBaoLichLamViecService.yeuCauMoiNhieuCa(cacYeuCau);
        log.info("Bác sĩ id={} xin nghỉ {} ca, nhóm {}", bacSi.getId(), cacYeuCau.size(), maNhom);
        return toResponses(cacYeuCau);
    }

    private LocalDateTime hanChotGui() {
        return LocalDateTime.now().plus(lichLamViecProperties.guiYeuCauTruocToiThieu());
    }

    /** Ca không có và ca của bác sĩ khác trả cùng 1 lỗi. */
    private LichLamViec timCaCuaToi(BacSi bacSi, Long idLichLamViec) {
        return lichLamViecRepository.timTheoIdKemBacSiVaPhongKham(idLichLamViec)
                .filter(timThay -> timThay.getBacSi().getId().equals(bacSi.getId()))
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.KHONG_TIM_THAY, "loi.KHONG_TIM_THAY.caLamViec"));
    }

    /** Ca còn hoạt động, còn đủ xa giờ bắt đầu, chưa có yêu cầu nào còn chờ. */
    private void kiemTraCaGuiDuoc(LichLamViec ca, LocalDateTime hanChot) {
        if (ca.getTrangThai() != TrangThaiLichLamViec.HOAT_DONG) {
            throw new LoiNghiepVu(MaLoi.CA_KHONG_SUA_DUOC);
        }
        if (ca.getNgayLamViec().atTime(ca.getGioBatDau()).isBefore(hanChot)) {
            throw new LoiNghiepVu(MaLoi.QUA_HAN_GUI_YEU_CAU, "loi.QUA_HAN_GUI_YEU_CAU.truocGio",
                    String.valueOf(lichLamViecProperties.guiYeuCauTruocToiThieu().toHours()));
        }
        if (yeuCauDoiLichRepository.existsByLichLamViecIdAndTrangThaiIn(ca.getId(), TrangThaiYeuCau.DANG_CHO)) {
            throw new LoiNghiepVu(MaLoi.CA_DA_CO_YEU_CAU_CHO_DUYET);
        }
    }

    private static YeuCauDoiLich yeuCauMoi(BacSi bacSi, LichLamViec ca, LoaiYeuCauDoiLich loai, String lyDo) {
        YeuCauDoiLich yeuCau = new YeuCauDoiLich();
        yeuCau.setBacSi(bacSi);
        yeuCau.setLichLamViec(ca);
        yeuCau.setLoaiYeuCau(loai);
        yeuCau.setLyDo(lyDo.trim());
        return yeuCau;
    }

    private void luu(YeuCauDoiLich yeuCau) {
        try {
            yeuCauDoiLichRepository.saveAndFlush(yeuCau);
        } catch (DataIntegrityViolationException ex) {
            // 2 lần gửi cùng lúc: UNIQUE uk_yeu_cau_doi_lich_cho_duyet chặn lần sau
            throw new LoiNghiepVu(MaLoi.CA_DA_CO_YEU_CAU_CHO_DUYET);
        }
    }

    /** Bác sĩ khác mình, đang công tác (tài khoản đã kích hoạt), cùng chuyên khoa với phòng của ca. */
    private BacSi bacSiNhanHopLe(BacSi bacSi, LichLamViec ca, Long idBacSiNhan) {
        if (idBacSiNhan == null || idBacSiNhan.equals(bacSi.getId())) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.bacSiNhan");
        }
        Long idChuyenKhoa = ca.getPhongKham().getChuyenKhoa().getId();
        return bacSiChiDocRepository.timTheoCacId(List.of(idBacSiNhan)).stream()
                .filter(nhan -> nhan.getTrangThai() == TrangThaiBacSi.DANG_CONG_TAC
                        && nhan.getTaiKhoan().getTrangThai() == TrangThaiTaiKhoan.DA_KICH_HOAT
                        && nhan.getChuyenKhoa().getId().equals(idChuyenKhoa))
                .findFirst()
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.bacSiNhan"));
    }

    private void dienMongMuon(YeuCauDoiLich yeuCau, LichLamViec ca, GuiYeuCauDoiLichRequest request,
            LocalDateTime hanChot) {
        if (request.ngayMongMuon() == null || request.gioBatDauMongMuon() == null
                || request.gioKetThucMongMuon() == null) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE,
                    "loi.DU_LIEU_KHONG_HOP_LE.doiCaThieuThongTin");
        }
        if (!request.gioKetThucMongMuon().isAfter(request.gioBatDauMongMuon())) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.gioKetThucMongMuon");
        }
        if (request.ngayMongMuon().atTime(request.gioBatDauMongMuon()).isBefore(hanChot)) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.caMongMuonQuaGan", String.valueOf(lichLamViecProperties.guiYeuCauTruocToiThieu().toHours()));
        }
        if (request.ngayMongMuon().isAfter(LocalDateTime.now().toLocalDate()
                .plusDays(lichLamViecProperties.soNgayXepTruocToiDa()))) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.caQuaXa", String.valueOf(lichLamViecProperties.soNgayXepTruocToiDa()));
        }
        PhongKham phongMongMuon = null;
        Long idPhong = request.idPhongKhamMongMuon();
        if (idPhong != null && !idPhong.equals(ca.getPhongKham().getId())) {
            phongMongMuon = phongKhamChiDocRepository.findById(idPhong)
                    .filter(phong -> phong.getTrangThai() == TrangThaiPhongKham.HOAT_DONG
                            && phong.getChuyenKhoa().getId().equals(ca.getBacSi().getChuyenKhoa().getId()))
                    .orElseThrow(() -> new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE,
                            "loi.DU_LIEU_KHONG_HOP_LE.phongMongMuon"));
        }
        boolean khongDoiGi = phongMongMuon == null && request.ngayMongMuon().equals(ca.getNgayLamViec())
                && request.gioBatDauMongMuon().equals(ca.getGioBatDau())
                && request.gioKetThucMongMuon().equals(ca.getGioKetThuc());
        if (khongDoiGi) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.caMongMuonTrung");
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
        if (!TrangThaiYeuCau.DANG_CHO.contains(yeuCau.getTrangThai())) {
            throw new LoiNghiepVu(MaLoi.YEU_CAU_DA_XU_LY);
        }
        rutYeuCau(yeuCau);
        log.info("Bác sĩ id={} rút yêu cầu id={}", idBacSi, idYeuCau);
        return toResponse(yeuCau);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<YeuCauDoiLichResponse> rutNhom(Long idTaiKhoanBacSi, String maNhom) {
        Long idBacSi = taiKhoanService.layBacSiDangHoatDong(idTaiKhoanBacSi).getId();
        List<ThanhVienNhom> nhom = yeuCauDoiLichRepository.timTheoNhom(maNhom).stream()
                .filter(thanhVien -> thanhVien.getIdBacSi().equals(idBacSi))
                .toList();
        if (nhom.isEmpty()) {
            throw khongTimThay();
        }
        List<YeuCauDoiLich> daRut = new ArrayList<>();
        for (ThanhVienNhom thanhVien : nhom) {
            YeuCauDoiLich yeuCau = yeuCauDoiLichRepository.findByIdForUpdate(thanhVien.getIdYeuCau())
                    .orElseThrow(YeuCauDoiLichServiceImpl::khongTimThay);
            if (TrangThaiYeuCau.DANG_CHO.contains(yeuCau.getTrangThai())) {
                rutYeuCau(yeuCau);
                daRut.add(yeuCau);
            }
        }
        if (daRut.isEmpty()) {
            throw new LoiNghiepVu(MaLoi.YEU_CAU_DA_XU_LY);
        }
        log.info("Bác sĩ id={} rút {} yêu cầu của nhóm {}", idBacSi, daRut.size(), maNhom);
        return toResponses(daRut);
    }

    private void rutYeuCau(YeuCauDoiLich yeuCau) {
        yeuCau.setTrangThai(TrangThaiYeuCau.DA_RUT);
        yeuCau.setNgayXuLy(LocalDateTime.now());
        if (yeuCau.getLoaiYeuCau() == LoaiYeuCauDoiLich.NHUONG_CA) {
            thongBaoLichLamViecService.nhuongCaDaRut(yeuCau);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TrangDuLieu<YeuCauDoiLichResponse> guiChoToi(Long idTaiKhoanBacSi, TrangThaiYeuCau trangThai, int trang,
            int kichThuoc) {
        BacSi bacSi = taiKhoanService.layBacSiDangHoatDong(idTaiKhoanBacSi);
        return toTrang(
                yeuCauDoiLichRepository.timGuiChoBacSi(bacSi.getId(), trangThai, PageRequest.of(trang, kichThuoc)));
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public YeuCauDoiLichResponse dongY(Long idTaiKhoanBacSi, Long idYeuCau) {
        BacSi toi = taiKhoanService.layBacSiDangHoatDong(idTaiKhoanBacSi);
        Long idCa = idCaCuaYeuCau(idYeuCau);
        LichLamViec ca = lichLamViecRepository.findByIdForUpdate(idCa).orElseThrow(YeuCauDoiLichServiceImpl::khongTimThay);
        YeuCauDoiLich yeuCau = khoaYeuCauGuiChoToi(idYeuCau, toi.getId());
        if (ca.getTrangThai() != TrangThaiLichLamViec.HOAT_DONG
                || !ca.getNgayLamViec().atTime(ca.getGioBatDau()).isAfter(LocalDateTime.now())) {
            throw new LoiNghiepVu(MaLoi.CA_KHONG_SUA_DUOC);
        }
        quanLyCaService.kiemTraNhanCa(toi.getId(), ca);
        yeuCau.setTrangThai(TrangThaiYeuCau.CHO_DUYET);
        yeuCau.setNgayBacSiNhanPhanHoi(LocalDateTime.now());
        thongBaoLichLamViecService.dongNghiepPhanHoi(yeuCau);
        thongBaoLichLamViecService.yeuCauMoi(yeuCau);
        log.info("Bác sĩ id={} đồng ý nhận ca id={} (yêu cầu id={})", toi.getId(), idCa, idYeuCau);
        return toResponse(yeuCau);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public YeuCauDoiLichResponse tuChoiNhan(Long idTaiKhoanBacSi, Long idYeuCau, String ghiChu) {
        BacSi toi = taiKhoanService.layBacSiDangHoatDong(idTaiKhoanBacSi);
        YeuCauDoiLich yeuCau = khoaYeuCauGuiChoToi(idYeuCau, toi.getId());
        yeuCau.setTrangThai(TrangThaiYeuCau.DONG_NGHIEP_TU_CHOI);
        yeuCau.setGhiChuBacSiNhan(ghiChu == null || ghiChu.isBlank() ? null : ghiChu.trim());
        yeuCau.setNgayBacSiNhanPhanHoi(LocalDateTime.now());
        thongBaoLichLamViecService.dongNghiepPhanHoi(yeuCau);
        log.info("Bác sĩ id={} từ chối nhận ca (yêu cầu id={})", toi.getId(), idYeuCau);
        return toResponse(yeuCau);
    }

    /** Yêu cầu nhường ca gửi cho mình (của người khác: 404), còn chờ mình đồng ý. */
    private YeuCauDoiLich khoaYeuCauGuiChoToi(Long idYeuCau, Long idBacSi) {
        YeuCauDoiLich yeuCau = yeuCauDoiLichRepository.findByIdForUpdate(idYeuCau)
                .filter(timThay -> timThay.getBacSiNhan() != null && timThay.getBacSiNhan().getId().equals(idBacSi))
                .orElseThrow(YeuCauDoiLichServiceImpl::khongTimThay);
        if (yeuCau.getTrangThai() != TrangThaiYeuCau.CHO_DONG_Y) {
            throw new LoiNghiepVu(MaLoi.YEU_CAU_DA_XU_LY);
        }
        return yeuCau;
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

        NguonThayDoi nguon = NguonThayDoi.duyetYeuCau(idYeuCau);
        switch (yeuCau.getLoaiYeuCau()) {
            case XIN_NGHI -> quanLyCaService.huyCaDaKhoa(ca, quanTri, LyDoHuyCa.BAC_SI_NGHI_CA, nguon);
            case NHUONG_CA -> quanLyCaService.doiBacSiCaDaKhoa(ca, quanTri, yeuCau.getBacSiNhan().getId(), nguon);
            case DOI_CA -> duyetDoiCa(ca, yeuCau, quanTri, nguon);
        }
        dong(yeuCau, TrangThaiYeuCau.DA_DUYET, quanTri, ghiChu);
        thongBaoLichLamViecService.ketQuaYeuCau(yeuCau);
        log.info("Quản trị viên id={} duyệt yêu cầu {} id={} của ca id={}", quanTri.getId(), yeuCau.getLoaiYeuCau(),
                idYeuCau, idCa);
        return toResponse(yeuCau);
    }

    private void duyetDoiCa(LichLamViec ca, YeuCauDoiLich yeuCau, QuanTriVien quanTri, NguonThayDoi nguon) {
        Long idPhongMoi = yeuCau.getPhongKhamMongMuon() == null ? ca.getPhongKham().getId()
                : yeuCau.getPhongKhamMongMuon().getId();
        boolean chiDoiPhong = yeuCau.getNgayMongMuon().equals(ca.getNgayLamViec())
                && yeuCau.getGioBatDauMongMuon().equals(ca.getGioBatDau())
                && yeuCau.getGioKetThucMongMuon().equals(ca.getGioKetThuc());
        if (chiDoiPhong) {
            quanLyCaService.suaCaDaKhoa(ca, quanTri, idPhongMoi, ca.getGioBatDau(), ca.getGioKetThuc(),
                    ca.getSoLuotToiDaMoiGio(), ca.getThoiLuongLuotPhut(), nguon);
            return;
        }
        quanLyCaService.huyCaDaKhoa(ca, quanTri, LyDoHuyCa.BAC_SI_DOI_CA, nguon);
        // Ca cũ đã DA_HUY nên không tự trùng với ca mới (kiểm tra trùng chỉ xét ca còn hoạt động)
        lichLamViecRepository.flush();
        quanLyCaService.taoCaMoi(ca.getBacSi().getId(), idPhongMoi, quanTri, yeuCau.getNgayMongMuon(),
                yeuCau.getGioBatDauMongMuon(), yeuCau.getGioKetThucMongMuon(), ca.getSoLuotToiDaMoiGio(),
                ca.getThoiLuongLuotPhut(), nguon);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public YeuCauDoiLichResponse tuChoi(Long idTaiKhoanQuanTri, Long idYeuCau, String ghiChu) {
        QuanTriVien quanTri = quanLyCaService.layQuanTriVien(idTaiKhoanQuanTri);
        kiemTraCoGhiChuTuChoi(ghiChu);
        Long idCa = idCaCuaYeuCau(idYeuCau);
        lichLamViecRepository.findByIdForUpdate(idCa).orElseThrow(YeuCauDoiLichServiceImpl::khongTimThay);
        YeuCauDoiLich yeuCau = khoaYeuCauChoDuyet(idYeuCau);
        dong(yeuCau, TrangThaiYeuCau.TU_CHOI, quanTri, ghiChu);
        thongBaoLichLamViecService.ketQuaYeuCau(yeuCau);
        log.info("Quản trị viên id={} từ chối yêu cầu id={}", quanTri.getId(), idYeuCau);
        return toResponse(yeuCau);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public KetQuaXuLyNhomResponse duyetNhom(Long idTaiKhoanQuanTri, String maNhom, String ghiChu) {
        QuanTriVien quanTri = quanLyCaService.layQuanTriVien(idTaiKhoanQuanTri);
        NhomDaKhoa nhom = khoaNhom(maNhom);
        LocalDateTime bayGio = LocalDateTime.now();
        List<YeuCauDoiLich> daXuLy = new ArrayList<>();
        List<KetQuaXuLyNhomResponse.BoQua> boQua = new ArrayList<>();
        for (int i = 0; i < nhom.cacYeuCau().size(); i++) {
            YeuCauDoiLich yeuCau = nhom.cacYeuCau().get(i);
            LichLamViec ca = nhom.cacCa().get(i);
            if (yeuCau.getTrangThai() != TrangThaiYeuCau.CHO_DUYET) {
                continue;
            }
            // Không gọi khoaCaConSuaDuoc (ném lỗi): ca đã bắt đầu thì bỏ qua, các ca khác vẫn được duyệt
            if (ca.getTrangThai() != TrangThaiLichLamViec.HOAT_DONG
                    || !ca.getNgayLamViec().atTime(ca.getGioBatDau()).isAfter(bayGio)) {
                boQua.add(new KetQuaXuLyNhomResponse.BoQua(yeuCau.getId(), messageSource.getMessage(
                        MaLoi.CA_KHONG_SUA_DUOC.khoa(), null, NgonNgu.hienTai().locale())));
                continue;
            }
            quanLyCaService.huyCaDaKhoa(ca, quanTri, LyDoHuyCa.BAC_SI_NGHI_CA,
                    NguonThayDoi.duyetYeuCau(yeuCau.getId()));
            dong(yeuCau, TrangThaiYeuCau.DA_DUYET, quanTri, ghiChu);
            daXuLy.add(yeuCau);
        }
        if (daXuLy.isEmpty()) {
            throw new LoiNghiepVu(boQua.isEmpty() ? MaLoi.YEU_CAU_DA_XU_LY : MaLoi.CA_KHONG_SUA_DUOC);
        }
        return ketThucNhom(daXuLy, boQua, quanTri, maNhom);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public KetQuaXuLyNhomResponse tuChoiNhom(Long idTaiKhoanQuanTri, String maNhom, String ghiChu) {
        QuanTriVien quanTri = quanLyCaService.layQuanTriVien(idTaiKhoanQuanTri);
        kiemTraCoGhiChuTuChoi(ghiChu);
        List<YeuCauDoiLich> daXuLy = new ArrayList<>();
        for (YeuCauDoiLich yeuCau : khoaNhom(maNhom).cacYeuCau()) {
            if (yeuCau.getTrangThai() == TrangThaiYeuCau.CHO_DUYET) {
                dong(yeuCau, TrangThaiYeuCau.TU_CHOI, quanTri, ghiChu);
                daXuLy.add(yeuCau);
            }
        }
        if (daXuLy.isEmpty()) {
            throw new LoiNghiepVu(MaLoi.YEU_CAU_DA_XU_LY);
        }
        return ketThucNhom(daXuLy, List.of(), quanTri, maNhom);
    }

    /** Các ca và các yêu cầu của 1 nhóm, đã khoá, cùng thứ tự (id ca tăng dần). */
    private record NhomDaKhoa(List<LichLamViec> cacCa, List<YeuCauDoiLich> cacYeuCau) {
    }

    private NhomDaKhoa khoaNhom(String maNhom) {
        List<ThanhVienNhom> nhom = yeuCauDoiLichRepository.timTheoNhom(maNhom);
        if (nhom.isEmpty()) {
            throw khongTimThay();
        }
        List<LichLamViec> cacCa = new ArrayList<>();
        for (ThanhVienNhom thanhVien : nhom) {
            cacCa.add(lichLamViecRepository.findByIdForUpdate(thanhVien.getIdLichLamViec())
                    .orElseThrow(YeuCauDoiLichServiceImpl::khongTimThay));
        }
        List<YeuCauDoiLich> cacYeuCau = new ArrayList<>();
        for (ThanhVienNhom thanhVien : nhom) {
            cacYeuCau.add(yeuCauDoiLichRepository.findByIdForUpdate(thanhVien.getIdYeuCau())
                    .orElseThrow(YeuCauDoiLichServiceImpl::khongTimThay));
        }
        return new NhomDaKhoa(cacCa, cacYeuCau);
    }

    /** Báo bác sĩ 1 lần cho cả nhóm, trả các yêu cầu đã xử lý theo ngày giờ của ca. */
    private KetQuaXuLyNhomResponse ketThucNhom(List<YeuCauDoiLich> daXuLy, List<KetQuaXuLyNhomResponse.BoQua> boQua,
            QuanTriVien quanTri, String maNhom) {
        daXuLy.sort(Comparator.comparing((YeuCauDoiLich yeuCau) -> yeuCau.getLichLamViec().getNgayLamViec())
                .thenComparing(yeuCau -> yeuCau.getLichLamViec().getGioBatDau()));
        thongBaoLichLamViecService.ketQuaNhom(daXuLy);
        log.info("Quản trị viên id={} {} {} yêu cầu của nhóm {} (bỏ qua {})", quanTri.getId(),
                daXuLy.get(0).getTrangThai(), daXuLy.size(), maNhom, boQua.size());
        return new KetQuaXuLyNhomResponse(toResponses(daXuLy), boQua);
    }

    private static void kiemTraCoGhiChuTuChoi(String ghiChu) {
        if (ghiChu == null || ghiChu.isBlank()) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.ghiChuTuChoi");
        }
    }

    /** Đọc không khoá, chỉ để biết phải khoá ca nào trước. */
    private Long idCaCuaYeuCau(Long idYeuCau) {
        return yeuCauDoiLichRepository.timIdLichLamViec(idYeuCau).orElseThrow(YeuCauDoiLichServiceImpl::khongTimThay);
    }

    private YeuCauDoiLich khoaYeuCauChoDuyet(Long idYeuCau) {
        YeuCauDoiLich yeuCau = yeuCauDoiLichRepository.findByIdForUpdate(idYeuCau)
                .orElseThrow(YeuCauDoiLichServiceImpl::khongTimThay);
        if (yeuCau.getTrangThai() == TrangThaiYeuCau.CHO_DONG_Y) {
            throw new LoiNghiepVu(MaLoi.YEU_CAU_CHO_DONG_NGHIEP);
        }
        if (yeuCau.getTrangThai() != TrangThaiYeuCau.CHO_DUYET) {
            throw new LoiNghiepVu(MaLoi.YEU_CAU_DA_XU_LY);
        }
        return yeuCau;
    }

    /** Đóng yêu cầu, không báo ai: nơi gọi báo (từng yêu cầu, hoặc 1 lần cho cả nhóm). */
    private static void dong(YeuCauDoiLich yeuCau, TrangThaiYeuCau trangThai, QuanTriVien quanTri, String ghiChu) {
        yeuCau.setTrangThai(trangThai);
        yeuCau.setAdminXuLy(quanTri);
        yeuCau.setGhiChuXuLy(ghiChu == null || ghiChu.isBlank() ? null : ghiChu.trim());
        yeuCau.setNgayXuLy(LocalDateTime.now());
    }

    private static LoiNghiepVu khongTimThay() {
        return new LoiNghiepVu(MaLoi.KHONG_TIM_THAY, "loi.KHONG_TIM_THAY.yeuCau");
    }

    private YeuCauDoiLichResponse toResponse(YeuCauDoiLich yeuCau) {
        return toResponses(List.of(yeuCau)).get(0);
    }

    private List<YeuCauDoiLichResponse> toResponses(List<YeuCauDoiLich> cacYeuCau) {
        yeuCauDoiLichRepository.flush();
        Map<Long, SoLuotCuaCa> soLuot = soLuotTheoCa(cacYeuCau);
        return cacYeuCau.stream().map(yeuCau -> toResponse(yeuCau, soLuot)).toList();
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
                yeuCau.getLyDo(), caKhamMapper.toBacSiTomTat(yeuCau.getBacSi()), caResponse, yeuCau.getNgayMongMuon(),
                yeuCau.getGioBatDauMongMuon(), yeuCau.getGioKetThucMongMuon(),
                yeuCau.getPhongKhamMongMuon() == null ? null
                        : caKhamMapper.toPhongKhamTomTat(yeuCau.getPhongKhamMongMuon()),
                yeuCau.getBacSiNhan() == null ? null : caKhamMapper.toBacSiTomTat(yeuCau.getBacSiNhan()),
                yeuCau.getGhiChuBacSiNhan(), yeuCau.getNgayBacSiNhanPhanHoi(), yeuCau.getMaNhom(),
                VanBanHeThong.doc(yeuCau.getGhiChuXuLy(), messageSource, NgonNgu.hienTai()), yeuCau.getNgayGui(),
                yeuCau.getNgayXuLy());
    }

}
