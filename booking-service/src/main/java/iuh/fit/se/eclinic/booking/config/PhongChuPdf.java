package iuh.fit.se.eclinic.booking.config;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;

/** Font nhúng của bản in PDF: Noto Sans thường và đậm (SIL OFL 1.1), đủ chữ tiếng Việt. */
@Component
public class PhongChuPdf {

    private final BaseFont thuong;
    private final BaseFont dam;

    public PhongChuPdf() {
        this.thuong = nap("NotoSans-Regular.ttf");
        this.dam = nap("NotoSans-Bold.ttf");
    }

    private static BaseFont nap(String tenTep) {
        try (InputStream vao = new ClassPathResource("fonts/" + tenTep).getInputStream()) {
            // Tên phải kết thúc bằng .ttf để OpenPDF đọc là TrueType; IDENTITY_H = Unicode, không giới hạn bảng mã
            return BaseFont.createFont(tenTep, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, false, vao.readAllBytes(), null);
        } catch (IOException | DocumentException e) {
            throw new IllegalStateException("Không nạp được font PDF " + tenTep, e);
        }
    }

    public BaseFont baseThuong() {
        return thuong;
    }

    public Font thuong(float co) {
        return new Font(thuong, co);
    }

    public Font thuong(float co, Color mau) {
        return new Font(thuong, co, Font.NORMAL, mau);
    }

    public Font dam(float co) {
        return new Font(dam, co);
    }

    /** Đổi chữ người dùng gõ sang NFC trước khi in, vì font chỉ đặt dấu đúng với chữ dựng sẵn. */
    public static String nfc(String chu) {
        return chu == null ? "" : Normalizer.normalize(chu, Normalizer.Form.NFC);
    }

}
