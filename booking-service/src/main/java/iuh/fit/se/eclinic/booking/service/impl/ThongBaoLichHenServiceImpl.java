package iuh.fit.se.eclinic.booking.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.repository.SuKienThongBaoRepository;
import iuh.fit.se.eclinic.booking.service.ThongBaoLichHenService;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.booking.SuKienThongBao;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.LoaiThongBao;
import lombok.RequiredArgsConstructor;

/**
 * MANDATORY: gọi ngoài transaction là lỗi lập trình, vì khi đó thông báo có thể được ghi mà lịch hẹn không đổi (hoặc
 * ngược lại).
 */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class ThongBaoLichHenServiceImpl implements ThongBaoLichHenService {

    private static final DateTimeFormatter GIO_NGAY = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");

    private final SuKienThongBaoRepository suKienThongBaoRepository;

    @Override
    public void lichHenMoi(LichHen lichHen) {
        ghi(LoaiThongBao.LICH_HEN_MOI, taiKhoanBacSi(lichHen), lichHen, "Yêu cầu đặt lịch mới: bệnh nhân "
                + tenBenhNhan(lichHen) + ", " + gioKham(lichHen) + " (mã " + lichHen.getMaTraCuu() + ").");
    }

    @Override
    public void daXacNhan(LichHen lichHen) {
        String noiDung = tenBacSi(lichHen) + " đã xác nhận lịch khám của " + tenBenhNhan(lichHen) + " lúc "
                + gioKham(lichHen) + " (mã " + lichHen.getMaTraCuu() + ").";
        for (Long idTaiKhoan : taiKhoanPhiaBenhNhan(lichHen)) {
            ghi(LoaiThongBao.LICH_HEN_DA_XAC_NHAN, idTaiKhoan, lichHen, noiDung);
        }
    }

    @Override
    public void biTuChoi(LichHen lichHen) {
        String noiDung = tenBacSi(lichHen) + " đã từ chối lịch khám của " + tenBenhNhan(lichHen) + " lúc "
                + gioKham(lichHen) + " (mã " + lichHen.getMaTraCuu() + "). Lý do: " + lichHen.getLyDoHuy();
        for (Long idTaiKhoan : taiKhoanPhiaBenhNhan(lichHen)) {
            ghi(LoaiThongBao.LICH_HEN_BI_TU_CHOI, idTaiKhoan, lichHen, noiDung);
        }
    }

    @Override
    public void benhNhanDaHuy(LichHen lichHen) {
        String noiDung = "Bệnh nhân " + tenBenhNhan(lichHen) + " đã hủy lịch khám lúc " + gioKham(lichHen) + " (mã "
                + lichHen.getMaTraCuu() + ")." + (lichHen.getLyDoHuy() == null ? "" : " Lý do: " + lichHen.getLyDoHuy());
        ghi(LoaiThongBao.LICH_HEN_DA_HUY, taiKhoanBacSi(lichHen), lichHen, noiDung);
    }

    @Override
    public void benhNhanDaDoi(LichHen lichCu, LichHen lichMoi) {
        if (lichCu.getBacSi().getId().equals(lichMoi.getBacSi().getId())) {
            ghi(LoaiThongBao.LICH_HEN_DA_DOI, taiKhoanBacSi(lichMoi), lichMoi, "Bệnh nhân " + tenBenhNhan(lichMoi)
                    + " đã đổi lịch khám từ " + gioKham(lichCu) + " sang " + gioKham(lichMoi) + " (mã mới "
                    + lichMoi.getMaTraCuu() + "), chờ bạn xác nhận.");
            return;
        }
        ghi(LoaiThongBao.LICH_HEN_DA_HUY, taiKhoanBacSi(lichCu), lichCu, "Bệnh nhân " + tenBenhNhan(lichCu)
                + " đã đổi lịch khám lúc " + gioKham(lichCu) + " (mã " + lichCu.getMaTraCuu()
                + ") sang bác sĩ khác; lịch hẹn này đã hủy.");
        lichHenMoi(lichMoi);
    }

    private void ghi(LoaiThongBao loai, Long idTaiKhoan, LichHen lichHen, String noiDung) {
        SuKienThongBao suKien = new SuKienThongBao();
        suKien.setLoai(loai);
        suKien.setIdTaiKhoan(idTaiKhoan);
        suKien.setIdLichHen(lichHen.getId());
        suKien.setNoiDung(noiDung);
        suKienThongBaoRepository.save(suKien);
    }

    private static Long taiKhoanBacSi(LichHen lichHen) {
        return lichHen.getBacSi().getTaiKhoan().getId();
    }

    /** Tài khoản đã đặt và tài khoản của hồ sơ bệnh nhân, bỏ null, mỗi tài khoản 1 lần. */
    static Set<Long> taiKhoanPhiaBenhNhan(LichHen lichHen) {
        Set<Long> cacId = new LinkedHashSet<>();
        TaiKhoan taiKhoanDat = lichHen.getTaiKhoanDat();
        if (taiKhoanDat != null) {
            cacId.add(taiKhoanDat.getId());
        }
        TaiKhoan chuHoSo = lichHen.getHoSoBenhNhan().getTaiKhoan();
        if (chuHoSo != null) {
            cacId.add(chuHoSo.getId());
        }
        return cacId;
    }

    /** Họ tên người đặt đã nhập cho lượt khám này; lịch hẹn tạo trước V10 thì lấy của hồ sơ. */
    private static String tenBenhNhan(LichHen lichHen) {
        return lichHen.getHoTenDaNhap() != null ? lichHen.getHoTenDaNhap() : lichHen.getHoSoBenhNhan().getHoTen();
    }

    private static String tenBacSi(LichHen lichHen) {
        return "Bác sĩ " + lichHen.getBacSi().getTaiKhoan().getHoTen();
    }

    private static String gioKham(LichHen lichHen) {
        LocalDateTime gioBatDau = lichHen.getKhungGio().getGioBatDau();
        return GIO_NGAY.format(gioBatDau);
    }

}
