package iuh.fit.se.eclinic.catalog.service;

/** Phát khi xoá 1 chuyên khoa có ảnh minh hoạ, để dọn ảnh riêng của nó trong kho sau khi giao dịch đã commit. */
public record ChuyenKhoaDaXoaEvent(Long idChuyenKhoa, String anhMinhHoa) {
}
