package iuh.fit.se.eclinic.chatbot.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Nhận ra tin nhắn có dấu hiệu cấp cứu bằng danh sách cụm từ ở {@code chatbot/khan-cap.txt} (tiếng Việt và tiếng Anh).
 * Chạy TRƯỚC khi gọi mô hình: trúng thì trả câu "gọi 115" cố định, không phụ thuộc mô hình có trả lời hay không.
 * <p>
 * Mỗi dòng của file là 1 dấu hiệu, gồm 1 hoặc nhiều phần nối bằng {@code +}; tin nhắn trúng khi có đủ mọi phần. Mỗi
 * phần khớp từ đầu 1 từ ("breath" khớp "breathing"). Phần viết không dấu được so với tin nhắn đã bỏ dấu (người dùng gõ
 * có dấu hay không đều trúng); phần viết có dấu chỉ so với tin nhắn giữ nguyên dấu, dùng cho cụm bỏ dấu thì trùng với
 * từ thường gặp ("tự tử" / "từ từ").
 */
@Component
public class KiemTraKhanCap {

    private static final String TEP = "chatbot/khan-cap.txt";

    private final List<List<String>> dauHieu = new ArrayList<>();

    public KiemTraKhanCap() {
        String noiDung;
        try {
            noiDung = new ClassPathResource(TEP).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        for (String dong : noiDung.split("\\R")) {
            if (dong.isBlank() || dong.stripLeading().startsWith("#")) {
                continue;
            }
            List<String> cacPhan = new ArrayList<>();
            for (String phan : dong.split("\\+")) {
                String chuan = chuanHoa(phan, true);
                if (!chuan.isBlank()) {
                    cacPhan.add(chuan);
                }
            }
            if (!cacPhan.isEmpty()) {
                dauHieu.add(cacPhan);
            }
        }
    }

    public boolean laKhanCap(String tinNhan) {
        String coDau = chuanHoa(tinNhan, true);
        String khongDau = chuanHoa(tinNhan, false);
        return dauHieu.stream().anyMatch(cacPhan -> cacPhan.stream()
                .allMatch(phan -> (laKhongDau(phan) ? khongDau : coDau).contains(phan)));
    }

    private static boolean laKhongDau(String phan) {
        return phan.chars().allMatch(c -> c < 128);
    }

    /**
     * Chữ thường, bỏ dấu nháy ("can't" thành "cant"), mọi ký tự không phải chữ / số thành 1 dấu cách, thêm 1 dấu cách ở
     * đầu để {@code contains} chỉ khớp từ đầu 1 từ.
     */
    private static String chuanHoa(String vanBan, boolean giuDau) {
        String chuoi = Normalizer.normalize(vanBan.toLowerCase(), giuDau ? Normalizer.Form.NFC : Normalizer.Form.NFD);
        if (!giuDau) {
            chuoi = chuoi.replaceAll("\\p{M}+", "").replace('đ', 'd');
        }
        chuoi = chuoi.replaceAll("['’`]", "").replaceAll("[^\\p{L}\\p{Nd}]+", " ").strip();
        return " " + chuoi;
    }

}
