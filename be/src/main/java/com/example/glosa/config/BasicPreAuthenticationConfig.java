package com.example.glosa.config;

import java.security.Principal;
import java.util.Base64;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;

/**
 * Spring Security config which populates the security context with information
 * from an existing Basic authorization header without any validation
 * (the actual validation can e.g. be performed by an Nginx reverse proxy which
 * then just forwards this request including the validated authorization header)
 */
@Configuration
class BasicPreAuthenticationConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.addFilterBefore(new HeaderPseudoAuthenticationFilter(), AbstractPreAuthenticatedProcessingFilter.class);
        http.csrf().disable();
        return http.build();
    }

    static class HeaderPseudoAuthenticationFilter extends AbstractPreAuthenticatedProcessingFilter {
        HeaderPseudoAuthenticationFilter() {
            setAuthenticationManager(
                    new AuthenticationManager() {
                        @Override
                        public Authentication authenticate(Authentication authentication)
                                throws AuthenticationException {
                            authentication.setAuthenticated(true);
                            // tbd: implement getDetails to provide pre-authentication source
                            return authentication;
                        }
                    });
        }

        @AllArgsConstructor
        static class PreAuthPrincipal implements Principal {
            private String name;

            @Override
            public String getName() {
                return name;
            }
        }

        @Override
        protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
            String authorizationHeader = request.getHeader("authorization");
            if (!StringUtils.hasText(authorizationHeader))
                throw new IllegalStateException("user header not set");
            String credentials = authorizationHeader.split("Basic ")[1];
            String username = new String(Base64.getDecoder().decode(credentials)).split(":")[0];
            return new PreAuthPrincipal(username);
        }

        @Override
        protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
            return "null";
        }
    }

}