package iuh.fit.se.eclinic.gateway.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * Ghi header Server-Timing "gw;dur=<ms>": thời gian từ lúc gateway nhận request tới lúc bắt đầu trả response (gồm cả
 * service phía sau). Trình duyệt hiện số này trong DevTools; phần còn lại của thời gian chờ là mạng, Vercel, Caddy.
 */
public class ThoiGianXuLyFilter extends OncePerRequestFilter {

    static final String TEN_HEADER = "Server-Timing";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        GhiThoiGian boc = new GhiThoiGian(response, System.nanoTime());
        try {
            filterChain.doFilter(request, boc);
        } finally {
            // Response không có body (204, 304...) chưa commit thì ghi ở đây
            boc.ghiHeader();
        }
    }

    /** Thêm header đúng 1 lần, ngay trước khi response commit (header không đổi được sau đó). */
    static final class GhiThoiGian extends HttpServletResponseWrapper {

        private final long batDau;
        private boolean daGhi;

        GhiThoiGian(HttpServletResponse response, long batDau) {
            super(response);
            this.batDau = batDau;
        }

        void ghiHeader() {
            if (daGhi || isCommitted()) {
                return;
            }
            daGhi = true;
            double ms = (System.nanoTime() - batDau) / 1_000_000.0;
            addHeader(TEN_HEADER, String.format(Locale.ROOT, "gw;dur=%.1f", ms));
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            ghiHeader();
            return super.getOutputStream();
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            ghiHeader();
            return super.getWriter();
        }

        @Override
        public void flushBuffer() throws IOException {
            ghiHeader();
            super.flushBuffer();
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            ghiHeader();
            super.sendError(sc, msg);
        }

        @Override
        public void sendError(int sc) throws IOException {
            ghiHeader();
            super.sendError(sc);
        }

        @Override
        public void sendRedirect(String location) throws IOException {
            ghiHeader();
            super.sendRedirect(location);
        }
    }

}
