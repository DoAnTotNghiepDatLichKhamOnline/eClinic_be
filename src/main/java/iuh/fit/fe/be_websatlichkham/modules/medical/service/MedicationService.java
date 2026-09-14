package iuh.fit.fe.be_websatlichkham.modules.medical.service;

import java.util.List;

import iuh.fit.fe.be_websatlichkham.modules.medical.entity.Medication;

public interface MedicationService {

    /**
     * Lấy thuốc theo tên (không phân biệt hoa thường / khoảng trắng thừa), chưa có thì tạo mới
     * với {@code verified = false}. An toàn khi nhiều bác sĩ cùng thêm 1 tên thuốc.
     *
     * @param createdByDoctorId bác sĩ đang kê toa, có thể null
     */
    Medication getOrCreate(String name, String unit, Long createdByDoctorId);

    /** Gợi ý thuốc theo từ khoá (tối đa 20). */
    List<Medication> search(String keyword);

}
