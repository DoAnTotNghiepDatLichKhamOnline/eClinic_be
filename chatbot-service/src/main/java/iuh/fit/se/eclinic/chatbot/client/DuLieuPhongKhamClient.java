package iuh.fit.se.eclinic.chatbot.client;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import iuh.fit.se.eclinic.chatbot.config.ChatbotProperties;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import lombok.extern.slf4j.Slf4j;

/** Đọc bác sĩ và khung giờ qua API công khai, nên không có dữ liệu cá nhân của ai. */
@Slf4j
@Component
public class DuLieuPhongKhamClient {

    private static final Duration CHO_KET_NOI = Duration.ofSeconds(2);
    private static final Duration CHO_PHAN_HOI = Duration.ofSeconds(5);
    private static final int SO_BAC_SI_TOI_DA = 100;

    private static final ParameterizedTypeReference<PhanHoiApi<TrangDuLieu<BacSiCongKhai>>> KIEU_TRANG_BAC_SI =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<PhanHoiApi<BacSiCongKhai>> KIEU_BAC_SI =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<PhanHoiApi<List<NgaySomNhat>>> KIEU_NGAY_SOM_NHAT =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<PhanHoiApi<List<CaKham>>> KIEU_CA_KHAM =
            new ParameterizedTypeReference<>() {
            };

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BacSiCongKhai(Long id, String hoTen, String anhDaiDien, String hocVi, String chucVu,
            Integer soNamKinhNghiem, Long idChuyenKhoa, String tenChuyenKhoa) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NgaySomNhat(Long idBacSi, String ngay, long soChoConLai) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CaKham(Long idLichLamViec, String ngay, PhongKham phongKham, List<KhungGio> khungGio) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PhongKham(Long id, String tenPhong, String tang) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KhungGio(String gioBatDau, String gioKetThuc, int soChoConLai) {
    }

    /** Service kia không trả lời hoặc trả lỗi: chatbot vẫn trả lời, chỉ không có dữ liệu tra cứu. */
    public static class KhongTraCuuDuoc extends RuntimeException {

        private static final long serialVersionUID = 1L;
    }

    private final RestClient catalog;
    private final RestClient booking;

    public DuLieuPhongKhamClient(ChatbotProperties properties) {
        this.catalog = taoRestClient(properties.catalogUrl());
        this.booking = taoRestClient(properties.bookingUrl());
    }

    /** Bác sĩ đang hiển thị công khai của chuyên khoa, theo tên. */
    public List<BacSiCongKhai> timBacSi(Long idChuyenKhoa) {
        PhanHoiApi<TrangDuLieu<BacSiCongKhai>> ketQua = goi("catalog-service", () -> catalog.get()
                .uri("/api/catalog/bac-si?idChuyenKhoa={id}&kichThuoc={n}", idChuyenKhoa, SO_BAC_SI_TOI_DA)
                .retrieve()
                .body(KIEU_TRANG_BAC_SI));
        return duLieu(ketQua).noiDung();
    }

    /** Rỗng khi không có bác sĩ đó hoặc bác sĩ không còn hiển thị công khai (API trả 404). */
    public Optional<BacSiCongKhai> layBacSi(Long idBacSi) {
        try {
            PhanHoiApi<BacSiCongKhai> ketQua = catalog.get()
                    .uri("/api/catalog/bac-si/{id}", idBacSi)
                    .retrieve()
                    .body(KIEU_BAC_SI);
            return Optional.of(duLieu(ketQua));
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            log.warn("catalog-service trả HTTP {} cho tra cứu của chatbot", ex.getStatusCode().value());
            throw new KhongTraCuuDuoc();
        } catch (RestClientException ex) {
            log.warn("Không gọi được catalog-service cho tra cứu của chatbot ({})", ex.getClass().getSimpleName());
            throw new KhongTraCuuDuoc();
        }
    }

    /** Ngày còn chỗ sớm nhất của từng bác sĩ thuộc chuyên khoa; bác sĩ hết chỗ không có trong kết quả. */
    public List<NgaySomNhat> ngaySomNhat(Long idChuyenKhoa) {
        return duLieu(goi("booking-service", () -> booking.get()
                .uri("/api/booking/khung-gio/ngay-som-nhat?idChuyenKhoa={id}", idChuyenKhoa)
                .retrieve()
                .body(KIEU_NGAY_SOM_NHAT)));
    }

    /** Các ca còn đặt được của bác sĩ trong 7 ngày tính từ {@code tuNgay}, kèm khung 1 giờ. */
    public List<CaKham> lichBacSi(Long idBacSi, LocalDate tuNgay) {
        return duLieu(goi("booking-service", () -> booking.get()
                .uri("/api/booking/khung-gio/nhieu-ngay?idBacSi={id}&tuNgay={tu}", idBacSi, tuNgay.toString())
                .retrieve()
                .body(KIEU_CA_KHAM)));
    }

    static <T> T goi(String dichVu, Supplier<T> lenh) {
        try {
            return lenh.get();
        } catch (RestClientResponseException ex) {
            log.warn("{} trả HTTP {} cho tra cứu của chatbot", dichVu, ex.getStatusCode().value());
            throw new KhongTraCuuDuoc();
        } catch (RestClientException ex) {
            log.warn("Không gọi được {} cho tra cứu của chatbot ({})", dichVu, ex.getClass().getSimpleName());
            throw new KhongTraCuuDuoc();
        }
    }

    static <T> T duLieu(PhanHoiApi<T> phanHoi) {
        if (phanHoi == null || phanHoi.duLieu() == null) {
            throw new KhongTraCuuDuoc();
        }
        return phanHoi.duLieu();
    }

    static RestClient taoRestClient(String url) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CHO_KET_NOI);
        factory.setReadTimeout(CHO_PHAN_HOI);
        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(url)
                // Tên chuyên khoa, phòng, chức vụ trả về theo ngôn ngữ của request đang chat
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().setAcceptLanguageAsLocales(List.of(NgonNgu.hienTai().locale()));
                    return execution.execute(request, body);
                })
                .build();
    }

}
