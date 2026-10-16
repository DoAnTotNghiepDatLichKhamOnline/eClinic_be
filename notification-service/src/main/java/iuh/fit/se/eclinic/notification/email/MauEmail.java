package iuh.fit.se.eclinic.notification.email;

import java.util.Locale;
import java.util.Set;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.thongbao.KhungEmail;
import iuh.fit.se.eclinic.common.thongbao.SoanThongBao;
import lombok.RequiredArgsConstructor;

/**
 * Soạn email lịch hẹn và email tái khám từ mẫu + tham số theo 1 ngôn ngữ. Câu chữ nằm trong thong-diep/notification*.properties: khoá của
 * mẫu ({@code email.datLich}, {@code email.doiLich}, {@code email.nhacLich}, {@code email.bacSiHuy}...) là tiêu đề, {@code <mau>.moDau} là đoạn mở đầu, các dòng chi tiết
 * và câu kết dùng chung ở {@code email.lichHen.*}.
 * <p>
 * Tham số (service nguồn ghi bằng {@code ThamSoThongBao}, đúng thứ tự): 0 họ tên bệnh nhân, 1 giờ khám, 2 họ tên bác sĩ,
 * 3 tên phòng, 4 tầng (rỗng nếu phòng không ghi tầng), 5 mã tra cứu, 6 link phiếu khám. Email không có lý do khám,
 * chuyên khoa, số điện thoại, CCCD. Lời chào không nêu tên: người nhận là người đặt, có thể không phải bệnh nhân.
 * <p>
 * Email tái khám ({@code email.taiKham}, {@code email.taiKhamDoiNgay}, {@code email.nhacTaiKham}) có 4 tham số: 0 họ tên
 * bệnh nhân, 1 ngày tái khám, 2 họ tên bác sĩ, 3 link trang đặt lịch. Tiêu đề không có tham số; không có chẩn đoán, ghi
 * chú, đơn thuốc; câu kết nói rõ chưa có lịch hẹn nào được đặt.
 */
@Component
@RequiredArgsConstructor
public class MauEmail {

    /** @param lienKet link trong email, chỉ để ghi log ở chế độ console */
    public record Thu(String tieuDe, String html, String lienKet) {
    }

    /** email.nhacLichChoXacNhan giữ cho email đã xếp hàng trước DOANTOTNGH-11 (không còn lịch hẹn chờ xác nhận). */
    private static final Set<String> CAC_MAU = Set.of("email.datLich", "email.doiLich", "email.nhacLich",
            "email.nhacLichChoXacNhan", "email.bacSiHuy");
    private static final int SO_THAM_SO = 7;
    private static final Set<String> CAC_MAU_TAI_KHAM = Set.of("email.taiKham", "email.taiKhamDoiNgay",
            "email.nhacTaiKham");
    private static final int SO_THAM_SO_TAI_KHAM = 4;

    private final SoanThongBao soanThongBao;
    private final MessageSource messageSource;

    /** @throws IllegalArgumentException mẫu không có, hoặc tham số không đọc được / không đủ */
    public Thu soan(String khoaMau, String thamSoJson, NgonNgu ngonNgu) {
        if (CAC_MAU_TAI_KHAM.contains(khoaMau)) {
            return soanTaiKham(khoaMau, thamSoJson, ngonNgu);
        }
        if (!CAC_MAU.contains(khoaMau)) {
            throw new IllegalArgumentException("Không có mẫu email này");
        }
        String[] thamSo = soanThongBao.thamSo(thamSoJson, ngonNgu);
        if (thamSo.length != SO_THAM_SO) {
            throw new IllegalArgumentException("Mẫu email cần " + SO_THAM_SO + " tham số");
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
                .nut("email.lichHen.nut", lienKet)
                .doan("email.lichHen.ketThuc")
                .doan("email.lichHen.khongPhaiBan");
        // Tiêu đề là chữ thường, không phải HTML: không escape
        String tieuDe = messageSource.getMessage(khoaMau, new Object[] {maTraCuu}, khoaMau, locale);
        return new Thu(tieuDe, khung.xong(), lienKet);
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
}
