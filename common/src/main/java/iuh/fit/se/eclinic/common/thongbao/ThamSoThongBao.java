package iuh.fit.se.eclinic.common.thongbao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.json.JsonMapper;

/** Tham số của 1 mẫu thông báo, theo đúng thứ tự {0}, {1}... của mẫu. */
public final class ThamSoThongBao {

    static final String GIO_NGAY = "gn";
    static final String NGAY = "n";
    static final String GIO = "g";
    static final String TIENG_VIET = "vi";
    static final String TIENG_ANH = "en";
    static final String KHOA = "k";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final List<Object> giaTri = new ArrayList<>();

    private ThamSoThongBao() {
    }

    public static ThamSoThongBao tao() {
        return new ThamSoThongBao();
    }

    /** Chữ chép nguyên văn ở mọi ngôn ngữ. */
    public ThamSoThongBao chu(String chu) {
        giaTri.add(chu == null ? "" : chu);
        return this;
    }

    public ThamSoThongBao so(long so) {
        return chu(String.valueOf(so));
    }

    public ThamSoThongBao gioNgay(LocalDateTime gioNgay) {
        giaTri.add(Map.of(GIO_NGAY, gioNgay.truncatedTo(ChronoUnit.MINUTES).toString()));
        return this;
    }

    public ThamSoThongBao ngay(LocalDate ngay) {
        giaTri.add(Map.of(NGAY, ngay.toString()));
        return this;
    }

    public ThamSoThongBao gio(LocalTime gio) {
        giaTri.add(Map.of(GIO, gio.truncatedTo(ChronoUnit.MINUTES).toString()));
        return this;
    }

    /** Chữ có 2 ngôn ngữ; bản tiếng Anh trống thì người đọc tiếng Anh thấy bản tiếng Việt. */
    public ThamSoThongBao haiNgonNgu(String tiengViet, String tiengAnh) {
        Map<String, String> chu = new LinkedHashMap<>();
        chu.put(TIENG_VIET, tiengViet == null ? "" : tiengViet);
        if (tiengAnh != null && !tiengAnh.isBlank()) {
            chu.put(TIENG_ANH, tiengAnh);
        }
        giaTri.add(chu);
        return this;
    }

    /** Câu do hệ thống viết: lưu khoá, lúc đọc mới lấy câu của ngôn ngữ đó. */
    public ThamSoThongBao khoa(String khoaThongDiep) {
        giaTri.add(Map.of(KHOA, khoaThongDiep));
        return this;
    }

    public String toJson() {
        return JSON.writeValueAsString(giaTri);
    }
}
