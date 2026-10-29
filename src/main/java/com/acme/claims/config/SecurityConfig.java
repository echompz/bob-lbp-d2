package com.acme.claims.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * HTTP Basic security with three demo roles.
 *
 * <table>
 *   <tr><th>User</th><th>Password</th><th>Roles</th></tr>
 *   <tr><td>adjuster</td><td>adjuster123</td><td>ADJUSTER</td></tr>
 *   <tr><td>manager</td><td>manager123</td><td>ADJUSTER, MANAGER</td></tr>
 *   <tr><td>auditor</td><td>auditor123</td><td>AUDITOR</td></tr>
 * </table>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication()
                .withUser("adjuster").password("adjuster123").roles("ADJUSTER")
                .and()
                .withUser("manager").password("manager123").roles("ADJUSTER", "MANAGER")
                .and()
                .withUser("auditor").password("auditor123").roles("AUDITOR");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .headers().frameOptions().sameOrigin()
            .and()
            .authorizeRequests()
                .antMatchers("/health", "/info", "/h2-console/**",
                        "/swagger-ui.html", "/swagger-resources/**", "/v2/api-docs", "/webjars/**").permitAll()
                .antMatchers(HttpMethod.POST, "/api/claims/*/decision", "/api/claims/*/pay").hasRole("MANAGER")
                .antMatchers("/api/reports/**", "/api/audit/**").hasAnyRole("MANAGER", "AUDITOR")
                .antMatchers(HttpMethod.GET, "/api/**").hasAnyRole("ADJUSTER", "MANAGER", "AUDITOR")
                .antMatchers("/api/**").hasAnyRole("ADJUSTER", "MANAGER")
                .anyRequest().authenticated()
            .and()
            .httpBasic().realmName("ClaimTrack");
    }
}
