package com.example.avatar;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;
@Component
public class AdminAuth implements HandlerInterceptor {
 private final String username, password;
 private final Map<String, Long> tokens = new ConcurrentHashMap<>();
 private final Map<String, long[]> attempts = new ConcurrentHashMap<>();
 public AdminAuth(@Value("${app.admin-username}") String username, @Value("${app.admin-password}") String password) {
  if (password == null || password.length() < 12) throw new IllegalArgumentException("ADMIN_PASSWORD must contain at least 12 characters");
  this.username=username; this.password=password;
 }
 public synchronized Map<String,Object> login(String user, String pass, String ip) {
  long now=Instant.now().getEpochSecond();
  attempts.entrySet().removeIf(e -> e.getValue()[1] < now);
  tokens.entrySet().removeIf(e -> e.getValue() < now);
  long[] attempt=attempts.computeIfAbsent(ip, k -> new long[]{0,now+900});
  if (attempt[0]>=10) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"尝试过多，请15分钟后重试");
  if(!equal(username,user) || !equal(password,pass)) {attempt[0]++;throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"账号或密码错误");}
  attempts.remove(ip);
  String token=UUID.randomUUID().toString()+UUID.randomUUID().toString();
  tokens.put(token,now+28800);
  Map<String,Object> result=new HashMap<>();result.put("token",token);result.put("expiresIn",28800);return result;
 }
 private boolean equal(String a,String b) {return b!=null&&MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8));}
 public void logout(HttpServletRequest request){tokens.remove(bearer(request));}
 private String bearer(HttpServletRequest request){String header=request.getHeader("Authorization");return header!=null&&header.startsWith("Bearer ")?header.substring(7):"";}
 @Override public boolean preHandle(HttpServletRequest request, javax.servlet.http.HttpServletResponse response,Object handler) {
  if("OPTIONS".equals(request.getMethod())) return true;
  String token=bearer(request);Long expiry=tokens.get(token);
  if(expiry==null||expiry<Instant.now().getEpochSecond()){tokens.remove(token);throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"登录已失效，请重新登录");}
  return true;
 }
}
