package iuh.fit.se.eclinic.booking.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFName;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import iuh.fit.se.eclinic.booking.config.LichLamViecProperties;
import iuh.fit.se.eclinic.booking.dto.request.DongCaNhap;
import iuh.fit.se.eclinic.booking.dto.response.CaLamViecResponse;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaNhapExcelResponse;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaNhapExcelResponse.LoiDong;
import iuh.fit.se.eclinic.booking.repository.BacSiChiDocRepository;
import iuh.fit.se.eclinic.booking.repository.PhongKhamChiDocRepository;
import iuh.fit.se.eclinic.booking.service.ExcelLichLamViecService;
import iuh.fit.se.eclinic.booking.service.LichLamViecService;
import iuh.fit.se.eclinic.booking.service.QuanLyCaService;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.catalog.PhongKham;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichLamViec;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.util.MaBacSi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Đọc tệp nhập KHÔNG trong transaction: lỗi của POI khi đọc tệp hỏng không thể đánh dấu transaction là rollback-only.
 * Kiểm tra với DB và lưu nằm ở {@link QuanLyCaService#nhapCa} (1 transaction). Độ rộng cột đặt cố định, không dùng
 * autoSizeColumn: hàm đó cần font của AWT, có thể không có trong image JRE không giao diện.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelLichLamViecServiceImpl implements ExcelLichLamViecService {

    /** Số dòng ca tối đa của 1 tệp nhập: giữ 1 transaction ở cỡ vừa phải, như số bản sao tối đa của 1 lần sao chép. */
    private static final int SO_DONG_TOI_DA = 1000;
    private static final String[] COT = {"A", "B", "C", "D", "E", "F", "G"};
    private static final String[] KHOA_COT_CA = {"maBacSi", "ngay", "gioBatDau", "gioKetThuc", "maPhong", "soLuot",
            "thoiLuong"};
    private static final String[] KHOA_COT_THONG_TIN = {"hoTen", "chuyenKhoa", "tenPhong", "tang", "daDat", "tongLuot"};
    private static final int[] RONG_COT = {12, 13, 12, 12, 10, 16, 16, 28, 24, 24, 10, 12, 12};
    private static final String[] VI_DU = {"BS0001", "2026-11-30 | 30/11/2026", "08:00", "11:30", "1", "4", "15"};
    private static final DateTimeFormatter NGAY_CHU = DateTimeFormatter.ofPattern("d/M/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter GIO_CHU = DateTimeFormatter.ofPattern("H:mm[:ss]")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final Pattern SO_NGUYEN = Pattern.compile("\\d{1,9}");
    private static final String TEN_DS_BAC_SI = "DanhSachBacSi";
    private static final String TEN_DS_PHONG = "DanhSachPhong";

    private final LichLamViecService lichLamViecService;
    private final QuanLyCaService quanLyCaService;
    private final BacSiChiDocRepository bacSiChiDocRepository;
    private final PhongKhamChiDocRepository phongKhamChiDocRepository;
    private final LichLamViecProperties lichLamViecProperties;
    private final MessageSource messageSource;

    /** Các kiểu ô dùng chung của 1 workbook. */
    private record KieuO(CellStyle tieuDe, CellStyle tieuDeThongTin, CellStyle ngay, CellStyle gio, CellStyle chuDai) {
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] xuat(LocalDate tuNgay, LocalDate denNgay, Long idChuyenKhoa, Long idBacSi, Long idPhongKham) {
        List<CaLamViecResponse> cacCa = lichLamViecService
                .lichToanVien(tuNgay, denNgay, idChuyenKhoa, idBacSi, idPhongKham).stream()
                .filter(ca -> ca.trangThai() == TrangThaiLichLamViec.HOAT_DONG)
                .toList();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            KieuO kieu = kieuO(workbook);
            Sheet trang = workbook.createSheet(cau("excel.trang.ca"));
            tieuDeTrangCa(trang, kieu, true);
            int soDong = 1;
            for (CaLamViecResponse ca : cacCa) {
                Row dong = trang.createRow(soDong++);
                dong.createCell(0).setCellValue(MaBacSi.tu(ca.bacSi().id()));
                Cell ngay = dong.createCell(1);
                ngay.setCellValue(ca.ngay());
                ngay.setCellStyle(kieu.ngay());
                oGio(dong, 2, ca.gioBatDau(), kieu);
                oGio(dong, 3, ca.gioKetThuc(), kieu);
                dong.createCell(4).setCellValue(ca.phongKham().id());
                dong.createCell(5).setCellValue(ca.soLuotToiDaMoiGio());
                dong.createCell(6).setCellValue(ca.thoiLuongLuotPhut());
                dong.createCell(7).setCellValue(ca.bacSi().hoTen());
                dong.createCell(8).setCellValue(ca.tenChuyenKhoa());
                dong.createCell(9).setCellValue(ca.phongKham().tenPhong());
                dong.createCell(10).setCellValue(ca.phongKham().tang());
                dong.createCell(11).setCellValue(ca.soLuotDaDat());
                dong.createCell(12).setCellValue(ca.tongSoLuot());
            }
            return ghi(workbook);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] mau() {
        NgonNgu ngonNgu = NgonNgu.hienTai();
        List<BacSi> cacBacSi = bacSiChiDocRepository.timDangCongTac(null, null).stream()
                .filter(bacSi -> bacSi.getTaiKhoan().getTrangThai() == TrangThaiTaiKhoan.DA_KICH_HOAT)
                .toList();
        List<PhongKham> cacPhong = phongKhamChiDocRepository.timDangHoatDong();
        String tenTrangCa = cau("excel.trang.ca");
        String tenTrangBacSi = cau("excel.trang.bacSi");
        String tenTrangPhong = cau("excel.trang.phong");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            KieuO kieu = kieuO(workbook);
            XSSFSheet trangCa = workbook.createSheet(tenTrangCa);
            tieuDeTrangCa(trangCa, kieu, false);
            // Ô gõ mới trong cột ngày / giờ nhận sẵn định dạng ngày / giờ
            trangCa.setDefaultColumnStyle(1, kieu.ngay());
            trangCa.setDefaultColumnStyle(2, kieu.gio());
            trangCa.setDefaultColumnStyle(3, kieu.gio());

            huongDan(workbook.createSheet(cau("excel.trang.huongDan")), kieu, tenTrangCa, tenTrangBacSi, tenTrangPhong);

            Sheet trangBacSi = workbook.createSheet(tenTrangBacSi);
            tieuDe(trangBacSi, kieu, new String[] {"maBacSi", "hoTen", "chuyenKhoa"}, new int[] {12, 32, 28});
            for (int i = 0; i < cacBacSi.size(); i++) {
                BacSi bacSi = cacBacSi.get(i);
                Row dong = trangBacSi.createRow(i + 1);
                dong.createCell(0).setCellValue(MaBacSi.tu(bacSi.getId()));
                dong.createCell(1).setCellValue(bacSi.getTaiKhoan().getHoTen());
                dong.createCell(2).setCellValue(bacSi.getChuyenKhoa().ten(ngonNgu));
            }

            Sheet trangPhong = workbook.createSheet(tenTrangPhong);
            tieuDe(trangPhong, kieu, new String[] {"maPhong", "tenPhong", "tang", "chuyenKhoa"},
                    new int[] {10, 28, 10, 28});
            for (int i = 0; i < cacPhong.size(); i++) {
                PhongKham phong = cacPhong.get(i);
                Row dong = trangPhong.createRow(i + 1);
                dong.createCell(0).setCellValue(phong.getId());
                dong.createCell(1).setCellValue(phong.ten(ngonNgu));
                dong.createCell(2).setCellValue(phong.tang(ngonNgu));
                dong.createCell(3).setCellValue(phong.getChuyenKhoa().ten(ngonNgu));
            }

            danhSachChon(workbook, trangCa, TEN_DS_BAC_SI, tenTrangBacSi, cacBacSi.size(), 0);
            danhSachChon(workbook, trangCa, TEN_DS_PHONG, tenTrangPhong, cacPhong.size(), 4);
            return ghi(workbook);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public KetQuaNhapExcelResponse nhap(Long idTaiKhoanQuanTri, MultipartFile tep, boolean luu) {
        List<DongCaNhap> cacDong = new ArrayList<>();
        List<LoiDong> loi = new ArrayList<>();
        int soDong;
        try (XSSFWorkbook workbook = moTep(tep)) {
            soDong = docTrangCa(workbook.getSheetAt(0), cacDong, loi);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        List<DongCaNhap> conLai = boDongTrungTrongTep(cacDong, loi);
        if (luu && !loi.isEmpty()) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.nhapExcelCoLoi",
                    String.valueOf(loi.size()));
        }
        // Còn lỗi ô vẫn xét các dòng đúng dạng với DB (rồi rollback), để 1 lần xem trước thấy được mọi lỗi
        KetQuaNhapExcelResponse ketQua = conLai.isEmpty()
                ? new KetQuaNhapExcelResponse(false, 0, List.of(), List.of())
                : quanLyCaService.nhapCa(idTaiKhoanQuanTri, conLai, luu);
        loi.addAll(ketQua.loi());
        loi.sort(Comparator.comparingInt(LoiDong::dong)
                .thenComparing(LoiDong::cot, Comparator.nullsLast(Comparator.naturalOrder())));
        return new KetQuaNhapExcelResponse(ketQua.daLuu(), soDong, ketQua.hopLe(), loi);
    }

    /** Mở tệp dạng .xlsx; tệp rỗng, hỏng hoặc không phải .xlsx (kể cả .xls) -> 400 nhapExcelTep. */
    private static XSSFWorkbook moTep(MultipartFile tep) {
        if (tep == null || tep.isEmpty()) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.nhapExcelTep");
        }
        try (InputStream vao = tep.getInputStream()) {
            return new XSSFWorkbook(vao);
        } catch (IOException | RuntimeException e) {
            log.debug("Không mở được tệp nhập lịch {}: {}", tep.getOriginalFilename(), e.toString());
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.nhapExcelTep");
        }
    }

    /**
     * Đọc các dòng ca (bỏ dòng 1 và dòng trống ở cả 7 cột A-G): dòng đúng dạng vào {@code cacDong}, ô sai vào
     * {@code loi}.
     *
     * @return số dòng ca
     */
    private int docTrangCa(Sheet trang, List<DongCaNhap> cacDong, List<LoiDong> loi) {
        int soDong = 0;
        for (Row dong : trang) {
            if (dong.getRowNum() == 0 || dongTrong(dong)) {
                continue;
            }
            if (++soDong > SO_DONG_TOI_DA) {
                throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.nhapExcelQuaNhieu",
                        String.valueOf(SO_DONG_TOI_DA));
            }
            docDong(dong, cacDong, loi);
        }
        if (soDong == 0) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.nhapExcelRong");
        }
        return soDong;
    }

    private void docDong(Row dong, List<DongCaNhap> cacDong, List<LoiDong> loi) {
        int soDong = dong.getRowNum() + 1;
        int soLoiTruoc = loi.size();
        String maBacSi = null;
        Long idBacSi = null;
        Cell oMa = dong.getCell(0);
        if (oTrong(oMa)) {
            them(loi, soDong, 0, "nhapExcel.thieu");
        } else {
            if (loai(oMa) == CellType.STRING) {
                maBacSi = oMa.getStringCellValue().strip().toUpperCase(Locale.ROOT);
                idBacSi = MaBacSi.docId(maBacSi);
            }
            if (idBacSi == null) {
                them(loi, soDong, 0, "nhapExcel.maBacSi");
            }
        }
        LocalDate ngay = docNgay(dong.getCell(1), soDong, loi);
        LocalTime gioBatDau = docGio(dong.getCell(2), soDong, 2, loi);
        LocalTime gioKetThuc = docGio(dong.getCell(3), soDong, 3, loi);
        Long idPhongKham = docSo(dong.getCell(4), soDong, 4, 1, 999_999_999, "nhapExcel.maPhong", loi);
        Long soLuot = docSo(dong.getCell(5), soDong, 5, 1, 60, "nhapExcel.so", loi);
        Long thoiLuong = docSo(dong.getCell(6), soDong, 6, 1, 60, "nhapExcel.so", loi);
        if (loi.size() == soLoiTruoc) {
            cacDong.add(new DongCaNhap(soDong, idBacSi, maBacSi, ngay, gioBatDau, gioKetThuc, idPhongKham,
                    soLuot.intValue(), thoiLuong.intValue()));
        }
    }

    /** Ô ngày (số ngày của Excel) hoặc chữ yyyy-MM-dd / dd/MM/yyyy; null và thêm lỗi nếu sai. */
    private LocalDate docNgay(Cell o, int soDong, List<LoiDong> loi) {
        if (oTrong(o)) {
            them(loi, soDong, 1, "nhapExcel.thieu");
            return null;
        }
        LocalDate ngay = null;
        if (loai(o) == CellType.NUMERIC) {
            double giaTri = o.getNumericCellValue();
            if (giaTri >= 1 && DateUtil.isValidExcelDate(giaTri)) {
                ngay = o.getLocalDateTimeCellValue().toLocalDate();
            }
        } else if (loai(o) == CellType.STRING) {
            String chu = o.getStringCellValue().strip();
            ngay = docNgayChu(chu, DateTimeFormatter.ISO_LOCAL_DATE);
            if (ngay == null) {
                ngay = docNgayChu(chu, NGAY_CHU);
            }
        }
        if (ngay == null) {
            them(loi, soDong, 1, "nhapExcel.ngay");
        }
        return ngay;
    }

    private static LocalDate docNgayChu(String chu, DateTimeFormatter dinhDang) {
        try {
            return LocalDate.parse(chu, dinhDang);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Ô giờ (phần của 1 ngày, làm tròn tới phút) hoặc chữ H:mm / HH:mm:ss; null và thêm lỗi nếu sai. */
    private LocalTime docGio(Cell o, int soDong, int cot, List<LoiDong> loi) {
        if (oTrong(o)) {
            them(loi, soDong, cot, "nhapExcel.thieu");
            return null;
        }
        LocalTime gio = null;
        if (loai(o) == CellType.NUMERIC) {
            double giaTri = o.getNumericCellValue();
            long phut = Math.round(giaTri * 24 * 60);
            if (giaTri >= 0 && phut < 24 * 60) {
                gio = LocalTime.of((int) (phut / 60), (int) (phut % 60));
            }
        } else if (loai(o) == CellType.STRING) {
            try {
                gio = LocalTime.parse(o.getStringCellValue().strip(), GIO_CHU).truncatedTo(ChronoUnit.MINUTES);
            } catch (DateTimeParseException e) {
                gio = null;
            }
        }
        if (gio == null) {
            them(loi, soDong, cot, "nhapExcel.gio");
        }
        return gio;
    }

    /** Số nguyên trong [toiThieu, toiDa] (ô số hoặc chữ toàn chữ số); null và thêm lỗi nếu sai. */
    private Long docSo(Cell o, int soDong, int cot, long toiThieu, long toiDa, String khoaLoi, List<LoiDong> loi) {
        if (oTrong(o)) {
            them(loi, soDong, cot, "nhapExcel.thieu");
            return null;
        }
        Long so = null;
        if (loai(o) == CellType.NUMERIC) {
            double giaTri = o.getNumericCellValue();
            if (giaTri == Math.rint(giaTri) && Math.abs(giaTri) < 1e10) {
                so = (long) giaTri;
            }
        } else if (loai(o) == CellType.STRING) {
            String chu = o.getStringCellValue().strip();
            if (SO_NGUYEN.matcher(chu).matches()) {
                so = Long.valueOf(chu);
            }
        }
        if (so == null || so < toiThieu || so > toiDa) {
            them(loi, soDong, cot, khoaLoi, String.valueOf(toiThieu), String.valueOf(toiDa));
            return null;
        }
        return so;
    }

    /**
     * Dòng trùng giờ với 1 dòng trước nó trong tệp (cùng ngày, cùng bác sĩ hoặc cùng phòng) thành lỗi "trùng với dòng
     * X" và không được xét tiếp với DB.
     *
     * @return các dòng còn lại, giữ thứ tự
     */
    private List<DongCaNhap> boDongTrungTrongTep(List<DongCaNhap> cacDong, List<LoiDong> loi) {
        List<DongCaNhap> conLai = new ArrayList<>();
        for (int i = 0; i < cacDong.size(); i++) {
            DongCaNhap dong = cacDong.get(i);
            DongCaNhap trungVoi = null;
            for (int j = 0; j < i && trungVoi == null; j++) {
                DongCaNhap truoc = cacDong.get(j);
                boolean cungNguoiHoacPhong = truoc.idBacSi().equals(dong.idBacSi())
                        || truoc.idPhongKham().equals(dong.idPhongKham());
                if (truoc.ngay().equals(dong.ngay()) && cungNguoiHoacPhong
                        && truoc.gioBatDau().isBefore(dong.gioKetThuc()) && dong.gioBatDau().isBefore(truoc.gioKetThuc())) {
                    trungVoi = truoc;
                }
            }
            if (trungVoi == null) {
                conLai.add(dong);
            } else {
                loi.add(new LoiDong(dong.dong(), null, cau("nhapExcel.trungTrongTep", String.valueOf(trungVoi.dong()))));
            }
        }
        return conLai;
    }

    private static boolean dongTrong(Row dong) {
        for (int cot = 0; cot < COT.length; cot++) {
            if (!oTrong(dong.getCell(cot))) {
                return false;
            }
        }
        return true;
    }

    private static boolean oTrong(Cell o) {
        if (o == null) {
            return true;
        }
        CellType loai = loai(o);
        return loai == CellType.BLANK || loai == CellType.STRING && o.getStringCellValue().isBlank();
    }

    /** Kiểu giá trị của ô; ô công thức lấy kiểu của kết quả đã lưu trong tệp (không tính lại). */
    private static CellType loai(Cell o) {
        return o.getCellType() == CellType.FORMULA ? o.getCachedFormulaResultType() : o.getCellType();
    }

    private void them(List<LoiDong> loi, int soDong, int cot, String khoa, String... thamSo) {
        loi.add(new LoiDong(soDong, COT[cot], cau(khoa, thamSo)));
    }

    private String cau(String khoa, String... thamSo) {
        return messageSource.getMessage(khoa, thamSo, khoa, NgonNgu.hienTai().locale());
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
        Font nghieng = workbook.createFont();
        nghieng.setItalic(true);
        CellStyle tieuDeThongTin = workbook.createCellStyle();
        tieuDeThongTin.setFont(nghieng);
        tieuDeThongTin.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
        tieuDeThongTin.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        tieuDeThongTin.setBorderBottom(BorderStyle.THIN);
        tieuDeThongTin.setWrapText(true);
        CellStyle ngay = workbook.createCellStyle();
        ngay.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd"));
        CellStyle gio = workbook.createCellStyle();
        gio.setDataFormat(workbook.createDataFormat().getFormat("hh:mm"));
        CellStyle chuDai = workbook.createCellStyle();
        chuDai.setWrapText(true);
        return new KieuO(tieuDe, tieuDeThongTin, ngay, gio, chuDai);
    }

    /** Dòng tiêu đề A-G của trang ca (cộng các cột thông tin của tệp xuất), cố định khi cuộn. */
    private void tieuDeTrangCa(Sheet trang, KieuO kieu, boolean coCotThongTin) {
        tieuDe(trang, kieu, KHOA_COT_CA, RONG_COT);
        if (coCotThongTin) {
            Row dong = trang.getRow(0);
            for (int i = 0; i < KHOA_COT_THONG_TIN.length; i++) {
                int cot = KHOA_COT_CA.length + i;
                Cell o = dong.createCell(cot);
                o.setCellValue(cau("excel.cot." + KHOA_COT_THONG_TIN[i]));
                o.setCellStyle(kieu.tieuDeThongTin());
                trang.setColumnWidth(cot, RONG_COT[cot] * 256);
            }
        }
    }

    private void tieuDe(Sheet trang, KieuO kieu, String[] cacKhoa, int[] doRong) {
        Row dong = trang.createRow(0);
        for (int cot = 0; cot < cacKhoa.length; cot++) {
            Cell o = dong.createCell(cot);
            o.setCellValue(cau("excel.cot." + cacKhoa[cot]));
            o.setCellStyle(kieu.tieuDe());
            trang.setColumnWidth(cot, doRong[cot] * 256);
        }
        trang.createFreezePane(0, 1);
    }

    private void huongDan(Sheet trang, KieuO kieu, String tenTrangCa, String tenTrangBacSi, String tenTrangPhong) {
        trang.setColumnWidth(0, 8 * 256);
        trang.setColumnWidth(1, 80 * 256);
        trang.setColumnWidth(2, 24 * 256);
        int soDong = 0;
        Cell tieuDe = trang.createRow(soDong++).createCell(0);
        tieuDe.setCellValue(cau("excel.huongDan.tieuDe"));
        tieuDe.setCellStyle(kieu.tieuDe());
        dongChu(trang, soDong++, kieu, cau("excel.huongDan.moDau", tenTrangCa));
        soDong++;
        Row dauBang = trang.createRow(soDong++);
        String[] cotBang = {cau("excel.huongDan.cot"), cau("excel.huongDan.noiDung"), cau("excel.huongDan.viDu")};
        for (int i = 0; i < cotBang.length; i++) {
            Cell o = dauBang.createCell(i);
            o.setCellValue(cotBang[i]);
            o.setCellStyle(kieu.tieuDe());
        }
        for (int i = 0; i < COT.length; i++) {
            Row dong = trang.createRow(soDong++);
            dong.createCell(0).setCellValue(COT[i]);
            Cell noiDung = dong.createCell(1);
            noiDung.setCellValue(cau("excel.cot." + KHOA_COT_CA[i]) + ": "
                    + cau("excel.huongDan." + COT[i], tenTrangBacSi, tenTrangPhong));
            noiDung.setCellStyle(kieu.chuDai());
            dong.createCell(2).setCellValue(VI_DU[i]);
        }
        soDong++;
        Cell quyTac = trang.createRow(soDong++).createCell(0);
        quyTac.setCellValue(cau("excel.huongDan.quyTac"));
        quyTac.setCellStyle(kieu.tieuDe());
        String[] cacQuyTac = {
                cau("excel.huongDan.quyTac1", String.valueOf(lichLamViecProperties.soNgayXepTruocToiDa())),
                cau("excel.huongDan.quyTac2", String.valueOf(lichLamViecProperties.soGioToiDaMoiTuan())),
                cau("excel.huongDan.quyTac3"),
                cau("excel.huongDan.quyTac4", String.valueOf(SO_DONG_TOI_DA)),
                cau("excel.huongDan.quyTac5"),
                cau("excel.huongDan.quyTac6")};
        for (String dong : cacQuyTac) {
            dongChu(trang, soDong++, kieu, "- " + dong);
        }
    }

    /** 1 dòng chữ ở cột B (cột rộng, xuống dòng khi dài). */
    private static void dongChu(Sheet trang, int soDong, KieuO kieu, String chu) {
        Cell o = trang.createRow(soDong).createCell(1);
        o.setCellValue(chu);
        o.setCellStyle(kieu.chuDai());
    }

    /**
     * Danh sách chọn cho 1 cột của trang ca (dòng 2 tới dòng ca cuối cùng có thể có), lấy từ cột A của trang danh mục
     * qua 1 tên vùng. Gõ giá trị ngoài danh sách chỉ bị cảnh báo: khi nhập, mã vẫn được kiểm tra lại với DB.
     */
    private static void danhSachChon(XSSFWorkbook workbook, XSSFSheet trangCa, String tenVung, String tenTrang,
            int soMuc, int cot) {
        if (soMuc == 0) {
            return;
        }
        XSSFName vung = workbook.createName();
        vung.setNameName(tenVung);
        vung.setRefersToFormula("'" + tenTrang + "'!$A$2:$A$" + (soMuc + 1));
        XSSFDataValidationHelper troGiup = new XSSFDataValidationHelper(trangCa);
        DataValidation kiemTra = troGiup.createValidation(troGiup.createFormulaListConstraint(tenVung),
                new CellRangeAddressList(1, SO_DONG_TOI_DA, cot, cot));
        // XSSF: true = HIỆN mũi tên chọn (ngược nghĩa với tên hàm, xem hướng dẫn của POI)
        kiemTra.setSuppressDropDownArrow(true);
        kiemTra.setErrorStyle(DataValidation.ErrorStyle.WARNING);
        kiemTra.setShowErrorBox(true);
        trangCa.addValidationData(kiemTra);
    }

    private static void oGio(Row dong, int cot, LocalTime gio, KieuO kieu) {
        Cell o = dong.createCell(cot);
        o.setCellValue(gio.toSecondOfDay() / 86_400.0);
        o.setCellStyle(kieu.gio());
    }

    private static byte[] ghi(XSSFWorkbook workbook) throws IOException {
        ByteArrayOutputStream ra = new ByteArrayOutputStream();
        workbook.write(ra);
        return ra.toByteArray();
    }

}
