package iuh.fit.fe.be_websatlichkham.modules.clinic.entity;

import iuh.fit.fe.be_websatlichkham.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cơ sở khám bệnh.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "locations")
public class Location extends AuditableEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

}
