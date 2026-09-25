package iuh.fit.fe.be_websatlichkham.common.entity.chatbot;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.fe.be_websatlichkham.common.entity.BaseEntity;
import iuh.fit.fe.be_websatlichkham.common.entity.identity.TaiKhoan;
import iuh.fit.fe.be_websatlichkham.common.enums.TrangThaiPhienChat;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Phiên chat với chatbot (ERD: PhienChat — CHAT-01). Khách và Bệnh nhân dùng được, không cần đăng nhập.
 * <p>
 * Khác ERD: {@code taiKhoan} cho phép NULL (Khách); thêm {@code maDinhDanhKhach} — định danh phía client
 * để Khách chưa đăng nhập mở lại phiên chat.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "phien_chat")
@AttributeOverride(name = "id", column = @Column(name = "id_phien_chat"))
public class PhienChat extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tai_khoan")
    private TaiKhoan taiKhoan;

    @Column(name = "ma_dinh_danh_khach", length = 64)
    private String maDinhDanhKhach;

    @Column(name = "thoi_gian_bat_dau", nullable = false, updatable = false)
    private LocalDateTime thoiGianBatDau;

    @Column(name = "thoi_gian_ket_thuc")
    private LocalDateTime thoiGianKetThuc;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "trang_thai", nullable = false, length = 20)
    private TrangThaiPhienChat trangThai = TrangThaiPhienChat.DANG_MO;

    @PrePersist
    void onPrePersist() {
        if (thoiGianBatDau == null) {
            thoiGianBatDau = LocalDateTime.now();
        }
    }

}
