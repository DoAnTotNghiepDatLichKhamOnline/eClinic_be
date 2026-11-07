package iuh.fit.se.eclinic.identity;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.RecordApplicationEvents;

import iuh.fit.se.eclinic.common.luutru.LuuTruAnh;
import iuh.fit.se.eclinic.common.testsupport.MySqlTestcontainersConfiguration;
import iuh.fit.se.eclinic.common.testsupport.RedisTestcontainersConfiguration;
import iuh.fit.se.eclinic.identity.client.XacMinhTokenGoogle;

/**
 * Cấu hình chung cho mọi test tích hợp của identity-service (MySQL + Redis thật).
 * Mọi lớp con dùng chung một Spring context: chỉ một lần khởi động container và Flyway.
 * Thêm mock hay thuộc tính riêng vào một lớp con sẽ tách ra context mới, nên đặt ở đây.
 */
@SpringBootTest
@Import({ MySqlTestcontainersConfiguration.class, RedisTestcontainersConfiguration.class })
@RecordApplicationEvents
@TestPropertySource(properties = { "app.dang-nhap.an-han-dung-lai=5s", "app.lien-ket.thoi-gian-cho-gui-lai=5s" })
abstract class KiemThuTichHop {

    @MockitoBean protected LuuTruAnh luuTruAnh;
    @MockitoBean protected XacMinhTokenGoogle xacMinhTokenGoogle;

}
