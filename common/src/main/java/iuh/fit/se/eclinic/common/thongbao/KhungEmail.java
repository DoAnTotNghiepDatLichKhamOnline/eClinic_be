package iuh.fit.se.eclinic.common.thongbao;

import java.nio.charset.StandardCharsets;

import org.springframework.web.util.HtmlUtils;

/**
 * Khung HTML của 1 email: lời chào, rồi các đoạn, dòng chi tiết và nút theo thứ tự gọi. Dùng chung cho email của
 * identity-service (tài khoản) và notification-service (lịch hẹn).
 * <p>
 * Câu chữ lấy qua {@link LayCau} (nơi gọi quyết định ngôn ngữ). Mọi giá trị chèn vào câu được đổi sang chữ và escape
 * trước; câu trong file thông điệp được phép chứa thẻ {@code <b>}.
 */
public final class KhungEmail {

    /** Lấy câu của 1 khoá thông điệp với các tham số đã escape. */
    @FunctionalInterface
    public interface LayCau {
        String cau(String khoa, Object[] thamSo);
    }

    private static final String MO_KHUNG =
            "<div style=\"font-family:Arial,sans-serif;font-size:15px;color:#222;max-width:560px\">\n";
    private static final String KIEU_NUT = "display:inline-block;padding:10px 20px;background:#1a73e8;color:#fff;"
            + "text-decoration:none;border-radius:4px";

    private final LayCau layCau;
    private final StringBuilder html = new StringBuilder(MO_KHUNG);
    private boolean dangMoDanhSach;

    /** @param hoTen null = lời chào không nêu tên */
    public KhungEmail(LayCau layCau, String hoTen) {
        this.layCau = layCau;
        html.append("  <p>").append(hoTen == null ? cau("email.chaoKhongTen") : cau("email.chao", hoTen))
                .append("</p>\n");
    }

    public KhungEmail doan(String khoa, Object... giaTri) {
        dongDanhSach();
        html.append("  <p>").append(cau(khoa, giaTri)).append("</p>\n");
        return this;
    }

    /** 1 dòng chi tiết; các dòng gọi liền nhau thành 1 danh sách. */
    public KhungEmail dong(String khoa, Object... giaTri) {
        if (!dangMoDanhSach) {
            dangMoDanhSach = true;
            html.append("  <ul>\n");
        }
        html.append("    <li>").append(cau(khoa, giaTri)).append("</li>\n");
        return this;
    }

    /** Nút bấm, kèm 1 đoạn ghi lại liên kết cho ứng dụng email không hiện nút. */
    public KhungEmail nut(String khoaNhan, String lienKet) {
        dongDanhSach();
        String url = escape(lienKet);
        html.append("  <p><a href=\"").append(url).append("\" style=\"").append(KIEU_NUT).append("\">")
                .append(cau(khoaNhan)).append("</a></p>\n");
        html.append("  <p>").append(cau("email.lienKetDuPhong")).append("<br><a href=\"").append(url).append("\">")
                .append(url).append("</a></p>\n");
        return this;
    }

    public String xong() {
        dongDanhSach();
        return html + "</div>\n";
    }

    private void dongDanhSach() {
        if (dangMoDanhSach) {
            dangMoDanhSach = false;
            html.append("  </ul>\n");
        }
    }

    private String cau(String khoa, Object... giaTri) {
        Object[] thamSo = new Object[giaTri.length];
        for (int i = 0; i < giaTri.length; i++) {
            thamSo[i] = escape(String.valueOf(giaTri[i]));
        }
        return layCau.cau(khoa, thamSo);
    }

    private static String escape(String giaTri) {
        return HtmlUtils.htmlEscape(giaTri == null ? "" : giaTri, StandardCharsets.UTF_8.name());
    }
}
