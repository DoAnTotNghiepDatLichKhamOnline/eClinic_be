package iuh.fit.se.eclinic.common.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Cho trình duyệt và CDN của Vercel dùng lại response 200 của route GET này trong {@code app.cache-cong-khai.giay} giây
 * (xem {@link CacheCongKhaiAdvice}). CHỈ đặt cho route trả cùng 1 kết quả cho mọi người gọi (không phụ thuộc tài khoản,
 * không phụ thuộc khung giờ còn trống): response được đánh dấu {@code public}, nên cache dùng chung sẽ đưa nó cho người
 * khác.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheCongKhai {

    /** Tham số query làm kết quả thay đổi theo thời gian thực (vd lọc theo ngày còn chỗ): có tham số này thì không cache. */
    String[] boQuaKhiCo() default {};

}
