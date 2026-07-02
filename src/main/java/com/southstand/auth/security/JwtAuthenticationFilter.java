package com.southstand.auth.security;

import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final SecurityResponseWriter responseWriter;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, SecurityResponseWriter responseWriter) {
        this.jwtTokenService = jwtTokenService;
        this.responseWriter = responseWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (StringUtils.hasText(token)) {
                LoginUserContext userContext = jwtTokenService.parse(token);
                CurrentUserHolder.set(userContext);
                JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                        userContext,
                        List.of(new SimpleGrantedAuthority("ROLE_" + userContext.getRoleType()))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
        } catch (BusinessException ex) {
            SecurityContextHolder.clearContext();
            responseWriter.write(response, ex.getErrorCode());
        } finally {
            CurrentUserHolder.clear();
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization)) {
            return null;
        }
        if (!authorization.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return authorization.substring(7).trim();
    }
}
