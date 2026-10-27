package iuh.fit.se.eclinic.catalog.client;

import java.util.List;
import java.util.function.Supplier;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import iuh.fit.se.eclinic.catalog.config.DichVuNoiBoProperties;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.security.KhoaNoiBo;
import lombok.extern.slf4j.Slf4j;

/** Phần chung của client gọi API nội bộ: khoá nội bộ, thời gian chờ và đổi lỗi của service kia. */
@Slf4j
final class GoiNoiBo {

    /** Phần của PhanHoiApi cần đọc khi service kia báo lỗi. */
    private record LoiTraVe(String maLoi, String thongDiep) {
    }

    private GoiNoiBo() {
    }

    static RestClient taoRestClient(String url, DichVuNoiBoProperties properties, KhoaNoiBo khoaNoiBo) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.thoiGianChoKetNoi());
        factory.setReadTimeout(properties.thoiGianChoPhanHoi());
        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(url)
                .defaultHeader(KhoaNoiBo.TEN_HEADER, khoaNoiBo.giaTri())
                // Thông điệp lỗi của service kia được chuyển nguyên cho người dùng: phải cùng ngôn ngữ với request
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().setAcceptLanguageAsLocales(List.of(NgonNgu.hienTai().locale()));
                    return execution.execute(request, body);
                })
                .build();
    }

    /** @param dichVu tên service được gọi, chỉ để ghi log */
    static <T> T goi(String dichVu, Supplier<T> lenh) {
        try {
            return lenh.get();
        } catch (RestClientResponseException ex) {
            int maHttp = ex.getStatusCode().value();
            if (maHttp >= 400 && maHttp < 500 && maHttp != 401) {
                LoiNghiepVu loi = docLoi(ex);
                if (loi != null) {
                    throw loi;
                }
            }
            log.warn("{} trả HTTP {} cho API nội bộ{}", dichVu, maHttp,
                    maHttp == 401 ? " (kiểm tra INTERNAL_API_KEY)" : "");
            throw new LoiNghiepVu(MaLoi.DICH_VU_NOI_BO_LOI);
        } catch (RestClientException ex) {
            log.warn("Không gọi được API nội bộ của {} ({})", dichVu, ex.getClass().getSimpleName());
            throw new LoiNghiepVu(MaLoi.DICH_VU_NOI_BO_LOI);
        }
    }

    private static LoiNghiepVu docLoi(RestClientResponseException ex) {
        try {
            LoiTraVe loi = ex.getResponseBodyAs(LoiTraVe.class);
            if (loi == null || loi.maLoi() == null) {
                return null;
            }
            MaLoi maLoi = MaLoi.valueOf(loi.maLoi());
            return loi.thongDiep() == null ? new LoiNghiepVu(maLoi) : new LoiNghiepVu(maLoi, loi.thongDiep());
        } catch (RuntimeException e) {
            // Thân không phải PhanHoiApi hoặc mã lỗi lạ: coi như service kia hỏng
            return null;
        }
    }

}
