package iuh.fit.se.eclinic.common.entity.chatbot;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.se.eclinic.common.entity.CreatableEntity;
import iuh.fit.se.eclinic.common.enums.LoaiTinNhanChat;
import iuh.fit.se.eclinic.common.enums.NguoiGui;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Tin nhắn trong phiên chat. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tin_nhan_chat")
@AttributeOverride(name = "id", column = @Column(name = "id_tin_nhan"))
public class TinNhanChat extends CreatableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_phien_chat", nullable = false)
    private PhienChat phienChat;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "nguoi_gui", nullable = false, length = 20)
    private NguoiGui nguoiGui;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "loai", nullable = false, length = 20)
    private LoaiTinNhanChat loai;

    @Column(name = "noi_dung", nullable = false, columnDefinition = "TEXT")
    private String noiDung;

    /** Gợi ý kèm câu trả lời của chatbot (JSON của {@code GoiYResponse}); null = không có. */
    @Column(name = "goi_y", columnDefinition = "TEXT")
    private String goiY;

}
