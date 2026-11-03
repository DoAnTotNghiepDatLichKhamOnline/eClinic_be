package iuh.fit.se.eclinic.common.entity.catalog;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import iuh.fit.se.eclinic.common.enums.LoaiAnhBacSi;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Ảnh giới thiệu của bác sĩ (ảnh làm việc, chứng chỉ) hiện trong trang chi tiết bác sĩ. Không phải bảng: cả danh sách ảnh
 * của 1 bác sĩ là 1 mảng JSON ở cột bac_si.anh_gioi_thieu (V45), đọc / ghi bằng {@link #docDanhSach} /
 * {@link #vietDanhSach}. id duy nhất trong từng bác sĩ (cấp từ bac_si.anh_id_cuoi).
 */
@Getter
@Setter
@NoArgsConstructor
public class AnhBacSi {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Comparator<AnhBacSi> THU_TU_HIEN_THI = Comparator.comparingInt(AnhBacSi::getThuTu)
            .thenComparing(AnhBacSi::getId);

    private Long id;

    private LoaiAnhBacSi loai;

    private String url;

    /** Mã ảnh trong kho ảnh, dùng để xoá; null = ảnh nằm ngoài kho (dữ liệu mẫu). */
    private String maLuuTru;

    private String chuThich;

    private String chuThichEn;

    /** Thứ tự hiển thị, nhỏ đứng trước. */
    private int thuTu;

    private LocalDateTime ngayTao;

    // Chữ theo ngôn ngữ: bản tiếng Anh nếu có, không thì tiếng Việt
    public String chuThich(NgonNgu ngonNgu) {
        return ngonNgu.chon(chuThich, chuThichEn);
    }

    /** Danh sách ảnh từ JSON của cột, theo thứ tự hiển thị; null = chưa có ảnh. */
    public static List<AnhBacSi> docDanhSach(String json) {
        if (json == null) {
            return new ArrayList<>();
        }
        List<AnhBacSi> danhSach = new ArrayList<>(JSON.readValue(json, new TypeReference<List<AnhBacSi>>() {
        }));
        danhSach.sort(THU_TU_HIEN_THI);
        return danhSach;
    }

    /** JSON để ghi vào cột, theo thứ tự hiển thị; danh sách rỗng thành null. */
    public static String vietDanhSach(List<AnhBacSi> danhSach) {
        if (danhSach.isEmpty()) {
            return null;
        }
        return JSON.writeValueAsString(danhSach.stream().sorted(THU_TU_HIEN_THI).toList());
    }
}
