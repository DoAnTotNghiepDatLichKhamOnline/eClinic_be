package iuh.fit.se.eclinic.booking.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.booking.config.DatLichProperties;
import iuh.fit.se.eclinic.booking.config.PhieuKhamProperties;
import iuh.fit.se.eclinic.booking.dto.response.BenhNhanPhieuKhamResponse;
import iuh.fit.se.eclinic.booking.dto.response.DatLichResponse;
import iuh.fit.se.eclinic.booking.dto.response.DanhGiaCuaToiResponse;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaKhamResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenChiTietCuaToiResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenCuaToiResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;
import iuh.fit.se.eclinic.booking.dto.response.NguoiDatLich;
import iuh.fit.se.eclinic.booking.dto.response.NguoiGiamHoPhieuKhamResponse;
import iuh.fit.se.eclinic.booking.dto.response.PhieuKhamResponse;
import iuh.fit.se.eclinic.booking.util.CheThongTin;
import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.booking.NguoiGiamHo;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;
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
    private final DatLichProperties datLichProperties;

    /**
     * Hạn chót bệnh nhân hủy / đổi lịch trên hệ thống: giờ khám trừ {@code app.dat-lich.huy-doi-truoc-toi-thieu}. Lịch
     * hẹn đang chờ đổi vì ca khám bị hủy ({@code canDoiLich}) thì được hủy / đổi tới đúng giờ khám cũ.
     */
    public LocalDateTime hanHuyDoi(LichHen lichHen) {
        LocalDateTime gioKham = lichHen.getKhungGio().getGioBatDau();
        return lichHen.isCanDoiLich() ? gioKham : gioKham.minus(datLichProperties.huyDoiTruocToiThieu());
    }

    /** Lịch còn hiệu lực và chưa quá hạn hủy / đổi (chưa xét ai được làm). */
    private boolean conHuyDoiDuoc(LichHen lichHen) {
        TrangThaiLichHen trangThai = lichHen.getTrangThai();
        return (trangThai == TrangThaiLichHen.CHO_XAC_NHAN || trangThai == TrangThaiLichHen.DA_XAC_NHAN)
                && LocalDateTime.now().isBefore(hanHuyDoi(lichHen));
    }

    /**
     * @param gioBatDauKhung  khung 1 giờ chứa lượt khám của lịch hẹn
     * @param gioKetThucKhung giờ kết thúc khung đó
     * @param bacSiDoPhongKhamXep true nếu người đặt chọn "bác sĩ bất kỳ"
     */
    public DatLichResponse toDatLichResponse(LichHen lichHen, LocalDateTime gioBatDauKhung,
            LocalDateTime gioKetThucKhung, boolean bacSiDoPhongKhamXep) {
        LocalDateTime gioKhamDuKien = lichHen.getKhungGio().getGioBatDau();
        return new DatLichResponse(lichHen.getMaTokenPhieuKham(), lichHen.getMaTraCuu(),
                phieuKhamProperties.lienKet(lichHen.getMaTokenPhieuKham()), lichHen.getSoThuTu(),
                gioKhamDuKien.toLocalDate(), gioKhamDuKien, gioBatDauKhung, gioKetThucKhung, lichHen.getTrangThai(),
                caKhamMapper.toBacSiTomTat(lichHen.getBacSi()),
                caKhamMapper.toPhongKhamTomTat(lichHen.getPhongKham()), hoTenBenhNhanDaNhap(lichHen),
                hoTenGiamHoDaNhap(lichHen), lichHen.getTaiKhoanDat() != null, bacSiDoPhongKhamXep);
    }

    /**
     * 1 dòng "lịch hẹn của tôi". Khung 1 giờ tính lại từ ca của lượt khám. Lịch của chính chủ tài khoản hiện họ tên
     * trong hồ sơ của họ (người xem là chủ hồ sơ); lịch của người khác hiện họ tên người đặt đã nhập.
     *
     * @param idHoSoCuaToi id hồ sơ bệnh nhân đã liên kết của tài khoản đang xem; null nếu tài khoản chưa có hồ sơ
     * @param idTaiKhoan   tài khoản đang xem
     */
    public LichHenCuaToiResponse toLichHenCuaToi(LichHen lichHen, Long idHoSoCuaToi, Long idTaiKhoan) {
        LocalDateTime gioKhamDuKien = lichHen.getKhungGio().getGioBatDau();
        LocalDate ngay = gioKhamDuKien.toLocalDate();
        LichLamViec ca = lichHen.getKhungGio().getLichLamViec();
        LocalTime gioBatDauKhung = gioBatDauKhung(lichHen);
        LocalTime gioKetThucKhung = gioKetThucKhung(lichHen, gioBatDauKhung);

        HoSoBenhNhan hoSo = lichHen.getHoSoBenhNhan();
        boolean laBanThan = hoSo.getId().equals(idHoSoCuaToi);
        NguoiDatLich nguoiDat = nguoiDat(lichHen, idTaiKhoan);
        return new LichHenCuaToiResponse(lichHen.getMaTokenPhieuKham(), lichHen.getMaTraCuu(),
                phieuKhamProperties.lienKet(lichHen.getMaTokenPhieuKham()), lichHen.getTrangThai(),
                lichHen.getSoThuTu(), ngay, gioKhamDuKien, ngay.atTime(gioBatDauKhung), ngay.atTime(gioKetThucKhung),
                caKhamMapper.toBacSiTomTat(lichHen.getBacSi()),
                lichHen.getBacSi().getChuyenKhoa().getTenChuyenKhoa(),
                caKhamMapper.toPhongKhamTomTat(lichHen.getPhongKham()),
                laBanThan ? hoSo.getHoTen() : hoTenBenhNhanDaNhap(lichHen), laBanThan, hoTenGiamHoDaNhap(lichHen),
                lichHen.getLyDoKham(), lichHen.getNgayTao(), nguoiDat,
                laBanThan && lichHen.isCanDoiChieu(), lichHen.getLyDoHuy(),
                conHuyDoiDuoc(lichHen) && (laBanThan || nguoiDat == NguoiDatLich.TOI), hanHuyDoi(lichHen),
                canDoiLich(lichHen));
    }

    /**
     * Chi tiết 1 lịch hẹn cho tài khoản được xem lịch đó. Ngày sinh, giới tính theo cùng quy ước với họ tên của
     * {@link #toLichHenCuaToi}. SĐT, email liên hệ là thứ người đặt nhập nên chỉ trả khi chính tài khoản này đặt.
     *
     * @param dong   dòng danh sách của lịch hẹn này (từ {@link #toLichHenCuaToi})
     * @param ketQua kết quả khám nếu tài khoản được xem, không thì null
     */
    public LichHenChiTietCuaToiResponse toChiTietCuaToi(LichHen lichHen, LichHenCuaToiResponse dong,
            KetQuaKhamResponse ketQua, boolean duocDanhGia, DanhGiaCuaToiResponse danhGia) {
        HoSoBenhNhan hoSo = lichHen.getHoSoBenhNhan();
        boolean dungHoSo = dong.laBanThan() || lichHen.getHoTenDaNhap() == null;
        boolean toiDat = dong.nguoiDat() == NguoiDatLich.TOI;
        return new LichHenChiTietCuaToiResponse(dong,
                dungHoSo ? hoSo.getNgaySinh() : lichHen.getNgaySinhDaNhap(),
                dungHoSo ? hoSo.getGioiTinh() : lichHen.getGioiTinhDaNhap(),
                toiDat ? lichHen.getSoDienThoaiLienHe() : null, toiDat ? lichHen.getEmailLienHe() : null, ketQua,
                duocDanhGia, danhGia);
    }

    private static NguoiDatLich nguoiDat(LichHen lichHen, Long idTaiKhoan) {
        TaiKhoan taiKhoanDat = lichHen.getTaiKhoanDat();
        if (taiKhoanDat == null) {
            return NguoiDatLich.KHACH;
        }
        return taiKhoanDat.getId().equals(idTaiKhoan) ? NguoiDatLich.TOI : NguoiDatLich.TAI_KHOAN_KHAC;
    }

    /**
     * Phiếu khám công khai: CCCD, SĐT đã che. Khung 1 giờ tính lại từ ca của lượt khám. SĐT của bệnh nhân là SĐT liên hệ
     * của lượt khám; khi có người giám hộ thì SĐT đó là của người giám hộ nên chỉ hiện ở phần người giám hộ. Họ tên, năm
     * sinh, giới tính và người giám hộ là những gì người đặt đã nhập, không phải dữ liệu của hồ sơ.
     */
    public PhieuKhamResponse toPhieuKhamResponse(LichHen lichHen) {
        LocalDateTime gioKhamDuKien = lichHen.getKhungGio().getGioBatDau();
        LocalDate ngay = gioKhamDuKien.toLocalDate();
        LichLamViec ca = lichHen.getKhungGio().getLichLamViec();
        LocalTime gioBatDauKhung = gioBatDauKhung(lichHen);
        LocalTime gioKetThucKhung = gioKetThucKhung(lichHen, gioBatDauKhung);

        HoSoBenhNhan hoSo = lichHen.getHoSoBenhNhan();
        NguoiGiamHo nguoiGiamHo = lichHen.getNguoiGiamHo();
        boolean coBanDaNhap = lichHen.getHoTenDaNhap() != null;
        LocalDate ngaySinh = coBanDaNhap ? lichHen.getNgaySinhDaNhap() : hoSo.getNgaySinh();
        BenhNhanPhieuKhamResponse benhNhan = new BenhNhanPhieuKhamResponse(hoTenBenhNhanDaNhap(lichHen),
                ngaySinh == null ? null : ngaySinh.getYear(),
                coBanDaNhap ? lichHen.getGioiTinhDaNhap() : hoSo.getGioiTinh(), CheThongTin.cccd(hoSo.getCccd()),
                nguoiGiamHo == null ? CheThongTin.soDienThoai(lichHen.getSoDienThoaiLienHe()) : null);

        return new PhieuKhamResponse(lichHen.getMaTokenPhieuKham(), lichHen.getMaTraCuu(),
                phieuKhamProperties.lienKet(lichHen.getMaTokenPhieuKham()), lichHen.getTrangThai(),
                lichHen.getSoThuTu(), ngay, gioKhamDuKien, ngay.atTime(gioBatDauKhung), ngay.atTime(gioKetThucKhung),
                caKhamMapper.toBacSiTomTat(lichHen.getBacSi()),
                lichHen.getBacSi().getChuyenKhoa().getTenChuyenKhoa(),
                caKhamMapper.toPhongKhamTomTat(lichHen.getPhongKham()), lichHen.getLyDoKham(), lichHen.getNgayTao(),
                benhNhan, nguoiGiamHo == null ? null : toNguoiGiamHoPhieuKham(lichHen, nguoiGiamHo),
                nguoiGiamHo != null, lichHen.getLyDoHuy(), conHuyDoiDuoc(lichHen), hanHuyDoi(lichHen),
                canDoiLich(lichHen));
    }

    /**
     * Giờ bắt đầu khung 1 giờ chứa lượt khám của lịch hẹn. Lượt nằm ngoài giờ của ca (lịch hẹn đã hủy / bị từ chối /
     * đã khám, sau đó quản trị viên sửa giờ ca) thì lấy chính giờ của lượt, không tính theo ca.
     */
    private static LocalTime gioBatDauKhung(LichHen lichHen) {
        LichLamViec ca = lichHen.getKhungGio().getLichLamViec();
        LocalTime gioLuot = lichHen.getKhungGio().getGioBatDau().toLocalTime();
        if (gioLuot.isBefore(ca.getGioBatDau()) || !gioLuot.isBefore(ca.getGioKetThuc())) {
            return gioLuot;
        }
        return ChiaCaLamViec.gioBatDauKhung(ca.getGioBatDau(), gioLuot);
    }

    /** Giờ kết thúc khung ứng với {@link #gioBatDauKhung(LichHen)}; lượt nằm ngoài giờ của ca thì là giờ kết thúc lượt. */
    private static LocalTime gioKetThucKhung(LichHen lichHen, LocalTime gioBatDauKhung) {
        LichLamViec ca = lichHen.getKhungGio().getLichLamViec();
        LocalTime gioLuot = lichHen.getKhungGio().getGioBatDau().toLocalTime();
        if (gioLuot.isBefore(ca.getGioBatDau()) || !gioLuot.isBefore(ca.getGioKetThuc())) {
            return lichHen.getKhungGio().getGioKetThuc().toLocalTime();
        }
        return ChiaCaLamViec.gioKetThucKhung(gioBatDauKhung, ca.getGioKetThuc());
    }

    /**
     * 1 dòng lịch hẹn cho bác sĩ / quản trị viên: SĐT không che, không có mã phiếu khám và CCCD. {@code benhNhan},
     * {@code nguoiGiamHo} là dữ liệu đang lưu; {@code doiChieu} là những gì người đặt nhập cho lượt khám này.
     *
     * @param kemBacSi false ở danh sách của chính bác sĩ (trường {@code bacSi} để null)
     */
    public LichHenTrongCaResponse toTrongCaResponse(LichHen lichHen, boolean kemBacSi) {
        LocalDateTime gioKhamDuKien = lichHen.getKhungGio().getGioBatDau();
        LocalDate ngay = gioKhamDuKien.toLocalDate();
        LichLamViec ca = lichHen.getKhungGio().getLichLamViec();
        LocalTime gioBatDauKhung = gioBatDauKhung(lichHen);
        LocalTime gioKetThucKhung = gioKetThucKhung(lichHen, gioBatDauKhung);

        HoSoBenhNhan hoSo = lichHen.getHoSoBenhNhan();
        NguoiGiamHo nguoiGiamHo = lichHen.getNguoiGiamHo();
        Integer tuoi = hoSo.getNgaySinh() == null ? null : Period.between(hoSo.getNgaySinh(), ngay).getYears();
        return new LichHenTrongCaResponse(lichHen.getId(), lichHen.getMaTraCuu(), ca.getId(), lichHen.getTrangThai(),
                lichHen.getSoThuTu(),
                gioKhamDuKien, ngay.atTime(gioBatDauKhung), ngay.atTime(gioKetThucKhung),
                kemBacSi ? caKhamMapper.toBacSiTomTat(lichHen.getBacSi()) : null,
                caKhamMapper.toPhongKhamTomTat(lichHen.getPhongKham()), lichHen.getLyDoKham(), lichHen.getNgayTao(),
                hoSo.getId(),
                new LichHenTrongCaResponse.BenhNhan(hoSo.getHoTen(), hoSo.getNgaySinh(), tuoi, hoSo.getGioiTinh(),
                        nguoiGiamHo == null ? lichHen.getSoDienThoaiLienHe() : null),
                nguoiGiamHo == null ? null
                        : new LichHenTrongCaResponse.NguoiGiamHo(nguoiGiamHo.getHoTen(), nguoiGiamHo.getQuanHe(),
                                nguoiGiamHo.getSoDienThoai()),
                new LichHenTrongCaResponse.DoiChieu(lichHen.isCanDoiChieu(), lichHen.getHoTenDaNhap(),
                        lichHen.getNgaySinhDaNhap(), lichHen.getGioiTinhDaNhap(), lichHen.getHoTenGiamHoDaNhap()),
                lichHen.getLyDoHuy(), canDoiLich(lichHen));
    }

    /** Ca khám đã bị hủy và lịch hẹn còn hiệu lực: bệnh nhân phải đổi sang khung giờ khác (hoặc hủy). */
    private static boolean canDoiLich(LichHen lichHen) {
        return lichHen.isCanDoiLich() && lichHen.getTrangThai().chiemKhungGio();
    }

    /** SĐT liên hệ của lượt khám có người giám hộ là SĐT người giám hộ vừa nhập; lịch hẹn cũ chưa có thì lấy dòng đã lưu. */
    private static NguoiGiamHoPhieuKhamResponse toNguoiGiamHoPhieuKham(LichHen lichHen, NguoiGiamHo nguoiGiamHo) {
        String soDienThoai = lichHen.getSoDienThoaiLienHe() != null ? lichHen.getSoDienThoaiLienHe()
                : nguoiGiamHo.getSoDienThoai();
        return new NguoiGiamHoPhieuKhamResponse(hoTenGiamHoDaNhap(lichHen), nguoiGiamHo.getQuanHe(),
                CheThongTin.soDienThoai(soDienThoai));
    }

    /** Họ tên người khám như người đặt đã nhập; lịch hẹn tạo trước khi có bản sao này thì lấy của hồ sơ. */
    private static String hoTenBenhNhanDaNhap(LichHen lichHen) {
        return lichHen.getHoTenDaNhap() != null ? lichHen.getHoTenDaNhap() : lichHen.getHoSoBenhNhan().getHoTen();
    }

    /** Họ tên người giám hộ như người đặt đã nhập (null nếu lượt khám không có người giám hộ), cùng quy ước như trên. */
    private static String hoTenGiamHoDaNhap(LichHen lichHen) {
        NguoiGiamHo nguoiGiamHo = lichHen.getNguoiGiamHo();
        if (nguoiGiamHo == null) {
            return null;
        }
        return lichHen.getHoTenGiamHoDaNhap() != null ? lichHen.getHoTenGiamHoDaNhap() : nguoiGiamHo.getHoTen();
    }

}
