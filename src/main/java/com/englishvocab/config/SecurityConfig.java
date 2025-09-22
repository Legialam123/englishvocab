package com.englishvocab.config;

import com.englishvocab.security.CustomUserDetailsService;
import com.englishvocab.security.CustomOAuth2UserService;
import com.englishvocab.security.CustomOidcUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final CustomUserDetailsService userDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;
    
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
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF cho API endpoints
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/auth/api/**")
            )
            
            // Cấu hình authorization
            .authorizeHttpRequests(authz -> authz
                // Cho phép truy cập public endpoints
                .requestMatchers(
                    "/auth/login", 
                    "/auth/register", 
                    "/auth/api/**",
                    "/css/**", 
                    "/js/**", 
                    "/images/**",
                    "/favicon.ico",
                    "/error"
                ).permitAll()
                
                // Yêu cầu quyền ADMIN cho admin endpoints
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                // Yêu cầu authentication cho các endpoints khác
                .anyRequest().authenticated()
            )
            
            // Cấu hình form login
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/auth/login?error=true")
                .usernameParameter("usernameOrEmail")
                .passwordParameter("password")
                .permitAll()
            )
            
            // Cấu hình OAuth2 login
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/auth/login")
                .defaultSuccessUrl("/dashboard", true)
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
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            )
            
            // Cấu hình remember-me
            .rememberMe(remember -> remember
                .key("englishvocab-remember-me")
                .tokenValiditySeconds(7 * 24 * 60 * 60) // 7 ngày
                .userDetailsService(userDetailsService)
            );
        
        return http.build();
    }
}
