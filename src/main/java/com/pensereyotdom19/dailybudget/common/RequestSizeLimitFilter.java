package com.pensereyotdom19.dailybudget.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Refuses request bodies over 64 KB. The biggest real request (a bill with all its months) is
 * under 2 KB, so this only stops someone trying to tire out the free server with huge uploads,
 * also on the sign-in endpoints that need no token.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestSizeLimitFilter extends OncePerRequestFilter {
    static final int MAX_BYTES = 64 * 1024;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > MAX_BYTES) {
            response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Request too large");
            return;
        }
        // A body without a length (chunked) is counted while it is read.
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override
            public ServletInputStream getInputStream() throws IOException {
                ServletInputStream in = super.getInputStream();
                return new ServletInputStream() {
                    private long read;

                    @Override
                    public int read() throws IOException {
                        int b = in.read();
                        if (b >= 0 && ++read > MAX_BYTES) throw new IOException("Request too large");
                        return b;
                    }

                    @Override
                    public int read(byte[] buf, int off, int len) throws IOException {
                        int n = in.read(buf, off, len);
                        if (n > 0 && (read += n) > MAX_BYTES) throw new IOException("Request too large");
                        return n;
                    }

                    @Override public boolean isFinished() { return in.isFinished(); }
                    @Override public boolean isReady() { return in.isReady(); }
                    @Override public void setReadListener(ReadListener listener) { in.setReadListener(listener); }
                };
            }
        }, response);
    }
}
