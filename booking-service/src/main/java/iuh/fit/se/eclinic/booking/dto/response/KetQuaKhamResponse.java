package iuh.fit.se.eclinic.booking.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import iuh.fit.se.eclinic.common.enums.DuongDungThuoc;

/** Kết quả 1 lượt khám từ hồ sơ bệnh án, cho hồ sơ khám của bác sĩ và lịch sử khám của bệnh nhân. */
public record KetQuaKhamResponse(String lyDoKham, SinhHieu sinhHieu, String khamLamSang, String chanDoan,
        IcdChinh icdChinh, String chanDoanKemTheo, String ghiChu, LocalDate ngayTaiKhamDeXuat, List<DonThuoc> donThuoc,
        LocalDateTime ngaySuaCuoi,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer phienBan,
        @JsonInclude(JsonInclude.Include.NON_NULL) LocalDateTime hanSua,
        @JsonInclude(JsonInclude.Include.NON_NULL) String maSoKham,
        @JsonInclude(JsonInclude.Include.NON_NULL) String linkSoKham) {

    /** Mạch, nhịp thở lần/phút; nhiệt độ °C; huyết áp mmHg; cân nặng kg; chiều cao cm. Chỉ số không đo là null. */
    public record SinhHieu(Integer mach, BigDecimal nhietDo, Integer huyetApTamThu, Integer huyetApTamTruong,
            Integer nhipTho, BigDecimal canNang, BigDecimal chieuCao) {
    }

    public record IcdChinh(String ma, String ten, boolean manTinh) {
    }

    public record DonThuoc(Long idThuoc, String tenThuoc, String hoatChat, String hamLuong, DuongDungThuoc duongDung, String donVi,
            String lieuDung, Integer soLanMoiNgay, Integer soNgayDung, Integer soLuong, String ghiChuSuDung) {
    }
}
