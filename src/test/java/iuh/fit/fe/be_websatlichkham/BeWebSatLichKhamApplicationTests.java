package iuh.fit.fe.be_websatlichkham;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Context load thành công = Flyway chạy được V1__init_schema.sql và Hibernate validate khớp mọi entity.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BeWebSatLichKhamApplicationTests {

    @Test
    void contextLoads() {
    }

}
