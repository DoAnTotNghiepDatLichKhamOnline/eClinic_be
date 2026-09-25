package iuh.fit.fe.be_websatlichkham.medical;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import iuh.fit.fe.be_websatlichkham.common.entity.medical.Thuoc;
import iuh.fit.fe.be_websatlichkham.common.testsupport.MySqlTestcontainersConfiguration;
import iuh.fit.fe.be_websatlichkham.medical.service.ThuocService;

@SpringBootTest
@Import(MySqlTestcontainersConfiguration.class)
class ThuocServiceTest {

    @Autowired ThuocService thuocService;

    @Test
    void getOrCreateIsCaseAndWhitespaceInsensitive() {
        String ten = "Paracetamol " + UUID.randomUUID().toString().substring(0, 8);

        Thuoc created = thuocService.getOrCreate("  " + ten + "  ", "viên", null);
        Thuoc again = thuocService.getOrCreate(ten.toUpperCase(), "viên", null);

        assertThat(again.getId()).isEqualTo(created.getId());
        assertThat(created.getTenThuoc()).isEqualTo(ten);
        assertThat(created.isDaXacMinh()).isFalse();
    }

}
