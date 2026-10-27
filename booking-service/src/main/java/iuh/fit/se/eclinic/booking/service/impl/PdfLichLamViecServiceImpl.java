package iuh.fit.se.eclinic.booking.service.impl;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.Collator;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;

import iuh.fit.se.eclinic.booking.config.PhongChuPdf;
import iuh.fit.se.eclinic.booking.dto.response.CaLamViecResponse;
import iuh.fit.se.eclinic.booking.repository.BacSiChiDocRepository;
import iuh.fit.se.eclinic.booking.repository.LichLamViecRepository;
import iuh.fit.se.eclinic.booking.service.LichLamViecService;
import iuh.fit.se.eclinic.booking.service.PdfLichLamViecService;
import iuh.fit.se.eclinic.booking.service.TaiKhoanService;
import iuh.fit.se.eclinic.booking.util.KhoangNgay;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichLamViec;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import iuh.fit.se.eclinic.common.util.MaBacSi;
import lombok.RequiredArgsConstructor;

/** Bố cục 2 loại bản in. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PdfLichLamViecServiceImpl implements PdfLichLamViecService {

    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter NGAY_THANG = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter GIO = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter LUC_IN = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final Color NEN_TIEU_DE = new Color(0xE8, 0xF0, 0xFE);
    private static final Color NEN_NHOM = new Color(0xF1, 0xF3, 0xF4);
    private static final Color CHU_PHU = new Color(0x5F, 0x63, 0x68);
    private static final float LE = 36;

    private final LichLamViecService lichLamViecService;
    private final LichLamViecRepository lichLamViecRepository;
    private final BacSiChiDocRepository bacSiChiDocRepository;
    private final TaiKhoanService taiKhoanService;
    private final PhongChuPdf phongChu;
    private final MessageSource messageSource;

    @Override
    public TepPdf cuaToi(Long idTaiKhoan, LocalDate tuNgay, LocalDate denNgay) {
        BacSi bacSi = taiKhoanService.layBacSiDangHoatDong(idTaiKhoan);
        return lichBacSi(bacSi, lichLamViecService.lichCuaBacSi(idTaiKhoan, tuNgay, denNgay), tuNgay, denNgay);
    }

    @Override
    public TepPdf cuaBacSi(Long idBacSi, LocalDate tuNgay, LocalDate denNgay) {
        KhoangNgay.kiemTra(tuNgay, denNgay);
        BacSi bacSi = bacSiChiDocRepository.timTheoCacId(List.of(idBacSi)).stream().findFirst()
                .orElseThrow(() -> new LoiKhongTimThay("BacSi", idBacSi));
        return lichBacSi(bacSi, lichLamViecService.lichToanVien(tuNgay, denNgay, null, idBacSi, null), tuNgay,
                denNgay);
    }

    private TepPdf lichBacSi(BacSi bacSi, List<CaLamViecResponse> tatCa, LocalDate tuNgay, LocalDate denNgay) {
        NgonNgu ngonNgu = NgonNgu.hienTai();
        Locale locale = ngonNgu.locale();
        List<CaLamViecResponse> cacCa = tatCa.stream().filter(ca -> ca.trangThai() == TrangThaiLichLamViec.HOAT_DONG)
                .toList();
        String maBacSi = MaBacSi.tu(bacSi.getId());
        ByteArrayOutputStream ra = new ByteArrayOutputStream();
        Document taiLieu = new Document(PageSize.A4, LE, LE, LE, LE + 12);
        try {
            PdfWriter.getInstance(taiLieu, ra);
            taiLieu.open();
            taiLieu.add(new Paragraph("eClinic", phongChu.thuong(9, CHU_PHU)));
            Paragraph tieuDe = new Paragraph(cau(locale, "pdf.bacSi.tieuDe"), phongChu.dam(16));
            tieuDe.setSpacingAfter(6);
            taiLieu.add(tieuDe);
            taiLieu.add(new Paragraph(cau(locale, "pdf.bacSi.bacSi", tenCoHocVi(bacSi.getHocVi(),
                    bacSi.getTaiKhoan().getHoTen()), maBacSi), phongChu.thuong(10)));
            taiLieu.add(new Paragraph(cau(locale, "pdf.bacSi.chuyenKhoa",
                    PhongChuPdf.nfc(bacSi.getChuyenKhoa().ten(ngonNgu))), phongChu.thuong(10)));
            Paragraph khoang = new Paragraph(cau(locale, "pdf.khoangNgay", tuNgay.format(NGAY), denNgay.format(NGAY)),
                    phongChu.thuong(10));
            khoang.setSpacingAfter(10);
            taiLieu.add(khoang);
            if (cacCa.isEmpty()) {
                taiLieu.add(new Paragraph(cau(locale, "pdf.bacSi.khongCoCa"), phongChu.thuong(10, CHU_PHU)));
            } else {
                taiLieu.add(bangCa(cacCa, locale));
                long tongPhut = cacCa.stream().mapToLong(ca -> soPhut(ca.gioBatDau(), ca.gioKetThuc())).sum();
                Paragraph tong = new Paragraph(cau(locale, "pdf.bacSi.tong", String.valueOf(cacCa.size()),
                        soGio(tongPhut, locale)), phongChu.dam(10));
                tong.setSpacingBefore(6);
                taiLieu.add(tong);
            }
            taiLieu.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Không dựng được PDF lịch làm việc", e);
        }
        return new TepPdf("lich-lam-viec_" + maBacSi + "_" + tuNgay + "_" + denNgay + ".pdf",
                danhSoTrang(ra.toByteArray(), locale));
    }

    private PdfPTable bangCa(List<CaLamViecResponse> cacCa, Locale locale) {
        PdfPTable bang = new PdfPTable(new float[] {3.2f, 2.2f, 1.2f, 3.2f, 1.6f, 1.8f});
        bang.setWidthPercentage(100);
        bang.setHeaderRows(1);
        for (String khoa : new String[] {"ngay", "gio", "soGio", "phong", "tang", "luot"}) {
            bang.addCell(oTieuDe(cau(locale, "pdf.cot." + khoa), 9));
        }
        Font chu = phongChu.thuong(9);
        for (CaLamViecResponse ca : cacCa) {
            bang.addCell(o(thu(ca.ngay(), locale) + ", " + ca.ngay().format(NGAY), chu));
            bang.addCell(o(khungGio(ca.gioBatDau(), ca.gioKetThuc()), chu));
            bang.addCell(oGiua(soGio(soPhut(ca.gioBatDau(), ca.gioKetThuc()), locale), chu));
            bang.addCell(o(PhongChuPdf.nfc(ca.phongKham().tenPhong()), chu));
            bang.addCell(o(PhongChuPdf.nfc(ca.phongKham().tang()), chu));
            bang.addCell(oGiua(ca.soLuotDaDat() + " / " + ca.tongSoLuot(), chu));
        }
        return bang;
    }

    @Override
    public TepPdf toanVien(LocalDate tuNgay, LocalDate denNgay, Long idChuyenKhoa) {
        KhoangNgay.kiemTra(tuNgay, denNgay);
        NgonNgu ngonNgu = NgonNgu.hienTai();
        Locale locale = ngonNgu.locale();
        LocalDate thuHaiDau = tuNgay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate chuNhatCuoi = denNgay.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        // Lấy đủ các tuần (có thể quá 42 ngày) để cột giờ trong tuần không bị thiếu
        List<LichLamViec> cacCa = lichLamViecRepository.timTrongKhoang(thuHaiDau, chuNhatCuoi, idChuyenKhoa, null, null)
                .stream().filter(ca -> ca.getTrangThai() == TrangThaiLichLamViec.HOAT_DONG).toList();
        String tenChuyenKhoa = idChuyenKhoa == null ? null : cacCa.stream().findFirst()
                .map(ca -> PhongChuPdf.nfc(ca.getBacSi().getChuyenKhoa().ten(ngonNgu))).orElse(null);
        ByteArrayOutputStream ra = new ByteArrayOutputStream();
        // Khổ ngang thật (842 x 595); A4.rotate() cho khổ dọc kèm cờ /Rotate 90
        Document taiLieu = new Document(new Rectangle(PageSize.A4.getHeight(), PageSize.A4.getWidth()), LE, LE, LE,
                LE + 12);
        try {
            PdfWriter.getInstance(taiLieu, ra);
            taiLieu.open();
            for (LocalDate thuHai = thuHaiDau; thuHai.isBefore(chuNhatCuoi); thuHai = thuHai.plusWeeks(1)) {
                if (!thuHai.equals(thuHaiDau)) {
                    taiLieu.newPage();
                }
                LocalDate dauTuan = thuHai;
                LocalDate cuoiTuan = thuHai.plusDays(6);
                List<LichLamViec> trongTuan = cacCa.stream()
                        .filter(ca -> !ca.getNgayLamViec().isBefore(dauTuan) && !ca.getNgayLamViec().isAfter(cuoiTuan))
                        .toList();
                trangTuan(taiLieu, dauTuan, trongTuan, tenChuyenKhoa, locale, ngonNgu);
            }
            taiLieu.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Không dựng được PDF lịch toàn viện", e);
        }
        return new TepPdf("lich-lam-viec-toan-vien_" + tuNgay + "_" + denNgay + ".pdf",
                danhSoTrang(ra.toByteArray(), locale));
    }

    private void trangTuan(Document taiLieu, LocalDate thuHai, List<LichLamViec> cacCa, String tenChuyenKhoa,
            Locale locale, NgonNgu ngonNgu) {
        taiLieu.add(new Paragraph("eClinic", phongChu.thuong(9, CHU_PHU)));
        taiLieu.add(new Paragraph(cau(locale, "pdf.toanVien.tieuDe"), phongChu.dam(14)));
        Paragraph tuan = new Paragraph(cau(locale, "pdf.toanVien.tuan", thuHai.format(NGAY),
                thuHai.plusDays(6).format(NGAY)) + (tenChuyenKhoa == null ? ""
                        : " · " + cau(locale, "pdf.toanVien.chuyenKhoa", tenChuyenKhoa)), phongChu.thuong(10));
        tuan.setSpacingAfter(8);
        taiLieu.add(tuan);
        if (cacCa.isEmpty()) {
            taiLieu.add(new Paragraph(cau(locale, "pdf.toanVien.khongCoCa"), phongChu.thuong(10, CHU_PHU)));
            return;
        }
        float[] doRong = new float[9];
        doRong[0] = 2.4f;
        for (int i = 1; i <= 7; i++) {
            doRong[i] = 1.55f;
        }
        doRong[8] = 0.8f;
        PdfPTable bang = new PdfPTable(doRong);
        bang.setWidthPercentage(100);
        bang.setHeaderRows(1);
        bang.addCell(oTieuDe(cau(locale, "pdf.cot.bacSi"), 8));
        for (int i = 0; i < 7; i++) {
            LocalDate ngay = thuHai.plusDays(i);
            bang.addCell(oTieuDe(thu(ngay, locale) + "\n" + ngay.format(NGAY_THANG), 8));
        }
        bang.addCell(oTieuDe(cau(locale, "pdf.cot.gioTuan"), 8));

        // Bác sĩ theo chuyên khoa rồi họ tên (thứ tự chữ cái của ngôn ngữ đang in)
        Collator thuTu = Collator.getInstance(locale);
        Map<Long, List<LichLamViec>> theoBacSi = new LinkedHashMap<>();
        cacCa.stream()
                .sorted(Comparator.comparing((LichLamViec ca) -> ca.getBacSi().getChuyenKhoa().ten(ngonNgu), thuTu)
                        .thenComparing(ca -> ca.getBacSi().getTaiKhoan().getHoTen(), thuTu)
                        .thenComparing(ca -> ca.getBacSi().getId())
                        .thenComparing(LichLamViec::getNgayLamViec)
                        .thenComparing(LichLamViec::getGioBatDau))
                .forEach(ca -> theoBacSi.computeIfAbsent(ca.getBacSi().getId(), k -> new ArrayList<>()).add(ca));
        Font chu = phongChu.thuong(7.5f);
        Font chuPhu = phongChu.thuong(7, CHU_PHU);
        Long chuyenKhoaTruoc = null;
        for (List<LichLamViec> caCuaBacSi : theoBacSi.values()) {
            BacSi bacSi = caCuaBacSi.get(0).getBacSi();
            if (!bacSi.getChuyenKhoa().getId().equals(chuyenKhoaTruoc)) {
                chuyenKhoaTruoc = bacSi.getChuyenKhoa().getId();
                PdfPCell nhom = o(PhongChuPdf.nfc(bacSi.getChuyenKhoa().ten(ngonNgu)), phongChu.dam(8));
                nhom.setColspan(9);
                nhom.setBackgroundColor(NEN_NHOM);
                bang.addCell(nhom);
            }
            Phrase ten = new Phrase(PhongChuPdf.nfc(bacSi.getTaiKhoan().getHoTen()), phongChu.dam(7.5f));
            ten.add(new Phrase("\n" + MaBacSi.tu(bacSi.getId()), chuPhu));
            bang.addCell(o(ten));
            long tongPhut = 0;
            for (int i = 0; i < 7; i++) {
                LocalDate ngay = thuHai.plusDays(i);
                // Mỗi ca 2 dòng: giờ, rồi tên phòng (chữ xám)
                Phrase trongNgay = new Phrase();
                for (LichLamViec ca : caCuaBacSi) {
                    if (ca.getNgayLamViec().equals(ngay)) {
                        trongNgay.add(new Phrase((trongNgay.isEmpty() ? "" : "\n")
                                + khungGio(ca.getGioBatDau(), ca.getGioKetThuc()) + "\n", chu));
                        trongNgay.add(new Phrase(PhongChuPdf.nfc(ca.getPhongKham().ten(ngonNgu)), chuPhu));
                        tongPhut += soPhut(ca.getGioBatDau(), ca.getGioKetThuc());
                    }
                }
                bang.addCell(o(trongNgay));
            }
            bang.addCell(oGiua(soGio(tongPhut, locale), phongChu.dam(8)));
        }
        taiLieu.add(bang);
    }

    /** Đóng "Trang i / n" và giờ in lên chân mọi trang. */
    private byte[] danhSoTrang(byte[] pdf, Locale locale) {
        String inLuc = cau(locale, "pdf.inLuc", LocalDateTime.now().format(LUC_IN));
        ByteArrayOutputStream ra = new ByteArrayOutputStream();
        try {
            PdfReader doc = new PdfReader(pdf);
            PdfStamper dong = new PdfStamper(doc, ra);
            int tongSo = doc.getNumberOfPages();
            Font chu = phongChu.thuong(8, CHU_PHU);
            for (int i = 1; i <= tongSo; i++) {
                Rectangle trang = doc.getPageSize(i);
                float y = LE - 6;
                ColumnText.showTextAligned(dong.getOverContent(i), Element.ALIGN_LEFT, new Phrase(inLuc, chu), LE, y, 0);
                ColumnText.showTextAligned(dong.getOverContent(i), Element.ALIGN_RIGHT,
                        new Phrase(cau(locale, "pdf.trang", String.valueOf(i), String.valueOf(tongSo)), chu),
                        trang.getWidth() - LE, y, 0);
            }
            dong.close();
            doc.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (DocumentException e) {
            throw new IllegalStateException("Không đánh số trang được cho PDF lịch làm việc", e);
        }
        return ra.toByteArray();
    }

    private PdfPCell oTieuDe(String chu, float co) {
        PdfPCell o = o(chu, phongChu.dam(co));
        o.setBackgroundColor(NEN_TIEU_DE);
        o.setHorizontalAlignment(Element.ALIGN_CENTER);
        return o;
    }

    private static PdfPCell o(String chu, Font font) {
        return o(new Phrase(chu, font));
    }

    private static PdfPCell o(Phrase chu) {
        PdfPCell o = new PdfPCell(chu);
        o.setPadding(4);
        o.setBorderColor(Color.LIGHT_GRAY);
        return o;
    }

    private static PdfPCell oGiua(String chu, Font font) {
        PdfPCell o = o(chu, font);
        o.setHorizontalAlignment(Element.ALIGN_CENTER);
        return o;
    }

    private String cau(Locale locale, String khoa, String... thamSo) {
        return messageSource.getMessage(khoa, thamSo, khoa, locale);
    }

    private static String tenCoHocVi(String hocVi, String hoTen) {
        String ten = PhongChuPdf.nfc(hoTen);
        return hocVi == null || hocVi.isBlank() ? ten : PhongChuPdf.nfc(hocVi) + " " + ten;
    }

    /** "Thứ Hai" / "Monday": viết hoa chữ đầu (dữ liệu tiếng Việt của JDK có thể viết thường). */
    private static String thu(LocalDate ngay, Locale locale) {
        String ten = ngay.getDayOfWeek().getDisplayName(TextStyle.FULL, locale);
        return ten.isEmpty() ? ten : Character.toUpperCase(ten.charAt(0)) + ten.substring(1);
    }

    private static String khungGio(LocalTime batDau, LocalTime ketThuc) {
        return batDau.format(GIO) + "–" + ketThuc.format(GIO);
    }

    private static long soPhut(LocalTime batDau, LocalTime ketThuc) {
        return Duration.between(batDau, ketThuc).toMinutes();
    }

    /** Số giờ có tối đa 2 chữ số lẻ, dấu thập phân theo ngôn ngữ (37,5 / 37.5). */
    private static String soGio(long soPhut, Locale locale) {
        return new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(locale)).format(soPhut / 60.0);
    }

}
