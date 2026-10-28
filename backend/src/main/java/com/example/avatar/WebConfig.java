package com.example.avatar;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
@Configuration
public class WebConfig implements WebMvcConfigurer {
 private final AdminAuth auth;private final String[] origins;
 public WebConfig(AdminAuth auth,@Value("${app.allowed-origins}") String origins){this.auth=auth;this.origins=origins.split(",");}
 public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(auth).addPathPatterns("/api/admin/**").excludePathPatterns("/api/admin/login");}
 public void addCorsMappings(CorsRegistry registry){registry.addMapping("/api/**").allowedOrigins(origins).allowedMethods("GET","POST","DELETE","OPTIONS").allowedHeaders("Authorization","Content-Type").maxAge(3600);}
}
