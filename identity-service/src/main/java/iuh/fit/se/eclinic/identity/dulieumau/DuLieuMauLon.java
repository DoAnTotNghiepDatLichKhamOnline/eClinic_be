package iuh.fit.se.eclinic.identity.dulieumau;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import iuh.fit.se.eclinic.common.enums.GioiTinh;
import iuh.fit.se.eclinic.identity.dulieumau.DuLieuMau.Buoi;

/** Nội dung dữ liệu mẫu lớn: khoảng 100 dòng mỗi bảng, nối tiếp số thứ tự của {@link DuLieuMau}. */
public final class DuLieuMauLon {

    /** Không vượt 100 bác sĩ: dropdown lấy kichThuoc=100, là mức tối đa của server. */
    public static final int SO_BAC_SI_TOI_DA = 100;

    /** Bác sĩ từ số 1 tới số này có ca và lịch hẹn trong quá khứ. */
    public static final int SO_BAC_SI_CO_LICH_SU = 42;

    public static final int SO_BENH_NHAN_DAU = 7;
    public static final int SO_BENH_NHAN_CUOI = 102;

    /** Người thân không có tài khoản, số thứ tự nối sau bệnh nhân để CCCD không trùng. */
    public static final int SO_NGUOI_THAN_DAU = 201;
    public static final int SO_NGUOI_THAN = 34;
    public static final int SO_TAI_KHOAN_LUU_MOI_NGUOI_THAN = 3;

    public static final int SO_QUAN_TRI_DAU = 2;
    public static final int SO_QUAN_TRI_CUOI = 100;

    /** Phòng trống, không gắn bác sĩ. */
    public static final List<Integer> PHONG_DU_PHONG = List.of(301, 302, 303, 304);
    public static final int PHONG_NGUNG_HOAT_DONG = 304;

    private DuLieuMauLon() {
    }

    public static String emailBacSi(int so) {
        return "bacsi%02d@eclinic.local".formatted(so);
    }

    public static String emailBenhNhan(int so) {
        return "benhnhan%02d@eclinic.local".formatted(so);
    }

    public static String emailQuanTri(int so) {
        return "quantri%03d@eclinic.local".formatted(so);
    }

    /** Tài khoản tạo cuối cùng; có nghĩa là dữ liệu mẫu lớn đã chạy xong. */
    public static String emailDanhDau() {
        return emailQuanTri(SO_QUAN_TRI_CUOI);
    }

    // 4 số cuối thành 6 số: "%02d" của dữ liệu mẫu nhỏ sinh ra SĐT 11 số, CCCD 13 số từ số 100
    public static String soDienThoaiBacSi(int so) {
        return "0901%06d".formatted(so);
    }

    public static String soDienThoaiBenhNhan(int so) {
        return "0902%06d".formatted(so);
    }

    public static String soDienThoaiQuanTri(int so) {
        return "0903%06d".formatted(so);
    }

    public static String cccd(int so) {
        return "079%09d".formatted(so);
    }

    public static String soGiayPhep(int so) {
        return "CCHN-%06d".formatted(so);
    }

    public static int soPhong(int soBacSi) {
        return 200 + soBacSi;
    }

    public static String tenPhong(int soPhong) {
        return "Phòng " + soPhong;
    }

    public static String tenPhongEn(int soPhong) {
        return "Room " + soPhong;
    }

    public static int soTang(int soPhong) {
        return 2 + soPhong % 6;
    }

    /** Ca thứ nhất trong tuần của bác sĩ, thứ 2 tới thứ 7. */
    public static DayOfWeek thuDauTuan(int so) {
        return DayOfWeek.of(so % 6 + 1);
    }

    /** Mỗi bác sĩ 2 ca 1 tuần, cách nhau 3 ngày, cùng 1 buổi. Null nếu nghỉ. */
    public static Buoi buoi(int so, DayOfWeek thu) {
        if (thu != thuDauTuan(so) && thu != DayOfWeek.of((so + 3) % 6 + 1)) {
            return null;
        }
        return so % 2 == 0 ? Buoi.SANG : Buoi.CHIEU;
    }

    public static GioiTinh gioiTinh(int so) {
        return so % 2 == 0 ? GioiTinh.NU : GioiTinh.NAM;
    }

    public static String hoTen(int so) {
        boolean nu = gioiTinh(so) == GioiTinh.NU;
        List<String> dem = nu ? DEM_NU : DEM_NAM;
        List<String> ten = nu ? TEN_NU : TEN_NAM;
        return HO.get(so % HO.size()) + " " + dem.get(so % dem.size()) + " " + ten.get(so % ten.size());
    }

    public static LocalDate ngaySinh(int so) {
        return LocalDate.of(1950 + so % 55, so % 12 + 1, so % 28 + 1);
    }

    public static String diaChi(int so) {
        return "%d %s, TP. Hồ Chí Minh".formatted(so, DUONG.get(so % DUONG.size()));
    }

    public static String anhDaiDien(int so) {
        return ANH_DAI_DIEN.get(so % ANH_DAI_DIEN.size());
    }

    private static final List<String> HO = List.of("Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ", "Võ",
            "Đặng", "Bùi", "Đỗ", "Hồ", "Ngô", "Dương", "Lý");

    private static final List<String> DEM_NAM = List.of("Văn", "Hữu", "Đức", "Minh", "Quốc", "Thành", "Gia");

    private static final List<String> DEM_NU = List.of("Thị", "Ngọc", "Thu", "Thanh", "Kim", "Bảo");

    private static final List<String> TEN_NAM = List.of("An", "Bình", "Cường", "Dũng", "Đạt", "Hải", "Hiếu", "Hùng",
            "Khánh", "Khoa", "Long", "Minh", "Nam", "Phát", "Phúc", "Quân", "Sơn", "Tâm", "Thắng", "Tuấn", "Việt");

    private static final List<String> TEN_NU = List.of("Anh", "Châu", "Diệp", "Giang", "Hà", "Hạnh", "Hoa", "Hương",
            "Lan", "Linh", "Mai", "My", "Ngân", "Nhung", "Oanh", "Phương", "Quỳnh", "Thảo", "Trang", "Vy", "Yến");

    private static final List<String> DUONG = List.of("Lê Lợi, Quận 1", "Võ Văn Tần, Quận 3", "Nguyễn Trãi, Quận 5",
            "Phan Đăng Lưu, Bình Thạnh", "Quang Trung, Gò Vấp", "Cộng Hoà, Tân Bình");

    /** Dùng chung vài ảnh: 100 URL khác nhau chỉ làm trình duyệt tải thêm mà không demo được gì hơn. */
    private static final List<String> ANH_DAI_DIEN = List.of(
            "https://placehold.co/400x400/0f766e/ffffff/png?text=BS",
            "https://placehold.co/400x400/1565c0/ffffff/png?text=BS",
            "https://placehold.co/400x400/8d6e63/ffffff/png?text=BS",
            "https://placehold.co/400x400/6a1b9a/ffffff/png?text=BS",
            "https://placehold.co/400x400/c62828/ffffff/png?text=BS",
            "https://placehold.co/400x400/2e7d32/ffffff/png?text=BS");

    public static final List<String> HOC_VI = List.of("BSCKI", "BSCKII", "ThS.BS", "TS.BS");

    public record ChuyenKhoaLon(String ten, String tenEn, String moTa, String moTaEn, String trieuChung,
            String trieuChungEn) {
    }

    public static final List<ChuyenKhoaLon> CHUYEN_KHOA = List.of(
            new ChuyenKhoaLon("Thần kinh", "Neurology",
                    "Khám và điều trị đau đầu, mất ngủ, động kinh, tai biến mạch máu não.",
                    "Diagnosis and treatment of headache, insomnia, epilepsy and stroke.",
                    "đau đầu, chóng mặt, mất ngủ, tê tay chân, run tay, co giật, giảm trí nhớ",
                    "headache, dizziness, insomnia, numb limbs, tremor, seizures, memory loss"),
            new ChuyenKhoaLon("Tiêu hoá", "Gastroenterology",
                    "Khám và điều trị bệnh dạ dày, ruột, gan, mật, tuỵ; tư vấn nội soi tiêu hoá.",
                    "Care for stomach, bowel, liver, gallbladder and pancreas conditions; endoscopy advice.",
                    "đau bụng, ợ chua, đầy hơi, tiêu chảy, táo bón, đi ngoài ra máu, vàng da",
                    "abdominal pain, heartburn, bloating, diarrhoea, constipation, bloody stool, jaundice"),
            new ChuyenKhoaLon("Hô hấp", "Pulmonology",
                    "Khám và điều trị hen phế quản, viêm phổi, bệnh phổi tắc nghẽn mạn tính.",
                    "Care for asthma, pneumonia and chronic obstructive pulmonary disease.",
                    "ho kéo dài, khó thở, khò khè, tức ngực, ho ra đờm, khó thở khi nằm",
                    "persistent cough, shortness of breath, wheezing, chest tightness, productive cough"),
            new ChuyenKhoaLon("Nội tiết", "Endocrinology",
                    "Khám và điều trị đái tháo đường, bệnh tuyến giáp, rối loạn nội tiết.",
                    "Care for diabetes, thyroid disease and hormonal disorders.",
                    "khát nước nhiều, tiểu nhiều, sụt cân, bướu cổ, hồi hộp, tăng cân nhanh",
                    "excessive thirst, frequent urination, weight loss, goitre, palpitations, rapid weight gain"),
            new ChuyenKhoaLon("Cơ xương khớp", "Rheumatology",
                    "Khám và điều trị viêm khớp, thoái hoá khớp, gout, loãng xương.",
                    "Care for arthritis, osteoarthritis, gout and osteoporosis.",
                    "đau khớp, sưng khớp, cứng khớp buổi sáng, đau lưng, đau vai gáy, gout",
                    "joint pain, swollen joints, morning stiffness, back pain, neck and shoulder pain, gout"),
            new ChuyenKhoaLon("Thận - Tiết niệu", "Nephrology and Urology",
                    "Khám và điều trị sỏi thận, viêm đường tiết niệu, bệnh thận mạn.",
                    "Care for kidney stones, urinary tract infections and chronic kidney disease.",
                    "tiểu buốt, tiểu rắt, tiểu ra máu, đau hông lưng, phù mặt, tiểu đêm nhiều",
                    "painful urination, urgency, blood in urine, flank pain, facial swelling, frequent night urination"),
            new ChuyenKhoaLon("Ung bướu", "Oncology",
                    "Tầm soát, tư vấn và theo dõi điều trị các bệnh ung thư thường gặp.",
                    "Screening, counselling and treatment follow-up for common cancers.",
                    "khối u bất thường, sụt cân không rõ nguyên nhân, hạch to, tầm soát ung thư",
                    "unusual lump, unexplained weight loss, enlarged lymph nodes, cancer screening"),
            new ChuyenKhoaLon("Huyết học", "Hematology",
                    "Khám và điều trị thiếu máu, rối loạn đông máu, bệnh lý tế bào máu.",
                    "Care for anaemia, clotting disorders and blood cell conditions.",
                    "da xanh, mệt mỏi, chảy máu lâu cầm, bầm tím bất thường, chóng mặt khi đứng dậy",
                    "pale skin, fatigue, prolonged bleeding, unusual bruising, dizziness on standing"),
            new ChuyenKhoaLon("Truyền nhiễm", "Infectious Diseases",
                    "Khám và điều trị sốt xuất huyết, cúm, viêm gan virus, bệnh nhiễm khuẩn.",
                    "Care for dengue, influenza, viral hepatitis and bacterial infections.",
                    "sốt cao, phát ban, đau mỏi người, sốt kéo dài, tiêu chảy cấp, nổi hạch",
                    "high fever, rash, body aches, prolonged fever, acute diarrhoea, swollen glands"),
            new ChuyenKhoaLon("Tâm thần", "Psychiatry",
                    "Khám và tư vấn lo âu, trầm cảm, rối loạn giấc ngủ, căng thẳng kéo dài.",
                    "Assessment and counselling for anxiety, depression, sleep disorders and chronic stress.",
                    "lo âu, buồn chán kéo dài, mất ngủ, căng thẳng, hoảng sợ, mất hứng thú",
                    "anxiety, persistent low mood, insomnia, stress, panic, loss of interest"),
            new ChuyenKhoaLon("Y học cổ truyền", "Traditional Medicine",
                    "Khám, châm cứu, xoa bóp bấm huyệt và điều trị bằng thuốc y học cổ truyền.",
                    "Consultation, acupuncture, acupressure and herbal treatment.",
                    "đau mỏi vai gáy, đau thần kinh toạ, mất ngủ, liệt mặt, phục hồi sau tai biến",
                    "neck and shoulder pain, sciatica, insomnia, facial palsy, recovery after stroke"),
            new ChuyenKhoaLon("Phục hồi chức năng", "Rehabilitation",
                    "Tập phục hồi vận động sau chấn thương, phẫu thuật, tai biến.",
                    "Movement rehabilitation after injury, surgery or stroke.",
                    "yếu tay chân sau tai biến, hạn chế vận động, đau sau chấn thương, tập đi lại",
                    "weak limbs after stroke, limited movement, pain after injury, gait training"),
            new ChuyenKhoaLon("Dinh dưỡng", "Nutrition",
                    "Tư vấn dinh dưỡng theo bệnh lý, thừa cân, béo phì, suy dinh dưỡng.",
                    "Dietary advice for medical conditions, overweight, obesity and malnutrition.",
                    "thừa cân, béo phì, biếng ăn, sụt cân, tư vấn chế độ ăn, ăn kiêng cho tiểu đường",
                    "overweight, obesity, poor appetite, weight loss, diet advice, diabetic diet"),
            new ChuyenKhoaLon("Chấn thương chỉnh hình", "Orthopedics",
                    "Khám và điều trị gãy xương, trật khớp, chấn thương thể thao.",
                    "Care for fractures, dislocations and sports injuries.",
                    "gãy xương, trật khớp, bong gân, đau gối, đau cổ tay, cong vẹo cột sống",
                    "fractures, dislocations, sprains, knee pain, wrist pain, scoliosis"),
            new ChuyenKhoaLon("Ngoại tổng quát", "General Surgery",
                    "Khám và phẫu thuật ruột thừa, thoát vị, sỏi mật, u phần mềm.",
                    "Assessment and surgery for appendicitis, hernia, gallstones and soft-tissue lumps.",
                    "đau bụng dưới bên phải, khối phồng vùng bẹn, u mỡ, trĩ, áp xe",
                    "lower right abdominal pain, groin bulge, lipoma, haemorrhoids, abscess"),
            new ChuyenKhoaLon("Ngoại thần kinh", "Neurosurgery",
                    "Khám và điều trị phẫu thuật bệnh lý não, tuỷ sống, thoát vị đĩa đệm.",
                    "Surgical care for brain and spinal conditions, including herniated discs.",
                    "đau lưng lan xuống chân, thoát vị đĩa đệm, tê bì chân, chấn thương sọ não",
                    "back pain radiating to the leg, herniated disc, leg numbness, head injury"),
            new ChuyenKhoaLon("Lão khoa", "Geriatrics",
                    "Chăm sóc sức khoẻ người cao tuổi, quản lý nhiều bệnh mạn tính cùng lúc.",
                    "Health care for older adults, managing several chronic conditions together.",
                    "hay quên, té ngã nhiều, ăn kém, tiểu không tự chủ, dùng nhiều loại thuốc",
                    "forgetfulness, frequent falls, poor appetite, incontinence, multiple medications"),
            new ChuyenKhoaLon("Dị ứng - Miễn dịch", "Allergy and Immunology",
                    "Khám và điều trị dị ứng thuốc, thức ăn, viêm mũi dị ứng, mày đay mạn tính.",
                    "Care for drug and food allergies, allergic rhinitis and chronic urticaria.",
                    "nổi mề đay, ngứa, sưng môi, dị ứng thức ăn, dị ứng thuốc, hắt hơi theo mùa",
                    "hives, itching, swollen lips, food allergy, drug allergy, seasonal sneezing"),
            new ChuyenKhoaLon("Nam khoa", "Andrology",
                    "Khám và tư vấn sức khoẻ sinh sản nam giới.",
                    "Assessment and counselling for male reproductive health.",
                    "đau tinh hoàn, tiểu khó, rối loạn cương, hiếm muộn ở nam, tư vấn sinh sản",
                    "testicular pain, difficulty urinating, erectile dysfunction, male infertility"),
            new ChuyenKhoaLon("Sơ sinh", "Neonatology",
                    "Khám và theo dõi trẻ sơ sinh, trẻ sinh non, vàng da sơ sinh.",
                    "Care and follow-up for newborns, premature babies and neonatal jaundice.",
                    "trẻ sơ sinh vàng da, bú kém, sốt ở trẻ dưới 3 tháng, chậm tăng cân sau sinh",
                    "newborn jaundice, poor feeding, fever under 3 months, slow weight gain after birth"),
            new ChuyenKhoaLon("Phẫu thuật tạo hình", "Plastic Surgery",
                    "Điều trị sẹo, tạo hình sau chấn thương và các dị tật bẩm sinh vùng mặt.",
                    "Treatment of scars, reconstruction after injury and congenital facial defects.",
                    "sẹo lồi, sẹo co kéo, sụp mi, dị tật vành tai, vết thương khó lành",
                    "keloid scars, contracture scars, drooping eyelids, ear deformities, slow-healing wounds"),
            new ChuyenKhoaLon("Y học thể thao", "Sports Medicine",
                    "Khám, tư vấn tập luyện và điều trị chấn thương cho người chơi thể thao.",
                    "Assessment, training advice and injury care for active people.",
                    "đau khi chạy bộ, căng cơ, đau gót chân, chấn thương dây chằng, tư vấn tập luyện",
                    "running pain, muscle strain, heel pain, ligament injury, exercise advice"));

    /** 1 hoạt chất với 3 hàm lượng, thành 3 dòng trong danh mục thuốc. */
    public record NhomThuocLon(String ten, List<String> hamLuong, String nhom, String nhomEn) {
    }

    public static final List<NhomThuocLon> THUOC = List.of(
            new NhomThuocLon("Azithromycin", List.of("100mg", "250mg", "500mg"), "Kháng sinh", "Antibiotic"),
            new NhomThuocLon("Clarithromycin", List.of("125mg", "250mg", "500mg"), "Kháng sinh", "Antibiotic"),
            new NhomThuocLon("Ciprofloxacin", List.of("250mg", "500mg", "750mg"), "Kháng sinh", "Antibiotic"),
            new NhomThuocLon("Levofloxacin", List.of("250mg", "500mg", "750mg"), "Kháng sinh", "Antibiotic"),
            new NhomThuocLon("Cefixim", List.of("100mg", "200mg", "400mg"), "Kháng sinh", "Antibiotic"),
            new NhomThuocLon("Cefpodoxim", List.of("50mg", "100mg", "200mg"), "Kháng sinh", "Antibiotic"),
            new NhomThuocLon("Doxycyclin", List.of("50mg", "100mg", "200mg"), "Kháng sinh", "Antibiotic"),
            new NhomThuocLon("Metronidazol", List.of("200mg", "250mg", "500mg"), "Kháng sinh", "Antibiotic"),
            new NhomThuocLon("Fluconazol", List.of("50mg", "100mg", "150mg"), "Thuốc kháng nấm", "Antifungal"),
            new NhomThuocLon("Acyclovir", List.of("200mg", "400mg", "800mg"), "Thuốc kháng virus", "Antiviral"),
            new NhomThuocLon("Naproxen", List.of("250mg", "275mg", "500mg"), "Thuốc giảm đau, kháng viêm",
                    "Pain relief and anti-inflammatory"),
            new NhomThuocLon("Diclofenac", List.of("25mg", "50mg", "75mg"), "Thuốc giảm đau, kháng viêm",
                    "Pain relief and anti-inflammatory"),
            new NhomThuocLon("Celecoxib", List.of("100mg", "200mg", "400mg"), "Thuốc giảm đau, kháng viêm",
                    "Pain relief and anti-inflammatory"),
            new NhomThuocLon("Esomeprazol", List.of("10mg", "20mg", "40mg"), "Thuốc giảm tiết acid dạ dày",
                    "Stomach acid reducer"),
            new NhomThuocLon("Pantoprazol", List.of("20mg", "40mg", "80mg"), "Thuốc giảm tiết acid dạ dày",
                    "Stomach acid reducer"),
            new NhomThuocLon("Famotidin", List.of("10mg", "20mg", "40mg"), "Thuốc giảm tiết acid dạ dày",
                    "Stomach acid reducer"),
            new NhomThuocLon("Losartan", List.of("25mg", "50mg", "100mg"), "Thuốc hạ huyết áp", "Antihypertensive"),
            new NhomThuocLon("Enalapril", List.of("5mg", "10mg", "20mg"), "Thuốc hạ huyết áp", "Antihypertensive"),
            new NhomThuocLon("Bisoprolol", List.of("2,5mg", "5mg", "10mg"), "Thuốc hạ huyết áp", "Antihypertensive"),
            new NhomThuocLon("Atorvastatin", List.of("10mg", "20mg", "40mg"), "Thuốc hạ mỡ máu", "Lipid-lowering drug"),
            new NhomThuocLon("Rosuvastatin", List.of("5mg", "10mg", "20mg"), "Thuốc hạ mỡ máu", "Lipid-lowering drug"),
            new NhomThuocLon("Simvastatin", List.of("10mg", "20mg", "40mg"), "Thuốc hạ mỡ máu", "Lipid-lowering drug"),
            new NhomThuocLon("Gliclazid", List.of("30mg", "60mg", "80mg"), "Thuốc hạ đường huyết", "Antidiabetic"),
            new NhomThuocLon("Levothyroxin", List.of("25mcg", "50mcg", "100mcg"), "Hormon tuyến giáp", "Thyroid hormone"),
            new NhomThuocLon("Prednisolon", List.of("5mg", "10mg", "20mg"), "Corticoid", "Corticosteroid"),
            new NhomThuocLon("Fexofenadin", List.of("60mg", "120mg", "180mg"), "Thuốc chống dị ứng", "Antihistamine"),
            new NhomThuocLon("Montelukast", List.of("4mg", "5mg", "10mg"), "Thuốc phòng hen", "Asthma controller"),
            new NhomThuocLon("Vitamin D3", List.of("400IU", "1000IU", "2000IU"), "Vitamin", "Vitamin"));

    /** Nội dung 1 lần khám: mã ICD lấy từ danh mục, có trong tệp danh-muc/icd10.tsv. */
    public record LanKhamLon(String lyDoKham, String khamLamSang, String chanDoan, String maIcd) {
    }

    public static final List<LanKhamLon> LAN_KHAM = List.of(
            new LanKhamLon("Sốt, đau họng 2 ngày", "Họng đỏ, phổi không ran", "Viêm mũi họng cấp", "J06.9"),
            new LanKhamLon("Đau rát họng, nuốt đau", "Amidan sưng đỏ", "Viêm họng cấp", "J02.9"),
            new LanKhamLon("Ợ chua, nóng rát sau ăn", "Bụng mềm, ấn thượng vị không đau", "Trào ngược dạ dày thực quản",
                    "K21.9"),
            new LanKhamLon("Đau thượng vị khi đói", "Ấn đau nhẹ vùng thượng vị", "Viêm dạ dày", "K29.7"),
            new LanKhamLon("Khám định kỳ huyết áp", "Huyết áp cao hơn mục tiêu", "Tăng huyết áp nguyên phát", "I10"),
            new LanKhamLon("Khát nước, tiểu nhiều", "Đường huyết lúc đói tăng", "Đái tháo đường type 2", "E11.9"),
            new LanKhamLon("Nổi mụn vùng mặt", "Mụn viêm rải rác hai má", "Mụn trứng cá thông thường", "L70.0"),
            new LanKhamLon("Đỏ mắt, ngứa mắt", "Kết mạc cương tụ", "Viêm kết mạc", "H10.9"),
            new LanKhamLon("Đau răng hàm dưới", "Răng 36 có lỗ sâu", "Sâu răng", "K02.9"),
            new LanKhamLon("Đau cổ gáy kéo dài", "Co cứng cơ vùng cổ", "Đau vùng cổ", "M54.2"),
            new LanKhamLon("Đau nửa đầu từng cơn", "Khám thần kinh bình thường", "Đau nửa đầu", "G43.9"),
            new LanKhamLon("Khò khè về đêm", "Phổi có ran rít", "Hen phế quản", "J45.9"));

    public static final List<String> LY_DO_DOI_LICH = List.of("Có lịch hội chẩn đột xuất", "Đi công tác theo phân công",
            "Việc gia đình đột xuất", "Tham dự hội nghị chuyên ngành", "Sức khoẻ không đảm bảo");

    public static final List<String> NHAN_XET = List.of("Bác sĩ khám kỹ, giải thích dễ hiểu.",
            "Phòng khám sạch sẽ, chờ không lâu.", "Bác sĩ tận tình, nhẹ nhàng.", "Đúng giờ hẹn, thủ tục nhanh.",
            "Tư vấn rõ ràng, sẽ quay lại tái khám.");

    public static final List<String> CAU_HOI_CHAT = List.of("Tôi đau đầu nên khám khoa nào?",
            "Phòng khám làm việc mấy giờ?", "Trẻ sốt 39 độ có cần đi khám ngay không?", "Đặt lịch khám thế nào?",
            "Khám tổng quát cần nhịn ăn không?");

    public static final List<String> TRA_LOI_CHAT = List.of("Bạn có thể khám tại chuyên khoa Thần kinh hoặc Nội tổng quát.",
            "Phòng khám làm việc từ thứ 2 đến thứ 7, sáng 8 giờ, chiều 13 giờ 30.",
            "Trẻ sốt cao nên được bác sĩ khám sớm, bạn có thể đặt lịch khoa Nhi.",
            "Bạn chọn chuyên khoa, bác sĩ, khung giờ rồi xác nhận thông tin là xong.",
            "Bạn nên nhịn ăn 8 giờ trước khi lấy máu xét nghiệm.");

}
