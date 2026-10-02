package iuh.fit.se.eclinic.booking.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.config.DatLichProperties;
import iuh.fit.se.eclinic.booking.dto.request.BenhNhanRequest;
import iuh.fit.se.eclinic.booking.dto.request.DatLichRequest;
import iuh.fit.se.eclinic.booking.dto.request.NguoiGiamHoRequest;
import iuh.fit.se.eclinic.booking.dto.response.DatLichResponse;
import iuh.fit.se.eclinic.booking.mapper.LichHenMapper;
import iuh.fit.se.eclinic.booking.repository.HoSoBenhNhanRepository;
import iuh.fit.se.eclinic.booking.repository.KhungGioKhamRepository;
import iuh.fit.se.eclinic.booking.repository.LichHenRepository;
import iuh.fit.se.eclinic.booking.repository.LichLamViecRepository;
import iuh.fit.se.eclinic.booking.repository.NguoiGiamHoRepository;
import iuh.fit.se.eclinic.booking.service.DatLichService;
import iuh.fit.se.eclinic.booking.service.TaiKhoanService;
import iuh.fit.se.eclinic.booking.util.ChuanHoaTen;
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

    /**
     * READ_COMMITTED chứ không dùng REPEATABLE READ mặc định của MySQL: request phải chờ khoá (2 người cùng đặt 1 khung,
     * hoặc bấm đặt 2 lần) sau khi được khoá phải thấy lịch hẹn mà request trước vừa commit, không đọc lại snapshot cũ.
     * <p>
     * Thứ tự khoá cố định: các lượt của khung giờ trước, hồ sơ bệnh nhân sau. Mọi request đi cùng thứ tự nên không deadlock.
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DatLichResponse datLich(DatLichRequest request, Long idTaiKhoan) {
        LocalDateTime bayGio = LocalDateTime.now();
        TaiKhoan taiKhoanDat = idTaiKhoan == null ? null : taiKhoanService.layBenhNhanDangHoatDong(idTaiKhoan);
        LichLamViec ca = lichLamViecRepository.timTheoIdKemBacSiVaPhongKham(request.idLichLamViec())
                .orElseThrow(() -> new LoiKhongTimThay("LichLamViec", request.idLichLamViec()));
        LocalDateTime gioBatDauKhung = request.gioBatDauKhung();
        LocalDateTime gioKetThucKhung = gioKetThucKhung(ca, gioBatDauKhung);
        kiemTraCaNhanDatLich(ca, bayGio.toLocalDate());

        BenhNhanRequest benhNhan = request.benhNhan();
        NguoiGiamHoRequest giamHo = nguoiGiamHoCanDung(benhNhan, request.nguoiGiamHo(), ca.getNgayLamViec());
        // BOOK-11: liên hệ ưu tiên SĐT người giám hộ
        String soDienThoaiLienHe = giamHo != null ? giamHo.soDienThoai() : benhNhan.soDienThoai();
        boolean datChoBanThan = taiKhoanDat != null && Boolean.TRUE.equals(request.datChoBanThan());
        // Đọc không khoá, trước khi khoá khung giờ: các từ chối về hồ sơ của tài khoản không giữ khoá nào
        TaiKhoan chuHoSoMoi = datChoBanThan && canTaoHoSoCuaTaiKhoan(taiKhoanDat, benhNhan.cccd()) ? taiKhoanDat : null;

        List<KhungGioKham> cacLuot = khungGioKhamRepository.khoaCacLuotCuaKhung(ca.getId(), gioBatDauKhung,
                gioKetThucKhung);
        KhungGioKham luot = chonLuot(cacLuot, bayGio.plus(datLichProperties.datTruocToiThieu()));

        kiemTraGioiHanTheoSoDienThoai(soDienThoaiLienHe, bayGio);
        HoSoBenhNhan hoSo = timHoacTaoHoSo(benhNhan, chuHoSoMoi, gioBatDauKhung, gioKetThucKhung, bayGio);
        NguoiGiamHo nguoiGiamHo = giamHo == null ? null : timHoacTaoNguoiGiamHo(hoSo, giamHo);

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
        lichHen.setMaTokenPhieuKham(TokenNgauNhien.tao());
        try {
            lichHenRepository.saveAndFlush(lichHen);
        } catch (DataIntegrityViolationException ex) {
            // Chốt chặn cuối: UNIQUE uk_lich_hen_khung_gio_hieu_luc (lượt còn CON_TRONG nhưng đã có lịch hẹn chiếm chỗ)
            log.warn("Đặt lịch vi phạm ràng buộc DB ở lượt khám {}: {}", luot.getId(),
                    ex.getMostSpecificCause().getMessage());
            throw new LoiNghiepVu(MaLoi.KHUNG_GIO_KHONG_CON_TRONG);
        }
        return lichHenMapper.toDatLichResponse(lichHen, gioBatDauKhung, gioKetThucKhung);
    }

    /**
     * {@code gioBatDauKhung} phải đúng là giờ bắt đầu 1 khung 1 giờ của ca (khung tính từ giờ bắt đầu ca).
     *
     * @return giờ kết thúc khung đó
     */
    private static LocalDateTime gioKetThucKhung(LichLamViec ca, LocalDateTime gioBatDauKhung) {
        LocalDate ngay = ca.getNgayLamViec();
        LocalTime gio = gioBatDauKhung.toLocalTime();
        boolean thuocCa = gioBatDauKhung.toLocalDate().equals(ngay) && !gio.isBefore(ca.getGioBatDau())
                && gio.isBefore(ca.getGioKetThuc());
        if (!thuocCa || !ChiaCaLamViec.gioBatDauKhung(ca.getGioBatDau(), gio).equals(gio)) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "gioBatDauKhung không phải khung giờ của ca khám này");
        }
        return ngay.atTime(ChiaCaLamViec.gioKetThucKhung(gio, ca.getGioKetThuc()));
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
     * BOOK-11: tuổi tính theo ngày khám. Dưới 18 tuổi phải có người giám hộ từ đủ 18 tuổi (cũng theo ngày khám) và khác
     * CCCD với người khám.
     *
     * @return người giám hộ của lượt khám; null nếu người khám từ đủ 18 tuổi (phần người giám hộ gửi kèm bị bỏ qua)
     */
    private static NguoiGiamHoRequest nguoiGiamHoCanDung(BenhNhanRequest benhNhan, NguoiGiamHoRequest giamHo,
            LocalDate ngayKham) {
        if (tuoi(benhNhan.ngaySinh(), ngayKham) >= TUOI_THANH_NIEN) {
            return null;
        }
        if (giamHo == null) {
            throw new LoiNghiepVu(MaLoi.THIEU_NGUOI_GIAM_HO);
        }
        if (giamHo.cccd().equals(benhNhan.cccd())) {
            throw new LoiNghiepVu(MaLoi.NGUOI_GIAM_HO_KHONG_HOP_LE,
                    "Số CCCD người giám hộ phải khác số CCCD của bệnh nhân");
        }
        if (tuoi(giamHo.ngaySinh(), ngayKham) < TUOI_THANH_NIEN) {
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
     * BOOK-03: CCCD chưa có hồ sơ thì tạo mới. Đã có thì dùng lại, với điều kiện họ tên và ngày sinh nhập vào khớp hồ
     * sơ; không cập nhật gì vào hồ sơ đã có ngoài việc điền ngày sinh / giới tính còn trống (việc đặt lịch không sửa dữ
     * liệu của hồ sơ; chủ tài khoản sửa hồ sơ của mình ở API riêng). Hồ sơ đã có được khoá để các lần đặt của cùng bệnh
     * nhân chạy lần lượt.
     *
     * @param chuHoSoMoi khác null khi đặt cho bản thân mà tài khoản chưa có hồ sơ: hồ sơ tạo mới được gắn vào tài khoản
     *                   này, còn nếu CCCD vừa có hồ sơ (request khác tạo sau lần kiểm tra không khoá) thì từ chối
     */
    private HoSoBenhNhan timHoacTaoHoSo(BenhNhanRequest benhNhan, TaiKhoan chuHoSoMoi, LocalDateTime gioBatDauKhung,
            LocalDateTime gioKetThucKhung, LocalDateTime bayGio) {
        Optional<HoSoBenhNhan> daCo = hoSoBenhNhanRepository.findByCccdForUpdate(benhNhan.cccd());
        if (daCo.isEmpty()) {
            HoSoBenhNhan hoSo = new HoSoBenhNhan();
            hoSo.setCccd(benhNhan.cccd());
            hoSo.setHoTen(ChuanHoaTen.gon(benhNhan.hoTen()));
            hoSo.setNgaySinh(benhNhan.ngaySinh());
            hoSo.setGioiTinh(benhNhan.gioiTinh());
            hoSo.setSoDienThoai(benhNhan.soDienThoai());
            if (chuHoSoMoi != null) {
                hoSo.setTaiKhoan(chuHoSoMoi);
                hoSo.setTrangThaiLienKet(TrangThaiLienKet.DA_LIEN_KET);
            }
            // 2 request cùng lúc với cùng CCCD mới ở 2 khung khác nhau, hoặc 2 lần "đặt cho bản thân" đầu tiên của 1 tài
            // khoản: request sau vi phạm UNIQUE cccd / id_tai_khoan -> 409 XUNG_DOT_DU_LIEU
            return hoSoBenhNhanRepository.saveAndFlush(hoSo);
        }
        if (chuHoSoMoi != null) {
            throw new LoiNghiepVu(MaLoi.CCCD_DA_CO_HO_SO);
        }
        HoSoBenhNhan hoSo = daCo.get();
        if (!khop(hoSo.getHoTen(), hoSo.getNgaySinh(), benhNhan.hoTen(), benhNhan.ngaySinh())) {
            throw new LoiNghiepVu(MaLoi.THONG_TIN_BENH_NHAN_KHONG_KHOP);
        }
        if (lichHenRepository.coLichTrongKhoang(hoSo.getId(), TRANG_THAI_CON_HIEU_LUC, gioBatDauKhung,
                gioKetThucKhung)) {
            throw new LoiNghiepVu(MaLoi.LICH_HEN_TRUNG_GIO);
        }
        int toiDa = datLichProperties.soLichHieuLucToiDaMoiHoSo();
        if (lichHenRepository.demLichCuaHoSoTu(hoSo.getId(), TRANG_THAI_CON_HIEU_LUC, bayGio) >= toiDa) {
            throw new LoiNghiepVu(MaLoi.VUOT_GIOI_HAN_DAT_LICH,
                    "Bệnh nhân đang giữ tối đa " + toiDa + " lịch hẹn sắp tới, không thể đặt thêm");
        }
        if (hoSo.getNgaySinh() == null) {
            hoSo.setNgaySinh(benhNhan.ngaySinh());
        }
        if (hoSo.getGioiTinh() == null) {
            hoSo.setGioiTinh(benhNhan.gioiTinh());
        }
        return hoSo;
    }

    /** Người giám hộ đã khai cho hồ sơ (theo CCCD) thì dùng lại nếu họ tên và ngày sinh khớp, không sửa dòng đã lưu. */
    private NguoiGiamHo timHoacTaoNguoiGiamHo(HoSoBenhNhan hoSo, NguoiGiamHoRequest giamHo) {
        Optional<NguoiGiamHo> daCo = nguoiGiamHoRepository.findByHoSoBenhNhanIdAndCccd(hoSo.getId(), giamHo.cccd());
        if (daCo.isEmpty()) {
            NguoiGiamHo nguoiGiamHo = new NguoiGiamHo();
            nguoiGiamHo.setHoSoBenhNhan(hoSo);
            nguoiGiamHo.setHoTen(ChuanHoaTen.gon(giamHo.hoTen()));
            nguoiGiamHo.setQuanHe(giamHo.quanHe());
            nguoiGiamHo.setSoDienThoai(giamHo.soDienThoai());
            nguoiGiamHo.setCccd(giamHo.cccd());
            nguoiGiamHo.setNgaySinh(giamHo.ngaySinh());
            return nguoiGiamHoRepository.save(nguoiGiamHo);
        }
        NguoiGiamHo nguoiGiamHo = daCo.get();
        if (!khop(nguoiGiamHo.getHoTen(), nguoiGiamHo.getNgaySinh(), giamHo.hoTen(), giamHo.ngaySinh())) {
            throw new LoiNghiepVu(MaLoi.NGUOI_GIAM_HO_KHONG_HOP_LE,
                    "Số CCCD người giám hộ này đã được khai với họ tên hoặc ngày sinh khác, vui lòng kiểm tra lại");
        }
        if (nguoiGiamHo.getNgaySinh() == null) {
            nguoiGiamHo.setNgaySinh(giamHo.ngaySinh());
        }
        return nguoiGiamHo;
    }

    /** Họ tên giống nhau (không xét dấu, hoa/thường) và ngày sinh trùng; ngày sinh đang lưu còn trống thì coi là khớp. */
    private static boolean khop(String hoTenDaLuu, LocalDate ngaySinhDaLuu, String hoTen, LocalDate ngaySinh) {
        return ChuanHoaTen.giongNhau(hoTenDaLuu, hoTen) && (ngaySinhDaLuu == null || ngaySinhDaLuu.equals(ngaySinh));
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
