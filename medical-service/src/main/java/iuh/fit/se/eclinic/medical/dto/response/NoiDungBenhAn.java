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

/**
 * Nội dung hồ sơ bệnh án tại 1 thời điểm: lưu thành JSON trong lịch sử sửa (trước / sau khi sửa) và trả nguyên dạng
 * này khi xem lịch sử. Thuốc giữ cả tên, hàm lượng: 2 trường này không đổi được khi thuốc đã được kê.
 *
 * @param donThuoc theo thứ tự bác sĩ đã gửi
 */
public record NoiDungBenhAn(String chanDoan, String ghiChu, LocalDate ngayTaiKhamDeXuat, List<DongThuoc> donThuoc) {

    public record DongThuoc(Long idThuoc, String tenThuoc, String hamLuong, String lieuDung, Integer soLanMoiNgay,
            Integer soNgayDung, String ghiChuSuDung) {
    }

    /** Gọi trong transaction: đơn thuốc và thuốc là quan hệ lazy. */
    public static NoiDungBenhAn tu(HoSoBenhAn benhAn) {
        return new NoiDungBenhAn(benhAn.getChanDoan(), benhAn.getGhiChu(), benhAn.getNgayTaiKhamDeXuat(),
                new ArrayList<>(benhAn.getChiTietDonThuoc().stream()
                        .map(chiTiet -> {
                            Thuoc thuoc = chiTiet.getThuoc();
                            return new DongThuoc(thuoc.getId(), thuoc.getTenThuoc(), thuoc.getHamLuong(),
                                    chiTiet.getLieuDung(), chiTiet.getSoLanMoiNgay(), chiTiet.getSoNgayDung(),
                                    chiTiet.getGhiChuSuDung());
                        })
                        .toList()));
    }

    /**
     * Các phần khác nhau giữa 2 nội dung. Đơn thuốc so theo từng thuốc (mỗi thuốc 1 dòng): chỉ đổi thứ tự các dòng thì
     * không tính là đổi.
     */
    public static List<TruongBenhAn> cacTruongDoi(NoiDungBenhAn truoc, NoiDungBenhAn sau) {
        List<TruongBenhAn> doi = new ArrayList<>();
        if (!Objects.equals(truoc.chanDoan(), sau.chanDoan())) {
            doi.add(TruongBenhAn.CHAN_DOAN);
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
