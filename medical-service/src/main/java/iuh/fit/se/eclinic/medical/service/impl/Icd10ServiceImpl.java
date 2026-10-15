package iuh.fit.se.eclinic.medical.service.impl;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import iuh.fit.se.eclinic.common.entity.medical.Icd10;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.util.MauTimKiem;
import iuh.fit.se.eclinic.medical.repository.Icd10Repository;
import iuh.fit.se.eclinic.medical.service.Icd10Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class Icd10ServiceImpl implements Icd10Service {

    private static final int SO_GOI_Y_TOI_DA = 20;

    private final Icd10Repository icd10Repository;

    @Override
    public Icd10 layDeGhi(String ma) {
        String chuan = ma.trim().toUpperCase(Locale.ROOT);
        return icd10Repository.findById(chuan)
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.maIcd", chuan));
    }

    @Override
    public List<Icd10> timKiem(String tuKhoa) {
        if (!StringUtils.hasText(tuKhoa)) {
            return List.of();
        }
        String gon = tuKhoa.trim().replaceAll("\s+", " ");
        return icd10Repository.goiY(MauTimKiem.batDau(gon), MauTimKiem.chua(gon), PageRequest.of(0, SO_GOI_Y_TOI_DA));
    }

}
