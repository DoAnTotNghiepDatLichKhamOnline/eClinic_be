package iuh.fit.se.eclinic.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Sinh token ngẫu nhiên (liên kết email, refresh token, mã phiếu khám) và băm SHA-256. Token liên kết / refresh token
 * chỉ lưu bản băm; mã phiếu khám lưu nguyên vì phải tra được theo chính nó.
 */
public final class TokenNgauNhien {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SO_BYTE = 32;

    private TokenNgauNhien() {
    }

    /** 256 bit ngẫu nhiên, base64url không padding (43 ký tự, dùng thẳng trên URL). */
    public static String tao() {
        byte[] bytes = new byte[SO_BYTE];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 hex của token gốc. */
    public static String bam(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

}
