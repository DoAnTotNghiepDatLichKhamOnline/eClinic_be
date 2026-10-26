package iuh.fit.se.eclinic.chatbot.service;

/** Người đang gửi tin nhắn, nhìn từ các hàm tra cứu dữ liệu CÁ NHÂN ({@link TraCuuPhongKham}). */
public record NguoiHoi(String token, boolean duocTraCuuCaNhan, boolean daDongY) {

    public static final NguoiHoi KHONG_CO_DU_LIEU_CA_NHAN = new NguoiHoi(null, false, false);

    /** Được đọc dữ liệu cá nhân ngay bây giờ. */
    public boolean duocDoc() {
        return duocTraCuuCaNhan && daDongY;
    }

    /** Không in token ra log dù vô tình. */
    @Override
    public String toString() {
        return "NguoiHoi[duocTraCuuCaNhan=" + duocTraCuuCaNhan + ", daDongY=" + daDongY + "]";
    }

}
