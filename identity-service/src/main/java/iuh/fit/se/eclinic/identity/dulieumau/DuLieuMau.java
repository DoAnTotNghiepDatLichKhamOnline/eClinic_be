package iuh.fit.se.eclinic.identity.dulieumau;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import iuh.fit.se.eclinic.common.enums.GioiTinh;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;

/**
 * Nội dung dữ liệu mẫu: chuyên khoa + phòng khám, bác sĩ, bệnh nhân và mẫu xếp ca theo thứ trong tuần.
 * Email, số điện thoại, số giấy phép, CCCD sinh từ số thứ tự nên không trùng nhau.
 */
public final class DuLieuMau {

    /** Độ dài 1 khung giờ khám. Ca 3,5 giờ chia được 7 khung. */
    public static final int SO_PHUT_MOI_KHUNG = 30;

    private DuLieuMau() {
    }

    /** Buổi làm việc trong ngày. */
    public enum Buoi {
        SANG(LocalTime.of(8, 0), LocalTime.of(11, 30)),
        CHIEU(LocalTime.of(13, 30), LocalTime.of(17, 0));

        private final LocalTime gioBatDau;
        private final LocalTime gioKetThuc;

        Buoi(LocalTime gioBatDau, LocalTime gioKetThuc) {
            this.gioBatDau = gioBatDau;
            this.gioKetThuc = gioKetThuc;
        }

        public LocalTime gioBatDau() {
            return gioBatDau;
        }

        public LocalTime gioKetThuc() {
            return gioKetThuc;
        }
    }

    /**
     * Mẫu xếp ca. 2 bác sĩ cùng chuyên khoa dùng chung 1 phòng nên luôn làm khác buổi: không bao giờ trùng phòng.
     * Chủ nhật không ai làm. CHINH làm 21 giờ / tuần, PHU làm 17,5 giờ / tuần.
     */
    public enum NhomCa {
        /** Sáng thứ 2, 4, 6, 7; chiều thứ 3, 5. */
        CHINH(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY),
                Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)),
        /** Chiều thứ 2, 4, 6; sáng thứ 3, 5. */
        PHU(Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
                Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY));

        private final Set<DayOfWeek> ngaySang;
        private final Set<DayOfWeek> ngayChieu;

        NhomCa(Set<DayOfWeek> ngaySang, Set<DayOfWeek> ngayChieu) {
            this.ngaySang = ngaySang;
            this.ngayChieu = ngayChieu;
        }

        /** Buổi làm việc của ngày đó, null nếu nghỉ. */
        public Buoi buoi(DayOfWeek thu) {
            if (ngaySang.contains(thu)) {
                return Buoi.SANG;
            }
            return ngayChieu.contains(thu) ? Buoi.CHIEU : null;
        }
    }

    /** Mỗi chuyên khoa có 1 phòng khám. */
    public record ChuyenKhoaMau(String ten, String moTa, String tenPhong, String tang) {
    }

    public record BacSiMau(int so, String hoTen, String chuyenKhoa, NhomCa nhomCa, String hocVi, int soNamKinhNghiem,
            String tieuSu) {

        public String email() {
            return "bacsi%02d@eclinic.local".formatted(so);
        }

        public String soDienThoai() {
            return "09010000%02d".formatted(so);
        }

        public String soGiayPhep() {
            return "CCHN-0000%02d".formatted(so);
        }
    }

    /** Thông tin hồ sơ bệnh nhân (theo CCCD) của tài khoản bệnh nhân đã kích hoạt. */
    public record HoSoMau(LocalDate ngaySinh, GioiTinh gioiTinh, String diaChi) {
    }

    /**
     * @param hoSo          null: tài khoản chưa có hồ sơ bệnh nhân
     * @param lyDoVoHieuHoa chỉ có khi trạng thái là VO_HIEU_HOA
     */
    public record BenhNhanMau(int so, String hoTen, TrangThaiTaiKhoan trangThai, String lyDoVoHieuHoa, HoSoMau hoSo) {

        public String email() {
            return "benhnhan%02d@eclinic.local".formatted(so);
        }

        public String soDienThoai() {
            return "09020000%02d".formatted(so);
        }

        public String cccd() {
            return "0790000000%02d".formatted(so);
        }
    }

    public static final List<ChuyenKhoaMau> CHUYEN_KHOA = List.of(
            new ChuyenKhoaMau("Nội tổng quát",
                    "Khám và điều trị các bệnh nội khoa thường gặp: hô hấp, tiêu hoá, nội tiết, khám sức khoẻ tổng quát.",
                    "Phòng 101", "Tầng 1"),
            new ChuyenKhoaMau("Nhi khoa",
                    "Khám, tư vấn dinh dưỡng và điều trị bệnh cho trẻ em từ sơ sinh đến 15 tuổi.",
                    "Phòng 102", "Tầng 1"),
            new ChuyenKhoaMau("Da liễu",
                    "Khám và điều trị các bệnh về da, tóc, móng: mụn trứng cá, viêm da, dị ứng, nấm da.",
                    "Phòng 103", "Tầng 1"),
            new ChuyenKhoaMau("Tai Mũi Họng",
                    "Khám và điều trị viêm xoang, viêm họng, viêm tai giữa, nội soi tai mũi họng.",
                    "Phòng 104", "Tầng 1"),
            new ChuyenKhoaMau("Tim mạch",
                    "Khám và theo dõi tăng huyết áp, rối loạn nhịp tim, bệnh mạch vành, suy tim.",
                    "Phòng 105", "Tầng 1"),
            new ChuyenKhoaMau("Sản phụ khoa",
                    "Khám thai định kỳ, khám phụ khoa, tư vấn sức khoẻ sinh sản.",
                    "Phòng 106", "Tầng 1"),
            new ChuyenKhoaMau("Mắt",
                    "Khám và điều trị tật khúc xạ, viêm kết mạc, đục thuỷ tinh thể, đo thị lực.",
                    "Phòng 107", "Tầng 1"),
            new ChuyenKhoaMau("Răng Hàm Mặt",
                    "Khám răng miệng, trám răng, nhổ răng, lấy cao răng, tư vấn chỉnh nha.",
                    "Phòng 108", "Tầng 1"));

    /** 4 chuyên khoa đầu có 2 bác sĩ (CHINH + PHU), 4 chuyên khoa sau có 1 bác sĩ. */
    public static final List<BacSiMau> BAC_SI = List.of(
            new BacSiMau(1, "Nguyễn Văn An", "Nội tổng quát", NhomCa.CHINH, "TS.BS", 18,
                    "Hơn 18 năm khám và điều trị bệnh nội khoa, thế mạnh về bệnh lý tiêu hoá và nội tiết."),
            new BacSiMau(2, "Trần Thị Bích", "Nội tổng quát", NhomCa.PHU, "ThS.BS", 9,
                    "Chuyên khám sức khoẻ tổng quát và quản lý bệnh mạn tính: đái tháo đường, rối loạn mỡ máu."),
            new BacSiMau(3, "Lê Hoàng Cường", "Nhi khoa", NhomCa.CHINH, "BSCKII", 20,
                    "20 năm kinh nghiệm nhi khoa, chuyên về hô hấp và dinh dưỡng trẻ em."),
            new BacSiMau(4, "Phạm Thị Dung", "Nhi khoa", NhomCa.PHU, "BSCKI", 8,
                    "Khám và tư vấn tiêm chủng, theo dõi phát triển thể chất cho trẻ nhỏ."),
            new BacSiMau(5, "Hoàng Minh Đức", "Da liễu", NhomCa.CHINH, "ThS.BS", 12,
                    "Điều trị mụn trứng cá, viêm da cơ địa, vảy nến và các bệnh da dị ứng."),
            new BacSiMau(6, "Vũ Thị Giang", "Da liễu", NhomCa.PHU, "BS", 5,
                    "Khám da liễu tổng quát, tư vấn chăm sóc da và điều trị nấm da."),
            new BacSiMau(7, "Đặng Quốc Hải", "Tai Mũi Họng", NhomCa.CHINH, "BSCKII", 16,
                    "Nội soi và điều trị viêm xoang mạn tính, viêm tai giữa, viêm amidan."),
            new BacSiMau(8, "Bùi Thị Hương", "Tai Mũi Họng", NhomCa.PHU, "BSCKI", 7,
                    "Khám tai mũi họng cho người lớn và trẻ em, điều trị viêm mũi dị ứng."),
            new BacSiMau(9, "Ngô Thanh Khoa", "Tim mạch", NhomCa.CHINH, "PGS.TS.BS", 25,
                    "25 năm trong chuyên ngành tim mạch, chuyên về tăng huyết áp và bệnh mạch vành."),
            new BacSiMau(10, "Đỗ Thị Lan", "Sản phụ khoa", NhomCa.CHINH, "ThS.BS", 14,
                    "Khám thai, siêu âm sản khoa và điều trị các bệnh phụ khoa thường gặp."),
            new BacSiMau(11, "Lý Gia Minh", "Mắt", NhomCa.CHINH, "BSCKI", 10,
                    "Khám tật khúc xạ, điều trị viêm kết mạc và tư vấn phẫu thuật đục thuỷ tinh thể."),
            new BacSiMau(12, "Trương Thị Ngọc", "Răng Hàm Mặt", NhomCa.CHINH, "BS", 6,
                    "Khám và điều trị răng miệng tổng quát, nha khoa trẻ em."));

    /** 01–04 đã kích hoạt và có hồ sơ bệnh nhân; 05 chưa xác thực email; 06 bị vô hiệu hoá. */
    public static final List<BenhNhanMau> BENH_NHAN = List.of(
            new BenhNhanMau(1, "Nguyễn Thị Mai", TrangThaiTaiKhoan.DA_KICH_HOAT, null,
                    new HoSoMau(LocalDate.of(1995, 3, 12), GioiTinh.NU,
                            "12 Nguyễn Văn Bảo, Phường 4, Gò Vấp, TP. Hồ Chí Minh")),
            new BenhNhanMau(2, "Trần Văn Nam", TrangThaiTaiKhoan.DA_KICH_HOAT, null,
                    new HoSoMau(LocalDate.of(1988, 11, 5), GioiTinh.NAM,
                            "45 Lê Lợi, Bến Nghé, Quận 1, TP. Hồ Chí Minh")),
            new BenhNhanMau(3, "Lê Thị Oanh", TrangThaiTaiKhoan.DA_KICH_HOAT, null,
                    new HoSoMau(LocalDate.of(2001, 7, 23), GioiTinh.NU,
                            "78 Quang Trung, Phường 10, Gò Vấp, TP. Hồ Chí Minh")),
            new BenhNhanMau(4, "Phạm Quốc Phong", TrangThaiTaiKhoan.DA_KICH_HOAT, null,
                    new HoSoMau(LocalDate.of(1976, 1, 30), GioiTinh.NAM,
                            "5 Phan Văn Trị, Phường 7, Bình Thạnh, TP. Hồ Chí Minh")),
            new BenhNhanMau(5, "Hoàng Thị Quyên", TrangThaiTaiKhoan.CHO_XAC_NHAN, null, null),
            new BenhNhanMau(6, "Vũ Đình Sơn", TrangThaiTaiKhoan.VO_HIEU_HOA,
                    "Tài khoản mẫu bị vô hiệu hoá để thử chức năng quản lý tài khoản", null));

}
