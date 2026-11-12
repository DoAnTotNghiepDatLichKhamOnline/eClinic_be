package iuh.fit.se.eclinic.catalog.dto.response;

/** Chuyên khoa công khai; anhMinhHoa null = chưa có ảnh. */
public record ChuyenKhoaResponse(Long id, String tenChuyenKhoa, String moTa, String anhMinhHoa) {
}
