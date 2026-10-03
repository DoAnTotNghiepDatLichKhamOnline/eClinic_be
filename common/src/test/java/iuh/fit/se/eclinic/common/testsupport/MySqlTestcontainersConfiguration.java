package iuh.fit.se.eclinic.common.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * MySQL thật chạy bằng Docker cho test (cùng image với docker-compose.yml).
 * Dùng: {@code @Import(MySqlTestcontainersConfiguration.class)} trên class test của service.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MySqlTestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        // Cùng charset/collation với docker-compose.yml: so sánh chuỗi không phân biệt hoa/thường và dấu
        return new MySQLContainer(DockerImageName.parse("mysql:8.4"))
                .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci");
    }

}
