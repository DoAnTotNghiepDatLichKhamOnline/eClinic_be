package iuh.fit.fe.be_websatlichkham.modules.user.entity;

import java.time.LocalDate;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.fe.be_websatlichkham.common.entity.AuditableEntity;
import iuh.fit.fe.be_websatlichkham.modules.user.enums.Gender;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Hồ sơ bệnh nhân.
 * <ul>
 *   <li>Bệnh nhân có tài khoản: {@code user != null}, họ tên/SĐT lấy từ {@link User}.</li>
 *   <li>Khách vãng lai (lễ tân tạo hộ): {@code user == null}, bắt buộc {@code fullName} + {@code phone}
 *       (DB CHECK ck_patients_guest_info). Khi khách đăng ký tài khoản sau này thì gắn {@code user} vào.</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "patients")
public class Patient extends AuditableEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "gender", length = 10)
    private Gender gender;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "insurance_number", length = 50)
    private String insuranceNumber;

    public boolean isGuest() {
        return user == null;
    }

}
