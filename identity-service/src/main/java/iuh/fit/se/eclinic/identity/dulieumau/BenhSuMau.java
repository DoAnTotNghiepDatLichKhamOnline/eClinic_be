package iuh.fit.se.eclinic.identity.dulieumau;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import iuh.fit.se.eclinic.common.util.DanhMucIcd10;

/**
 * Nội dung lịch sử khám mẫu (DOANTOTNGH-28): mỗi bệnh nhân mẫu có 1 bệnh mạn tính (tái khám đều với cùng bác sĩ) và 1
 * đợt bệnh cấp tính (khám lần đầu rồi tái khám sau 7 ngày). Thuốc lấy theo tên + hàm lượng trong danh mục mẫu
 * ({@link DuLieuMau#THUOC}, {@link DuLieuMauLon#THUOC}); mã ICD có trong {@code danh-muc/icd10.tsv}. Chỉ bệnh của người
 * lớn, không theo giới tính, vì bệnh nhân mẫu đều là người lớn, nam nữ xen kẽ.
 */
final class BenhSuMau {

    /**
     * 1 dòng đơn thuốc. {@code soNgayDung} null = dùng tới lần tái khám (bệnh mạn tính); {@code soLuongCoDinh} khác null
     * khi thuốc tính theo bình / lọ chứ không theo liều.
     */
    record ThuocKe(String ten, String hamLuong, int soMoiLan, String donVi, int soLanMoiNgay, Integer soNgayDung,
            Integer soLuongCoDinh, String ghiChuSuDung) {

        String lieuDung() {
            return soMoiLan + " " + donVi + "/lần";
        }

        int soNgay(int soNgayGiua) {
            return soNgayDung != null ? soNgayDung : soNgayGiua;
        }

        int soLuong(int soNgayGiua) {
            return soLuongCoDinh != null ? soLuongCoDinh : soMoiLan * soLanMoiNgay * soNgay(soNgayGiua);
        }
    }

    /**
     * 1 bệnh: lần đầu và các lần tái khám khác nhau ở lý do, khám lâm sàng; huyết áp tâm thu đi dần từ {@code haDau} tới
     * {@code haCuoi} (bệnh được kiểm soát), nhiệt độ lần đầu {@code nhietDoDau}, các lần sau bình thường.
     */
    record ChuoiBenh(String chuyenKhoa, String maIcd, String chanDoan, String lyDoLanDau, String lyDoTaiKham,
            String khamLanDau, String khamTaiKham, String ghiChu, int soNgayGiua, int haDau, int haCuoi,
            double nhietDoDau, List<ThuocKe> thuoc) {
    }

    private static ThuocKe vien(String ten, String hamLuong, int soLanMoiNgay, String ghiChu) {
        return new ThuocKe(ten, hamLuong, 1, "viên", soLanMoiNgay, null, null, ghiChu);
    }

    private static ThuocKe vienNgan(String ten, String hamLuong, int soLanMoiNgay, int soNgay, String ghiChu) {
        return new ThuocKe(ten, hamLuong, 1, "viên", soLanMoiNgay, soNgay, null, ghiChu);
    }

    private static final ThuocKe SALBUTAMOL = new ThuocKe("Salbutamol", "100mcg/liều", 2, "nhát", 2, null, 1,
            "Xịt khi khó thở, tối đa 4 lần/ngày");

    static final List<ChuoiBenh> MAN_TINH = List.of(
            new ChuoiBenh("Tim mạch", "I10", "Tăng huyết áp nguyên phát",
                    "Đau đầu, chóng mặt, đo huyết áp tại nhà cao", "Tái khám định kỳ tăng huyết áp",
                    "Huyết áp cao, tim đều, phổi không ran, không phù chân",
                    "Huyết áp ổn định hơn lần trước, tim đều, không phù",
                    "Ăn nhạt, hạn chế rượu bia, đo huyết áp sáng tối và ghi lại", 28, 158, 128, 36.7,
                    List.of(vien("Amlodipin", "5mg", 1, "Uống buổi sáng"),
                            vien("Losartan", "50mg", 1, "Uống buổi sáng, cùng giờ mỗi ngày"))),
            new ChuoiBenh("Nội tiết", "E11.9", "Đái tháo đường type 2",
                    "Khát nước, tiểu nhiều, sụt cân", "Tái khám đái tháo đường, mang kết quả đường huyết",
                    "Đường huyết lúc đói 9,8 mmol/L, không tê bì chân",
                    "Đường huyết lúc đói giảm dần, bàn chân không tổn thương",
                    "Ăn ít tinh bột, đường; đi bộ 30 phút mỗi ngày; tự đo đường huyết", 35, 132, 124, 36.6,
                    List.of(vien("Metformin", "500mg", 2, "Uống ngay sau ăn sáng và tối"),
                            vien("Gliclazid", "30mg", 1, "Uống trước ăn sáng 30 phút"))),
            new ChuoiBenh("Nội tiết", "E03.9", "Suy giáp",
                    "Mệt mỏi, sợ lạnh, tăng cân", "Tái khám suy giáp, kiểm tra TSH",
                    "Da khô, mạch chậm, tuyến giáp không to", "Bớt mệt, TSH về gần mức bình thường",
                    "Uống thuốc lúc đói, cách thuốc khác ít nhất 4 giờ", 35, 118, 116, 36.4,
                    List.of(vien("Levothyroxin", "50mcg", 1, "Uống lúc đói, trước ăn sáng 30 phút"),
                            vien("Vitamin D3", "1000IU", 1, "Uống sau ăn trưa"))),
            new ChuoiBenh("Tiêu hoá", "K21.0", "Trào ngược dạ dày - thực quản có viêm thực quản",
                    "Ợ chua, nóng rát sau xương ức", "Tái khám trào ngược dạ dày",
                    "Bụng mềm, ấn thượng vị đau nhẹ", "Hết ợ chua ban đêm, bụng mềm không đau",
                    "Không nằm ngay sau ăn, kê cao đầu giường, tránh cà phê, đồ cay", 28, 122, 118, 36.6,
                    List.of(vien("Esomeprazol", "20mg", 1, "Uống trước ăn sáng 30 phút"),
                            vien("Domperidon", "10mg", 2, "Uống trước ăn 15 phút"))),
            new ChuoiBenh("Tiêu hoá", "K29.5", "Viêm dạ dày mạn",
                    "Đau âm ỉ thượng vị, đầy bụng", "Tái khám viêm dạ dày",
                    "Ấn đau vùng thượng vị, không phản ứng thành bụng", "Giảm đau thượng vị, ăn uống tốt hơn",
                    "Ăn đúng bữa, tránh rượu bia, thuốc giảm đau không kê đơn", 30, 120, 118, 36.6,
                    List.of(vien("Pantoprazol", "40mg", 1, "Uống trước ăn sáng 30 phút"),
                            vien("Famotidin", "20mg", 1, "Uống trước khi ngủ"))),
            new ChuoiBenh("Hô hấp", "J45.9", "Hen phế quản",
                    "Khò khè, khó thở về đêm", "Tái khám hen phế quản",
                    "Phổi có ran rít hai bên", "Phổi ít ran, cơn khó thở thưa hơn",
                    "Tránh khói bụi, lông thú; luôn mang theo bình xịt", 30, 124, 120, 36.8,
                    List.of(SALBUTAMOL, vien("Montelukast", "10mg", 1, "Uống buổi tối"))),
            new ChuoiBenh("Hô hấp", "J44.9", "Bệnh phổi tắc nghẽn mạn tính",
                    "Ho khạc đờm, khó thở khi gắng sức", "Tái khám bệnh phổi tắc nghẽn mạn tính",
                    "Phổi rì rào phế nang giảm, ran ngáy rải rác", "Bớt khó thở, phổi ít ran",
                    "Bỏ thuốc lá, tập thở chúm môi, tiêm phòng cúm hằng năm", 30, 134, 128, 36.9,
                    List.of(SALBUTAMOL, vienNgan("Prednisolon", "5mg", 1, 7, "Uống sau ăn sáng, 7 ngày"))),
            new ChuoiBenh("Cơ xương khớp", "M17.9", "Thoái hoá khớp gối",
                    "Đau gối khi đi lại, lên xuống cầu thang", "Tái khám thoái hoá khớp gối",
                    "Gối lạo xạo khi vận động, không sưng nóng", "Giảm đau gối, đi lại tốt hơn",
                    "Giảm cân, tránh ngồi xổm, tập đạp xe nhẹ", 30, 136, 130, 36.6,
                    List.of(vienNgan("Celecoxib", "200mg", 1, 14, "Uống sau ăn, dùng khi đau"),
                            vien("Vitamin D3", "1000IU", 1, "Uống sau ăn trưa"))),
            new ChuoiBenh("Cơ xương khớp", "M81.9", "Loãng xương",
                    "Đau lưng âm ỉ, thấp dần", "Tái khám loãng xương",
                    "Ấn đau cột sống thắt lưng, không dấu thần kinh khu trú", "Đỡ đau lưng, vận động tốt",
                    "Tắm nắng sáng, ăn đủ canxi, phòng té ngã", 35, 128, 124, 36.6,
                    List.of(vien("Vitamin D3", "2000IU", 1, "Uống sau ăn trưa"),
                            vienNgan("Paracetamol", "500mg", 2, 7, "Uống khi đau, cách nhau ít nhất 6 giờ"))),
            new ChuoiBenh("Da liễu", "L20.9", "Viêm da cơ địa",
                    "Ngứa, nổi mảng đỏ khô ở khuỷu tay", "Tái khám viêm da cơ địa",
                    "Mảng da đỏ, khô, bong vảy hai khuỷu", "Mảng da nhạt màu, bớt ngứa",
                    "Dưỡng ẩm ngày 2 lần, tránh xà phòng mạnh, không gãi", 28, 118, 116, 36.6,
                    List.of(vien("Cetirizin", "10mg", 1, "Uống buổi tối"),
                            vienNgan("Prednisolon", "5mg", 1, 5, "Uống sau ăn sáng, 5 ngày"))),
            new ChuoiBenh("Dị ứng - Miễn dịch", "J30.4", "Viêm mũi dị ứng",
                    "Hắt hơi, chảy mũi, ngứa mũi buổi sáng", "Tái khám viêm mũi dị ứng",
                    "Niêm mạc mũi nhợt, phù nề", "Bớt hắt hơi, niêm mạc mũi hồng",
                    "Đeo khẩu trang, giữ nhà sạch bụi, rửa mũi nước muối", 30, 120, 118, 36.7,
                    List.of(vien("Fexofenadin", "180mg", 1, "Uống buổi sáng"),
                            vien("Montelukast", "10mg", 1, "Uống buổi tối"))),
            new ChuoiBenh("Thần kinh", "G43.9", "Đau nửa đầu (migraine)",
                    "Đau nửa đầu từng cơn kèm buồn nôn", "Tái khám đau nửa đầu, mang nhật ký cơn đau",
                    "Khám thần kinh bình thường", "Cơn đau thưa hơn, ngắn hơn",
                    "Ngủ đủ giấc, tránh thức khuya, ghi nhật ký cơn đau", 30, 122, 118, 36.6,
                    List.of(vienNgan("Naproxen", "500mg", 2, 7, "Uống sau ăn khi có cơn đau"),
                            vienNgan("Paracetamol", "500mg", 2, 7, "Uống khi đau, cách nhau ít nhất 6 giờ"))),
            new ChuoiBenh("Nội tổng quát", "E78.5", "Rối loạn lipid máu",
                    "Khám sức khoẻ, mỡ máu cao", "Tái khám rối loạn lipid máu, mang kết quả xét nghiệm",
                    "Thể trạng thừa cân, tim phổi bình thường", "Mỡ máu giảm, không đau cơ",
                    "Hạn chế mỡ động vật, đồ chiên; tập thể dục đều", 35, 134, 126, 36.6,
                    List.of(vien("Atorvastatin", "20mg", 1, "Uống buổi tối"),
                            vien("Vitamin D3", "1000IU", 1, "Uống sau ăn trưa"))));

    static final List<ChuoiBenh> CAP_TINH = List.of(
            new ChuoiBenh("Tai Mũi Họng", "J02.9", "Viêm họng cấp",
                    "Sốt, đau rát họng, nuốt đau", "Tái khám viêm họng",
                    "Họng đỏ, amidan sưng", "Hết sốt, họng bớt đỏ",
                    "Súc họng nước muối, uống nhiều nước ấm", 7, 118, 116, 38.4,
                    List.of(vienNgan("Amoxicillin", "500mg", 2, 7, "Uống sau ăn, đủ 7 ngày"),
                            vienNgan("Paracetamol", "500mg", 3, 3, "Uống khi sốt trên 38,5 độ"))),
            new ChuoiBenh("Hô hấp", "J20.9", "Viêm phế quản cấp",
                    "Ho có đờm, sốt nhẹ 3 ngày", "Tái khám viêm phế quản",
                    "Phổi ran ngáy rải rác", "Bớt ho, phổi không ran",
                    "Uống nhiều nước, giữ ấm cổ ngực", 7, 120, 118, 38.0,
                    List.of(vienNgan("Azithromycin", "500mg", 1, 3, "Uống trước ăn 1 giờ, 3 ngày"),
                            vienNgan("Paracetamol", "500mg", 3, 3, "Uống khi sốt trên 38,5 độ"))),
            new ChuoiBenh("Thận - Tiết niệu", "N39.0", "Nhiễm trùng đường tiết niệu",
                    "Tiểu buốt, tiểu rắt", "Tái khám nhiễm trùng tiết niệu, mang kết quả nước tiểu",
                    "Ấn đau nhẹ hạ vị, chạm thận âm tính", "Hết tiểu buốt, nước tiểu trong",
                    "Uống 2 lít nước mỗi ngày, không nhịn tiểu", 7, 122, 118, 37.8,
                    List.of(vienNgan("Ciprofloxacin", "500mg", 2, 7, "Uống sau ăn, đủ 7 ngày"),
                            vienNgan("Paracetamol", "500mg", 2, 3, "Uống khi đau, sốt"))),
            new ChuoiBenh("Mắt", "H10.9", "Viêm kết mạc",
                    "Đỏ mắt, ngứa, nhiều ghèn", "Tái khám viêm kết mạc",
                    "Kết mạc cương tụ, giác mạc trong", "Mắt hết đỏ, hết ghèn",
                    "Rửa tay thường xuyên, không dụi mắt, dùng khăn riêng", 7, 118, 116, 36.8,
                    List.of(new ThuocKe("Cloramphenicol", "0,4%", 1, "giọt", 4, 7, 1, "Nhỏ mỗi mắt, cách nhau 4 giờ"),
                            vienNgan("Loratadin", "10mg", 1, 7, "Uống buổi tối"))),
            new ChuoiBenh("Da liễu", "B35.4", "Nấm da thân",
                    "Mảng da ngứa hình vòng ở lưng", "Tái khám nấm da",
                    "Mảng đỏ bờ rõ, có vảy, giữa nhạt màu", "Mảng da mờ dần, hết ngứa",
                    "Giữ da khô thoáng, giặt riêng quần áo, khăn tắm", 7, 120, 118, 36.7,
                    List.of(vienNgan("Fluconazol", "150mg", 1, 4, "Uống 1 viên mỗi tuần, 4 tuần"),
                            vienNgan("Cetirizin", "10mg", 1, 7, "Uống buổi tối khi ngứa"))),
            new ChuoiBenh("Cơ xương khớp", "M54.5", "Đau vùng thắt lưng",
                    "Đau lưng sau mang vác nặng", "Tái khám đau thắt lưng",
                    "Co cứng cơ cạnh sống thắt lưng, Lasègue âm tính", "Giảm đau, cúi người dễ hơn",
                    "Tránh mang vác nặng, chườm ấm, tập giãn cơ lưng", 7, 126, 122, 36.6,
                    List.of(vienNgan("Diclofenac", "50mg", 2, 5, "Uống sau ăn no"),
                            vienNgan("Paracetamol", "500mg", 2, 5, "Uống khi đau"))),
            new ChuoiBenh("Tiêu hoá", "K29.7", "Viêm dạ dày cấp",
                    "Đau thượng vị, buồn nôn sau ăn", "Tái khám viêm dạ dày",
                    "Ấn đau thượng vị, bụng mềm", "Hết buồn nôn, bụng không đau",
                    "Ăn mềm, chia nhỏ bữa, tránh đồ cay, rượu bia", 7, 118, 116, 37.2,
                    List.of(vienNgan("Omeprazol", "20mg", 1, 14, "Uống trước ăn sáng 30 phút"),
                            vienNgan("Domperidon", "10mg", 3, 5, "Uống trước ăn 15 phút"))),
            new ChuoiBenh("Thần kinh", "G44.2", "Đau đầu dạng căng thẳng",
                    "Đau đầu như bó chặt, căng cứng gáy", "Tái khám đau đầu",
                    "Khám thần kinh bình thường, co cứng cơ gáy", "Bớt đau đầu, ngủ tốt hơn",
                    "Nghỉ ngơi hợp lý, giảm dùng màn hình buổi tối", 7, 124, 120, 36.6,
                    List.of(vienNgan("Ibuprofen", "400mg", 2, 5, "Uống sau ăn khi đau"),
                            vienNgan("Paracetamol", "500mg", 2, 5, "Uống khi đau, cách nhau ít nhất 6 giờ"))));

    private BenhSuMau() {
    }

    /** Bệnh mạn tính của bệnh nhân số {@code so}: bước 5 (nguyên tố cùng nhau với 13) để bệnh nhân liền nhau khác bệnh. */
    static ChuoiBenh manTinh(int so) {
        return MAN_TINH.get(Math.floorMod(so * 5, MAN_TINH.size()));
    }

    static ChuoiBenh capTinh(int so) {
        return CAP_TINH.get(Math.floorMod(so * 3, CAP_TINH.size()));
    }

    /**
     * Tên chuyên khoa / mã ICD / thuốc nào không khớp danh mục ({@code tenChuyenKhoa}: tên chuyên khoa đang có,
     * {@code thuocDaCo}: khoá {@link #khoaThuoc}). Rỗng là khớp hết.
     */
    static List<String> kiemTra(Set<String> tenChuyenKhoa, Set<String> thuocDaCo) {
        List<String> loi = new ArrayList<>();
        for (List<ChuoiBenh> nhom : List.of(MAN_TINH, CAP_TINH)) {
            for (ChuoiBenh benh : nhom) {
                if (!tenChuyenKhoa.contains(benh.chuyenKhoa())) {
                    loi.add("chuyên khoa " + benh.chuyenKhoa());
                }
                if (DanhMucIcd10.tim(benh.maIcd()).isEmpty()) {
                    loi.add("ICD " + benh.maIcd());
                }
                for (ThuocKe thuoc : benh.thuoc()) {
                    if (!thuocDaCo.contains(khoaThuoc(thuoc.ten(), thuoc.hamLuong()))) {
                        loi.add("thuốc " + thuoc.ten() + " " + thuoc.hamLuong());
                    }
                }
            }
        }
        return loi;
    }

    /** Khoá so khớp thuốc: tên + hàm lượng, bỏ khoảng trắng thừa, chữ thường (như tenChuanHoa / hamLuongChuanHoa). */
    static String khoaThuoc(String ten, String hamLuong) {
        return chuan(ten) + "|" + chuan(hamLuong);
    }

    private static String chuan(String chuoi) {
        return chuoi == null ? "" : chuoi.trim().replaceAll("\\s+", " ").toLowerCase(java.util.Locale.ROOT);
    }
}
