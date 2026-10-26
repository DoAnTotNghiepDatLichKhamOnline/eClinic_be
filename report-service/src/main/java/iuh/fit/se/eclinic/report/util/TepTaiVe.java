package iuh.fit.se.eclinic.report.util;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Response tải tệp (báo cáo Excel): trình duyệt lưu thành tệp tên {@code tenTep}. API tải tệp không khai báo
 * {@code produces} để lỗi (kỳ sai...) vẫn trả JSON như các API khác. Gateway cho frontend đọc header
 * Content-Disposition (CorsConfig). Cùng cách làm với TepTaiVe của booking-service.
 */
public final class TepTaiVe {

    public static final MediaType XLSX = MediaType
            .parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private TepTaiVe() {
    }

    public static ResponseEntity<byte[]> taiVe(byte[] tep, String tenTep, MediaType loai) {
        return ResponseEntity.ok().contentType(loai)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(tenTep).build().toString())
                .body(tep);
    }

}
