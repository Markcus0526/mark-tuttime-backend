package com.tuttime.admin.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import java.io.IOException;

/** Filter to ensure multipart requests are parsed before reaching controllers. */
@Slf4j
public class MultipartParsingFilter extends OncePerRequestFilter {

    private final StandardServletMultipartResolver multipartResolver = new StandardServletMultipartResolver();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                     @NonNull HttpServletResponse response, 
                                     @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        
        if (multipartResolver.isMultipart(request)) {
            log.debug("Multipart request detected: {}", request.getRequestURI());
            HttpServletRequest wrappedRequest = multipartResolver.resolveMultipart(request);
            try {
                filterChain.doFilter(wrappedRequest, response);
            } finally {
                if (wrappedRequest instanceof MultipartHttpServletRequest) {
                    multipartResolver.cleanupMultipart((MultipartHttpServletRequest) wrappedRequest);
                }
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }
}

