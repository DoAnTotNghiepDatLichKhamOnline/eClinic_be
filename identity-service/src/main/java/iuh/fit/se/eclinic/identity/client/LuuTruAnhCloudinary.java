package iuh.fit.se.eclinic.identity.client;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.identity.config.CloudinaryProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * Kho ảnh Cloudinary qua Upload API (REST), không dùng SDK: chỉ cần 2 lời gọi upload và destroy.
 * <p>
 * Mỗi request được ký: SHA-1 của các tham số (xếp theo tên, nối {@code ten=giaTri} bằng {@code &}) + API secret;
 * {@code file}, {@code api_key} không nằm trong chữ ký. Ảnh được tải lên với {@code overwrite} nên mỗi mã chỉ có 1 ảnh,
 * và {@code invalidate} để CDN bỏ bản cũ. Ảnh lớn được thu về tối đa 512 x 512 ngay khi lưu.
 */
@Slf4j
@Component
public class LuuTruAnhCloudinary implements LuuTruAnh {

    /** Tên định dạng theo cách gọi của Cloudinary. */
    private static final String DINH_DANG_CHO_PHEP = "jpg,png,webp";
    private static final String THU_NHO = "c_limit,h_512,w_512";
    private static final String MAY_CHU_ANH = "https://res.cloudinary.com/";
    private static final int DO_DAI_LOI_TOI_DA = 300;
    private static final Duration CHO_KET_NOI_TOI_DA = Duration.ofSeconds(5);
    private static final ParameterizedTypeReference<Map<String, Object>> KIEU_JSON = new ParameterizedTypeReference<>() {
    };

    private final CloudinaryProperties properties;
    private final RestClient restClient;

    @Autowired
    public LuuTruAnhCloudinary(CloudinaryProperties properties) {
        this(properties, RestClient.builder().requestFactory(taoRequestFactory(properties)));
        if (!properties.daCauHinh()) {
            log.warn("Chưa đặt đủ CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET: "
                    + "tải ảnh đại diện (POST /api/users/me/avatar) sẽ trả 503.");
        }
    }

    /** Cho test: builder đã gắn server giả. */
    LuuTruAnhCloudinary(CloudinaryProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder.build();
    }

    @Override
    public boolean daCauHinh() {
        return properties.daCauHinh();
    }

    @Override
    public String taiLen(String ma, byte[] noiDung) {
        kiemTraDaCauHinh();
        SortedMap<String, String> thamSo = new TreeMap<>();
        thamSo.put("allowed_formats", DINH_DANG_CHO_PHEP);
        thamSo.put("invalidate", "true");
        thamSo.put("overwrite", "true");
        thamSo.put("public_id", publicId(ma));
        thamSo.put("timestamp", String.valueOf(Instant.now().getEpochSecond()));
        thamSo.put("transformation", THU_NHO);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        thamSo.forEach(body::add);
        body.add("api_key", properties.apiKey());
        body.add("signature", ky(thamSo, properties.apiSecret()));
        body.add("file", new ByteArrayResource(noiDung) {
            @Override
            public String getFilename() {
                // Cloudinary chỉ nhận phần tệp có tên; tên thật của ảnh là public_id
                return "anh";
            }
        });

        Map<String, Object> ketQua = goi("upload", MediaType.MULTIPART_FORM_DATA, body, true);
        Object url = ketQua == null ? null : ketQua.get("secure_url");
        if (url == null || url.toString().isBlank()) {
            log.warn("Cloudinary upload không trả về secure_url (public_id={})", thamSo.get("public_id"));
            throw new LoiNghiepVu(MaLoi.LUU_TRU_ANH_KHONG_KHA_DUNG);
        }
        return url.toString();
    }

    @Override
    public void xoa(String ma) {
        kiemTraDaCauHinh();
        SortedMap<String, String> thamSo = new TreeMap<>();
        thamSo.put("invalidate", "true");
        thamSo.put("public_id", publicId(ma));
        thamSo.put("timestamp", String.valueOf(Instant.now().getEpochSecond()));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        thamSo.forEach(body::add);
        body.add("api_key", properties.apiKey());
        body.add("signature", ky(thamSo, properties.apiSecret()));

        // Trả {"result":"ok"} hoặc {"result":"not found"}: cả hai đều là "ảnh không còn"
        goi("destroy", MediaType.APPLICATION_FORM_URLENCODED, body, false);
    }

    @Override
    public boolean laAnhCuaKho(String url) {
        return url != null && properties.daCauHinh() && url.startsWith(MAY_CHU_ANH + properties.cloudName() + "/");
    }

    /**
     * Chữ ký của Upload API: các tham số (đã xếp theo tên) nối thành {@code a=1&b=2}, nối tiếp API secret, băm SHA-1,
     * viết dạng hex.
     */
    static String ky(SortedMap<String, String> thamSo, String apiSecret) {
        String chuoi = thamSo.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&")) + apiSecret;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(chuoi.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Map<String, Object> goi(String thaoTac, MediaType kieuBody, MultiValueMap<String, Object> body,
            boolean loi400LaAnhSai) {
        try {
            return restClient.post()
                    .uri(properties.urlApi() + "/v1_1/{cloudName}/image/{thaoTac}", properties.cloudName(), thaoTac)
                    .contentType(kieuBody)
                    .body(body)
                    .retrieve()
                    .body(KIEU_JSON);
        } catch (RestClientResponseException ex) {
            // Body lỗi của Cloudinary là {"error":{"message":"..."}}, không chứa secret hay chữ ký
            String chiTiet = rutGon(ex.getResponseBodyAsString());
            if (loi400LaAnhSai && ex.getStatusCode().value() == HttpStatus.BAD_REQUEST.value()) {
                log.info("Cloudinary từ chối ảnh: {}", chiTiet);
                throw new LoiNghiepVu(MaLoi.ANH_KHONG_HOP_LE);
            }
            log.warn("Cloudinary {} lỗi HTTP {}: {}", thaoTac, ex.getStatusCode().value(), chiTiet);
            throw new LoiNghiepVu(MaLoi.LUU_TRU_ANH_KHONG_KHA_DUNG);
        } catch (RestClientException ex) {
            // Không kết nối được, quá thời gian chờ, response không đọc được
            log.warn("Không gọi được Cloudinary {}: {}", thaoTac, ex.getMessage());
            throw new LoiNghiepVu(MaLoi.LUU_TRU_ANH_KHONG_KHA_DUNG);
        }
    }

    private void kiemTraDaCauHinh() {
        if (!properties.daCauHinh()) {
            throw new LoiNghiepVu(MaLoi.LUU_TRU_ANH_KHONG_KHA_DUNG);
        }
    }

    private String publicId(String ma) {
        return properties.thuMuc() + "/" + ma;
    }

    private static String rutGon(String noiDung) {
        if (noiDung == null) {
            return "";
        }
        return noiDung.length() > DO_DAI_LOI_TOI_DA ? noiDung.substring(0, DO_DAI_LOI_TOI_DA) : noiDung;
    }

    private static SimpleClientHttpRequestFactory taoRequestFactory(CloudinaryProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // Kết nối chậm + trả lời chậm cộng lại vẫn phải dưới read-timeout của gateway
        Duration choKetNoi = properties.thoiGianCho().compareTo(CHO_KET_NOI_TOI_DA) > 0
                ? CHO_KET_NOI_TOI_DA
                : properties.thoiGianCho();
        factory.setConnectTimeout(choKetNoi);
        factory.setReadTimeout(properties.thoiGianCho());
        return factory;
    }

}
