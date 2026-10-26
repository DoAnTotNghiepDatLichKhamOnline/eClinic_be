package iuh.fit.se.eclinic.report.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.report.dto.response.BaoCaoThongKeResponse;
import iuh.fit.se.eclinic.report.dto.response.BaoCaoThongKeResponse.ChiSo;
import iuh.fit.se.eclinic.report.dto.response.BaoCaoThongKeResponse.DiemThoiGian;
import iuh.fit.se.eclinic.report.dto.response.BaoCaoThongKeResponse.DongBacSi;
import iuh.fit.se.eclinic.report.dto.response.BaoCaoThongKeResponse.DongChuyenKhoa;
import iuh.fit.se.eclinic.report.dto.response.BaoCaoThongKeResponse.KenhDat;
import iuh.fit.se.eclinic.report.dto.response.BaoCaoThongKeResponse.ThoiGianDatTruoc;
import iuh.fit.se.eclinic.report.dto.response.BaoCaoThongKeResponse.TongQuan;
import iuh.fit.se.eclinic.report.dto.response.BaoCaoThongKeResponse.TrangThai;
import iuh.fit.se.eclinic.report.service.ExcelBaoCaoService;
import lombok.RequiredArgsConstructor;

/**
 * Dựng tệp từ báo cáo đã tính (không truy vấn lại). Độ rộng cột đặt cố định, không dùng autoSizeColumn: hàm đó cần font
 * của AWT, có thể không có trong image JRE không giao diện. Tỷ lệ là ô số với định dạng 0.0"%", để trống khi null.
 */
@Service
@RequiredArgsConstructor
public class ExcelBaoCaoServiceImpl implements ExcelBaoCaoService {

    private static final String[] KHOA_COT_CHI_SO = {"lichHen", "luotKham", "benhNhanDaKham", "daHuy", "khongDen",
            "tyLeHuy", "tyLeKhongDen", "luotDaDat", "tongLuot", "tyLeLapDay"};
    private static final int RONG_CHI_SO = 14;
    private static final int SO_DONG_GHI_CHU = 9;

    private final MessageSource messageSource;

    private record KieuO(CellStyle tieuDe, CellStyle muc, CellStyle ngay, CellStyle ngayGio, CellStyle tyLe,
            CellStyle chuDai) {
    }

    @Override
    public byte[] xuat(BaoCaoThongKeResponse baoCao) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            KieuO kieu = kieuO(workbook);
            trangTongQuan(workbook.createSheet(cau("baoCao.trang.tongQuan")), kieu, baoCao);
            trangChuyenKhoa(workbook.createSheet(cau("baoCao.trang.theoChuyenKhoa")), kieu, baoCao.theoChuyenKhoa());
            trangBacSi(workbook.createSheet(cau("baoCao.trang.theoBacSi")), kieu, baoCao.theoBacSi());
            trangThoiGian(workbook.createSheet(cau("baoCao.trang.theoThoiGian")), kieu, baoCao.theoThoiGian());
            trangGhiChu(workbook.createSheet(cau("baoCao.trang.ghiChu")), kieu);
            ByteArrayOutputStream ra = new ByteArrayOutputStream();
            workbook.write(ra);
            return ra.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Cột A nhãn, cột B giá trị; mỗi mục bắt đầu bằng 1 dòng tiêu đề. */
    private void trangTongQuan(Sheet trang, KieuO kieu, BaoCaoThongKeResponse baoCao) {
        trang.setColumnWidth(0, 48 * 256);
        trang.setColumnWidth(1, 24 * 256);
        TongQuan tq = baoCao.tongQuan();
        TrangThai tt = baoCao.trangThai();
        KenhDat kenh = baoCao.kenhDat();
        ThoiGianDatTruoc dt = baoCao.thoiGianDatTruoc();
        int[] dong = {0};
        muc(trang, dong, kieu, "boLoc");
        chu(trang, dong, "ky", cau("baoCao.ky." + baoCao.ky()));
        ngay(trang, dong, kieu, "tuNgay", baoCao.tuNgay());
        ngay(trang, dong, kieu, "denNgay", baoCao.denNgay());
        chu(trang, dong, "chuyenKhoa", baoCao.idChuyenKhoa() == null || baoCao.theoChuyenKhoa().isEmpty()
                ? cau("baoCao.toanVien") : baoCao.theoChuyenKhoa().get(0).tenChuyenKhoa());
        Cell tinhLuc = nhan(trang, dong, "tinhLuc").createCell(1);
        tinhLuc.setCellValue(baoCao.tinhLuc().withNano(0));
        tinhLuc.setCellStyle(kieu.ngayGio());
        dong[0]++;
        muc(trang, dong, kieu, "tongQuan");
        so(trang, dong, "bacSiDangCongTac", tq.bacSiDangCongTac());
        so(trang, dong, "bacSiCoCa", tq.bacSiCoCa());
        so(trang, dong, "taiKhoanBenhNhanMoi", tq.taiKhoanBenhNhanMoi());
        so(trang, dong, "benhNhanDaKham", tq.benhNhanDaKham());
        so(trang, dong, "luotKham", tq.luotKham());
        so(trang, dong, "lichHen", tq.lichHen());
        so(trang, dong, "luotDatMoi", tq.luotDatMoi());
        tyLe(trang, dong, kieu, "tyLeHuy", tq.tyLeHuy());
        tyLe(trang, dong, kieu, "tyLeKhongDen", tq.tyLeKhongDen());
        so(trang, dong, "luotDaDat", tq.luotDaDat());
        so(trang, dong, "tongLuot", tq.tongLuot());
        tyLe(trang, dong, kieu, "tyLeLapDay", tq.tyLeLapDay());
        dong[0]++;
        muc(trang, dong, kieu, "trangThai");
        so(trang, dong, "chuaKham", tt.chuaKham());
        so(trang, dong, "daKham", tt.daKham());
        so(trang, dong, "khongDen", tt.khongDen());
        so(trang, dong, "benhNhanHuy", tt.benhNhanHuy());
        so(trang, dong, "bacSiHuy", tt.bacSiHuy());
        so(trang, dong, "quanTriHuy", tt.quanTriHuy());
        so(trang, dong, "canDoiLich", tt.canDoiLich());
        dong[0]++;
        muc(trang, dong, kieu, "kenhDat");
        so(trang, dong, "khach", kenh.khach());
        so(trang, dong, "taiKhoan", kenh.taiKhoan());
        so(trang, dong, "hotline", kenh.hotline());
        dong[0]++;
        muc(trang, dong, kieu, "thoiGianDatTruoc");
        Row trungBinh = nhan(trang, dong, "trungBinhGio");
        if (dt.trungBinhGio() != null) {
            trungBinh.createCell(1).setCellValue(dt.trungBinhGio().doubleValue());
        }
        so(trang, dong, "duoi1Ngay", dt.duoi1Ngay());
        so(trang, dong, "tu1Den3Ngay", dt.tu1Den3Ngay());
        so(trang, dong, "tu4Den7Ngay", dt.tu4Den7Ngay());
        so(trang, dong, "tu8Den14Ngay", dt.tu8Den14Ngay());
        so(trang, dong, "tren14Ngay", dt.tren14Ngay());
    }

    private void trangChuyenKhoa(Sheet trang, KieuO kieu, List<DongChuyenKhoa> cacDong) {
        tieuDe(trang, kieu, List.of("chuyenKhoa"), new int[] {30});
        int soDong = 1;
        for (DongChuyenKhoa dong : cacDong) {
            Row r = trang.createRow(soDong++);
            r.createCell(0).setCellValue(dong.tenChuyenKhoa());
            chiSo(r, 1, dong.chiSo(), kieu);
        }
    }

    private void trangBacSi(Sheet trang, KieuO kieu, List<DongBacSi> cacDong) {
        tieuDe(trang, kieu, List.of("maBacSi", "hoTen", "chuyenKhoa"), new int[] {12, 28, 26});
        int soDong = 1;
        for (DongBacSi dong : cacDong) {
            Row r = trang.createRow(soDong++);
            r.createCell(0).setCellValue(dong.maBacSi());
            r.createCell(1).setCellValue(dong.hoTen());
            r.createCell(2).setCellValue(dong.tenChuyenKhoa());
            chiSo(r, 3, dong.chiSo(), kieu);
        }
    }

    private void trangThoiGian(Sheet trang, KieuO kieu, List<DiemThoiGian> chuoi) {
        String[] khoa = {"tuNgay", "denNgay", "lichHen", "luotKham", "daHuy", "khongDen"};
        for (int cot = 0; cot < khoa.length; cot++) {
            trang.setColumnWidth(cot, RONG_CHI_SO * 256);
        }
        Row tieuDe = trang.createRow(0);
        for (int cot = 0; cot < khoa.length; cot++) {
            Cell o = tieuDe.createCell(cot);
            o.setCellValue(cau("baoCao.cot." + khoa[cot]));
            o.setCellStyle(kieu.tieuDe());
        }
        trang.createFreezePane(0, 1);
        int soDong = 1;
        for (DiemThoiGian diem : chuoi) {
            Row r = trang.createRow(soDong++);
            oNgay(r, 0, diem.tuNgay(), kieu);
            oNgay(r, 1, diem.denNgay(), kieu);
            r.createCell(2).setCellValue(diem.lichHen());
            r.createCell(3).setCellValue(diem.luotKham());
            r.createCell(4).setCellValue(diem.daHuy());
            r.createCell(5).setCellValue(diem.khongDen());
        }
    }

    private void trangGhiChu(Sheet trang, KieuO kieu) {
        trang.setColumnWidth(0, 120 * 256);
        Cell tieuDe = trang.createRow(0).createCell(0);
        tieuDe.setCellValue(cau("baoCao.trang.ghiChu"));
        tieuDe.setCellStyle(kieu.tieuDe());
        for (int i = 1; i <= SO_DONG_GHI_CHU; i++) {
            Cell o = trang.createRow(i).createCell(0);
            o.setCellValue(cau("baoCao.ghiChu." + i));
            o.setCellStyle(kieu.chuDai());
        }
    }

    /** Dòng tiêu đề: các cột riêng của trang rồi tới các cột chỉ số, cố định khi cuộn. */
    private void tieuDe(Sheet trang, KieuO kieu, List<String> cotRieng, int[] doRong) {
        Row dong = trang.createRow(0);
        int cot = 0;
        for (int i = 0; i < cotRieng.size(); i++, cot++) {
            Cell o = dong.createCell(cot);
            o.setCellValue(cau("baoCao.cot." + cotRieng.get(i)));
            o.setCellStyle(kieu.tieuDe());
            trang.setColumnWidth(cot, doRong[i] * 256);
        }
        for (String khoa : KHOA_COT_CHI_SO) {
            Cell o = dong.createCell(cot);
            o.setCellValue(cau("baoCao.cot." + khoa));
            o.setCellStyle(kieu.tieuDe());
            trang.setColumnWidth(cot++, RONG_CHI_SO * 256);
        }
        trang.createFreezePane(0, 1);
    }

    private static void chiSo(Row r, int cot, ChiSo cs, KieuO kieu) {
        r.createCell(cot).setCellValue(cs.lichHen());
        r.createCell(cot + 1).setCellValue(cs.luotKham());
        r.createCell(cot + 2).setCellValue(cs.benhNhanDaKham());
        r.createCell(cot + 3).setCellValue(cs.daHuy());
        r.createCell(cot + 4).setCellValue(cs.khongDen());
        oTyLe(r, cot + 5, cs.tyLeHuy(), kieu);
        oTyLe(r, cot + 6, cs.tyLeKhongDen(), kieu);
        r.createCell(cot + 7).setCellValue(cs.luotDaDat());
        r.createCell(cot + 8).setCellValue(cs.tongLuot());
        oTyLe(r, cot + 9, cs.tyLeLapDay(), kieu);
    }

    private void muc(Sheet trang, int[] dong, KieuO kieu, String khoa) {
        Row r = trang.createRow(dong[0]++);
        for (int cot = 0; cot < 2; cot++) {
            r.createCell(cot).setCellStyle(kieu.muc());
        }
        r.getCell(0).setCellValue(cau("baoCao.muc." + khoa));
    }

    private Row nhan(Sheet trang, int[] dong, String khoa) {
        Row r = trang.createRow(dong[0]++);
        r.createCell(0).setCellValue(cau("baoCao.nhan." + khoa));
        return r;
    }

    private void chu(Sheet trang, int[] dong, String khoa, String giaTri) {
        nhan(trang, dong, khoa).createCell(1).setCellValue(giaTri);
    }

    private void so(Sheet trang, int[] dong, String khoa, long giaTri) {
        nhan(trang, dong, khoa).createCell(1).setCellValue(giaTri);
    }

    private void ngay(Sheet trang, int[] dong, KieuO kieu, String khoa, LocalDate giaTri) {
        oNgay(nhan(trang, dong, khoa), 1, giaTri, kieu);
    }

    private void tyLe(Sheet trang, int[] dong, KieuO kieu, String khoa, BigDecimal giaTri) {
        oTyLe(nhan(trang, dong, khoa), 1, giaTri, kieu);
    }

    private static void oNgay(Row r, int cot, LocalDate giaTri, KieuO kieu) {
        Cell o = r.createCell(cot);
        o.setCellValue(giaTri);
        o.setCellStyle(kieu.ngay());
    }

    private static void oTyLe(Row r, int cot, BigDecimal giaTri, KieuO kieu) {
        if (giaTri != null) {
            Cell o = r.createCell(cot);
            o.setCellValue(giaTri.doubleValue());
            o.setCellStyle(kieu.tyLe());
        }
    }

    private String cau(String khoa) {
        return messageSource.getMessage(khoa, null, khoa, NgonNgu.hienTai().locale());
    }

    private static KieuO kieuO(XSSFWorkbook workbook) {
        Font dam = workbook.createFont();
        dam.setBold(true);
        CellStyle tieuDe = workbook.createCellStyle();
        tieuDe.setFont(dam);
        tieuDe.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        tieuDe.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        tieuDe.setBorderBottom(BorderStyle.THIN);
        tieuDe.setWrapText(true);
        CellStyle muc = workbook.createCellStyle();
        muc.setFont(dam);
        muc.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
        muc.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        muc.setBorderBottom(BorderStyle.THIN);
        CellStyle ngay = workbook.createCellStyle();
        ngay.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd"));
        CellStyle ngayGio = workbook.createCellStyle();
        ngayGio.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
        CellStyle tyLe = workbook.createCellStyle();
        tyLe.setDataFormat(workbook.createDataFormat().getFormat("0.0\"%\""));
        CellStyle chuDai = workbook.createCellStyle();
        chuDai.setWrapText(true);
        return new KieuO(tieuDe, muc, ngay, ngayGio, tyLe, chuDai);
    }

}
