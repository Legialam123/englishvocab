package com.englishvocab.security;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import java.io.IOException;
import java.util.Collection;

@Component
@Slf4j
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
    private final RequestCache requestCache = new HttpSessionRequestCache();
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                      HttpServletResponse response,
                                      Authentication authentication) throws ServletException, IOException {

        String username = resolveUsername(authentication);
        String provider = resolveProvider(authentication);
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        log.info("Login success: user={}, provider={}, roles={}", username, provider, authorities);

        clearAuthenticationAttributes(request);

        // Nếu có SavedRequest (user gõ URL được bảo vệ) thì super sẽ xử lý redirect
        // Nếu không, mình sẽ tự redirect dựa trên role.
        // Lấy request gốc mà user định vào
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest == null) {
            // Không có request cũ → redirect theo role
            String targetUrl = determineTargetUrl(authorities);
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } else {
            // Có request cũ → dùng lại behavior mặc định
            super.onAuthenticationSuccess(request, response, authentication);
        }
        
    }

    private String determineTargetUrl(Collection<? extends GrantedAuthority> authorities) {
        boolean isAdmin = authorities.stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return isAdmin ? "/admin/dashboard" : "/dashboard";
    }

    private String resolveUsername(Authentication authentication) {
    Object principal = authentication.getPrincipal();

    if (principal instanceof CustomUserPrincipal customUser) {
        // Đăng nhập form: ưu tiên username hoặc email (tùy bạn muốn hiển thị gì)
        return customUser.getEmail() != null ? customUser.getEmail() : customUser.getUsername();
    }

    if (principal instanceof OidcUserPrincipal oidcUser) {
        // OIDC: Google trả đầy đủ email
        return oidcUser.getEmail() != null ? oidcUser.getEmail() : oidcUser.getName();
    }

    if (principal instanceof OAuth2UserPrincipal oauth2User) {
        // OAuth2 (không OIDC): đa số Google vẫn trả email; fallback sang sub
        Object emailAttr = oauth2User.getAttributes().get("email");
        return emailAttr != null ? emailAttr.toString() : oauth2User.getName();
    }

    // Mặc định: authentication.getName() (có thể là username hoặc chuỗi ID)
    return authentication.getName();
}

private String resolveProvider(Authentication authentication) {
    Object principal = authentication.getPrincipal();

    if (principal instanceof OidcUserPrincipal) {
        return "GOOGLE_OIDC";
    }

    if (principal instanceof OAuth2UserPrincipal) {
        return "GOOGLE_OAUTH2";
    }

    if (principal instanceof CustomUserPrincipal) {
        return "FORM";
    }

    // fallback cho các provider khác nếu sau này thêm
    return "UNKNOWN";
}
}
