package iuh.fit.se.eclinic.medical.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import iuh.fit.se.eclinic.common.entity.medical.HoSoBenhAn;
import iuh.fit.se.eclinic.common.enums.DuongDungThuoc;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;

/** Hồ sơ bệnh án của 1 lượt khám, cho bác sĩ đã khám. */
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
        LocalDateTime hanSua,
        String maSoKham,
        String linkSoKham) {

    /** Sinh hiệu: mạch, nhịp thở lần/phút; nhiệt độ °C; huyết áp mmHg; cân nặng kg; chiều cao cm. */
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

    /** Thuốc lấy từ danh mục; khi sửa hồ sơ, gửi lại {@code idThuoc} để giữ dòng thuốc. */
    public record DonThuoc(Long idThuoc, String tenThuoc, String hoatChat, String hamLuong, DuongDungThuoc duongDung,
            String donVi, String lieuDung, Integer soLanMoiNgay, Integer soNgayDung, Integer soLuong,
            String ghiChuSuDung) {
    }
}
