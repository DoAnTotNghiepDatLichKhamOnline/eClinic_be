package iuh.fit.se.eclinic.booking.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.booking.config.PhieuKhamProperties;
import iuh.fit.se.eclinic.booking.dto.response.BenhNhanPhieuKhamResponse;
import iuh.fit.se.eclinic.booking.dto.response.DatLichResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenCuaToiResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;
import iuh.fit.se.eclinic.booking.dto.response.NguoiGiamHoPhieuKhamResponse;
import iuh.fit.se.eclinic.booking.dto.response.PhieuKhamResponse;
import iuh.fit.se.eclinic.booking.util.CheThongTin;
import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.booking.NguoiGiamHo;
import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;
import iuh.fit.se.eclinic.common.util.ChiaCaLamViec;
import lombok.RequiredArgsConstructor;

/**
 * Chuyển lịch hẹn sang DTO. Gọi trong transaction: bác sĩ, phòng khám, hồ sơ, người giám hộ là quan hệ lazy.
 */
@Component
@RequiredArgsConstructor
public class LichHenMapper {

    private final CaKhamMapper caKhamMapper;
    private final PhieuKhamProperties phieuKhamProperties;

    /**
     * @param gioBatDauKhung  khung 1 giờ chứa lượt khám của lịch hẹn
     * @param gioKetThucKhung giờ kết thúc khung đó
     * @param bacSiDoPhongKhamXep true nếu người đặt chọn "bác sĩ bất kỳ"
     */
    public DatLichResponse toDatLichResponse(LichHen lichHen, LocalDateTime gioBatDauKhung,
            LocalDateTime gioKetThucKhung, boolean bacSiDoPhongKhamXep) {
        LocalDateTime gioKhamDuKien = lichHen.getKhungGio().getGioBatDau();
        NguoiGiamHo nguoiGiamHo = lichHen.getNguoiGiamHo();
        return new DatLichResponse(lichHen.getMaTokenPhieuKham(), lichHen.getMaTraCuu(),
                phieuKhamProperties.lienKet(lichHen.getMaTokenPhieuKham()), lichHen.getSoThuTu(),
                gioKhamDuKien.toLocalDate(), gioKhamDuKien, gioBatDauKhung, gioKetThucKhung, lichHen.getTrangThai(),
                caKhamMapper.toBacSiTomTat(lichHen.getBacSi()),
                caKhamMapper.toPhongKhamTomTat(lichHen.getPhongKham()), lichHen.getHoSoBenhNhan().getHoTen(),
                nguoiGiamHo == null ? null : nguoiGiamHo.getHoTen(), lichHen.getTaiKhoanDat() != null,
                bacSiDoPhongKhamXep);
    }

    /**
     * 1 dòng "lịch hẹn của tôi". Khung 1 giờ tính lại từ ca của lượt khám.
     *
     * @param idHoSoCuaToi id hồ sơ bệnh nhân đã liên kết của tài khoản đang xem; null nếu tài khoản chưa có hồ sơ
     */
    public LichHenCuaToiResponse toLichHenCuaToi(LichHen lichHen, Long idHoSoCuaToi) {
        LocalDateTime gioKhamDuKien = lichHen.getKhungGio().getGioBatDau();
        LocalDate ngay = gioKhamDuKien.toLocalDate();
        LichLamViec ca = lichHen.getKhungGio().getLichLamViec();
        LocalTime gioBatDauKhung = ChiaCaLamViec.gioBatDauKhung(ca.getGioBatDau(), gioKhamDuKien.toLocalTime());
        LocalTime gioKetThucKhung = ChiaCaLamViec.gioKetThucKhung(gioBatDauKhung, ca.getGioKetThuc());

        HoSoBenhNhan hoSo = lichHen.getHoSoBenhNhan();
        NguoiGiamHo nguoiGiamHo = lichHen.getNguoiGiamHo();
        return new LichHenCuaToiResponse(lichHen.getMaTokenPhieuKham(), lichHen.getMaTraCuu(),
                phieuKhamProperties.lienKet(lichHen.getMaTokenPhieuKham()), lichHen.getTrangThai(),
                lichHen.getSoThuTu(), ngay, gioKhamDuKien, ngay.atTime(gioBatDauKhung), ngay.atTime(gioKetThucKhung),
                caKhamMapper.toBacSiTomTat(lichHen.getBacSi()),
                lichHen.getBacSi().getChuyenKhoa().getTenChuyenKhoa(),
                caKhamMapper.toPhongKhamTomTat(lichHen.getPhongKham()), hoSo.getHoTen(),
                hoSo.getId().equals(idHoSoCuaToi), nguoiGiamHo == null ? null : nguoiGiamHo.getHoTen(),
                lichHen.getLyDoKham(), lichHen.getNgayTao());
    }

    /**
     * Phiếu khám công khai: CCCD, SĐT đã che. Khung 1 giờ tính lại từ ca của lượt khám. SĐT của bệnh nhân là SĐT liên hệ
     * của lượt khám; khi có người giám hộ thì SĐT đó là của người giám hộ nên chỉ hiện ở phần người giám hộ.
     */
    public PhieuKhamResponse toPhieuKhamResponse(LichHen lichHen) {
        LocalDateTime gioKhamDuKien = lichHen.getKhungGio().getGioBatDau();
        LocalDate ngay = gioKhamDuKien.toLocalDate();
        LichLamViec ca = lichHen.getKhungGio().getLichLamViec();
        LocalTime gioBatDauKhung = ChiaCaLamViec.gioBatDauKhung(ca.getGioBatDau(), gioKhamDuKien.toLocalTime());
        LocalTime gioKetThucKhung = ChiaCaLamViec.gioKetThucKhung(gioBatDauKhung, ca.getGioKetThuc());

        HoSoBenhNhan hoSo = lichHen.getHoSoBenhNhan();
        NguoiGiamHo nguoiGiamHo = lichHen.getNguoiGiamHo();
        BenhNhanPhieuKhamResponse benhNhan = new BenhNhanPhieuKhamResponse(hoSo.getHoTen(),
                hoSo.getNgaySinh() == null ? null : hoSo.getNgaySinh().getYear(), hoSo.getGioiTinh(),
                CheThongTin.cccd(hoSo.getCccd()),
                nguoiGiamHo == null ? CheThongTin.soDienThoai(lichHen.getSoDienThoaiLienHe()) : null);

        return new PhieuKhamResponse(lichHen.getMaTokenPhieuKham(), lichHen.getMaTraCuu(),
                phieuKhamProperties.lienKet(lichHen.getMaTokenPhieuKham()), lichHen.getTrangThai(),
                lichHen.getSoThuTu(), ngay, gioKhamDuKien, ngay.atTime(gioBatDauKhung), ngay.atTime(gioKetThucKhung),
                caKhamMapper.toBacSiTomTat(lichHen.getBacSi()),
                lichHen.getBacSi().getChuyenKhoa().getTenChuyenKhoa(),
                caKhamMapper.toPhongKhamTomTat(lichHen.getPhongKham()), lichHen.getLyDoKham(), lichHen.getNgayTao(),
                benhNhan, nguoiGiamHo == null ? null : toNguoiGiamHoPhieuKham(nguoiGiamHo), nguoiGiamHo != null);
    }

    /**
     * 1 dòng lịch hẹn cho bác sĩ / quản trị viên: SĐT không che, không có mã phiếu khám và CCCD.
     *
     * @param kemBacSi false ở danh sách của chính bác sĩ (trường {@code bacSi} để null)
     */
    public LichHenTrongCaResponse toTrongCaResponse(LichHen lichHen, boolean kemBacSi) {
        LocalDateTime gioKhamDuKien = lichHen.getKhungGio().getGioBatDau();
        LocalDate ngay = gioKhamDuKien.toLocalDate();
        LichLamViec ca = lichHen.getKhungGio().getLichLamViec();
        LocalTime gioBatDauKhung = ChiaCaLamViec.gioBatDauKhung(ca.getGioBatDau(), gioKhamDuKien.toLocalTime());
        LocalTime gioKetThucKhung = ChiaCaLamViec.gioKetThucKhung(gioBatDauKhung, ca.getGioKetThuc());

        HoSoBenhNhan hoSo = lichHen.getHoSoBenhNhan();
        NguoiGiamHo nguoiGiamHo = lichHen.getNguoiGiamHo();
        Integer tuoi = hoSo.getNgaySinh() == null ? null : Period.between(hoSo.getNgaySinh(), ngay).getYears();
        return new LichHenTrongCaResponse(lichHen.getId(), lichHen.getMaTraCuu(), ca.getId(), lichHen.getTrangThai(), lichHen.getSoThuTu(),
                gioKhamDuKien, ngay.atTime(gioBatDauKhung), ngay.atTime(gioKetThucKhung),
                kemBacSi ? caKhamMapper.toBacSiTomTat(lichHen.getBacSi()) : null,
                caKhamMapper.toPhongKhamTomTat(lichHen.getPhongKham()), lichHen.getLyDoKham(), lichHen.getNgayTao(),
                new LichHenTrongCaResponse.BenhNhan(hoSo.getHoTen(), hoSo.getNgaySinh(), tuoi, hoSo.getGioiTinh(),
                        nguoiGiamHo == null ? lichHen.getSoDienThoaiLienHe() : null),
                nguoiGiamHo == null ? null
                        : new LichHenTrongCaResponse.NguoiGiamHo(nguoiGiamHo.getHoTen(), nguoiGiamHo.getQuanHe(),
                                nguoiGiamHo.getSoDienThoai()));
    }

    private static NguoiGiamHoPhieuKhamResponse toNguoiGiamHoPhieuKham(NguoiGiamHo nguoiGiamHo) {
        return new NguoiGiamHoPhieuKhamResponse(nguoiGiamHo.getHoTen(), nguoiGiamHo.getQuanHe(),
                CheThongTin.soDienThoai(nguoiGiamHo.getSoDienThoai()));
    }

}
