package iuh.fit.fe.be_websatlichkham.modules.user.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.fe.be_websatlichkham.common.entity.BaseEntity;
import iuh.fit.fe.be_websatlichkham.modules.clinic.entity.Location;
import iuh.fit.fe.be_websatlichkham.modules.user.enums.AdminPermissionLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "admins")
public class Admin extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "permission_level", nullable = false, length = 20)
    private AdminPermissionLevel permissionLevel = AdminPermissionLevel.STAFF;

    /** NULL = quản lý toàn hệ thống; có giá trị = chỉ quản lý 1 cơ sở. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "managed_location_id")
    private Location managedLocation;

}
