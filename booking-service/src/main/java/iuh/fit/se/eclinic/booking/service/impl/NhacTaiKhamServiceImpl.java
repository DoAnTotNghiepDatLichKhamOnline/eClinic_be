package iuh.fit.se.eclinic.booking.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import iuh.fit.se.eclinic.booking.config.NhacLichProperties;
import iuh.fit.se.eclinic.booking.config.NhacTaiKhamProperties;
import iuh.fit.se.eclinic.booking.repository.HoSoBenhAnChiDocRepository;
import iuh.fit.se.eclinic.booking.repository.LichHenRepository;
import iuh.fit.se.eclinic.booking.repository.NhacTaiKhamRepository;
import iuh.fit.se.eclinic.booking.service.NhacTaiKhamService;
import iuh.fit.se.eclinic.booking.service.ThongBaoLichHenService;
import iuh.fit.se.eclinic.common.entity.booking.NhacTaiKham;
import iuh.fit.se.eclinic.common.entity.medical.HoSoBenhAn;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;
import lombok.extern.slf4j.Slf4j;

/**
 * Không đặt {@code @Transactional} ở lớp: mỗi hồ sơ bệnh án 1 transaction riêng ({@link TransactionTemplate}), hồ sơ này
 * lỗi không làm mất phần của hồ sơ khác. Trong transaction đó: khoá dòng nhac_tai_kham (chưa có thì UNIQUE làm
 * transaction đến sau rollback), đọc lại hồ sơ bệnh án, ghi ngày đã báo cùng các dòng thông báo / email chờ gửi.
 * <p>
 * Chỉ ĐỌC ho_so_benh_an và lich_hen: bác sĩ sửa hồ sơ bệnh án không bao giờ đụng job này. Bác sĩ đổi ngày đúng lúc job
 * đang chạy thì lần chạy sau thấy ngày khác ngày đã báo và báo lại.
 */
@Slf4j
@Service
public class NhacTaiKhamServiceImpl implements NhacTaiKhamService {

    private static final List<TrangThaiLichHen> CON_HIEU_LUC = List.of(TrangThaiLichHen.DA_XAC_NHAN);

    private final HoSoBenhAnChiDocRepository hoSoBenhAnRepository;
    private final NhacTaiKhamRepository nhacTaiKhamRepository;
    private final LichHenRepository lichHenRepository;
    private final ThongBaoLichHenService thongBaoLichHenService;
    private final NhacTaiKhamProperties properties;
    private final NhacLichProperties nhacLichProperties;
    private final TransactionTemplate giaoDich;

    public NhacTaiKhamServiceImpl(HoSoBenhAnChiDocRepository hoSoBenhAnRepository,
            NhacTaiKhamRepository nhacTaiKhamRepository, LichHenRepository lichHenRepository,
            ThongBaoLichHenService thongBaoLichHenService, NhacTaiKhamProperties properties,
            NhacLichProperties nhacLichProperties, PlatformTransactionManager transactionManager) {
        this.hoSoBenhAnRepository = hoSoBenhAnRepository;
        this.nhacTaiKhamRepository = nhacTaiKhamRepository;
        this.lichHenRepository = lichHenRepository;
        this.thongBaoLichHenService = thongBaoLichHenService;
        this.properties = properties;
        this.nhacLichProperties = nhacLichProperties;
        this.giaoDich = new TransactionTemplate(transactionManager);
    }

    @Override
    public int xuLyDenHan() {
        LocalDateTime bayGio = LocalDateTime.now();
        LocalTime gio = bayGio.toLocalTime();
        if (gio.isBefore(nhacLichProperties.guiTu()) || !gio.isBefore(nhacLichProperties.guiDen())) {
            return 0;
        }
        LocalDate homNay = bayGio.toLocalDate();
        Limit lo = Limit.of(nhacLichProperties.kichThuocLo());
        int daGhi = 0;
        for (Long id : hoSoBenhAnRepository.timIdCanBaoTaiKham(homNay, lo)) {
            daGhi += chay(id, () -> bao(id, homNay));
        }
        if (properties.truocNgay() > 0) {
            LocalDate den = homNay.plusDays(properties.truocNgay());
            for (Long id : hoSoBenhAnRepository.timIdCanNhacTaiKham(homNay, den, lo)) {
                daGhi += chay(id, () -> nhac(id, bayGio));
            }
        }
        giaoDich.executeWithoutResult(trangThai -> nhacTaiKhamRepository.quenNgayDaXoa());
        return daGhi;
    }

    private int chay(Long idHoSoBenhAn, BooleanSupplier viec) {
        try {
            return Boolean.TRUE.equals(giaoDich.execute(trangThai -> viec.getAsBoolean())) ? 1 : 0;
        } catch (RuntimeException ex) {
            log.error("Báo / nhắc tái khám cho hồ sơ bệnh án id={} thất bại", idHoSoBenhAn, ex);
            return 0;
        }
    }

    /** @return true = đã ghi thông báo / email báo ngày tái khám */
    private boolean bao(Long id, LocalDate homNay) {
        NhacTaiKham daBao = nhacTaiKhamRepository.timDeKhoa(id).orElse(null);
        HoSoBenhAn benhAn = hoSoBenhAnRepository.findById(id).orElse(null);
        LocalDate ngay = benhAn == null ? null : benhAn.getNgayTaiKhamDeXuat();
        if (ngay == null || !ngay.isAfter(homNay) || (daBao != null && ngay.equals(daBao.getNgayDaBao()))) {
            return false;
        }
        boolean doiNgay = daBao != null && daBao.getNgayDaBao() != null;
        if (daBao == null) {
            daBao = new NhacTaiKham(id);
        }
        daBao.setNgayDaBao(ngay);
        // Hẹn khi đã ở trong khoảng nhắc trước: bệnh nhân vừa được báo, không nhắc thêm
        daBao.setDaNhacTruoc(!ngay.isAfter(homNay.plusDays(properties.truocNgay())));
        nhacTaiKhamRepository.save(daBao);
        thongBaoLichHenService.taiKhamDaHen(benhAn, doiNgay);
        return true;
    }

    /** @return true = đã ghi thông báo / email nhắc; false = không nhắc (đã có lịch hẹn sắp tới, hoặc ngày vừa đổi) */
    private boolean nhac(Long id, LocalDateTime bayGio) {
        NhacTaiKham daBao = nhacTaiKhamRepository.timDeKhoa(id).orElse(null);
        HoSoBenhAn benhAn = hoSoBenhAnRepository.findById(id).orElse(null);
        LocalDate homNay = bayGio.toLocalDate();
        LocalDate ngay = benhAn == null ? null : benhAn.getNgayTaiKhamDeXuat();
        if (daBao == null || daBao.isDaNhacTruoc() || ngay == null || !ngay.equals(daBao.getNgayDaBao())
                || !ngay.isAfter(homNay) || ngay.isAfter(homNay.plusDays(properties.truocNgay()))) {
            return false;
        }
        daBao.setDaNhacTruoc(true);
        // Đã tự đặt lịch với bác sĩ cùng chuyên khoa: không nhắc "hãy đặt lịch tái khám" nữa
        if (lichHenRepository.coLichSapToi(benhAn.getHoSoBenhNhan().getId(), CON_HIEU_LUC, bayGio,
                benhAn.getBacSi().getChuyenKhoa().getId())) {
            return false;
        }
        thongBaoLichHenService.nhacTaiKham(benhAn);
        return true;
    }

}
