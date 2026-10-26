package iuh.fit.se.eclinic.booking.client;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import iuh.fit.se.eclinic.booking.config.ThongBaoProperties;
import iuh.fit.se.eclinic.common.enums.LoaiThongBao;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.security.KhoaNoiBo;

/**
 * Gọi API nội bộ của notification-service (gọi thẳng service, không qua gateway; xem {@link KhoaNoiBo}).
 * <p>
 * Lỗi được ném nguyên dạng của RestClient để bên gọi phân biệt: {@code HttpClientErrorException} (4xx: dữ liệu gửi đi
 * sai) với mọi lỗi còn lại (mất kết nối, quá thời gian chờ, 5xx: thử lại sau).
 */
@Component
public class ThongBaoClient {

    private final RestClient restClient;

    public ThongBaoClient(ThongBaoProperties properties, KhoaNoiBo khoaNoiBo) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.thoiGianCho());
        factory.setReadTimeout(properties.thoiGianCho());
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(properties.url())
                .defaultHeader(KhoaNoiBo.TEN_HEADER, khoaNoiBo.giaTri())
                .build();
    }

    /**
     * @param maNguon mã duy nhất của sự kiện; gửi lại cùng mã thì notification-service không tạo thông báo thứ hai
     * @param khoaMau khoá mẫu câu + {@code thamSo} (mảng JSON): notification-service soạn câu theo ngôn ngữ người đọc
     * @param noiDung câu soạn sẵn, chỉ có ở sự kiện ghi trước V18 (khi đó {@code khoaMau} null)
     */
    public record ThongBaoCanGui(String maNguon, LoaiThongBao loai, Long idTaiKhoan, Long idLichHen, Long idYeuCau,
            String khoaMau, String thamSo, String noiDung) {
    }

    /**
     * 1 email cần gửi: notification-service giữ trong hàng chờ của nó và tự gửi (thử lại khi máy chủ mail lỗi).
     *
     * @param khoaMau mẫu email (vd {@code email.datLich}); {@code thamSo} theo thứ tự mẫu đó quy định
     * @param tepLich nội dung tệp .ics gửi kèm email, null = không kèm
     */
    public record EmailCanGui(String maNguon, String nguoiNhan, NgonNgu ngonNgu, String khoaMau, String thamSo,
            Long idLichHen, String tepLich) {
    }

    /** Trả về bình thường = notification-service đã nhận cả danh sách (kể cả dòng nó bỏ qua vì đã có). */
    public void gui(List<ThongBaoCanGui> danhSach) {
        guiToi("/noi-bo/thong-bao", danhSach);
    }

    /** Như {@link #gui}: trả về bình thường = đã nằm trong hàng chờ gửi của notification-service, chưa chắc đã gửi đi. */
    public void guiEmail(List<EmailCanGui> danhSach) {
        guiToi("/noi-bo/email", danhSach);
    }

    private void guiToi(String duongDan, List<?> danhSach) {
        restClient.post()
                .uri(duongDan)
                .contentType(MediaType.APPLICATION_JSON)
                .body(danhSach)
                .retrieve()
                .toBodilessEntity();
    }

}
