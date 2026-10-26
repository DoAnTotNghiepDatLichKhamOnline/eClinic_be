package iuh.fit.se.eclinic.catalog.client;

import java.util.HashMap;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import iuh.fit.se.eclinic.catalog.config.DichVuNoiBoProperties;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.security.KhoaNoiBo;

/**
 * Gọi API nội bộ /noi-bo/tai-khoan của identity-service: bảng tai_khoan thuộc identity-service nên catalog-service không
 * tự ghi. Lỗi: xem {@link GoiNoiBo}.
 */
@Component
public class TaiKhoanClient {

    private static final String DICH_VU = "identity-service";
    private static final ParameterizedTypeReference<PhanHoiApi<TaiKhoanDaTao>> KIEU_TRA_VE =
            new ParameterizedTypeReference<>() {
            };

    private record TaiKhoanDaTao(Long id) {
    }

    private final RestClient restClient;

    public TaiKhoanClient(DichVuNoiBoProperties properties, KhoaNoiBo khoaNoiBo) {
        this.restClient = GoiNoiBo.taoRestClient(properties.identityUrl(), properties, khoaNoiBo);
    }

    /** @return id tài khoản BAC_SI vừa tạo (đã kích hoạt, mật khẩu mặc định, phải đặt mật khẩu ở lần đăng nhập đầu) */
    public Long taoTaiKhoanBacSi(String hoTen, String email, String soDienThoai) {
        Map<String, Object> than = new HashMap<>();
        than.put("hoTen", hoTen);
        than.put("email", email);
        than.put("soDienThoai", soDienThoai);
        PhanHoiApi<TaiKhoanDaTao> ketQua = GoiNoiBo.goi(DICH_VU, () -> restClient.post()
                .uri("/noi-bo/tai-khoan/bac-si")
                .contentType(MediaType.APPLICATION_JSON)
                .body(than)
                .retrieve()
                .body(KIEU_TRA_VE));
        return ketQua.duLieu().id();
    }

    /** Xoá tài khoản vừa tạo khi không tạo được hồ sơ bác sĩ. */
    public void xoaTaiKhoanBacSi(Long idTaiKhoan) {
        GoiNoiBo.goi(DICH_VU, () -> restClient.delete()
                .uri("/noi-bo/tai-khoan/bac-si/{id}", idTaiKhoan)
                .retrieve()
                .toBodilessEntity());
    }

    /** @param soDienThoai null = bỏ số điện thoại */
    public void suaThongTin(Long idTaiKhoan, String hoTen, String soDienThoai) {
        Map<String, Object> than = new HashMap<>();
        than.put("hoTen", hoTen);
        than.put("soDienThoai", soDienThoai);
        GoiNoiBo.goi(DICH_VU, () -> restClient.put()
                .uri("/noi-bo/tai-khoan/{id}/thong-tin", idTaiKhoan)
                .contentType(MediaType.APPLICATION_JSON)
                .body(than)
                .retrieve()
                .toBodilessEntity());
    }

    /** Gọi lại được: tài khoản đã vô hiệu hoá thì identity-service không làm gì. */
    public void voHieuHoa(Long idTaiKhoan, String lyDo) {
        GoiNoiBo.goi(DICH_VU, () -> restClient.post()
                .uri("/noi-bo/tai-khoan/{id}/vo-hieu-hoa", idTaiKhoan)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("lyDo", lyDo))
                .retrieve()
                .toBodilessEntity());
    }

    /** Gọi lại được: tài khoản đang hoạt động thì identity-service không làm gì. */
    public void kichHoatLai(Long idTaiKhoan) {
        GoiNoiBo.goi(DICH_VU, () -> restClient.post()
                .uri("/noi-bo/tai-khoan/{id}/kich-hoat-lai", idTaiKhoan)
                .retrieve()
                .toBodilessEntity());
    }

}
