package iuh.fit.se.eclinic.identity.dulieumau;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

import iuh.fit.se.eclinic.common.dulieumau.AnhMau;
import iuh.fit.se.eclinic.common.entity.catalog.AnhBacSi;
import iuh.fit.se.eclinic.common.enums.LoaiAnhBacSi;
import jakarta.persistence.EntityManager;

/**
 * Ảnh giới thiệu mẫu của bác sĩ (mảng JSON ở bac_si.anh_gioi_thieu, xem {@link AnhBacSi}), dùng chung cho dữ liệu mẫu
 * chính và dữ liệu mẫu số lượng lớn.
 */
final class AnhGioiThieuMau {

    /** Tiền tố URL ảnh giữ chỗ mà dữ liệu mẫu cũ (trước DOANTOTNGH-27) đã ghi vào DB. */
    static final String TIEN_TO_ANH_GIU = "https://placehold.co/";

    /** Số ảnh giới thiệu mẫu ban đầu của mỗi bác sĩ ({@link #baAnh}): 2 ảnh làm việc + 1 chứng chỉ. */
    static final int SO_ANH_MAU = 3;

    private AnhGioiThieuMau() {
    }

    /** Chú thích tiếng Việt của ảnh mẫu thứ {@code thuTu} (1-3). Đổi chữ ở đây thì boSungTiengAnh không còn khớp. */
    static String chuThich(int thuTu, String hoTen, String tenChuyenKhoa, String soGiayPhep) {
        return switch (thuTu) {
            case 1 -> "Bác sĩ " + hoTen + " tại phòng khám " + tenChuyenKhoa;
            case 2 -> "Tư vấn cho người bệnh";
            default -> "Chứng chỉ hành nghề số " + soGiayPhep;
        };
    }

    /** Bản tiếng Anh của {@link #chuThich}; họ tên giữ nguyên. Chuyên khoa chưa có tên tiếng Anh thì ảnh 1 trả null. */
    static String chuThichEn(int thuTu, String hoTen, String tenChuyenKhoaEn, String soGiayPhep) {
        return switch (thuTu) {
            case 1 -> tenChuyenKhoaEn == null ? null : "Dr. " + hoTen + " at the " + tenChuyenKhoaEn + " clinic";
            case 2 -> "Consulting a patient";
            default -> "Practising certificate no. " + soGiayPhep;
        };
    }

    /**
     * Ảnh thật thay cho 1 ảnh giữ chỗ của bác sĩ mẫu số {@code so}, chọn theo màu nền trong URL cũ (ảnh làm việc 1 / 2)
     * hoặc loại ảnh (chứng chỉ), không theo thứ tự vì người dùng có thể đã sắp xếp lại.
     */
    static String thayAnhGiu(AnhBacSi anh, int so) {
        if (anh.getLoai() == LoaiAnhBacSi.CHUNG_CHI) {
            return AnhMau.chungChi(so);
        }
        return AnhMau.congViec(anh.getUrl().contains("/e3f2fd/") ? 2 * so - 1 : 2 * so - 2);
    }

    static boolean laAnhGiu(AnhBacSi anh) {
        return anh.getMaLuuTru() == null && anh.getUrl() != null && anh.getUrl().startsWith(TIEN_TO_ANH_GIU);
    }

    /**
     * 2 ảnh làm việc + 1 ảnh chứng chỉ của bác sĩ mẫu số {@code so}, id nối tiếp {@code idCuoi}, thứ tự 1-3.
     * {@code chuThich} / {@code chuThichEn} nhận thứ tự ảnh; chuThichEn có thể trả null (bổ sung sau).
     */
    static List<AnhBacSi> baAnh(int so, long idCuoi, IntFunction<String> chuThich, IntFunction<String> chuThichEn) {
        return List.of(
                anhMau(idCuoi + 1, 1, LoaiAnhBacSi.ANH_CONG_VIEC, AnhMau.congViec(2 * so - 2), chuThich, chuThichEn),
                anhMau(idCuoi + 2, 2, LoaiAnhBacSi.ANH_CONG_VIEC, AnhMau.congViec(2 * so - 1), chuThich, chuThichEn),
                anhMau(idCuoi + 3, 3, LoaiAnhBacSi.CHUNG_CHI, AnhMau.chungChi(so), chuThich, chuThichEn));
    }

    /**
     * 1 ảnh giải thưởng cho mỗi giải trong {@link HoSoBacSiMau#GIAI_THUONG} của bác sĩ mẫu số {@code so}, chú thích là
     * chính tên giải (2 ngôn ngữ), id nối tiếp {@code idCuoi}, thứ tự sau {@code thuTuCuoi}. Ảnh chọn theo vị trí của giải
     * trong mọi giải mẫu (xếp theo số bác sĩ) nên mỗi giải 1 ảnh khác nhau. Bác sĩ không có giải: danh sách rỗng.
     */
    static List<AnhBacSi> anhGiaiThuong(int so, long idCuoi, int thuTuCuoi) {
        HoSoBacSiMau.GiaiThuongMau vi = HoSoBacSiMau.GIAI_THUONG.get(so);
        if (vi == null) {
            return List.of();
        }
        HoSoBacSiMau.GiaiThuongMau en = DuLieuMauTiengAnh.GIAI_THUONG.get(so);
        int viTriDau = HoSoBacSiMau.GIAI_THUONG.entrySet().stream().filter(e -> e.getKey() < so)
                .mapToInt(e -> e.getValue().giaiThuong().size()).sum();
        List<AnhBacSi> danhSach = new ArrayList<>();
        for (int i = 0; i < vi.giaiThuong().size(); i++) {
            String chuThichEn = en != null && i < en.giaiThuong().size() ? en.giaiThuong().get(i) : null;
            String chuThichVi = vi.giaiThuong().get(i);
            danhSach.add(anhMau(idCuoi + i + 1, thuTuCuoi + i + 1, LoaiAnhBacSi.GIAI_THUONG,
                    AnhMau.giaiThuong(viTriDau + i), thuTu -> chuThichVi, thuTu -> chuThichEn));
        }
        return danhSach;
    }

    /** Ảnh mẫu không nằm trong kho ảnh ({@code maLuuTru} null): tạo dữ liệu mẫu không gọi Cloudinary. */
    private static AnhBacSi anhMau(long id, int thuTu, LoaiAnhBacSi loai, String url, IntFunction<String> chuThich,
            IntFunction<String> chuThichEn) {
        AnhBacSi anh = new AnhBacSi();
        anh.setId(id);
        anh.setLoai(loai);
        anh.setUrl(url);
        anh.setChuThich(chuThich.apply(thuTu));
        anh.setChuThichEn(chuThichEn.apply(thuTu));
        anh.setThuTu(thuTu);
        anh.setNgayTao(LocalDateTime.now());
        return anh;
    }

    /** Ghi lại cả danh sách ảnh (không rỗng) và nâng bộ đếm id ảnh của bác sĩ qua id lớn nhất trong đó. */
    static void ghi(EntityManager entityManager, Long idBacSi, List<AnhBacSi> danhSach) {
        entityManager.createNativeQuery("UPDATE bac_si SET anh_gioi_thieu = CAST(:json AS JSON),"
                        + " anh_id_cuoi = GREATEST(anh_id_cuoi, :idLonNhat) WHERE id_bac_si = :id")
                .setParameter("json", AnhBacSi.vietDanhSach(danhSach))
                .setParameter("idLonNhat", danhSach.stream().mapToLong(AnhBacSi::getId).max().orElse(0))
                .setParameter("id", idBacSi)
                .executeUpdate();
    }
}
