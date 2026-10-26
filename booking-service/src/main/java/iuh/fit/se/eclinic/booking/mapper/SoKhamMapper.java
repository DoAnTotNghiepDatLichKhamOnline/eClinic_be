package iuh.fit.se.eclinic.booking.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.booking.dto.response.SoKhamResponse;
import iuh.fit.se.eclinic.booking.dto.response.SoKhamTomTatResponse;
import iuh.fit.se.eclinic.booking.util.CheThongTin;
import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.medical.HoSoBenhAn;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import lombok.RequiredArgsConstructor;

/**
 * Chuyển hồ sơ bệnh án sang sổ khám bệnh (EXAM-03). Họ tên, ngày sinh, giới tính và người giám hộ là những gì người đặt
 * đã nhập, như phiếu khám. Gọi trong transaction, với hồ sơ tải bằng HoSoBenhAnChiDocRepository#timTheoMaSoKham.
 */
@Component
@RequiredArgsConstructor
public class SoKhamMapper {

    private final CaKhamMapper caKhamMapper;
    private final KetQuaKhamMapper ketQuaKhamMapper;

    public SoKhamTomTatResponse toTomTat(HoSoBenhAn benhAn) {
        LichHen lichHen = benhAn.getLichHen();
        LocalDateTime gioKham = lichHen.getKhungGio().getGioBatDau();
        LocalDate ngaySinh = ngaySinh(lichHen);
        return new SoKhamTomTatResponse(CheThongTin.hoTen(LichHenMapper.hoTenBenhNhanDaNhap(lichHen)),
                ngaySinh == null ? null : ngaySinh.getYear(), gioKham.toLocalDate(), gioKham.toLocalTime(),
                caKhamMapper.toBacSiTomTat(lichHen.getBacSi()),
                lichHen.getBacSi().getChuyenKhoa().ten(NgonNgu.hienTai()),
                caKhamMapper.toPhongKhamTomTat(lichHen.getPhongKham()));
    }

    /** Đơn thuốc là quan hệ lazy: nơi gọi nên tải sẵn bằng HoSoBenhAnChiDocRepository#timTheoLichHenKemDonThuoc. */
    public SoKhamResponse toResponse(HoSoBenhAn benhAn) {
        LichHen lichHen = benhAn.getLichHen();
        HoSoBenhNhan hoSo = lichHen.getHoSoBenhNhan();
        boolean coBanDaNhap = lichHen.getHoTenDaNhap() != null;
        SoKhamResponse.BenhNhan benhNhan = new SoKhamResponse.BenhNhan(LichHenMapper.hoTenBenhNhanDaNhap(lichHen),
                ngaySinh(lichHen), coBanDaNhap ? lichHen.getGioiTinhDaNhap() : hoSo.getGioiTinh(),
                CheThongTin.cccd(hoSo.getCccd()));
        return new SoKhamResponse(toTomTat(benhAn), benhNhan, LichHenMapper.hoTenGiamHoDaNhap(lichHen),
                ketQuaKhamMapper.toResponse(benhAn));
    }

    private static LocalDate ngaySinh(LichHen lichHen) {
        return lichHen.getHoTenDaNhap() != null ? lichHen.getNgaySinhDaNhap() : lichHen.getHoSoBenhNhan().getNgaySinh();
    }

}
