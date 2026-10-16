package iuh.fit.se.eclinic.chatbot.service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.chatbot.client.CongCu;
import iuh.fit.se.eclinic.chatbot.client.DuLieuCuaToiClient;
import iuh.fit.se.eclinic.chatbot.client.DuLieuCuaToiClient.LanKham;
import iuh.fit.se.eclinic.chatbot.client.DuLieuCuaToiClient.LichHenCuaToi;
import iuh.fit.se.eclinic.chatbot.client.DuLieuPhongKhamClient;
import iuh.fit.se.eclinic.chatbot.client.DuLieuPhongKhamClient.BacSiCongKhai;
import iuh.fit.se.eclinic.chatbot.client.DuLieuPhongKhamClient.CaKham;
import iuh.fit.se.eclinic.chatbot.client.DuLieuPhongKhamClient.KhongTraCuuDuoc;
import iuh.fit.se.eclinic.chatbot.client.DuLieuPhongKhamClient.NgaySomNhat;
import iuh.fit.se.eclinic.chatbot.dto.response.GoiYResponse;
import iuh.fit.se.eclinic.chatbot.repository.ChuyenKhoaChiDocRepository;
import iuh.fit.se.eclinic.common.entity.catalog.ChuyenKhoa;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Các hàm tra cứu CỐ ĐỊNH mà mô hình được xin gọi. Mô hình chỉ đưa tên hàm và tham số (chuỗi JSON chưa đáng tin); ở đây
 * kiểm tra tham số rồi mới đọc dữ liệu. Quy tắc cho mọi hàm ở đây (docs/QUY-UOC-CODE.md mục 7):
 * <ul>
 * <li>chỉ ĐỌC; không hàm nào nhận tham số "của ai". 2 hàm đầu đọc dữ liệu CÔNG KHAI của phòng khám;</li>
 * <li>2 hàm sau ({@link #LICH_HEN_CUA_TOI}, {@link #LAN_KHAM_CUA_TOI}) đọc dữ liệu của CHÍNH người đang chat: không có
 * tham số nào (mô hình viết gì vào tham số cũng bị bỏ), "của ai" là JWT của request ({@link NguoiHoi}), chỉ chạy khi
 * bệnh nhân đã đồng ý trong phiên, và chỉ đưa cho mô hình ngày giờ, bác sĩ, chuyên khoa, phòng, trạng thái, ngày tái
 * khám; họ tên và các mã chỉ đi vào thẻ {@code goiY};</li>
 * <li>tham số sai, id không có thật, hàm lạ, service kia không trả lời: trả kết quả báo lỗi cho mô hình, không ném
 * exception;</li>
 * <li>kết quả vừa là JSON cho mô hình đọc, vừa là phần {@code goiY} trả cho người dùng, nên thứ người dùng thấy trên thẻ
 * luôn đúng với dữ liệu, kể cả khi mô hình viết sai.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class TraCuuPhongKham {

    public static final String TIM_BAC_SI = "tim_bac_si";
    public static final String XEM_LICH_BAC_SI = "xem_lich_bac_si";

    public static final String LICH_HEN_CUA_TOI = "lich_hen_cua_toi";
    public static final String LAN_KHAM_CUA_TOI = "lan_kham_cua_toi";

    private static final int SO_BAC_SI_GOI_Y = 6;
    private static final Map<String, Object> KHONG_THAM_SO = Map.of("type", "object", "properties", Map.of());

    private static final List<CongCu> CONG_CU = List.of(
            new CongCu(TIM_BAC_SI,
                    "List the doctors of one specialty of this clinic, each with the earliest day that still has free"
                            + " appointment places. Call it whenever you recommend a specialty.",
                    Map.of("type", "object",
                            "properties", Map.of("idChuyenKhoa", Map.of("type", "integer",
                                    "description", "id of a specialty from the SPECIALTIES list")),
                            "required", List.of("idChuyenKhoa"))),
            new CongCu(XEM_LICH_BAC_SI,
                    "Show one doctor's bookable shifts for 7 days starting at a date: date, room, floor and the one-hour"
                            + " time frames that still have free places. The doctor id must come from tim_bac_si.",
                    Map.of("type", "object",
                            "properties", Map.of(
                                    "idBacSi", Map.of("type", "integer", "description", "doctor id from tim_bac_si"),
                                    "tuNgay", Map.of("type", "string",
                                            "description", "first day, yyyy-MM-dd; omit to start today")),
                            "required", List.of("idBacSi"))));

    /** Chỉ đưa cho mô hình khi {@link NguoiHoi#duocTraCuuCaNhan()}. */
    private static final List<CongCu> CONG_CU_CA_NHAN = List.of(
            new CongCu(LICH_HEN_CUA_TOI,
                    "The signed-in user's OWN upcoming appointments: date, time, doctor, specialty, room and status."
                            + " Takes no arguments; it cannot look up anyone else.",
                    KHONG_THAM_SO),
            new CongCu(LAN_KHAM_CUA_TOI,
                    "The signed-in user's OWN finished visits: date, doctor, specialty and the follow-up date the doctor"
                            + " recommended, if any. Takes no arguments; it cannot look up anyone else.",
                    KHONG_THAM_SO));
    private static final List<CongCu> CONG_CU_DAY_DU = java.util.stream.Stream
            .concat(CONG_CU.stream(), CONG_CU_CA_NHAN.stream()).toList();

    private final ChuyenKhoaChiDocRepository chuyenKhoaRepository;
    private final DuLieuPhongKhamClient client;
    private final DuLieuCuaToiClient cuaToiClient;
    private final JsonMapper jsonMapper;

    /**
     * Kết quả 1 lần tra cứu.
     *
     * @param choMoHinh  JSON gửi lại cho mô hình
     * @param chuyenKhoa phần đưa vào {@code goiY}; null khi lần tra cứu này không có phần đó (cũng vậy với 2 phần sau)
     * @param bacSi      bác sĩ của chuyên khoa
     * @param lichKham   các ca còn chỗ của 1 bác sĩ
     * @param lichHenCuaToi lịch hẹn sắp tới của người chat (thẻ cho người dùng, có họ tên và mã)
     * @param lanKham       các lượt đã khám của người chat
     * @param canDongY      true: mô hình xin dữ liệu cá nhân nhưng bệnh nhân chưa đồng ý trong phiên; CHƯA đọc gì, người
     *                      gọi phải dừng và hỏi ý bệnh nhân
     */
    public record KetQua(String choMoHinh, GoiYResponse.ChuyenKhoa chuyenKhoa, List<GoiYResponse.BacSi> bacSi,
            List<GoiYResponse.LichKham> lichKham, List<GoiYResponse.LichHenCuaToi> lichHenCuaToi,
            List<GoiYResponse.LanKham> lanKham, boolean canDongY) {

        static KetQua congKhai(String choMoHinh, GoiYResponse.ChuyenKhoa chuyenKhoa, List<GoiYResponse.BacSi> bacSi,
                List<GoiYResponse.LichKham> lichKham) {
            return new KetQua(choMoHinh, chuyenKhoa, bacSi, lichKham, null, null, false);
        }
    }

    /** Các hàm đưa cho mô hình: 2 hàm công khai, thêm 2 hàm "của tôi" khi người chat là bệnh nhân trong phiên của mình. */
    public List<CongCu> congCu(NguoiHoi nguoiHoi) {
        return nguoiHoi.duocTraCuuCaNhan() ? CONG_CU_DAY_DU : CONG_CU;
    }

    /**
     * @param thamSoJson tham số do mô hình viết
     * @param nguoiHoi   người đang chat, lấy từ JWT của request (không từ {@code thamSoJson})
     */
    public KetQua thucHien(String ten, String thamSoJson, NguoiHoi nguoiHoi) {
        if (LICH_HEN_CUA_TOI.equals(ten) || LAN_KHAM_CUA_TOI.equals(ten)) {
            // Tham số của 2 hàm này không được đọc: không có gì trong đó chọn được "của ai"
            return cuaToi(ten, nguoiHoi);
        }
        JsonNode thamSo;
        try {
            thamSo = jsonMapper.readTree(thamSoJson == null || thamSoJson.isBlank() ? "{}" : thamSoJson);
        } catch (JacksonException ex) {
            return loi("Arguments are not valid JSON.");
        }
        try {
            return switch (ten == null ? "" : ten) {
                case TIM_BAC_SI -> timBacSi(soNguyen(thamSo, "idChuyenKhoa"));
                case XEM_LICH_BAC_SI -> xemLichBacSi(soNguyen(thamSo, "idBacSi"), chuoi(thamSo, "tuNgay"));
                default -> loi("Unknown function. Only " + TIM_BAC_SI + " and " + XEM_LICH_BAC_SI + " exist.");
            };
        } catch (KhongTraCuuDuoc ex) {
            return loi("The clinic data is temporarily unavailable. Tell the user you cannot check doctors or"
                    + " schedules right now and that the booking page shows them. Do not guess.");
        }
    }

    private KetQua timBacSi(Long idChuyenKhoa) {
        ChuyenKhoa chuyenKhoa = idChuyenKhoa == null ? null : chuyenKhoaRepository.findById(idChuyenKhoa).orElse(null);
        if (chuyenKhoa == null) {
            return loi("idChuyenKhoa must be the id of a specialty from the SPECIALTIES list.");
        }
        Map<Long, NgaySomNhat> somNhat = client.ngaySomNhat(idChuyenKhoa).stream()
                .collect(Collectors.toMap(NgaySomNhat::idBacSi, Function.identity(), (a, b) -> a));
        // Bác sĩ còn chỗ sớm nhất đứng trước; bác sĩ hết chỗ đứng cuối (chuỗi ngày ISO so sánh được như chuỗi)
        List<GoiYResponse.BacSi> bacSi = client.timBacSi(idChuyenKhoa).stream()
                .map(b -> {
                    NgaySomNhat ngay = somNhat.get(b.id());
                    return new GoiYResponse.BacSi(b.id(), b.hoTen(), b.hocVi(), b.chucVu(), b.anhDaiDien(),
                            ngay == null ? null : ngay.ngay(), ngay == null ? 0 : ngay.soChoConLai());
                })
                .sorted(Comparator.comparing(GoiYResponse.BacSi::ngaySomNhat,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(SO_BAC_SI_GOI_Y)
                .toList();
        GoiYResponse.ChuyenKhoa goiYChuyenKhoa = new GoiYResponse.ChuyenKhoa(chuyenKhoa.getId(),
                chuyenKhoa.ten(NgonNgu.hienTai()));

        Map<String, Object> choMoHinh = new LinkedHashMap<>();
        choMoHinh.put("specialty", Map.of("id", goiYChuyenKhoa.id(), "name", goiYChuyenKhoa.ten()));
        List<Map<String, Object>> danhSach = new ArrayList<>();
        for (GoiYResponse.BacSi b : bacSi) {
            Map<String, Object> mot = new LinkedHashMap<>();
            mot.put("id", b.id());
            mot.put("name", b.hoTen());
            mot.put("degree", b.hocVi());
            mot.put("position", b.chucVu());
            mot.put("earliestDayWithFreePlaces", b.ngaySomNhat() == null ? "none in the booking window"
                    : b.ngaySomNhat() + " (" + tenThu(b.ngaySomNhat()) + ")");
            mot.put("freePlacesThatDay", b.soChoConLai());
            danhSach.add(mot);
        }
        choMoHinh.put("doctors", danhSach);
        if (danhSach.isEmpty()) {
            choMoHinh.put("note", "This specialty has no doctor taking appointments right now. Say so.");
        }
        return KetQua.congKhai(jsonMapper.writeValueAsString(choMoHinh), goiYChuyenKhoa, bacSi, null);
    }

    private KetQua xemLichBacSi(Long idBacSi, String tuNgay) {
        if (idBacSi == null) {
            return loi("idBacSi must be a doctor id returned by " + TIM_BAC_SI + ".");
        }
        LocalDate homNay = LocalDate.now();
        LocalDate tu = homNay;
        if (tuNgay != null && !tuNgay.isBlank()) {
            try {
                tu = LocalDate.parse(tuNgay.trim());
            } catch (DateTimeException ex) {
                return loi("tuNgay must be a date written yyyy-MM-dd.");
            }
            if (tu.isBefore(homNay)) {
                tu = homNay;
            }
        }
        BacSiCongKhai bacSi = client.layBacSi(idBacSi).orElse(null);
        if (bacSi == null) {
            return loi("No doctor with that id takes appointments. Use an id returned by " + TIM_BAC_SI + ".");
        }

        List<GoiYResponse.LichKham> lichKham = new ArrayList<>();
        List<Map<String, Object>> choMoHinhCacCa = new ArrayList<>();
        for (CaKham ca : client.lichBacSi(idBacSi, tu)) {
            List<GoiYResponse.KhungGio> conCho = ca.khungGio() == null ? List.of() : ca.khungGio().stream()
                    .filter(khung -> khung.soChoConLai() > 0)
                    .map(khung -> new GoiYResponse.KhungGio(khung.gioBatDau(), khung.gioKetThuc(),
                            khung.soChoConLai()))
                    .toList();
            if (conCho.isEmpty()) {
                continue;
            }
            String tenPhong = ca.phongKham() == null ? null : ca.phongKham().tenPhong();
            String tang = ca.phongKham() == null ? null : ca.phongKham().tang();
            lichKham.add(new GoiYResponse.LichKham(bacSi.id(), bacSi.hoTen(), ca.idLichLamViec(), ca.ngay(), tenPhong,
                    tang, conCho));

            Map<String, Object> mot = new LinkedHashMap<>();
            mot.put("date", ca.ngay() + " (" + tenThu(ca.ngay()) + ")");
            mot.put("room", tenPhong);
            mot.put("floor", tang);
            mot.put("freeTimeFrames", conCho.stream()
                    .map(khung -> gio(khung.gioBatDau()) + "-" + gio(khung.gioKetThuc()) + " (" + khung.soChoConLai()
                            + " free)")
                    .toList());
            choMoHinhCacCa.add(mot);
        }
        Map<String, Object> choMoHinh = new LinkedHashMap<>();
        choMoHinh.put("doctor", Map.of("id", bacSi.id(), "name", bacSi.hoTen()));
        choMoHinh.put("from", tu.toString());
        choMoHinh.put("to", tu.plusDays(6).toString());
        choMoHinh.put("shiftsWithFreePlaces", choMoHinhCacCa);
        if (choMoHinhCacCa.isEmpty()) {
            choMoHinh.put("note", "No free place in these 7 days. Say so; you may check a later week.");
        }
        GoiYResponse.ChuyenKhoa chuyenKhoa = bacSi.idChuyenKhoa() == null ? null
                : new GoiYResponse.ChuyenKhoa(bacSi.idChuyenKhoa(), bacSi.tenChuyenKhoa());
        return KetQua.congKhai(jsonMapper.writeValueAsString(choMoHinh), chuyenKhoa, null, lichKham);
    }

    private KetQua cuaToi(String ten, NguoiHoi nguoiHoi) {
        if (nguoiHoi == null || !nguoiHoi.duocTraCuuCaNhan()) {
            return loi("Personal data is not available in this chat. Tell the user that signed-in patients see their"
                    + " appointments and visit history on the site. Do not guess.");
        }
        if (!nguoiHoi.daDongY()) {
            return new KetQua(null, null, null, null, null, null, true);
        }
        try {
            return LICH_HEN_CUA_TOI.equals(ten) ? lichHenCuaToi(nguoiHoi.token()) : lanKhamCuaToi(nguoiHoi.token());
        } catch (KhongTraCuuDuoc ex) {
            return loi("The user's appointments are temporarily unavailable. Tell the user you cannot check them right"
                    + " now and that the appointment list on the site shows them. Do not guess.");
        }
    }

    private KetQua lichHenCuaToi(String token) {
        List<GoiYResponse.LichHenCuaToi> the = new ArrayList<>();
        List<Map<String, Object>> choMoHinhCacLich = new ArrayList<>();
        for (LichHenCuaToi lich : cuaToiClient.lichHenSapToi(token)) {
            String hoTenBacSi = lich.bacSi() == null ? null : lich.bacSi().hoTen();
            String tenPhong = lich.phongKham() == null ? null : lich.phongKham().tenPhong();
            String tang = lich.phongKham() == null ? null : lich.phongKham().tang();
            the.add(new GoiYResponse.LichHenCuaToi(lich.maPhieuKham(), lich.maTraCuu(), lich.linkPhieuKham(),
                    lich.trangThai(), lich.ngay(), lich.gioBatDauKhung(), lich.gioKetThucKhung(), hoTenBacSi,
                    lich.tenChuyenKhoa(), tenPhong, tang, lich.hoTenBenhNhan(), lich.laBanThan(), lich.canDoiLich()));

            // Cho mô hình: không họ tên bệnh nhân, không mã nào
            Map<String, Object> mot = new LinkedHashMap<>();
            mot.put("date", lich.ngay() + " (" + tenThu(lich.ngay()) + ")");
            mot.put("time", gio(lich.gioBatDauKhung()) + "-" + gio(lich.gioKetThucKhung()));
            mot.put("doctor", hoTenBacSi);
            mot.put("specialty", lich.tenChuyenKhoa());
            mot.put("room", tenPhong);
            mot.put("floor", tang);
            mot.put("status", lich.canDoiLich()
                    ? "the doctor's shift was cancelled: the user must reschedule or cancel this appointment"
                    : "confirmed");
            mot.put("forWhom", lich.laBanThan() ? "the user" : "another person the user booked for");
            choMoHinhCacLich.add(mot);
        }
        Map<String, Object> choMoHinh = new LinkedHashMap<>();
        choMoHinh.put("upcomingAppointments", choMoHinhCacLich);
        choMoHinh.put("note", choMoHinhCacLich.isEmpty() ? "The user has no upcoming appointment. Say so."
                : "Nearest first, at most 10. The site shows each one as a card with the patient name and booking"
                        + " code, which you do not have.");
        return new KetQua(jsonMapper.writeValueAsString(choMoHinh), null, null, null, the, null, false);
    }

    private KetQua lanKhamCuaToi(String token) {
        List<GoiYResponse.LanKham> the = new ArrayList<>();
        List<Map<String, Object>> choMoHinhCacLan = new ArrayList<>();
        for (LanKham lan : cuaToiClient.lanKham(token)) {
            LichHenCuaToi lich = lan.lichHen();
            if (lich == null) {
                continue;
            }
            String hoTenBacSi = lich.bacSi() == null ? null : lich.bacSi().hoTen();
            String taiKham = lan.ketQua() == null ? null : lan.ketQua().ngayTaiKhamDeXuat();
            the.add(new GoiYResponse.LanKham(lich.maPhieuKham(), lich.maTraCuu(), lich.ngay(), hoTenBacSi,
                    lich.tenChuyenKhoa(), lich.hoTenBenhNhan(), lich.laBanThan(), taiKham));

            Map<String, Object> mot = new LinkedHashMap<>();
            mot.put("date", lich.ngay() + " (" + tenThu(lich.ngay()) + ")");
            mot.put("doctor", hoTenBacSi);
            mot.put("specialty", lich.tenChuyenKhoa());
            mot.put("forWhom", lich.laBanThan() ? "the user" : "another person the user booked for");
            mot.put("followUpDateRecommended", taiKham == null ? "none" : taiKham + " (" + tenThu(taiKham) + ")");
            choMoHinhCacLan.add(mot);
        }
        Map<String, Object> choMoHinh = new LinkedHashMap<>();
        choMoHinh.put("finishedVisits", choMoHinhCacLan);
        choMoHinh.put("note", choMoHinhCacLan.isEmpty() ? "The user has no finished visit on record. Say so."
                : "Latest first, at most 10. A follow-up date is a recommendation only: no appointment is booked for"
                        + " it. Diagnosis, advice and prescription are on the visit history page; you do not have them.");
        return new KetQua(jsonMapper.writeValueAsString(choMoHinh), null, null, null, null, the, false);
    }

    private KetQua loi(String thongBao) {
        return KetQua.congKhai(jsonMapper.writeValueAsString(Map.of("error", thongBao)), null, null, null);
    }

    /** Số nguyên dương viết dạng số hoặc dạng chuỗi chữ số; mọi dạng khác là null. */
    private static Long soNguyen(JsonNode thamSo, String ten) {
        JsonNode giaTri = thamSo.get(ten);
        if (giaTri == null) {
            return null;
        }
        if (giaTri.isIntegralNumber() && giaTri.canConvertToLong()) {
            return giaTri.longValue() > 0 ? giaTri.longValue() : null;
        }
        if (giaTri.isString() && giaTri.asString().strip().matches("[1-9]\\d{0,17}")) {
            return Long.valueOf(giaTri.asString().strip());
        }
        return null;
    }

    private static String chuoi(JsonNode thamSo, String ten) {
        JsonNode giaTri = thamSo.get(ten);
        return giaTri != null && giaTri.isString() ? giaTri.asString() : null;
    }

    /** Tên thứ theo ngôn ngữ của request, để mô hình không phải tự tính thứ từ ngày. */
    private static String tenThu(String ngayIso) {
        try {
            return LocalDate.parse(ngayIso).getDayOfWeek().getDisplayName(TextStyle.FULL, NgonNgu.hienTai().locale());
        } catch (DateTimeException ex) {
            return "";
        }
    }

    /** "2026-10-14T08:00:00" -> "08:00". */
    private static String gio(String gioIso) {
        try {
            return LocalDateTime.parse(gioIso).toLocalTime().toString().substring(0, 5);
        } catch (DateTimeException | IndexOutOfBoundsException ex) {
            return gioIso;
        }
    }

}
