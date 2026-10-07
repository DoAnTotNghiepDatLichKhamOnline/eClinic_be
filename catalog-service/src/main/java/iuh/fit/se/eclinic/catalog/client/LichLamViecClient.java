package iuh.fit.se.eclinic.catalog.client;

import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import iuh.fit.se.eclinic.catalog.config.DichVuNoiBoProperties;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.security.KhoaNoiBo;

/**
 * Gọi API nội bộ /noi-bo/bac-si của booking-service: ca làm việc, lượt khám, lịch hẹn thuộc booking-service nên
 * catalog-service không tự ghi. Lỗi: xem {@link GoiNoiBo}.
 */
@Component
public class LichLamViecClient {

    private static final String DICH_VU = "booking-service";
    private static final ParameterizedTypeReference<PhanHoiApi<KetQuaHuyCa>> KIEU_TRA_VE =
            new ParameterizedTypeReference<>() {
            };

    /**
     * @param soCaDaHuy       số ca sắp tới vừa bị hủy
     * @param soLichHenCanDoi số lịch hẹn vừa được đánh dấu cần đổi lịch
     */
    public record KetQuaHuyCa(int soCaDaHuy, int soLichHenCanDoi) {
    }

    private final RestClient restClient;

    public LichLamViecClient(DichVuNoiBoProperties properties, KhoaNoiBo khoaNoiBo) {
        this.restClient = GoiNoiBo.taoRestClient(properties.bookingUrl(), properties, khoaNoiBo);
    }

    /** Hủy mọi ca chưa bắt đầu của bác sĩ. Gọi lại được: lần sau chỉ hủy những ca còn sót. */
    public KetQuaHuyCa huyCaSapToi(Long idBacSi, Long idTaiKhoanQuanTri, String lyDo) {
        PhanHoiApi<KetQuaHuyCa> ketQua = GoiNoiBo.goi(DICH_VU, () -> restClient.post()
                .uri("/noi-bo/bac-si/{id}/huy-ca-sap-toi", idBacSi)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("idTaiKhoanQuanTri", idTaiKhoanQuanTri, "lyDo", lyDo))
                .retrieve()
                .body(KIEU_TRA_VE));
        return ketQua.duLieu();
    }

}
