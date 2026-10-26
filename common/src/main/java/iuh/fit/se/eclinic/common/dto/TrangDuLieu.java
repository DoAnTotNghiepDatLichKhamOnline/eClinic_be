package iuh.fit.se.eclinic.common.dto;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Một trang dữ liệu, đặt trong "duLieu" của PhanHoiApi. Trang đánh số từ 0 (theo Spring Data).
 */
public record TrangDuLieu<T>(List<T> noiDung, int trang, int kichThuoc, long tongSoPhanTu, int tongSoTrang) {

    public static <T> TrangDuLieu<T> tu(Page<T> page) {
        return new TrangDuLieu<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

}
