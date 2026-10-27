package iuh.fit.se.eclinic.booking.util;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import iuh.fit.se.eclinic.common.config.AppTimeZone;

/** Soạn tệp lịch .ics (RFC 5545) cho 1 lịch hẹn: 1 VCALENDAR {@code METHOD:PUBLISH} chứa 1 VEVENT. */
public final class TepLichIcs {

    /** Trạng thái của sự kiện (thuộc tính STATUS). */
    public enum TrangThai {
        CONFIRMED,
        /** Ca khám đã bị hủy, lịch hẹn đang chờ đổi sang giờ khác. */
        TENTATIVE,
        CANCELLED
    }

    public record SuKien(String uid, int sequence, TrangThai trangThai, LocalDateTime batDau, LocalDateTime ketThuc,
            String tieuDe, String diaDiem, List<String> moTa, String lienKet, String nhac) {
    }

    private static final String XUONG_DONG = "\r\n";
    private static final int SO_BYTE_TOI_DA = 75;
    private static final ZoneId MUI_GIO = ZoneId.of(AppTimeZone.ID);
    private static final DateTimeFormatter UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(ZoneOffset.UTC);
    /** Lời nhắc trước giờ khám 1 tiếng. */
    private static final String NHAC_TRUOC = "-PT1H";

    private TepLichIcs() {
    }

    public static String tao(SuKien suKien, Instant bayGio) {
        StringBuilder tep = new StringBuilder();
        dong(tep, "BEGIN:VCALENDAR");
        dong(tep, "VERSION:2.0");
        dong(tep, "PRODID:-//eClinic//Dat lich kham//VI");
        dong(tep, "CALSCALE:GREGORIAN");
        dong(tep, "METHOD:PUBLISH");
        dong(tep, "BEGIN:VEVENT");
        dong(tep, "UID:" + suKien.uid());
        dong(tep, "SEQUENCE:" + suKien.sequence());
        dong(tep, "DTSTAMP:" + UTC.format(bayGio));
        dong(tep, "DTSTART:" + utc(suKien.batDau()));
        dong(tep, "DTEND:" + utc(suKien.ketThuc()));
        dong(tep, "STATUS:" + suKien.trangThai().name());
        dong(tep, "SUMMARY:" + chu(suKien.tieuDe()));
        dong(tep, "LOCATION:" + chu(suKien.diaDiem()));
        dong(tep, "DESCRIPTION:" + chu(String.join("\n", suKien.moTa())));
        dong(tep, "URL:" + suKien.lienKet());
        if (suKien.nhac() != null && suKien.trangThai() != TrangThai.CANCELLED) {
            dong(tep, "BEGIN:VALARM");
            dong(tep, "ACTION:DISPLAY");
            dong(tep, "TRIGGER:" + NHAC_TRUOC);
            dong(tep, "DESCRIPTION:" + chu(suKien.nhac()));
            dong(tep, "END:VALARM");
        }
        dong(tep, "END:VEVENT");
        dong(tep, "END:VCALENDAR");
        return tep.toString();
    }

    private static String utc(LocalDateTime gio) {
        return UTC.format(gio.atZone(MUI_GIO));
    }

    /** Escape giá trị kiểu TEXT: \ ; , và xuống dòng. */
    static String chu(String giaTri) {
        if (giaTri == null) {
            return "";
        }
        return giaTri.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,")
                .replace("\r\n", "\\n").replace("\n", "\\n").replace("\r", "\\n");
    }

    /** Thêm 1 dòng logic, gập thành các dòng tối đa 75 byte; dòng tiếp theo bắt đầu bằng 1 dấu cách. */
    static void dong(StringBuilder tep, String noiDung) {
        int soByte = 0;
        int i = 0;
        while (i < noiDung.length()) {
            int kyTu = noiDung.codePointAt(i);
            int doDai = new String(Character.toChars(kyTu)).getBytes(StandardCharsets.UTF_8).length;
            if (soByte + doDai > SO_BYTE_TOI_DA) {
                tep.append(XUONG_DONG).append(' ');
                soByte = 1;
            }
            tep.appendCodePoint(kyTu);
            soByte += doDai;
            i += Character.charCount(kyTu);
        }
        tep.append(XUONG_DONG);
    }

}
