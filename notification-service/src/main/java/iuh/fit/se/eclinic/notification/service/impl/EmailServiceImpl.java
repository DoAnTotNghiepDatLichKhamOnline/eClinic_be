package iuh.fit.se.eclinic.notification.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.data.domain.Limit;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.entity.notification.EmailGui;
import iuh.fit.se.eclinic.common.enums.TrangThaiEmail;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.notification.config.EmailProperties;
import iuh.fit.se.eclinic.notification.dto.request.TaoEmailRequest;
import iuh.fit.se.eclinic.notification.dto.response.KetQuaNhanThongBaoResponse;
import iuh.fit.se.eclinic.notification.email.MauEmail;
import iuh.fit.se.eclinic.notification.email.MayGuiEmail;
import iuh.fit.se.eclinic.notification.repository.EmailGuiRepository;
import iuh.fit.se.eclinic.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Log ở đây chỉ ghi id của dòng email trong hang_cho_gui và tên loại lỗi, không ghi địa chỉ nhận, tiêu đề hay nội dung. */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final String LOI_MAU = "MAU_KHONG_DUNG_DUOC";
    private static final int DO_DAI_LOI = 100;

    private final EmailGuiRepository emailGuiRepository;
    private final MauEmail mauEmail;
    private final MayGuiEmail mayGuiEmail;
    private final EmailProperties emailProperties;

    /** Chỉ ghi log khi chuyển trạng thái, để máy chủ mail ngừng lâu không làm đầy log. */
    private boolean dangLoiGui;

    /** 2 lô cùng {@code maNguon}: lô sau vi phạm UNIQUE và rollback, service nguồn gửi lại thì dòng cũ được bỏ qua. */
    @Override
    @Transactional
    public KetQuaNhanThongBaoResponse nhan(List<TaoEmailRequest> danhSach) {
        int daTao = 0;
        LocalDateTime bayGio = LocalDateTime.now();
        Set<String> trongLo = new HashSet<>();
        for (TaoEmailRequest request : danhSach) {
            if (!trongLo.add(request.maNguon()) || emailGuiRepository.existsByMaNguon(request.maNguon())) {
                continue;
            }
            try {
                // Soạn thử: email không dựng được thì từ chối ngay ở đây, không để nằm trong hàng chờ
                mauEmail.soan(request.khoaMau(), request.thamSo(), request.ngonNgu());
            } catch (IllegalArgumentException ex) {
                // Service nguồn gửi sai: 400 để nó ghi nhận lỗi của riêng dòng này
                throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.mauEmail");
            }
            EmailGui email = new EmailGui();
            email.setMaNguon(request.maNguon());
            email.setNguoiNhan(request.nguoiNhan());
            email.setNgonNgu(request.ngonNgu());
            email.setKhoaMau(request.khoaMau());
            email.setThamSo(request.thamSo());
            email.setIdLichHen(request.idLichHen());
            email.setTepLich(request.tepLich());
            email.setLanThuKeTiep(bayGio);
            emailGuiRepository.save(email);
            daTao++;
        }
        return new KetQuaNhanThongBaoResponse(daTao, danhSach.size() - daTao);
    }

    /** Không có {@code @Transactional}: lời gọi tới máy chủ mail không nằm trong transaction DB. */
    @Override
    public int guiDangCho() {
        List<EmailGui> denHan = emailGuiRepository.findByTrangThaiAndLanThuKeTiepLessThanEqualOrderByIdAsc(
                TrangThaiEmail.CHO_GUI, LocalDateTime.now(), Limit.of(emailProperties.kichThuocLo()));
        int daGui = 0;
        for (EmailGui email : denHan) {
            MauEmail.Thu thu;
            try {
                thu = mauEmail.soan(email.getKhoaMau(), email.getThamSo(), email.getNgonNgu());
            } catch (IllegalArgumentException ex) {
                // Thử lại cũng không dựng được: bỏ ngay
                emailGuiRepository.ghiLoi(email.getId(), email.getSoLanThu() + 1, TrangThaiEmail.THAT_BAI,
                        email.getLanThuKeTiep(), LOI_MAU);
                log.error("Bỏ email id={}: mẫu {} không dựng được", email.getId(), email.getKhoaMau());
                continue;
            }
            try {
                mayGuiEmail.gui(email.getNguoiNhan(), email.getNgonNgu(), thu, email.getTepLich());
            } catch (MailException ex) {
                ghiLoiGui(email, ex);
                continue;
            }
            emailGuiRepository.danhDauDaGui(email.getId(), LocalDateTime.now());
            daGui++;
        }
        if (daGui > 0 && dangLoiGui) {
            dangLoiGui = false;
            log.info("Đã gửi lại được email");
        }
        return daGui;
    }

    private void ghiLoiGui(EmailGui email, MailException ex) {
        String loi = tenLoi(ex);
        int soLanThu = email.getSoLanThu() + 1;
        List<Duration> cacLanCho = emailProperties.cacLanCho();
        if (soLanThu > cacLanCho.size()) {
            emailGuiRepository.ghiLoi(email.getId(), soLanThu, TrangThaiEmail.THAT_BAI, email.getLanThuKeTiep(), loi);
            log.error("Bỏ email id={} sau {} lần gửi lỗi ({})", email.getId(), soLanThu, loi);
            return;
        }
        emailGuiRepository.ghiLoi(email.getId(), soLanThu, TrangThaiEmail.CHO_GUI,
                LocalDateTime.now().plus(cacLanCho.get(soLanThu - 1)), loi);
        if (!dangLoiGui) {
            dangLoiGui = true;
            log.warn("Chưa gửi được email id={} ({}); email nằm chờ và được thử lại", email.getId(), loi);
        }
    }

    /** Tên loại lỗi và loại của nguyên nhân gốc; không lấy message (có thể chứa địa chỉ nhận). */
    private static String tenLoi(MailException ex) {
        Throwable goc = NestedExceptionUtils.getMostSpecificCause(ex);
        String ten = goc == ex ? ex.getClass().getSimpleName()
                : ex.getClass().getSimpleName() + "/" + goc.getClass().getSimpleName();
        return ten.length() > DO_DAI_LOI ? ten.substring(0, DO_DAI_LOI) : ten;
    }

    @Override
    public int donDaXong() {
        return emailGuiRepository.xoaDaXongTruoc(LocalDateTime.now().minusDays(emailProperties.soNgayGiu()));
    }

}
