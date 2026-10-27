package iuh.fit.se.eclinic.booking.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.booking.service.CauHinhService;
import lombok.RequiredArgsConstructor;

/** Khi khởi động: chưa có cấu hình hệ thống thì ghi từ biến môi trường, có rồi thì cảnh báo giá trị khác. */
@Component
@RequiredArgsConstructor
public class KhoiTaoCauHinhRunner implements ApplicationRunner {

    private final CauHinhService cauHinhService;

    @Override
    public void run(ApplicationArguments args) {
        cauHinhService.khoiTao();
    }

}
