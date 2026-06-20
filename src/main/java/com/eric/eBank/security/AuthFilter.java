package com.eric.eBank.security;

import com.eric.eBank.exceptions.CustomAuthenticationEntryPoint;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuthFilter extends OncePerRequestFilter {
    private final TokenService tokenService;
    private final CustomUserDetailsService customUserDetailsService;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = getTokeFromRequest(request);

        if (token != null) {
            String email;
            try {
                email = tokenService.getUserNameFromToken(token);
            }catch (Exception e){
                AuthenticationException authenticationException = new BadCredentialsException(e.getMessage());
                customAuthenticationEntryPoint.commence(request, response, authenticationException);
                return;
            }

            UserDetails authUser = customUserDetailsService.loadUserByUsername(email);
            if (tokenService.isTokenValid(token, authUser)) {
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        authUser, // principal：已驗證的使用者資訊
                        null, // credentials：密碼（已驗證完畢，不需要再存）
                        authUser.getAuthorities() // 使用者的權限清單（ROLE_USER、ROLE_ADMIN 等）
                );
                // 取得 request details（IP、session ID 等）
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // 將驗證成功的使用者資訊放入SecurityContext中，供後續的安全機制使用
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }

        try {
            filterChain.doFilter(request, response);
        }
        catch (Exception e) {
            log.error(e.getMessage());
        }

    }

    private String getTokeFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
