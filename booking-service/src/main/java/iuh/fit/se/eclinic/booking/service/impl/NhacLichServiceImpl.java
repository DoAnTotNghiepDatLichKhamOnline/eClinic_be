package iuh.fit.se.eclinic.booking.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import iuh.fit.se.eclinic.booking.config.NhacLichProperties;
import iuh.fit.se.eclinic.booking.repository.LichHenRepository;
import iuh.fit.se.eclinic.booking.repository.NhacLichHenRepository;
import iuh.fit.se.eclinic.booking.service.NhacLichService;
import iuh.fit.se.eclinic.booking.service.ThongBaoLichHenService;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.booking.NhacLichHen;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;
import lombok.extern.slf4j.Slf4j;

/**
 * Không đặt {@code @Transactional} ở lớp: mỗi lịch hẹn 1 transaction riêng ({@link TransactionTemplate}), lịch này lỗi
 * không làm mất phần của lịch khác. Trong transaction đó: khoá dòng lịch hẹn, kiểm tra lại (người dùng có thể vừa hủy),
 * ghi mốc đã xử lý và các dòng thông báo / email chờ gửi. Chỉ khoá, không sửa lịch hẹn: {@code version} không đổi.
 * <p>
 * Chỉ 1 thread của scheduler gọi class này (booking-service chạy 1 bản); nếu có 2 bản thì UNIQUE của nhac_lich_hen làm
 * transaction đến sau rollback, vẫn không nhắc trùng.
 */
@Slf4j
@Service
public class NhacLichServiceImpl implements NhacLichService {

    private static final List<TrangThaiLichHen> CON_HIEU_LUC = List.of(TrangThaiLichHen.CHO_XAC_NHAN,
            TrangThaiLichHen.DA_XAC_NHAN);

    private final LichHenRepository lichHenRepository;
    private final NhacLichHenRepository nhacLichHenRepository;
    private final ThongBaoLichHenService thongBaoLichHenService;
    private final NhacLichProperties properties;
    private final TransactionTemplate giaoDich;

    public NhacLichServiceImpl(LichHenRepository lichHenRepository, NhacLichHenRepository nhacLichHenRepository,
            ThongBaoLichHenService thongBaoLichHenService, NhacLichProperties properties,
            PlatformTransactionManager transactionManager) {
        this.lichHenRepository = lichHenRepository;
        this.nhacLichHenRepository = nhacLichHenRepository;
        this.thongBaoLichHenService = thongBaoLichHenService;
        this.properties = properties;
        this.giaoDich = new TransactionTemplate(transactionManager);
    }

    @Override
    public int nhacDenHan() {
        LocalDateTime bayGio = LocalDateTime.now();
        LocalTime gio = bayGio.toLocalTime();
        if (gio.isBefore(properties.guiTu()) || !gio.isBefore(properties.guiDen())) {
            return 0;
        }
        LocalDateTime tu = bayGio.plus(properties.conToiThieu());
        int daNhac = 0;
        // cacMoc đã xếp mốc gần giờ khám nhất trước
        for (Duration moc : properties.cacMoc()) {
            List<Long> cacId = lichHenRepository.timIdCanNhac(CON_HIEU_LUC, tu, bayGio.plus(moc), soPhut(moc),
                    Limit.of(properties.kichThuocLo()));
            for (Long id : cacId) {
                try {
                    if (Boolean.TRUE.equals(giaoDich.execute(trangThai -> nhacMot(id, moc, bayGio)))) {
                        daNhac++;
                    }
                } catch (RuntimeException ex) {
                    log.error("Nhắc lịch hẹn id={} ở mốc {} thất bại", id, moc, ex);
                }
            }
        }
        return daNhac;
    }

    /** @return true = đã ghi thông báo / email nhắc; false = không nhắc (bỏ mốc, hoặc lịch hẹn vừa đổi trạng thái) */
    private boolean nhacMot(Long id, Duration moc, LocalDateTime bayGio) {
        LichHen lichHen = lichHenRepository.findByIdForUpdate(id).orElse(null);
        if (lichHen == null || !CON_HIEU_LUC.contains(lichHen.getTrangThai()) || lichHen.isCanDoiLich()) {
            return false;
        }
        LocalDateTime gioKham = lichHen.getKhungGio().getGioBatDau();
        if (!gioKham.isAfter(bayGio.plus(properties.conToiThieu())) || gioKham.isAfter(bayGio.plus(moc))
                || nhacLichHenRepository.existsByIdLichHenAndSoPhutTruoc(id, soPhut(moc))) {
            return false;
        }
        // Đặt khi đã ở trong khoảng của mốc: người đặt vừa nhận email xác nhận, không nhắc mốc này
        // Đã nhắc ở 1 mốc gần giờ khám hơn (chỉ xảy ra khi cấu hình thêm mốc mới): không nhắc lùi lại mốc xa hơn
        boolean nhac = !lichHen.getNgayTao().isAfter(gioKham.minus(moc))
                && !nhacLichHenRepository.existsByIdLichHenAndSoPhutTruocLessThanAndDaGuiTrue(id, soPhut(moc));
        nhacLichHenRepository.save(new NhacLichHen(id, soPhut(moc), nhac));
        // Các mốc xa hơn cũng đã đến hạn mà chưa xử lý (ban đêm, service ngừng): ghi nhận, không nhắc thêm
        for (Duration mocXaHon : properties.cacMoc()) {
            if (mocXaHon.compareTo(moc) > 0
                    && !nhacLichHenRepository.existsByIdLichHenAndSoPhutTruoc(id, soPhut(mocXaHon))) {
                nhacLichHenRepository.save(new NhacLichHen(id, soPhut(mocXaHon), false));
            }
        }
        if (nhac) {
            thongBaoLichHenService.nhacLich(lichHen);
        }
        return nhac;
    }

    private static int soPhut(Duration moc) {
        return Math.toIntExact(moc.toMinutes());
    }

}
