package iuh.fit.se.eclinic.common.entity.booking;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.se.eclinic.common.entity.CreatableEntity;
import iuh.fit.se.eclinic.common.enums.KenhThongBao;
import iuh.fit.se.eclinic.common.enums.LoaiThongBao;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Thông báo chờ gửi sang notification-service (bảng outbox của booking-service). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "su_kien_thong_bao")
@AttributeOverride(name = "id", column = @Column(name = "id_su_kien"))
public class SuKienThongBao extends CreatableEntity {

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "kenh", nullable = false, length = 10)
    private KenhThongBao kenh = KenhThongBao.CHUONG;

    /** Null với dòng kênh EMAIL. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "loai", length = 30)
    private LoaiThongBao loai;

    /** Tài khoản nhận thông báo; null với dòng kênh EMAIL. */
    @Column(name = "id_tai_khoan")
    private Long idTaiKhoan;

    /** Địa chỉ nhận, chỉ có ở dòng kênh EMAIL (người nhận có thể là khách, không có tài khoản). */
    @Column(name = "email_nhan", length = 255)
    private String emailNhan;

    /** Ngôn ngữ của email, chỉ có ở dòng kênh EMAIL. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "ngon_ngu", length = 5)
    private NgonNgu ngonNgu;

    @Column(name = "id_lich_hen")
    private Long idLichHen;

    /** Yêu cầu đổi ca / xin nghỉ liên quan. */
    @Column(name = "id_yeu_cau")
    private Long idYeuCau;

    /** Khoá mẫu câu trong thong-diep/ (vd {@code thongBao.lichHenMoi}); câu chữ được soạn lúc đọc. */
    @Column(name = "khoa_mau", length = 80)
    private String khoaMau;

    /** Tham số của mẫu, mảng JSON ({@code ThamSoThongBao}). */
    @Column(name = "tham_so", columnDefinition = "TEXT")
    private String thamSo;

    /** Kênh EMAIL: nội dung tệp .ics đính kèm email (RFC 5545); null = không đính kèm. */
    @Column(name = "tep_lich", columnDefinition = "TEXT")
    private String tepLich;

    /** Câu soạn sẵn của sự kiện cũ; sự kiện mới để null và dùng {@code khoaMau}. */
    @Column(name = "noi_dung", columnDefinition = "TEXT")
    private String noiDung;

    /** Số lần notification-service từ chối riêng dòng này (dữ liệu sai); mất kết nối không tính. */
    @Column(name = "so_lan_loi", nullable = false)
    private int soLanLoi;

    /** Null = chưa gửi được. */
    @Column(name = "ngay_gui")
    private LocalDateTime ngayGui;

}
