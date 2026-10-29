package iuh.fit.se.eclinic.common.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import iuh.fit.se.eclinic.common.entity.medical.Icd10;

/**
 * Danh mục ICD-10 chỉ đọc, nạp 1 lần từ tệp {@code danh-muc/icd10.tsv} (trước đây là bảng icd10, V44 đã bỏ). Muốn thêm /
 * sửa mã thì sửa tệp; các dòng giữ thứ tự theo mã như {@code ORDER BY ma} của MySQL.
 */
public final class DanhMucIcd10 {

    private static final String TEP = "/danh-muc/icd10.tsv";

    /** Mã đã chuẩn hoá cùng tên đã bỏ dấu, để gợi ý không phải bỏ dấu lại mỗi lần. */
    private record Muc(Icd10 icd10, String ma, String ten, String tenEn) {
    }

    private static final List<Muc> DANH_SACH;
    private static final Map<String, Icd10> THEO_MA = new HashMap<>();

    static {
        List<Muc> danhSach = new ArrayList<>();
        try (InputStream vao = DanhMucIcd10.class.getResourceAsStream(TEP)) {
            if (vao == null) {
                throw new IllegalStateException("Không có tệp " + TEP);
            }
            BufferedReader doc = new BufferedReader(new InputStreamReader(vao, StandardCharsets.UTF_8));
            doc.readLine(); // dòng tiêu đề
            String dong;
            int so = 1;
            while ((dong = doc.readLine()) != null) {
                so++;
                if (dong.isEmpty()) {
                    continue;
                }
                String[] cot = dong.split("\t", -1);
                if (cot.length != 4 || cot[0].isBlank() || cot[1].isBlank() || cot[2].isBlank()
                        || !(cot[3].equals("0") || cot[3].equals("1"))) {
                    throw new IllegalStateException(TEP + " dòng " + so + " sai định dạng: " + dong);
                }
                Icd10 icd10 = new Icd10(cot[0], cot[1], cot[2], cot[3].equals("1"));
                if (THEO_MA.put(cot[0].toUpperCase(Locale.ROOT), icd10) != null) {
                    throw new IllegalStateException(TEP + " dòng " + so + " trùng mã " + cot[0]);
                }
                danhSach.add(new Muc(icd10, boDau(cot[0]), boDau(cot[1]), boDau(cot[2])));
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        DANH_SACH = List.copyOf(danhSach);
    }

    private DanhMucIcd10() {
    }

    /** Mã ICD-10 theo mã, không phân biệt hoa thường. */
    public static Optional<Icd10> tim(String ma) {
        return ma == null ? Optional.empty() : Optional.ofNullable(THEO_MA.get(ma.trim().toUpperCase(Locale.ROOT)));
    }

    /**
     * Mã bắt đầu bằng từ khoá, hoặc tên Việt / Anh chứa từ khoá, theo mã, tối đa {@code toiDa}. So khớp như {@code LIKE} của
     * collation utf8mb4_unicode_ci mà bảng icd10 cũ dùng: không phân biệt hoa thường và dấu, nhưng "đ" khác "d".
     */
    public static List<Icd10> goiY(String tuKhoa, int toiDa) {
        String tim = boDau(tuKhoa.trim());
        if (tim.isEmpty()) {
            return List.of();
        }
        return DANH_SACH.stream()
                .filter(muc -> muc.ma().startsWith(tim) || muc.ten().contains(tim) || muc.tenEn().contains(tim))
                .limit(toiDa)
                .map(Muc::icd10)
                .toList();
    }

    /** Bỏ dấu thanh / mũ / móc (NFD rồi bỏ dấu kết hợp) và về chữ thường; "đ" giữ nguyên như collation của MySQL. */
    static String boDau(String chu) {
        return Normalizer.normalize(chu, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

}
