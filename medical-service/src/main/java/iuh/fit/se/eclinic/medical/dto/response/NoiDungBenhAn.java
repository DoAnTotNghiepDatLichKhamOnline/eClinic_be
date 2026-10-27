package iuh.fit.se.eclinic.medical.dto.response;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import iuh.fit.se.eclinic.common.entity.medical.HoSoBenhAn;
import iuh.fit.se.eclinic.common.entity.medical.Thuoc;
import iuh.fit.se.eclinic.common.enums.TruongBenhAn;

/** Nội dung hồ sơ bệnh án tại 1 thời điểm, lưu thành JSON trong lịch sử sửa. */
public record NoiDungBenhAn(String lyDoKham, BenhAnResponse.SinhHieu sinhHieu, String khamLamSang, String chanDoan,
        String maIcdChinh, String chanDoanKemTheo, String ghiChu, LocalDate ngayTaiKhamDeXuat,
        List<DongThuoc> donThuoc) {

    public record DongThuoc(Long idThuoc, String tenThuoc, String hamLuong, String lieuDung, Integer soLanMoiNgay,
            Integer soNgayDung, Integer soLuong, String ghiChuSuDung) {
    }

    /** Gọi trong transaction: đơn thuốc và thuốc là quan hệ lazy. */
    public static NoiDungBenhAn tu(HoSoBenhAn benhAn) {
        return new NoiDungBenhAn(benhAn.getLyDoKham(), BenhAnResponse.SinhHieu.tu(benhAn), benhAn.getKhamLamSang(),
                benhAn.getChanDoan(), benhAn.getIcdChinh() == null ? null : benhAn.getIcdChinh().getMa(),
                benhAn.getChanDoanKemTheo(), benhAn.getGhiChu(), benhAn.getNgayTaiKhamDeXuat(),
                new ArrayList<>(benhAn.getChiTietDonThuoc().stream()
                        .map(chiTiet -> {
                            Thuoc thuoc = chiTiet.getThuoc();
                            return new DongThuoc(thuoc.getId(), thuoc.getTenThuoc(), thuoc.getHamLuong(),
                                    chiTiet.getLieuDung(), chiTiet.getSoLanMoiNgay(), chiTiet.getSoNgayDung(),
                                    chiTiet.getSoLuong(), chiTiet.getGhiChuSuDung());
                        })
                        .toList()));
    }

    /** Các phần khác nhau giữa 2 nội dung, theo thứ tự trên hồ sơ. */
    public static List<TruongBenhAn> cacTruongDoi(NoiDungBenhAn truoc, NoiDungBenhAn sau) {
        List<TruongBenhAn> doi = new ArrayList<>();
        if (!Objects.equals(truoc.lyDoKham(), sau.lyDoKham())) {
            doi.add(TruongBenhAn.LY_DO_KHAM);
        }
        if (!Objects.equals(truoc.sinhHieu(), sau.sinhHieu())) {
            doi.add(TruongBenhAn.SINH_HIEU);
        }
        if (!Objects.equals(truoc.khamLamSang(), sau.khamLamSang())) {
            doi.add(TruongBenhAn.KHAM_LAM_SANG);
        }
        if (!Objects.equals(truoc.chanDoan(), sau.chanDoan()) || !Objects.equals(truoc.maIcdChinh(), sau.maIcdChinh())) {
            doi.add(TruongBenhAn.CHAN_DOAN);
        }
        if (!Objects.equals(truoc.chanDoanKemTheo(), sau.chanDoanKemTheo())) {
            doi.add(TruongBenhAn.CHAN_DOAN_KEM_THEO);
        }
        if (!Objects.equals(truoc.ghiChu(), sau.ghiChu())) {
            doi.add(TruongBenhAn.GHI_CHU);
        }
        if (!Objects.equals(truoc.ngayTaiKhamDeXuat(), sau.ngayTaiKhamDeXuat())) {
            doi.add(TruongBenhAn.NGAY_TAI_KHAM);
        }
        if (!theoThuoc(truoc).equals(theoThuoc(sau))) {
            doi.add(TruongBenhAn.DON_THUOC);
        }
        return doi;
    }

    private static Map<Long, DongThuoc> theoThuoc(NoiDungBenhAn noiDung) {
        return noiDung.donThuoc() == null ? Map.of()
                : noiDung.donThuoc().stream().collect(Collectors.toMap(DongThuoc::idThuoc, Function.identity()));
    }

}
