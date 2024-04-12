package moyeora.myapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import moyeora.myapp.security.CustomAuthenticationFailureHandler;
import moyeora.myapp.security.CustomAuthenticationSuccessHandler;
import moyeora.myapp.security.OAuth.PrincipalOauth2UserService;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Log4j2
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
  private final CustomAuthenticationSuccessHandler authenticationSuccessHandler;

  private final CustomAuthenticationFailureHandler authenticationFailureHandler;

  private final AuthenticationDetailsSource<HttpServletRequest, WebAuthenticationDetails> authenticationDetailsSource;

  private final PrincipalOauth2UserService principalOauth2UserService;

  @Bean
  public WebSecurityCustomizer webSecurityCustomizer() {
    return (web) -> web.ignoring()
        .requestMatchers(PathRequest.toStaticResources().atCommonLocations());
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception{
    httpSecurity
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests((authorize) -> authorize
                .anyRequest().permitAll()
        )
        .logout((logout) -> logout
            .logoutRequestMatcher(new AntPathRequestMatcher("/auth/logout"))
            .logoutSuccessUrl("/")
            .invalidateHttpSession(true))
        .formLogin(form -> form
            .loginPage("/auth/form")
            .usernameParameter("email")
            .passwordParameter("password")
            .authenticationDetailsSource(authenticationDetailsSource)
            .loginProcessingUrl("/auth/login")
            .successHandler(authenticationSuccessHandler)
            .failureHandler(authenticationFailureHandler)
            .permitAll()
        )
        .oauth2Login((oauth2) -> oauth2
//            .authorizationEndpoint(endpoint -> endpoint.baseUri("/auth/oauth2"))
            .redirectionEndpoint(endpoint -> endpoint.baseUri("/oauth/callback/*"))
            .userInfoEndpoint(endpoint -> endpoint.userService(principalOauth2UserService))
        )
        .exceptionHandling((exceptionConfig) ->
            exceptionConfig.authenticationEntryPoint(unauthorizedEntryPoint).accessDeniedHandler(accessDeniedHandler)
        )
//        .addFilterBefore((Filter) jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
    ;

    return httpSecurity.build();
  }

  @Bean
  AuthenticationManager authenticationManager(
      AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }

  public final AuthenticationEntryPoint unauthorizedEntryPoint =
      (request, response, authException) -> {
        ErrorResponse fail = new ErrorResponse(HttpStatus.UNAUTHORIZED, "Spring security unauthorized...");
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        String json = new ObjectMapper().writeValueAsString(fail);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        PrintWriter writer = response.getWriter();
        writer.write(json);
        writer.flush();
      };

  public  final AccessDeniedHandler accessDeniedHandler =
      (request, response, accessDeniedException) -> {
        ErrorResponse fail = new ErrorResponse(HttpStatus.FORBIDDEN, "Spring security forbidden...");
        response.setStatus(HttpStatus.FORBIDDEN.value());
        String json = new ObjectMapper().writeValueAsString(fail);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        PrintWriter writer = response.getWriter();
        writer.write(json);
        writer.flush();
      };

  @Getter
  @RequiredArgsConstructor
  public class ErrorResponse {
    private final HttpStatus status;
    private final String message;
  }
}


