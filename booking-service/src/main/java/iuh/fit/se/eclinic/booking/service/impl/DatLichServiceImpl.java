package iuh.fit.se.eclinic.booking.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.config.DatLichProperties;
import iuh.fit.se.eclinic.booking.dto.request.BenhNhanRequest;
import iuh.fit.se.eclinic.booking.dto.request.DatLichRequest;
import iuh.fit.se.eclinic.booking.dto.request.NguoiGiamHoRequest;
import iuh.fit.se.eclinic.booking.dto.response.DatLichResponse;
import iuh.fit.se.eclinic.booking.event.DaDatLichChoNguoiThanEvent;
import iuh.fit.se.eclinic.booking.mapper.LichHenMapper;
import iuh.fit.se.eclinic.booking.repository.HoSoBenhNhanRepository;
import iuh.fit.se.eclinic.booking.repository.KhungGioKhamRepository;
import iuh.fit.se.eclinic.booking.repository.LichHenRepository;
import iuh.fit.se.eclinic.booking.repository.LichLamViecRepository;
import iuh.fit.se.eclinic.booking.repository.NguoiGiamHoRepository;
import iuh.fit.se.eclinic.booking.service.DatLichService;
import iuh.fit.se.eclinic.booking.service.TaiKhoanService;
import iuh.fit.se.eclinic.booking.util.ChuanHoaTen;
import iuh.fit.se.eclinic.booking.util.KhoaNhanDien;
import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.booking.NguoiGiamHo;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.entity.scheduling.KhungGioKham;
import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;
import iuh.fit.se.eclinic.common.enums.TrangThaiBacSi;
import iuh.fit.se.eclinic.common.enums.TrangThaiKhungGio;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichLamViec;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.util.ChiaCaLamViec;
import iuh.fit.se.eclinic.common.util.TokenNgauNhien;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatLichServiceImpl implements DatLichService {

    private static final int TUOI_THANH_NIEN = 18;

    private static final DateTimeFormatter NGAY_TRONG_MA_TRA_CUU = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int SO_LAN_THU_MA_TRA_CUU = 5;
    private static final String RANG_BUOC_MA_TRA_CUU = "uk_lich_hen_ma_tra_cuu";

    /** Ca đã chọn để đặt cùng các lượt (đã khoá) của khung giờ trong ca đó. */
    private record CaVaLuot(LichLamViec ca, LocalDateTime gioKetThucKhung, List<KhungGioKham> cacLuot) {
    }

    /**
     * Dòng đã lưu (hồ sơ bệnh nhân, người giám hộ) dùng cho lượt đặt này.
     *
     * @param khacThongTin true nếu dòng đã có từ trước và họ tên / ngày sinh nhập vào khác dòng đó
     */
    private record DaTim<T>(T dong, boolean khacThongTin) {
    }

    /** Lịch hẹn còn hiệu lực: tính vào trùng giờ và vào giới hạn số lịch đang giữ. */
    private static final List<TrangThaiLichHen> TRANG_THAI_CON_HIEU_LUC = List.of(TrangThaiLichHen.CHO_XAC_NHAN,
            TrangThaiLichHen.DA_XAC_NHAN);

    private final LichLamViecRepository lichLamViecRepository;
    private final KhungGioKhamRepository khungGioKhamRepository;
    private final HoSoBenhNhanRepository hoSoBenhNhanRepository;
    private final NguoiGiamHoRepository nguoiGiamHoRepository;
    private final LichHenRepository lichHenRepository;
    private final LichHenMapper lichHenMapper;
    private final DatLichProperties datLichProperties;
    private final TaiKhoanService taiKhoanService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * READ_COMMITTED chứ không dùng REPEATABLE READ mặc định của MySQL: request phải chờ khoá (2 người cùng đặt 1 khung,
     * hoặc bấm đặt 2 lần) sau khi được khoá phải thấy lịch hẹn mà request trước vừa commit, không đọc lại snapshot cũ.
     * <p>
     * Thứ tự khoá cố định: các lượt của khung giờ trước (đặt "bác sĩ bất kỳ" thì khoá khung của mọi ca ứng viên theo id ca
     * tăng dần), hồ sơ bệnh nhân sau. Mọi request đi cùng thứ tự nên không deadlock.
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DatLichResponse datLich(DatLichRequest request, Long idTaiKhoan) {
        LocalDateTime bayGio = LocalDateTime.now();
        TaiKhoan taiKhoanDat = idTaiKhoan == null ? null : taiKhoanService.layBenhNhanDangHoatDong(idTaiKhoan);
        LocalDateTime gioBatDauKhung = request.gioBatDauKhung();
        LocalDate ngayKham = gioBatDauKhung.toLocalDate();
        boolean bacSiBatKy = request.idLichLamViec() == null;
        if (bacSiBatKy == (request.idChuyenKhoa() == null)) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE,
                    "Gửi idLichLamViec (chọn bác sĩ) hoặc idChuyenKhoa (bác sĩ bất kỳ), không gửi cả hai");
        }
        // Đọc không khoá: ca đích danh, hoặc các ca của chuyên khoa có khung bắt đầu đúng giờ này
        List<LichLamViec> caUngVien = bacSiBatKy
                ? timCaUngVien(request.idChuyenKhoa(), gioBatDauKhung, bayGio.toLocalDate())
                : List.of(timCaDichDanh(request.idLichLamViec(), gioBatDauKhung, bayGio.toLocalDate()));

        BenhNhanRequest benhNhan = request.benhNhan();
        String cccd = rongThanhNull(benhNhan.cccd());
        NguoiGiamHoRequest giamHo = nguoiGiamHoCanDung(benhNhan, cccd, request.nguoiGiamHo(), ngayKham);
        // BOOK-11: liên hệ ưu tiên SĐT người giám hộ
        String soDienThoaiLienHe = giamHo != null ? giamHo.soDienThoai() : benhNhan.soDienThoai();
        boolean datChoBanThan = taiKhoanDat != null && Boolean.TRUE.equals(request.datChoBanThan());
        if (datChoBanThan && cccd == null) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "Đặt cho bản thân cần số CCCD của chủ tài khoản");
        }
        // Đọc không khoá, trước khi khoá khung giờ: các từ chối về hồ sơ của tài khoản không giữ khoá nào
        TaiKhoan chuHoSoMoi = datChoBanThan && canTaoHoSoCuaTaiKhoan(taiKhoanDat, cccd) ? taiKhoanDat : null;

        LocalDateTime moc = bayGio.plus(datLichProperties.datTruocToiThieu());
        CaVaLuot daChon = khoaVaChonCa(caUngVien, gioBatDauKhung, moc);
        LichLamViec ca = daChon.ca();
        LocalDateTime gioKetThucKhung = daChon.gioKetThucKhung();
        KhungGioKham luot = chonLuot(daChon.cacLuot(), moc);

        kiemTraGioiHanTheoSoDienThoai(soDienThoaiLienHe, bayGio);
        DaTim<HoSoBenhNhan> hoSoDaTim = timHoacTaoHoSo(benhNhan, cccd, giamHo, chuHoSoMoi, gioBatDauKhung,
                gioKetThucKhung, bayGio);
        HoSoBenhNhan hoSo = hoSoDaTim.dong();
        DaTim<NguoiGiamHo> giamHoDaTim = giamHo == null ? null : timHoacTaoNguoiGiamHo(hoSo, giamHo);
        NguoiGiamHo nguoiGiamHo = giamHoDaTim == null ? null : giamHoDaTim.dong();

        luot.setTrangThai(TrangThaiKhungGio.DA_DAT);
        LichHen lichHen = new LichHen();
        lichHen.setHoSoBenhNhan(hoSo);
        lichHen.setNguoiGiamHo(nguoiGiamHo);
        lichHen.setTaiKhoanDat(taiKhoanDat);
        lichHen.setBacSi(ca.getBacSi());
        lichHen.setKhungGio(luot);
        lichHen.setPhongKham(ca.getPhongKham());
        lichHen.setSoThuTu(soThuTu(ca, luot));
        lichHen.setLyDoKham(rongThanhNull(request.lyDoKham()));
        lichHen.setSoDienThoaiLienHe(soDienThoaiLienHe);
        lichHen.setEmailLienHe(rongThanhNull(benhNhan.email()));
        // Bản sao những gì người đặt nhập: hồ sơ đã có không bị sửa, nơi hiển thị cho người đặt dùng bản sao này
        lichHen.setHoTenDaNhap(ChuanHoaTen.gon(benhNhan.hoTen()));
        lichHen.setNgaySinhDaNhap(benhNhan.ngaySinh());
        lichHen.setGioiTinhDaNhap(benhNhan.gioiTinh());
        lichHen.setHoTenGiamHoDaNhap(giamHo == null ? null : ChuanHoaTen.gon(giamHo.hoTen()));
        lichHen.setCanDoiChieu(hoSoDaTim.khacThongTin() || (giamHoDaTim != null && giamHoDaTim.khacThongTin()));
        lichHen.setMaTraCuu(taoMaTraCuu(ngayKham));
        lichHen.setMaTokenPhieuKham(TokenNgauNhien.tao());
        try {
            lichHenRepository.saveAndFlush(lichHen);
        } catch (DataIntegrityViolationException ex) {
            if (String.valueOf(ex.getMostSpecificCause().getMessage()).contains(RANG_BUOC_MA_TRA_CUU)) {
                // 2 lịch hẹn cùng lúc sinh trùng mã tra cứu: không phải hết chỗ, để XuLyLoiHandler báo 409 thử lại
                throw ex;
            }
            // Chốt chặn cuối: UNIQUE uk_lich_hen_khung_gio_hieu_luc (lượt còn CON_TRONG nhưng đã có lịch hẹn chiếm chỗ)
            log.warn("Đặt lịch vi phạm ràng buộc DB ở lượt khám {}: {}", luot.getId(),
                    ex.getMostSpecificCause().getMessage());
            throw new LoiNghiepVu(MaLoi.KHUNG_GIO_KHONG_CON_TRONG);
        }
        // Đặt cho người thân khi đã đăng nhập: sau khi commit, lưu những gì vừa nhập để điền sẵn lần sau
        boolean laHoSoCuaTaiKhoan = taiKhoanDat != null && hoSo.getTaiKhoan() != null
                && hoSo.getTaiKhoan().getId().equals(taiKhoanDat.getId());
        if (taiKhoanDat != null && !datChoBanThan && !laHoSoCuaTaiKhoan
                && !Boolean.FALSE.equals(request.luuNguoiThan())) {
            eventPublisher.publishEvent(
                    new DaDatLichChoNguoiThanEvent(taiKhoanDat.getId(), hoSo.getId(), benhNhan, giamHo));
        }
        return lichHenMapper.toDatLichResponse(lichHen, gioBatDauKhung, gioKetThucKhung, bacSiBatKy);
    }

    /** Ca do người đặt chọn: phải có khung bắt đầu đúng {@code gioBatDauKhung} và còn nhận đặt lịch. */
    private LichLamViec timCaDichDanh(Long idLichLamViec, LocalDateTime gioBatDauKhung, LocalDate homNay) {
        LichLamViec ca = lichLamViecRepository.timTheoIdKemBacSiVaPhongKham(idLichLamViec)
                .orElseThrow(() -> new LoiKhongTimThay("LichLamViec", idLichLamViec));
        gioKetThucKhung(ca, gioBatDauKhung);
        kiemTraCaNhanDatLich(ca, homNay);
        return ca;
    }

    /**
     * "Bác sĩ bất kỳ": các ca đặt lịch được của chuyên khoa trong ngày có 1 khung bắt đầu đúng {@code gioBatDauKhung}
     * (cùng khoá với 1 dòng của GET /api/booking/khung-gio/gop), theo id ca tăng dần (thứ tự khoá).
     */
    private List<LichLamViec> timCaUngVien(Long idChuyenKhoa, LocalDateTime gioBatDauKhung, LocalDate homNay) {
        LocalDate ngay = gioBatDauKhung.toLocalDate();
        if (ngay.isBefore(homNay) || ngay.isAfter(homNay.plusDays(datLichProperties.soNgayDatTruocToiDa()))) {
            throw new LoiNghiepVu(MaLoi.KHUNG_GIO_KHONG_KHA_DUNG);
        }
        List<LichLamViec> caUngVien = lichLamViecRepository.timCaDatLichDuoc(ngay, null, idChuyenKhoa).stream()
                .filter(ca -> laGioBatDauKhung(ca, gioBatDauKhung))
                .sorted(Comparator.comparing(LichLamViec::getId))
                .toList();
        if (caUngVien.isEmpty()) {
            throw new LoiNghiepVu(MaLoi.KHUNG_GIO_KHONG_KHA_DUNG);
        }
        return caUngVien;
    }

    /**
     * Khoá các lượt của khung giờ ở MỌI ca ứng viên (theo thứ tự trong danh sách) rồi chọn ca còn nhiều lượt trống kịp
     * đặt nhất; bằng nhau thì ca đứng trước. Không ca nào còn lượt trống: trả ca đầu tiên có lượt đã đặt (để
     * {@link #chonLuot} báo "đủ lượt"), không có thì ca đầu tiên (báo "không nhận đặt").
     */
    private CaVaLuot khoaVaChonCa(List<LichLamViec> caUngVien, LocalDateTime gioBatDauKhung, LocalDateTime moc) {
        List<CaVaLuot> daKhoa = new ArrayList<>();
        for (LichLamViec ca : caUngVien) {
            LocalDateTime gioKetThucKhung = gioKetThucKhung(ca, gioBatDauKhung);
            daKhoa.add(new CaVaLuot(ca, gioKetThucKhung,
                    khungGioKhamRepository.khoaCacLuotCuaKhung(ca.getId(), gioBatDauKhung, gioKetThucKhung)));
        }
        CaVaLuot totNhat = null;
        long nhieuNhat = 0;
        CaVaLuot daDu = null;
        for (CaVaLuot ungVien : daKhoa) {
            long conTrong = demLuot(ungVien.cacLuot(), moc, TrangThaiKhungGio.CON_TRONG);
            if (conTrong > nhieuNhat) {
                nhieuNhat = conTrong;
                totNhat = ungVien;
            }
            if (daDu == null && demLuot(ungVien.cacLuot(), moc, TrangThaiKhungGio.DA_DAT) > 0) {
                daDu = ungVien;
            }
        }
        if (totNhat != null) {
            return totNhat;
        }
        return daDu != null ? daDu : daKhoa.get(0);
    }

    /** Số lượt còn kịp đặt (bắt đầu từ {@code moc} trở đi) đang ở trạng thái cho trước. */
    private static long demLuot(List<KhungGioKham> cacLuot, LocalDateTime moc, TrangThaiKhungGio trangThai) {
        return cacLuot.stream()
                .filter(luot -> !luot.getGioBatDau().isBefore(moc) && luot.getTrangThai() == trangThai)
                .count();
    }

    /** true nếu {@code gioBatDauKhung} là giờ bắt đầu 1 khung 1 giờ của ca. */
    private static boolean laGioBatDauKhung(LichLamViec ca, LocalDateTime gioBatDauKhung) {
        LocalTime gio = gioBatDauKhung.toLocalTime();
        return gioBatDauKhung.toLocalDate().equals(ca.getNgayLamViec()) && !gio.isBefore(ca.getGioBatDau())
                && gio.isBefore(ca.getGioKetThuc())
                && ChiaCaLamViec.gioBatDauKhung(ca.getGioBatDau(), gio).equals(gio);
    }

    /**
     * Mã tra cứu ngắn {@code ECL-<ngày khám>-<4 chữ số>}. Trùng mã đã có thì sinh lại; sau vài lần vẫn trùng (ngày có
     * rất nhiều lịch hẹn) thì dùng 6 chữ số. UNIQUE của cột là chốt chặn cuối.
     */
    private String taoMaTraCuu(LocalDate ngayKham) {
        String dau = "ECL-" + ngayKham.format(NGAY_TRONG_MA_TRA_CUU) + "-";
        for (int lan = 0; lan < SO_LAN_THU_MA_TRA_CUU; lan++) {
            String ma = dau + "%04d".formatted(ThreadLocalRandom.current().nextInt(10_000));
            if (!lichHenRepository.existsByMaTraCuu(ma)) {
                return ma;
            }
        }
        return dau + "%06d".formatted(ThreadLocalRandom.current().nextInt(1_000_000));
    }

    /**
     * {@code gioBatDauKhung} phải đúng là giờ bắt đầu 1 khung 1 giờ của ca (khung tính từ giờ bắt đầu ca).
     *
     * @return giờ kết thúc khung đó
     */
    private static LocalDateTime gioKetThucKhung(LichLamViec ca, LocalDateTime gioBatDauKhung) {
        if (!laGioBatDauKhung(ca, gioBatDauKhung)) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "gioBatDauKhung không phải khung giờ của ca khám này");
        }
        return ca.getNgayLamViec()
                .atTime(ChiaCaLamViec.gioKetThucKhung(gioBatDauKhung.toLocalTime(), ca.getGioKetThuc()));
    }

    /** Điều kiện ca nhận đặt lịch, giống lúc xem khung giờ (LichLamViecRepository#timCaDatLichDuoc). */
    private void kiemTraCaNhanDatLich(LichLamViec ca, LocalDate homNay) {
        BacSi bacSi = ca.getBacSi();
        boolean caDangNhan = ca.getTrangThai() == TrangThaiLichLamViec.HOAT_DONG
                && bacSi.getTrangThai() == TrangThaiBacSi.DANG_CONG_TAC
                && bacSi.getTaiKhoan().getTrangThai() == TrangThaiTaiKhoan.DA_KICH_HOAT;
        LocalDate ngay = ca.getNgayLamViec();
        boolean trongHan = !ngay.isBefore(homNay)
                && !ngay.isAfter(homNay.plusDays(datLichProperties.soNgayDatTruocToiDa()));
        if (!caDangNhan || !trongHan) {
            throw new LoiNghiepVu(MaLoi.KHUNG_GIO_KHONG_KHA_DUNG);
        }
    }

    /**
     * BOOK-11: tuổi tính theo ngày khám. Dưới 18 tuổi phải có người giám hộ khác CCCD với người khám; ngày sinh người
     * giám hộ không bắt buộc, có gửi thì phải từ đủ 18 tuổi (cũng theo ngày khám). Từ đủ 18 tuổi phải có CCCD; dưới 18
     * tuổi được bỏ trống CCCD (quy tắc #10).
     *
     * @param cccd CCCD người khám, null nếu bỏ trống
     * @return người giám hộ của lượt khám; null nếu người khám từ đủ 18 tuổi (phần người giám hộ gửi kèm bị bỏ qua)
     */
    private static NguoiGiamHoRequest nguoiGiamHoCanDung(BenhNhanRequest benhNhan, String cccd,
            NguoiGiamHoRequest giamHo, LocalDate ngayKham) {
        if (tuoi(benhNhan.ngaySinh(), ngayKham) >= TUOI_THANH_NIEN) {
            if (cccd == null) {
                throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE,
                        "Người khám từ đủ 18 tuổi phải có số CCCD (benhNhan.cccd)");
            }
            return null;
        }
        if (giamHo == null) {
            throw new LoiNghiepVu(MaLoi.THIEU_NGUOI_GIAM_HO);
        }
        if (giamHo.cccd().equals(cccd)) {
            throw new LoiNghiepVu(MaLoi.NGUOI_GIAM_HO_KHONG_HOP_LE,
                    "Số CCCD người giám hộ phải khác số CCCD của bệnh nhân");
        }
        if (giamHo.ngaySinh() != null && tuoi(giamHo.ngaySinh(), ngayKham) < TUOI_THANH_NIEN) {
            throw new LoiNghiepVu(MaLoi.NGUOI_GIAM_HO_KHONG_HOP_LE, "Người giám hộ phải từ đủ 18 tuổi");
        }
        return giamHo;
    }

    private static int tuoi(LocalDate ngaySinh, LocalDate ngayTinh) {
        return Period.between(ngaySinh, ngayTinh).getYears();
    }

    /**
     * Lượt trống sớm nhất của khung (quy tắc #12) trong các lượt còn kịp đặt (bắt đầu từ {@code moc} trở đi).
     * Không có: các lượt còn kịp đặt đều đã có người đặt thì khung đã đủ lượt, còn lại là khung không nhận đặt nữa
     * (đã qua hạn đặt, hoặc các lượt đã bị hủy).
     */
    private static KhungGioKham chonLuot(List<KhungGioKham> cacLuot, LocalDateTime moc) {
        boolean coLuotDaDat = false;
        for (KhungGioKham luot : cacLuot) {
            if (luot.getGioBatDau().isBefore(moc)) {
                continue;
            }
            if (luot.getTrangThai() == TrangThaiKhungGio.CON_TRONG) {
                return luot;
            }
            coLuotDaDat |= luot.getTrangThai() == TrangThaiKhungGio.DA_DAT;
        }
        throw new LoiNghiepVu(coLuotDaDat ? MaLoi.KHUNG_GIO_KHONG_CON_TRONG : MaLoi.KHUNG_GIO_KHONG_KHA_DUNG);
    }

    private void kiemTraGioiHanTheoSoDienThoai(String soDienThoai, LocalDateTime bayGio) {
        int toiDa = datLichProperties.soLichHieuLucToiDaMoiSoDienThoai();
        if (lichHenRepository.demLichTheoSoDienThoaiTu(soDienThoai, TRANG_THAI_CON_HIEU_LUC, bayGio) >= toiDa) {
            throw new LoiNghiepVu(MaLoi.VUOT_GIOI_HAN_DAT_LICH,
                    "Số điện thoại này đang giữ tối đa " + toiDa + " lịch hẹn sắp tới, không thể đặt thêm");
        }
    }

    /**
     * Đặt cho bản thân (BOOK-02): đối chiếu CCCD nhập vào với hồ sơ bệnh nhân của tài khoản.
     *
     * @return true nếu tài khoản chưa có hồ sơ và CCCD cũng chưa có hồ sơ: lần đặt này tạo hồ sơ gắn vào tài khoản;
     *         false nếu tài khoản đã có hồ sơ đúng CCCD này
     */
    private boolean canTaoHoSoCuaTaiKhoan(TaiKhoan taiKhoan, String cccd) {
        Optional<HoSoBenhNhan> cuaTaiKhoan = hoSoBenhNhanRepository.findByTaiKhoanId(taiKhoan.getId());
        if (cuaTaiKhoan.isEmpty()) {
            if (hoSoBenhNhanRepository.existsByCccd(cccd)) {
                // Gắn hồ sơ đã có vào tài khoản phải qua xác minh (quy tắc #3), việc đặt lịch không tự gắn
                throw new LoiNghiepVu(MaLoi.CCCD_DA_CO_HO_SO, "Số CCCD này đã có hồ sơ bệnh nhân nên chưa thể gắn vào"
                        + " tài khoản của bạn; hãy đặt lịch không chọn \"đặt cho bản thân\" hoặc liên hệ phòng khám");
            }
            return true;
        }
        HoSoBenhNhan hoSo = cuaTaiKhoan.get();
        if (hoSo.getTrangThaiLienKet() != TrangThaiLienKet.DA_LIEN_KET) {
            throw new LoiNghiepVu(MaLoi.HO_SO_CHO_XAC_MINH);
        }
        if (!hoSo.getCccd().equals(cccd)) {
            throw new LoiNghiepVu(MaLoi.THONG_TIN_BENH_NHAN_KHONG_KHOP,
                    "Số CCCD không trùng với hồ sơ bệnh nhân của tài khoản; nếu đặt cho người thân, hãy bỏ chọn"
                            + " \"đặt cho bản thân\"");
        }
        return false;
    }

    /**
     * BOOK-03: CCCD (hoặc khoá nhận diện của trẻ chưa có CCCD) chưa có hồ sơ thì tạo mới. Đã có thì dùng lại: số CCCD là
     * khoá nhận diện duy nhất, họ tên / ngày sinh nhập khác hồ sơ KHÔNG bị từ chối mà được báo lại để lịch hẹn đánh dấu
     * cần đối chiếu. Việc đặt lịch không sửa dữ liệu của hồ sơ (chủ tài khoản sửa hồ sơ của mình, quản trị viên sửa hồ
     * sơ bất kỳ ở API riêng); chỉ khi thông tin nhập khớp hồ sơ mới điền các trường hồ sơ còn trống. Hồ sơ đã có được
     * khoá để các lần đặt của cùng bệnh nhân chạy lần lượt.
     *
     * @param chuHoSoMoi khác null khi đặt cho bản thân mà tài khoản chưa có hồ sơ: hồ sơ tạo mới được gắn vào tài khoản
     *                   này, còn nếu CCCD vừa có hồ sơ (request khác tạo sau lần kiểm tra không khoá) thì từ chối
     */
    private DaTim<HoSoBenhNhan> timHoacTaoHoSo(BenhNhanRequest benhNhan, String cccd, NguoiGiamHoRequest giamHo,
            TaiKhoan chuHoSoMoi, LocalDateTime gioBatDauKhung, LocalDateTime gioKetThucKhung, LocalDateTime bayGio) {
        // Người khám dưới 18 tuổi (có người giám hộ): khoá nhận diện khi chưa có CCCD (quy tắc #10)
        String khoaNhanDien = giamHo == null ? null
                : KhoaNhanDien.tao(benhNhan.hoTen(), benhNhan.ngaySinh(), giamHo.cccd());
        Optional<HoSoBenhNhan> daCo;
        if (cccd == null) {
            // Chưa có CCCD: cùng họ tên + ngày sinh + CCCD người giám hộ là cùng 1 hồ sơ (kể cả hồ sơ đã được điền CCCD)
            daCo = hoSoBenhNhanRepository.findByKhoaNhanDienForUpdate(khoaNhanDien);
        } else {
            daCo = hoSoBenhNhanRepository.findByCccdForUpdate(cccd);
            if (daCo.isEmpty() && khoaNhanDien != null && chuHoSoMoi == null) {
                // Trẻ từng đặt lịch khi chưa có CCCD, nay đã có: điền CCCD vào hồ sơ cũ thay vì tạo hồ sơ thứ 2
                daCo = hoSoBenhNhanRepository.findByKhoaNhanDienForUpdate(khoaNhanDien)
                        .filter(hoSo -> hoSo.getCccd() == null);
                daCo.ifPresent(hoSo -> hoSo.setCccd(cccd));
            }
        }
        if (daCo.isEmpty()) {
            HoSoBenhNhan hoSo = new HoSoBenhNhan();
            hoSo.setCccd(cccd);
            if (cccd == null) {
                hoSo.setKhoaNhanDien(khoaNhanDien);
            }
            hoSo.setDiaChi(rongThanhNull(benhNhan.diaChi()));
            hoSo.setSoBaoHiemYTe(rongThanhNull(benhNhan.soBaoHiemYTe()));
            hoSo.setHoTen(ChuanHoaTen.gon(benhNhan.hoTen()));
            hoSo.setNgaySinh(benhNhan.ngaySinh());
            hoSo.setGioiTinh(benhNhan.gioiTinh());
            hoSo.setSoDienThoai(benhNhan.soDienThoai());
            if (chuHoSoMoi != null) {
                hoSo.setTaiKhoan(chuHoSoMoi);
                hoSo.setTrangThaiLienKet(TrangThaiLienKet.DA_LIEN_KET);
            }
            // 2 request cùng lúc với cùng CCCD mới ở 2 khung khác nhau, hoặc 2 lần "đặt cho bản thân" đầu tiên của 1 tài
            // khoản: request sau vi phạm UNIQUE cccd / khoa_nhan_dien / id_tai_khoan -> 409 XUNG_DOT_DU_LIEU
            return new DaTim<>(hoSoBenhNhanRepository.saveAndFlush(hoSo), false);
        }
        if (chuHoSoMoi != null) {
            throw new LoiNghiepVu(MaLoi.CCCD_DA_CO_HO_SO);
        }
        HoSoBenhNhan hoSo = daCo.get();
        boolean khacThongTin = !khop(hoSo.getHoTen(), hoSo.getNgaySinh(), benhNhan.hoTen(), benhNhan.ngaySinh());
        if (lichHenRepository.coLichTrongKhoang(hoSo.getId(), TRANG_THAI_CON_HIEU_LUC, gioBatDauKhung,
                gioKetThucKhung)) {
            throw new LoiNghiepVu(MaLoi.LICH_HEN_TRUNG_GIO);
        }
        int toiDa = datLichProperties.soLichHieuLucToiDaMoiHoSo();
        if (lichHenRepository.demLichCuaHoSoTu(hoSo.getId(), TRANG_THAI_CON_HIEU_LUC, bayGio) >= toiDa) {
            throw new LoiNghiepVu(MaLoi.VUOT_GIOI_HAN_DAT_LICH,
                    "Bệnh nhân đang giữ tối đa " + toiDa + " lịch hẹn sắp tới, không thể đặt thêm");
        }
        if (khacThongTin) {
            // Chưa đối chiếu được người đặt có đúng là người của hồ sơ: không ghi gì vào hồ sơ
            return new DaTim<>(hoSo, true);
        }
        if (hoSo.getNgaySinh() == null) {
            hoSo.setNgaySinh(benhNhan.ngaySinh());
        }
        if (hoSo.getGioiTinh() == null) {
            hoSo.setGioiTinh(benhNhan.gioiTinh());
        }
        if (hoSo.getDiaChi() == null) {
            hoSo.setDiaChi(rongThanhNull(benhNhan.diaChi()));
        }
        if (hoSo.getSoBaoHiemYTe() == null) {
            hoSo.setSoBaoHiemYTe(rongThanhNull(benhNhan.soBaoHiemYTe()));
        }
        return new DaTim<>(hoSo, false);
    }

    /**
     * Người giám hộ đã khai cho hồ sơ (theo CCCD) thì dùng lại và không sửa dòng đã lưu; họ tên / ngày sinh nhập khác
     * dòng đó thì báo lại để lịch hẹn đánh dấu cần đối chiếu.
     */
    private DaTim<NguoiGiamHo> timHoacTaoNguoiGiamHo(HoSoBenhNhan hoSo, NguoiGiamHoRequest giamHo) {
        Optional<NguoiGiamHo> daCo = nguoiGiamHoRepository.findByHoSoBenhNhanIdAndCccd(hoSo.getId(), giamHo.cccd());
        if (daCo.isEmpty()) {
            NguoiGiamHo nguoiGiamHo = new NguoiGiamHo();
            nguoiGiamHo.setHoSoBenhNhan(hoSo);
            nguoiGiamHo.setHoTen(ChuanHoaTen.gon(giamHo.hoTen()));
            nguoiGiamHo.setQuanHe(giamHo.quanHe());
            nguoiGiamHo.setSoDienThoai(giamHo.soDienThoai());
            nguoiGiamHo.setCccd(giamHo.cccd());
            nguoiGiamHo.setNgaySinh(giamHo.ngaySinh());
            return new DaTim<>(nguoiGiamHoRepository.save(nguoiGiamHo), false);
        }
        NguoiGiamHo nguoiGiamHo = daCo.get();
        if (!khop(nguoiGiamHo.getHoTen(), nguoiGiamHo.getNgaySinh(), giamHo.hoTen(), giamHo.ngaySinh())) {
            return new DaTim<>(nguoiGiamHo, true);
        }
        if (nguoiGiamHo.getNgaySinh() == null) {
            nguoiGiamHo.setNgaySinh(giamHo.ngaySinh());
        }
        return new DaTim<>(nguoiGiamHo, false);
    }

    /** Họ tên giống nhau (không xét dấu, hoa/thường) và ngày sinh trùng; ngày sinh đang lưu còn trống thì coi là khớp. */
    private static boolean khop(String hoTenDaLuu, LocalDate ngaySinhDaLuu, String hoTen, LocalDate ngaySinh) {
        return ChuanHoaTen.giongNhau(hoTenDaLuu, hoTen)
                && (ngaySinhDaLuu == null || ngaySinh == null || ngaySinhDaLuu.equals(ngaySinh));
    }

    /** Quy tắc #4, #12: số thứ tự = thứ hạng của lượt khám trong các lượt của phòng khám trong ngày, theo giờ khám. */
    private int soThuTu(LichLamViec ca, KhungGioKham luot) {
        return (int) khungGioKhamRepository.demLuotDungTruoc(ca.getPhongKham().getId(), ca.getNgayLamViec(),
                luot.getGioBatDau(), luot.getId()) + 1;
    }

    private static String rongThanhNull(String chuoi) {
        return chuoi == null || chuoi.isBlank() ? null : chuoi.trim();
    }

}
