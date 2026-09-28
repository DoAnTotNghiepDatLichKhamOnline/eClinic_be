package iuh.fit.se.eclinic.medical;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import iuh.fit.se.eclinic.common.entity.medical.Thuoc;
import iuh.fit.se.eclinic.common.testsupport.MySqlTestcontainersConfiguration;
import iuh.fit.se.eclinic.medical.service.ThuocService;

@SpringBootTest
@Import(MySqlTestcontainersConfiguration.class)
class ThuocServiceTest {

    @Autowired ThuocService thuocService;

    @Test
    void getOrCreateIsCaseAndWhitespaceInsensitive() {
        String ten = "Paracetamol " + UUID.randomUUID().toString().substring(0, 8);

        Thuoc created = thuocService.layHoacTao("  " + ten + "  ", "viên", null);
        Thuoc again = thuocService.layHoacTao(ten.toUpperCase(), "viên", null);

        assertThat(again.getId()).isEqualTo(created.getId());
        assertThat(created.getTenThuoc()).isEqualTo(ten);
        assertThat(created.isDaXacMinh()).isFalse();
    }

}
