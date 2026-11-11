package iuh.fit.se.eclinic.common.dulieumau;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import iuh.fit.se.eclinic.common.enums.GioiTinh;

/**
 * URL ảnh mẫu thật trên Cloudinary ({@code eclinic/mau/...}) cho dữ liệu mẫu, DOANTOTNGH-27.
 * <p>
 * FILE TỰ SINH, không sửa tay: sửa scripts/anh-mau/anh-mau.json (qua tai-len.js) rồi chạy
 * {@code node scripts/anh-mau/sinh-java.js}.
 * <p>
 * Ảnh dùng chung cho nhiều bản ghi và không thuộc bản ghi nào: ảnh giới thiệu mẫu để {@code maLuuTru} null, còn
 * xoá ảnh đại diện chỉ xoá khoá riêng của tài khoản, nên không thao tác nào xoá được ảnh trong thư mục này.
 */
public final class AnhMau {

    private AnhMau() {
    }

    /** Chân dung 12 bác sĩ mẫu chính: phần tử i là bác sĩ số i + 1 (nữ là số chẵn). */
    private static final List<String> BAC_SI = List.of(
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322383/eclinic/mau/bac-si/bs01.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322385/eclinic/mau/bac-si/bs02.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322386/eclinic/mau/bac-si/bs03.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322387/eclinic/mau/bac-si/bs04.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322388/eclinic/mau/bac-si/bs05.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322389/eclinic/mau/bac-si/bs06.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322390/eclinic/mau/bac-si/bs07.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322391/eclinic/mau/bac-si/bs08.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322392/eclinic/mau/bac-si/bs09.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322393/eclinic/mau/bac-si/bs10.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322394/eclinic/mau/bac-si/bs11.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322395/eclinic/mau/bac-si/bs12.jpg");

    /** Chân dung bác sĩ nam (gồm các bác sĩ chính nam) cho bác sĩ mẫu số lượng lớn. */
    private static final List<String> BAC_SI_NAM = List.of(
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322383/eclinic/mau/bac-si/bs01.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322386/eclinic/mau/bac-si/bs03.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322388/eclinic/mau/bac-si/bs05.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322390/eclinic/mau/bac-si/bs07.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322392/eclinic/mau/bac-si/bs09.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322394/eclinic/mau/bac-si/bs11.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322396/eclinic/mau/bac-si/nam01.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322397/eclinic/mau/bac-si/nam02.jpg");

    private static final List<String> BAC_SI_NU = List.of(
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322385/eclinic/mau/bac-si/bs02.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322387/eclinic/mau/bac-si/bs04.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322389/eclinic/mau/bac-si/bs06.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322391/eclinic/mau/bac-si/bs08.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322393/eclinic/mau/bac-si/bs10.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322395/eclinic/mau/bac-si/bs12.jpg");

    /** Ảnh đại diện bệnh nhân / quản trị viên (thường phục, không áo blouse). */
    private static final List<String> NGUOI_DUNG_NAM = List.of(
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322398/eclinic/mau/nguoi-dung/nam01.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322221/eclinic/mau/nguoi-dung/nam02.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322222/eclinic/mau/nguoi-dung/nam03.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322401/eclinic/mau/nguoi-dung/nam04.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322224/eclinic/mau/nguoi-dung/nam05.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322224/eclinic/mau/nguoi-dung/nam06.jpg");

    private static final List<String> NGUOI_DUNG_NU = List.of(
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322404/eclinic/mau/nguoi-dung/nu01.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322405/eclinic/mau/nguoi-dung/nu02.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322227/eclinic/mau/nguoi-dung/nu03.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322228/eclinic/mau/nguoi-dung/nu04.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322407/eclinic/mau/nguoi-dung/nu05.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322408/eclinic/mau/nguoi-dung/nu06.jpg");

    /** Ảnh làm việc (khám, tư vấn, phòng khám). */
    private static final List<String> CONG_VIEC = List.of(
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322231/eclinic/mau/cong-viec/cv01.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322232/eclinic/mau/cong-viec/cv02.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322234/eclinic/mau/cong-viec/cv03.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322235/eclinic/mau/cong-viec/cv04.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322237/eclinic/mau/cong-viec/cv05.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322239/eclinic/mau/cong-viec/cv06.jpg");

    /** Ảnh chứng chỉ / bằng cấp. */
    private static final List<String> CHUNG_CHI = List.of(
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322240/eclinic/mau/chung-chi/cc01.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794322244/eclinic/mau/chung-chi/cc02.jpg");

    /** Ảnh giải thưởng (cúp, huy chương). */
    private static final List<String> GIAI_THUONG = List.of(
            "https://res.cloudinary.com/deied72qz/image/upload/v1794397332/eclinic/mau/giai-thuong/gt01.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794397335/eclinic/mau/giai-thuong/gt02.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794397336/eclinic/mau/giai-thuong/gt03.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794397337/eclinic/mau/giai-thuong/gt04.jpg");

    /** Tên chuyên khoa mẫu (đúng như DuLieuMau / DuLieuMauLon) -> ảnh minh hoạ. */
    public static final Map<String, String> CHUYEN_KHOA = Map.ofEntries(
            Map.entry("Chấn thương chỉnh hình", "https://res.cloudinary.com/deied72qz/image/upload/v1794322293/eclinic/mau/chuyen-khoa/chan-thuong-chinh-hinh.jpg"),
            Map.entry("Cơ xương khớp", "https://res.cloudinary.com/deied72qz/image/upload/v1794322273/eclinic/mau/chuyen-khoa/co-xuong-khop.jpg"),
            Map.entry("Da liễu", "https://res.cloudinary.com/deied72qz/image/upload/v1794322250/eclinic/mau/chuyen-khoa/da-lieu.jpg"),
            Map.entry("Dị ứng - Miễn dịch", "https://res.cloudinary.com/deied72qz/image/upload/v1794322303/eclinic/mau/chuyen-khoa/di-ung-mien-dich.jpg"),
            Map.entry("Dinh dưỡng", "https://res.cloudinary.com/deied72qz/image/upload/v1794322292/eclinic/mau/chuyen-khoa/dinh-duong.jpg"),
            Map.entry("Hô hấp", "https://res.cloudinary.com/deied72qz/image/upload/v1794322268/eclinic/mau/chuyen-khoa/ho-hap.jpg"),
            Map.entry("Huyết học", "https://res.cloudinary.com/deied72qz/image/upload/v1794322284/eclinic/mau/chuyen-khoa/huyet-hoc.jpg"),
            Map.entry("Lão khoa", "https://res.cloudinary.com/deied72qz/image/upload/v1794322300/eclinic/mau/chuyen-khoa/lao-khoa.jpg"),
            Map.entry("Mắt", "https://res.cloudinary.com/deied72qz/image/upload/v1794322259/eclinic/mau/chuyen-khoa/mat.jpg"),
            Map.entry("Nam khoa", "https://res.cloudinary.com/deied72qz/image/upload/v1794322304/eclinic/mau/chuyen-khoa/nam-khoa.jpg"),
            Map.entry("Ngoại thần kinh", "https://res.cloudinary.com/deied72qz/image/upload/v1794322297/eclinic/mau/chuyen-khoa/ngoai-than-kinh.jpg"),
            Map.entry("Ngoại tổng quát", "https://res.cloudinary.com/deied72qz/image/upload/v1794322295/eclinic/mau/chuyen-khoa/ngoai-tong-quat.jpg"),
            Map.entry("Nhi khoa", "https://res.cloudinary.com/deied72qz/image/upload/v1794322248/eclinic/mau/chuyen-khoa/nhi-khoa.jpg"),
            Map.entry("Nội tiết", "https://res.cloudinary.com/deied72qz/image/upload/v1794322271/eclinic/mau/chuyen-khoa/noi-tiet.jpg"),
            Map.entry("Nội tổng quát", "https://res.cloudinary.com/deied72qz/image/upload/v1794322245/eclinic/mau/chuyen-khoa/noi-tong-quat.jpg"),
            Map.entry("Phẫu thuật tạo hình", "https://res.cloudinary.com/deied72qz/image/upload/v1794322310/eclinic/mau/chuyen-khoa/phau-thuat-tao-hinh.jpg"),
            Map.entry("Phục hồi chức năng", "https://res.cloudinary.com/deied72qz/image/upload/v1794322289/eclinic/mau/chuyen-khoa/phuc-hoi-chuc-nang.jpg"),
            Map.entry("Răng Hàm Mặt", "https://res.cloudinary.com/deied72qz/image/upload/v1794322260/eclinic/mau/chuyen-khoa/rang-ham-mat.jpg"),
            Map.entry("Sản phụ khoa", "https://res.cloudinary.com/deied72qz/image/upload/v1794322256/eclinic/mau/chuyen-khoa/san-phu-khoa.jpg"),
            Map.entry("Sơ sinh", "https://res.cloudinary.com/deied72qz/image/upload/v1794322308/eclinic/mau/chuyen-khoa/so-sinh.jpg"),
            Map.entry("Tai Mũi Họng", "https://res.cloudinary.com/deied72qz/image/upload/v1794322252/eclinic/mau/chuyen-khoa/tai-mui-hong.jpg"),
            Map.entry("Tâm thần", "https://res.cloudinary.com/deied72qz/image/upload/v1794322286/eclinic/mau/chuyen-khoa/tam-than.jpg"),
            Map.entry("Thần kinh", "https://res.cloudinary.com/deied72qz/image/upload/v1794322262/eclinic/mau/chuyen-khoa/than-kinh.jpg"),
            Map.entry("Thận - Tiết niệu", "https://res.cloudinary.com/deied72qz/image/upload/v1794322276/eclinic/mau/chuyen-khoa/than-tiet-nieu.jpg"),
            Map.entry("Tiêu hoá", "https://res.cloudinary.com/deied72qz/image/upload/v1794322265/eclinic/mau/chuyen-khoa/tieu-hoa.jpg"),
            Map.entry("Tim mạch", "https://res.cloudinary.com/deied72qz/image/upload/v1794322255/eclinic/mau/chuyen-khoa/tim-mach.jpg"),
            Map.entry("Truyền nhiễm", "https://res.cloudinary.com/deied72qz/image/upload/v1794322285/eclinic/mau/chuyen-khoa/truyen-nhiem.jpg"),
            Map.entry("Ung bướu", "https://res.cloudinary.com/deied72qz/image/upload/v1794322280/eclinic/mau/chuyen-khoa/ung-buou.jpg"),
            Map.entry("Y học cổ truyền", "https://res.cloudinary.com/deied72qz/image/upload/v1794322287/eclinic/mau/chuyen-khoa/y-hoc-co-truyen.jpg"),
            Map.entry("Y học thể thao", "https://res.cloudinary.com/deied72qz/image/upload/v1794322311/eclinic/mau/chuyen-khoa/y-hoc-the-thao.jpg"));

    /** Mọi URL ảnh mẫu. */
    public static final Set<String> TAT_CA = Set.copyOf(Stream.of(BAC_SI, BAC_SI_NAM, BAC_SI_NU, NGUOI_DUNG_NAM,
            NGUOI_DUNG_NU, CONG_VIEC, CHUNG_CHI, GIAI_THUONG, List.copyOf(CHUYEN_KHOA.values())).flatMap(List::stream).toList());

    /** Chân dung riêng của bác sĩ mẫu chính số {@code so} (1-12); ngoài khoảng đó trả null. */
    public static String bacSi(int so) {
        return so >= 1 && so <= BAC_SI.size() ? BAC_SI.get(so - 1) : null;
    }

    /** Chân dung bác sĩ cùng giới tính, xoay vòng theo {@code so} (KHAC dùng ảnh nam). */
    public static String bacSiTheoGioiTinh(GioiTinh gioiTinh, int so) {
        return chon(gioiTinh == GioiTinh.NU ? BAC_SI_NU : BAC_SI_NAM, so);
    }

    /** Ảnh đại diện người dùng cùng giới tính, xoay vòng theo {@code so} (KHAC dùng ảnh nam). */
    public static String nguoiDung(GioiTinh gioiTinh, int so) {
        return chon(gioiTinh == GioiTinh.NU ? NGUOI_DUNG_NU : NGUOI_DUNG_NAM, so);
    }

    public static String congViec(int i) {
        return chon(CONG_VIEC, i);
    }

    public static String chungChi(int i) {
        return chon(CHUNG_CHI, i);
    }

    public static String giaiThuong(int i) {
        return chon(GIAI_THUONG, i);
    }

    private static String chon(List<String> danhSach, int i) {
        return danhSach.get(Math.floorMod(i, danhSach.size()));
    }
}
