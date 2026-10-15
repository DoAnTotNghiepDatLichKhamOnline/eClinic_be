package iuh.fit.se.eclinic.medical.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import iuh.fit.se.eclinic.common.entity.medical.HoSoBenhAn;
import iuh.fit.se.eclinic.common.enums.DuongDungThuoc;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;

/**
 * Hồ sơ bệnh án của 1 lượt khám, cho bác sĩ đã khám.
 *
 * @param maTraCuu        mã tra cứu ngắn của lịch hẹn
 * @param sinhHieu        null nếu chưa đo chỉ số nào
 * @param icdChinh        mã ICD-10 của chẩn đoán chính; null nếu không chọn
 * @param chanDoanKemTheo các bệnh kèm theo, chữ tự do
 * @param ngayTao         thời điểm ghi nhận kết quả khám
 * @param phienBan        1 khi ghi nhận, tăng 1 mỗi lần sửa; gửi lại khi sửa
 * @param ngaySuaCuoi     lần sửa gần nhất; null = chưa sửa lần nào
 * @param hanSua          sau thời điểm này hồ sơ chỉ xem được
 */
public record BenhAnResponse(
        Long id,
        Long idLichHen,
        String maTraCuu,
        TrangThaiLichHen trangThaiLichHen,
        String lyDoKham,
        SinhHieu sinhHieu,
        String khamLamSang,
        String chanDoan,
        Icd10Response icdChinh,
        String chanDoanKemTheo,
        String ghiChu,
        LocalDate ngayTaiKhamDeXuat,
        List<DonThuoc> donThuoc,
        LocalDateTime ngayTao,
        int phienBan,
        LocalDateTime ngaySuaCuoi,
        LocalDateTime hanSua) {

    /**
     * Sinh hiệu: mạch, nhịp thở lần/phút; nhiệt độ °C; huyết áp mmHg; cân nặng kg; chiều cao cm. Chỉ số không đo là null.
     */
    public record SinhHieu(Integer mach, BigDecimal nhietDo, Integer huyetApTamThu, Integer huyetApTamTruong,
            Integer nhipTho, BigDecimal canNang, BigDecimal chieuCao) {

        /** @return null nếu hồ sơ chưa có chỉ số nào */
        public static SinhHieu tu(HoSoBenhAn benhAn) {
            SinhHieu sinhHieu = new SinhHieu(benhAn.getMach(), benhAn.getNhietDo(), benhAn.getHuyetApTamThu(),
                    benhAn.getHuyetApTamTruong(), benhAn.getNhipTho(), benhAn.getCanNang(), benhAn.getChieuCao());
            boolean trong = Stream.of(sinhHieu.mach, sinhHieu.nhietDo, sinhHieu.huyetApTamThu, sinhHieu.huyetApTamTruong,
                    sinhHieu.nhipTho, sinhHieu.canNang, sinhHieu.chieuCao).allMatch(chiSo -> chiSo == null);
            return trong ? null : sinhHieu;
        }
    }

    /**
     * Thuốc lấy từ danh mục; khi sửa hồ sơ, gửi lại {@code idThuoc} để giữ dòng thuốc.
     *
     * @param hoatChat null ở thuốc cũ chưa được quản trị viên bổ sung (cả {@code hamLuong}, {@code duongDung})
     * @param donVi    đơn vị của thuốc trong danh mục
     * @param soLuong  tổng số lượng cấp, theo {@code donVi}; null ở dòng ghi trước khi bắt buộc số lượng
     */
    public record DonThuoc(Long idThuoc, String tenThuoc, String hoatChat, String hamLuong, DuongDungThuoc duongDung,
            String donVi, String lieuDung, Integer soLanMoiNgay, Integer soNgayDung, Integer soLuong,
            String ghiChuSuDung) {
    }
}
