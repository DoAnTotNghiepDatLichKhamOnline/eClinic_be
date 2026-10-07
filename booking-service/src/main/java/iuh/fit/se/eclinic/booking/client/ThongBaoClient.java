package iuh.fit.se.eclinic.booking.client;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import iuh.fit.se.eclinic.booking.config.ThongBaoProperties;
import iuh.fit.se.eclinic.common.enums.LoaiThongBao;
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
     */
    public record ThongBaoCanGui(String maNguon, LoaiThongBao loai, Long idTaiKhoan, Long idLichHen, Long idYeuCau,
            String noiDung) {
    }

    /** Trả về bình thường = notification-service đã nhận cả danh sách (kể cả dòng nó bỏ qua vì đã có). */
    public void gui(List<ThongBaoCanGui> danhSach) {
        restClient.post()
                .uri("/noi-bo/thong-bao")
                .contentType(MediaType.APPLICATION_JSON)
                .body(danhSach)
                .retrieve()
                .toBodilessEntity();
    }

}
