package iuh.fit.se.eclinic.notification.email;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.thongbao.KhungEmail;
import iuh.fit.se.eclinic.common.thongbao.SoanThongBao;
import lombok.RequiredArgsConstructor;

/** Soạn email lịch hẹn và email tái khám từ mẫu + tham số theo 1 ngôn ngữ. */
@Component
@RequiredArgsConstructor
public class MauEmail {

    /** @param lienKet link trong email, chỉ để ghi log ở chế độ console */
    public record Thu(String tieuDe, String html, String lienKet) {
    }

    /** email.nhacLichChoXacNhan giữ cho email đã xếp hàng từ trước (không còn lịch hẹn chờ xác nhận). */
    private static final Set<String> CAC_MAU = Set.of("email.datLich", "email.doiLich", "email.nhacLich",
            "email.nhacLichChoXacNhan", "email.bacSiHuy", "email.doiBacSi", "email.canDoiLich",
            "email.quanTriHuy");
    private static final int SO_THAM_SO = 7;
    private static final String MAU_CAP_LAI = "email.capLaiLienKet";
    private static final Set<String> CAC_MAU_TAI_KHAM = Set.of("email.taiKham", "email.taiKhamDoiNgay",
            "email.nhacTaiKham");
    private static final int SO_THAM_SO_TAI_KHAM = 4;

    private final SoanThongBao soanThongBao;
    private final MessageSource messageSource;

    private static final String MAU_LICH_LAM_VIEC = "email.lichLamViec";
    private static final int SO_THAM_SO_CO_DINH_LICH = 6;
    private static final int SO_THAM_SO_MOI_CA = 4;
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] COT_LICH = {"email.lichLamViec.cot.ngay", "email.lichLamViec.cot.gio",
        "email.lichLamViec.cot.phong"};

    /** @throws IllegalArgumentException mẫu không có, hoặc tham số không đọc được / không đủ */
    public Thu soan(String khoaMau, String thamSoJson, NgonNgu ngonNgu) {
        if (CAC_MAU_TAI_KHAM.contains(khoaMau)) {
            return soanTaiKham(khoaMau, thamSoJson, ngonNgu);
        }
        if (MAU_LICH_LAM_VIEC.equals(khoaMau)) {
            return soanLichLamViec(thamSoJson, ngonNgu);
        }
        boolean capLai = MAU_CAP_LAI.equals(khoaMau);
        if (!capLai && !CAC_MAU.contains(khoaMau)) {
            throw new IllegalArgumentException("Không có mẫu email này");
        }
        String[] thamSo = soanThongBao.thamSo(thamSoJson, ngonNgu);
        int soThamSo = capLai ? SO_THAM_SO + 1 : SO_THAM_SO;
        if (thamSo.length != soThamSo) {
            throw new IllegalArgumentException("Mẫu email cần " + soThamSo + " tham số");
        }
        Locale locale = ngonNgu.locale();
        String maTraCuu = thamSo[5];
        String lienKet = thamSo[6];
        KhungEmail khung = new KhungEmail((khoa, giaTri) -> messageSource.getMessage(khoa, giaTri, khoa, locale), null)
                .doan(khoaMau + ".moDau")
                .dong("email.lichHen.benhNhan", thamSo[0])
                .dong("email.lichHen.thoiGian", thamSo[1])
                .dong("email.lichHen.bacSi", thamSo[2]);
        if (thamSo[4].isBlank()) {
            khung.dong("email.lichHen.phong", thamSo[3]);
        } else {
            khung.dong("email.lichHen.phong.coTang", thamSo[3], thamSo[4]);
        }
        khung.dong("email.lichHen.maTraCuu", maTraCuu)
                .nut("email.lichHen.nut", lienKet);
        String lienKetSoKham = capLai ? thamSo[SO_THAM_SO] : "";
        if (!lienKetSoKham.isBlank()) {
            khung.doan(MAU_CAP_LAI + ".soKham")
                    .nut(MAU_CAP_LAI + ".nutSoKham", lienKetSoKham);
        }
        khung.doan("email.lichHen.ketThuc")
                .doan("email.lichHen.khongPhaiBan");
        // Tiêu đề là chữ thường, không phải HTML: không escape
        String tieuDe = messageSource.getMessage(khoaMau, new Object[] {maTraCuu}, khoaMau, locale);
        return new Thu(tieuDe, khung.xong(), lienKetSoKham.isBlank() ? lienKet : lienKet + " " + lienKetSoKham);
    }

    private Thu soanTaiKham(String khoaMau, String thamSoJson, NgonNgu ngonNgu) {
        String[] thamSo = soanThongBao.thamSo(thamSoJson, ngonNgu);
        if (thamSo.length != SO_THAM_SO_TAI_KHAM) {
            throw new IllegalArgumentException("Mẫu email cần " + SO_THAM_SO_TAI_KHAM + " tham số");
        }
        Locale locale = ngonNgu.locale();
        String lienKet = thamSo[3];
        String html = new KhungEmail((khoa, giaTri) -> messageSource.getMessage(khoa, giaTri, khoa, locale), null)
                .doan(khoaMau + ".moDau")
                .dong("email.lichHen.benhNhan", thamSo[0])
                .dong("email.lichHen.bacSi", thamSo[2])
                .dong("email.taiKham.ngay", thamSo[1])
                .nut("email.taiKham.nut", lienKet)
                .doan("email.taiKham.ketThuc")
                .xong();
        return new Thu(messageSource.getMessage(khoaMau, null, khoaMau, locale), html, lienKet);
    }

    private Thu soanLichLamViec(String thamSoJson, NgonNgu ngonNgu) {
        String[] thamSo = soanThongBao.thamSo(thamSoJson, ngonNgu);
        int soThamSoCa = thamSo.length - SO_THAM_SO_CO_DINH_LICH;
        if (soThamSoCa <= 0 || soThamSoCa % SO_THAM_SO_MOI_CA != 0) {
            throw new IllegalArgumentException("Mẫu email lịch làm việc cần " + SO_THAM_SO_CO_DINH_LICH + " tham số + "
                    + SO_THAM_SO_MOI_CA + " tham số mỗi ca");
        }
        Locale locale = ngonNgu.locale();
        String tuNgay = thamSo[1];
        String denNgay = thamSo[2];
        String lienKet = thamSo[5];
        List<String[]> cacDong = new ArrayList<>();
        for (int i = SO_THAM_SO_CO_DINH_LICH; i < thamSo.length; i += SO_THAM_SO_MOI_CA) {
            cacDong.add(new String[] {thu(thamSo[i], locale) + ", " + thamSo[i], thamSo[i + 1] + "–" + thamSo[i + 2],
                thamSo[i + 3]});
        }
        KhungEmail khung = new KhungEmail((khoa, giaTri) -> messageSource.getMessage(khoa, giaTri, khoa, locale),
                thamSo[0])
                .doan(MAU_LICH_LAM_VIEC + ".moDau", tuNgay, denNgay, thamSo[3])
                .bang(COT_LICH, cacDong);
        if (!"0".equals(thamSo[4])) {
            khung.doan(MAU_LICH_LAM_VIEC + ".conNua", thamSo[4]);
        }
        String html = khung.nut(MAU_LICH_LAM_VIEC + ".nut", lienKet)
                .doan(MAU_LICH_LAM_VIEC + ".ketThuc")
                .xong();
        return new Thu(messageSource.getMessage(MAU_LICH_LAM_VIEC, new Object[] {tuNgay, denNgay}, MAU_LICH_LAM_VIEC,
                locale), html, lienKet);
    }

    /** Tên thứ của ngày dd/MM/yyyy theo ngôn ngữ ("Thứ Hai" / "Monday"), viết hoa chữ đầu. */
    private static String thu(String ngay, Locale locale) {
        try {
            String ten = LocalDate.parse(ngay, NGAY).getDayOfWeek().getDisplayName(TextStyle.FULL, locale);
            return ten.isEmpty() ? ten : Character.toUpperCase(ten.charAt(0)) + ten.substring(1);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Ngày của ca không đọc được");
        }
    }
}
