package iuh.fit.se.eclinic.booking.service.impl;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.config.NhacTaiKhamProperties;
import iuh.fit.se.eclinic.booking.config.PhieuKhamProperties;
import iuh.fit.se.eclinic.booking.repository.SuKienThongBaoRepository;
import iuh.fit.se.eclinic.booking.service.ThongBaoLichHenService;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.booking.SuKienThongBao;
import iuh.fit.se.eclinic.common.entity.catalog.PhongKham;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.entity.medical.HoSoBenhAn;
import iuh.fit.se.eclinic.common.enums.KenhThongBao;
import iuh.fit.se.eclinic.common.enums.LoaiThongBao;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;
import iuh.fit.se.eclinic.common.thongbao.ThamSoThongBao;
import lombok.RequiredArgsConstructor;

/**
 * MANDATORY: gọi ngoài transaction là lỗi lập trình, vì khi đó thông báo có thể được ghi mà lịch hẹn không đổi (hoặc
 * ngược lại).
 * <p>
 * Không soạn câu ở đây: mỗi thông báo ghi khoá mẫu ({@code thongBao.<mau>} trong thong-diep/notification*.properties) và
 * tham số; notification-service soạn câu theo ngôn ngữ của người đọc. Họ tên, mã tra cứu, lý do người dùng gõ là tham
 * số chữ thường nên giữ nguyên văn ở mọi ngôn ngữ.
 */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class ThongBaoLichHenServiceImpl implements ThongBaoLichHenService {

    private final SuKienThongBaoRepository suKienThongBaoRepository;
    private final PhieuKhamProperties phieuKhamProperties;
    private final NhacTaiKhamProperties nhacTaiKhamProperties;

    @Override
    public void lichHenMoi(LichHen lichHen) {
        ghi(LoaiThongBao.LICH_HEN_MOI, taiKhoanBacSi(lichHen), lichHen, "thongBao.lichHenMoi", ThamSoThongBao.tao()
                .chu(tenBenhNhan(lichHen)).gioNgay(gioKham(lichHen)).chu(lichHen.getMaTraCuu()));
    }

    @Override
    public void daXacNhan(LichHen lichHen) {
        ThamSoThongBao thamSo = ThamSoThongBao.tao().chu(tenBacSi(lichHen)).chu(tenBenhNhan(lichHen))
                .gioNgay(gioKham(lichHen)).chu(lichHen.getMaTraCuu());
        for (Long idTaiKhoan : taiKhoanPhiaBenhNhan(lichHen)) {
            ghi(LoaiThongBao.LICH_HEN_DA_XAC_NHAN, idTaiKhoan, lichHen, "thongBao.daXacNhan", thamSo);
        }
    }

    @Override
    public void biTuChoi(LichHen lichHen) {
        ThamSoThongBao thamSo = ThamSoThongBao.tao().chu(tenBacSi(lichHen)).chu(tenBenhNhan(lichHen))
                .gioNgay(gioKham(lichHen)).chu(lichHen.getMaTraCuu()).chu(lichHen.getLyDoHuy());
        for (Long idTaiKhoan : taiKhoanPhiaBenhNhan(lichHen)) {
            ghi(LoaiThongBao.LICH_HEN_BI_TU_CHOI, idTaiKhoan, lichHen, "thongBao.biTuChoi", thamSo);
        }
    }

    @Override
    public void benhNhanDaHuy(LichHen lichHen) {
        ThamSoThongBao thamSo = ThamSoThongBao.tao().chu(tenBenhNhan(lichHen)).gioNgay(gioKham(lichHen))
                .chu(lichHen.getMaTraCuu());
        if (lichHen.getLyDoHuy() == null) {
            ghi(LoaiThongBao.LICH_HEN_DA_HUY, taiKhoanBacSi(lichHen), lichHen, "thongBao.benhNhanDaHuy", thamSo);
            return;
        }
        ghi(LoaiThongBao.LICH_HEN_DA_HUY, taiKhoanBacSi(lichHen), lichHen, "thongBao.benhNhanDaHuy.coLyDo",
                thamSo.chu(lichHen.getLyDoHuy()));
    }

    @Override
    public void benhNhanDaDoi(LichHen lichCu, LichHen lichMoi) {
        if (lichCu.getBacSi().getId().equals(lichMoi.getBacSi().getId())) {
            ghi(LoaiThongBao.LICH_HEN_DA_DOI, taiKhoanBacSi(lichMoi), lichMoi, "thongBao.benhNhanDaDoi",
                    ThamSoThongBao.tao().chu(tenBenhNhan(lichMoi)).gioNgay(gioKham(lichCu)).gioNgay(gioKham(lichMoi))
                            .chu(lichMoi.getMaTraCuu()));
            return;
        }
        ghi(LoaiThongBao.LICH_HEN_DA_HUY, taiKhoanBacSi(lichCu), lichCu, "thongBao.benhNhanDoiBacSi",
                ThamSoThongBao.tao().chu(tenBenhNhan(lichCu)).gioNgay(gioKham(lichCu)).chu(lichCu.getMaTraCuu()));
        lichHenMoi(lichMoi);
    }

    @Override
    public void emailDaNhanLich(LichHen lichHen) {
        ghiEmail(lichHen, "email.datLich");
    }

    @Override
    public void emailDaDoiLich(LichHen lichMoi) {
        ghiEmail(lichMoi, "email.doiLich");
    }

    @Override
    public void nhacLich(LichHen lichHen) {
        boolean choXacNhan = lichHen.getTrangThai() == TrangThaiLichHen.CHO_XAC_NHAN;
        PhongKham phong = lichHen.getPhongKham();
        ThamSoThongBao thamSo = ThamSoThongBao.tao().chu(tenBenhNhan(lichHen)).gioNgay(gioKham(lichHen))
                .chu(tenBacSi(lichHen)).haiNgonNgu(phong.getTenPhong(), phong.getTenPhongEn())
                .chu(lichHen.getMaTraCuu());
        for (Long idTaiKhoan : taiKhoanPhiaBenhNhan(lichHen)) {
            ghi(LoaiThongBao.NHAC_LICH_KHAM, idTaiKhoan, lichHen,
                    choXacNhan ? "thongBao.nhacLich.choXacNhan" : "thongBao.nhacLich", thamSo);
        }
        ghiEmail(lichHen, choXacNhan ? "email.nhacLichChoXacNhan" : "email.nhacLich");
    }

    @Override
    public void taiKhamDaHen(HoSoBenhAn benhAn, boolean doiNgay) {
        ghiTaiKham(benhAn, doiNgay ? "thongBao.taiKham.doiNgay" : "thongBao.taiKham",
                doiNgay ? "email.taiKhamDoiNgay" : "email.taiKham");
    }

    @Override
    public void nhacTaiKham(HoSoBenhAn benhAn) {
        ghiTaiKham(benhAn, "thongBao.taiKham.nhac", "email.nhacTaiKham");
    }

    /**
     * Thứ tự tham số của mẫu thông báo tái khám: họ tên bệnh nhân, họ tên bác sĩ, ngày tái khám. Của mẫu email tái khám
     * (notification-service: {@code MauEmail}): họ tên bệnh nhân, ngày tái khám, họ tên bác sĩ, link trang đặt lịch.
     */
    private void ghiTaiKham(HoSoBenhAn benhAn, String khoaThongBao, String khoaEmail) {
        LichHen lichHen = benhAn.getLichHen();
        String tenBacSi = benhAn.getBacSi().getTaiKhoan().getHoTen();
        ThamSoThongBao thamSo = ThamSoThongBao.tao().chu(tenBenhNhan(lichHen)).chu(tenBacSi)
                .ngay(benhAn.getNgayTaiKhamDeXuat());
        for (Long idTaiKhoan : taiKhoanPhiaBenhNhan(lichHen)) {
            ghi(LoaiThongBao.NHAC_TAI_KHAM, idTaiKhoan, lichHen, khoaThongBao, thamSo);
        }
        SuKienThongBao email = emailCho(lichHen);
        if (email == null) {
            return;
        }
        email.setKhoaMau(khoaEmail);
        email.setThamSo(ThamSoThongBao.tao().chu(tenBenhNhan(lichHen)).ngay(benhAn.getNgayTaiKhamDeXuat())
                .chu(tenBacSi)
                .chu(phieuKhamProperties.frontendUrl() + nhacTaiKhamProperties.duongDanDatLich()).toJson());
        suKienThongBaoRepository.save(email);
    }

    /**
     * Thứ tự tham số của mọi mẫu email lịch hẹn (notification-service: {@code MauEmail}): họ tên bệnh nhân, giờ khám,
     * họ tên bác sĩ, tên phòng, tầng (chuỗi rỗng nếu phòng không ghi tầng), mã tra cứu, link phiếu khám.
     */
    private void ghiEmail(LichHen lichHen, String khoaMau) {
        SuKienThongBao suKien = emailCho(lichHen);
        if (suKien == null) {
            return;
        }
        PhongKham phong = lichHen.getPhongKham();
        suKien.setKhoaMau(khoaMau);
        suKien.setThamSo(ThamSoThongBao.tao().chu(tenBenhNhan(lichHen)).gioNgay(gioKham(lichHen))
                .chu(tenBacSi(lichHen)).haiNgonNgu(phong.getTenPhong(), phong.getTenPhongEn())
                .haiNgonNgu(phong.getTang(), phong.getTangEn()).chu(lichHen.getMaTraCuu())
                .chu(phieuKhamProperties.lienKet(lichHen.getMaTokenPhieuKham())).toJson());
        suKienThongBaoRepository.save(suKien);
    }

    /**
     * Dòng email chờ gửi cho người đã đặt lịch hẹn này, đã có địa chỉ nhận và ngôn ngữ, chưa có mẫu / tham số và chưa
     * lưu. Địa chỉ: email liên hệ của lịch hẹn, để trống thì email của tài khoản đã đặt. Ngôn ngữ: của tài khoản đã
     * đặt, khách thì của request đã tạo lịch hẹn.
     *
     * @return null nếu không có địa chỉ nào (khách đặt không nhập email)
     */
    private static SuKienThongBao emailCho(LichHen lichHen) {
        TaiKhoan taiKhoanDat = lichHen.getTaiKhoanDat();
        String emailNhan = lichHen.getEmailLienHe();
        if (emailNhan == null && taiKhoanDat != null) {
            emailNhan = taiKhoanDat.getEmail();
        }
        if (emailNhan == null) {
            return null;
        }
        NgonNgu ngonNgu = taiKhoanDat != null ? taiKhoanDat.getNgonNgu() : lichHen.getNgonNgu();
        SuKienThongBao suKien = new SuKienThongBao();
        suKien.setKenh(KenhThongBao.EMAIL);
        suKien.setEmailNhan(emailNhan);
        suKien.setNgonNgu(ngonNgu == null ? NgonNgu.VI : ngonNgu);
        suKien.setIdLichHen(lichHen.getId());
        return suKien;
    }

    private void ghi(LoaiThongBao loai, Long idTaiKhoan, LichHen lichHen, String khoaMau, ThamSoThongBao thamSo) {
        SuKienThongBao suKien = new SuKienThongBao();
        suKien.setLoai(loai);
        suKien.setIdTaiKhoan(idTaiKhoan);
        suKien.setIdLichHen(lichHen.getId());
        suKien.setKhoaMau(khoaMau);
        suKien.setThamSo(thamSo.toJson());
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

    /** Chỉ họ tên; chữ "Bác sĩ" / "Dr." nằm trong mẫu của từng ngôn ngữ. */
    private static String tenBacSi(LichHen lichHen) {
        return lichHen.getBacSi().getTaiKhoan().getHoTen();
    }

    private static LocalDateTime gioKham(LichHen lichHen) {
        return lichHen.getKhungGio().getGioBatDau();
    }
}
