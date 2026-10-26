package iuh.fit.se.eclinic.chatbot.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.common.entity.catalog.ChuyenKhoa;
import iuh.fit.se.eclinic.common.enums.NgonNgu;

/**
 * Chỉ dẫn hệ thống gửi cho mô hình, lấy từ {@code chatbot/loi-nhac-he-thong.txt}: phạm vi được trả lời, điều cấm, dấu
 * hiệu "ngoài phạm vi", ngôn ngữ trả lời, danh sách chuyên khoa của phòng khám (dữ liệu công khai), ngày hôm nay, và 1
 * trong 2 đoạn về dữ liệu cá nhân (có / không có 2 hàm "của tôi"). Bản thân chỉ dẫn không chứa dữ liệu cá nhân nào của
 * người dùng, kể cả tên; không có bí mật nào trong đó.
 * <p>
 * Chỉ dẫn này KHÔNG phải ranh giới bảo mật: mô hình có thể bị dụ làm trái. Phiên chat của ai, giới hạn số tin nhắn,
 * tình huống cấp cứu, ai được đọc lịch hẹn của ai đều do code kiểm tra; mô hình chỉ nhận dữ liệu của chính người đang
 * chat, sau khi người đó đồng ý.
 */
@Component
public class LoiNhacHeThong {

    /** Mô hình trả đúng chuỗi này khi câu hỏi ngoài phạm vi; server thay bằng câu từ chối của mình. */
    public static final String DANH_DAU_NGOAI_PHAM_VI = "[NGOAI_PHAM_VI]";

    /**
     * Mở đầu dòng ghi chú mà server gắn vào 1 câu trả lời cũ khi gửi lại lịch sử cho mô hình: các id đã hiện cho người
     * dùng cùng câu trả lời đó, để mô hình tra cứu tiếp được. Không lưu, không trả cho người dùng.
     */
    public static final String GHI_CHU_DA_HIEN = "[DATA SHOWN TO THE USER WITH THIS ANSWER]";

    private static final String TEP = "chatbot/loi-nhac-he-thong.txt";
    private static final String TEP_CO_CA_NHAN = "chatbot/du-lieu-ca-nhan-co.txt";
    private static final String TEP_KHONG_CA_NHAN = "chatbot/du-lieu-ca-nhan-khong.txt";
    /** Các dòng chỉ có trong chỉ dẫn: câu trả lời chứa 1 dòng là mô hình đang chép lại chỉ dẫn. */
    private static final List<String> DAU_HIEU_LO = List.of("WHAT YOU HELP WITH", "WHAT YOU KNOW ABOUT THE SITE",
            "SPECIALTIES OF THIS CLINIC", "LOOKING UP DOCTORS AND FREE TIMES", GHI_CHU_DA_HIEN);
    private static final int SO_CHUYEN_KHOA_TOI_DA = 40;
    private static final int DO_DAI_TOI_DA = 600;

    private final String mau;
    private final String coCaNhan;
    private final String khongCaNhan;

    public LoiNhacHeThong() {
        try {
            this.mau = new ClassPathResource(TEP).getContentAsString(StandardCharsets.UTF_8)
                    .replace("{{DANH_DAU}}", DANH_DAU_NGOAI_PHAM_VI)
                    .replace("{{GHI_CHU_DA_HIEN}}", GHI_CHU_DA_HIEN);
            this.coCaNhan = new ClassPathResource(TEP_CO_CA_NHAN).getContentAsString(StandardCharsets.UTF_8).strip();
            this.khongCaNhan = new ClassPathResource(TEP_KHONG_CA_NHAN).getContentAsString(StandardCharsets.UTF_8)
                    .strip();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * @param cacChuyenKhoa mọi chuyên khoa của phòng khám; tên, mô tả, triệu chứng lấy theo {@code ngonNgu}
     * @param coCaNhan      true: người chat được đưa 2 hàm đọc lịch hẹn, lượt khám của chính mình
     */
    public String soan(NgonNgu ngonNgu, List<ChuyenKhoa> cacChuyenKhoa, LocalDate homNay, boolean coCaNhan) {
        StringBuilder danhSach = new StringBuilder();
        for (ChuyenKhoa chuyenKhoa : cacChuyenKhoa.stream().limit(SO_CHUYEN_KHOA_TOI_DA).toList()) {
            danhSach.append("- id ").append(chuyenKhoa.getId()).append(" | ").append(motDong(chuyenKhoa.ten(ngonNgu)));
            themPhan(danhSach, "treats", chuyenKhoa.moTa(ngonNgu));
            themPhan(danhSach, "common symptoms", chuyenKhoa.trieuChung(ngonNgu));
            danhSach.append('\n');
        }
        if (danhSach.isEmpty()) {
            danhSach.append("(the clinic has not published any specialty yet)\n");
        }
        return mau.replace("{{NGON_NGU}}", ngonNgu == NgonNgu.EN ? "English" : "Vietnamese")
                .replace("{{HOM_NAY}}",
                        homNay + " (" + homNay.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + ")")
                .replace("{{DU_LIEU_CA_NHAN}}", coCaNhan ? this.coCaNhan : khongCaNhan)
                .replace("{{CHUYEN_KHOA}}", danhSach.toString().stripTrailing());
    }

    /**
     * Câu trả lời có chép lại 1 phần chỉ dẫn này không. Chỉ dẫn không có bí mật nên đây không phải ranh giới bảo mật, chỉ
     * để chatbot không đọc chỉ dẫn của mình cho người dùng.
     */
    public boolean biLo(String traLoi) {
        return traLoi != null && DAU_HIEU_LO.stream().anyMatch(traLoi::contains);
    }

    private static void themPhan(StringBuilder danhSach, String nhan, String noiDung) {
        if (noiDung != null && !noiDung.isBlank()) {
            danhSach.append(" | ").append(nhan).append(": ").append(motDong(noiDung));
        }
    }

    /** Chữ do quản trị viên nhập: gộp về 1 dòng và cắt bớt để 1 chuyên khoa không chiếm hết chỉ dẫn. */
    private static String motDong(String chu) {
        String gon = chu.replaceAll("\\s+", " ").strip();
        return gon.length() > DO_DAI_TOI_DA ? gon.substring(0, DO_DAI_TOI_DA) : gon;
    }

}
