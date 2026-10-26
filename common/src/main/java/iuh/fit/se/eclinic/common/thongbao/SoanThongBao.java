package iuh.fit.se.eclinic.common.thongbao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.common.enums.NgonNgu;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

/** Soạn câu thông báo từ khoá mẫu + tham số theo 1 ngôn ngữ. */
@Component
@RequiredArgsConstructor
public class SoanThongBao {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter GIO = DateTimeFormatter.ofPattern("HH:mm");
    private static final String KHOA_GIO_NGAY = "thongBao.dinhDang.gioNgay";

    private final MessageSource messageSource;

    public String soan(String khoaMau, String thamSoJson, NgonNgu ngonNgu) {
        Object[] thamSo = thamSo(thamSoJson, ngonNgu);
        return messageSource.getMessage(khoaMau, thamSo, khoaMau, ngonNgu.locale());
    }

    /** Từng tham số đã viết theo ngôn ngữ, chưa ghép vào câu nào. */
    public String[] thamSo(String thamSoJson, NgonNgu ngonNgu) {
        if (thamSoJson == null || thamSoJson.isBlank()) {
            return new String[0];
        }
        try {
            List<?> giaTri = JSON.readValue(thamSoJson, List.class);
            String[] thamSo = new String[giaTri.size()];
            for (int i = 0; i < thamSo.length; i++) {
                thamSo[i] = vietRa(giaTri.get(i), ngonNgu);
            }
            return thamSo;
        } catch (RuntimeException ex) {
            // Không kèm nội dung tham số (có họ tên bệnh nhân)
            throw new IllegalArgumentException("Tham số thông báo không đọc được: " + ex.getClass().getSimpleName());
        }
    }

    private String vietRa(Object giaTri, NgonNgu ngonNgu) {
        if (giaTri instanceof String chu) {
            return chu;
        }
        if (!(giaTri instanceof Map<?, ?> coKieu)) {
            throw new IllegalArgumentException("tham so");
        }
        if (coKieu.get(ThamSoThongBao.GIO_NGAY) instanceof String gioNgay) {
            LocalDateTime thoiDiem = LocalDateTime.parse(gioNgay);
            String khuon = thoiDiem.format(GIO) + " " + thoiDiem.format(NGAY);
            return messageSource.getMessage(KHOA_GIO_NGAY,
                    new Object[] {thoiDiem.format(GIO), thoiDiem.format(NGAY)}, khuon, ngonNgu.locale());
        }
        if (coKieu.get(ThamSoThongBao.NGAY) instanceof String ngay) {
            return LocalDate.parse(ngay).format(NGAY);
        }
        if (coKieu.get(ThamSoThongBao.GIO) instanceof String gio) {
            return LocalTime.parse(gio).format(GIO);
        }
        if (coKieu.get(ThamSoThongBao.KHOA) instanceof String khoa) {
            return messageSource.getMessage(khoa, null, khoa, ngonNgu.locale());
        }
        if (coKieu.get(ThamSoThongBao.TIENG_VIET) instanceof String tiengViet) {
            return ngonNgu.chon(tiengViet, coKieu.get(ThamSoThongBao.TIENG_ANH) instanceof String tiengAnh ? tiengAnh
                    : null);
        }
        throw new IllegalArgumentException("tham so");
    }
}
