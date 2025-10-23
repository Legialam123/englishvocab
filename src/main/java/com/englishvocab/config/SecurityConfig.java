package com.englishvocab.config;

import com.englishvocab.config.properties.RememberMeProperties;
import com.englishvocab.security.CustomAuthenticationSuccessHandler;
import com.englishvocab.security.CustomOAuth2UserService;
import com.englishvocab.security.CustomOidcUserService;
import com.englishvocab.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import java.time.Duration;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
    
    private static final String[] PUBLIC_ENDPOINTS = new String[] {
        "/",
        "/home",
        "/about",
        "/features",
        "/auth/login",
        "/auth/register",
        "/auth/api/**",
        "/css/**",
        "/js/**",
        "/images/**",
        "/favicon.ico",
        "/error",
        "/error/**"
    };

    private static final String[] ADMIN_ENDPOINTS = {"/admin/**"};

    private static final String[] USER_ENDPOINTS = {"/user/**"};

    private final CustomUserDetailsService userDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;
    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
    private final RememberMeProperties rememberMeProperties;
    
    /**
     * Cấu hình Password Encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    
    /**
     * Cấu hình Authentication Manager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    /**
     * Cấu hình Security Filter Chain
     */
    @Bean
    public SecurityFilterChain filterChain(
        HttpSecurity http,
        SessionRegistry sessionRegistry
    ) throws Exception {
        http
            // Tắt CSRF cho các API (nếu cần)
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/auth/api/**", "/api/**")
            )

            // Cấu hình authorization
            .authorizeHttpRequests(authz -> authz
                // Cho phép truy cập public endpoints
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                
                // Yêu cầu quyền ADMIN cho admin endpoints
                .requestMatchers(ADMIN_ENDPOINTS).hasRole("ADMIN")
                
                // Yêu cầu authentication cho user profile endpoints
                .requestMatchers(USER_ENDPOINTS).authenticated()
                
                // Yêu cầu authentication cho các endpoints khác
                .anyRequest().authenticated()
            )
            
            // Cấu hình form login
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .successHandler(customAuthenticationSuccessHandler)
                .failureUrl("/auth/login?error=true")
                .usernameParameter("usernameOrEmail")
                .passwordParameter("password")
                .permitAll()
            )
            
            // Cấu hình OAuth2 login
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/auth/login")
                .successHandler(customAuthenticationSuccessHandler)
                .failureUrl("/auth/login?error=oauth2")
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                    .oidcUserService(customOidcUserService)
                )
                .permitAll()
            )
            
            // Cấu hình logout
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/auth/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            
            // Cấu hình session management
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(sessionFixation -> sessionFixation.migrateSession())
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
                .sessionRegistry(sessionRegistry)
            )
            
            // Cấu hình remember-me
            .rememberMe(remember -> remember
                .key(rememberMeProperties.getKey())
                .rememberMeCookieName(rememberMeProperties.getCookieName())
                .rememberMeParameter(rememberMeProperties.getParameter())
                .tokenValiditySeconds((int) Duration.ofDays(rememberMeProperties.getValidityDays()).getSeconds())
                .userDetailsService(userDetailsService)
            )

            // (C) Xử lý lỗi 401/403 theo yêu cầu
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> res.sendRedirect("/error/401"))
                .accessDeniedHandler((req, res, e) -> res.sendRedirect("/error/403"))
            );
        
        return http.build();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
