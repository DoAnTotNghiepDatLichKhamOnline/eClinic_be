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

/**
 * Font nhúng của các bản in PDF: Noto Sans thường + đậm (resources/fonts, giấy phép SIL OFL 1.1 trong OFL.txt), có đủ
 * chữ tiếng Việt dựng sẵn. Nạp 1 lần lúc khởi động (thiếu tệp font thì service không chạy) rồi dùng chung cho mọi tệp
 * PDF; mỗi tệp chỉ nhúng các chữ nó dùng (subset).
 */
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

    /**
     * Chữ người dùng gõ (họ tên, tên phòng) có thể ở dạng dấu tách rời (NFD, vd gõ trên macOS): font chỉ đặt dấu đúng
     * chỗ với chữ dựng sẵn, nên đổi sang NFC trước khi in.
     */
    public static String nfc(String chu) {
        return chu == null ? "" : Normalizer.normalize(chu, Normalizer.Form.NFC);
    }

}
