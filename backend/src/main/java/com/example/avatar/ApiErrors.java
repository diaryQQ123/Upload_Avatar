package com.example.avatar;
import java.util.Collections;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
@RestControllerAdvice
public class ApiErrors {
 private final Logger log=LoggerFactory.getLogger(ApiErrors.class);
 @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<Map<String,String>> status(ResponseStatusException e){return ResponseEntity.status(e.getStatus()).body(Collections.singletonMap("message",e.getReason()==null?"请求失败":e.getReason()));}
 @ExceptionHandler(MaxUploadSizeExceededException.class) public ResponseEntity<Map<String,String>> size(Exception e){return ResponseEntity.status(413).body(Collections.singletonMap("message","图片不能超过5MB"));}
 @ExceptionHandler({org.springframework.web.bind.MissingServletRequestParameterException.class,org.springframework.web.multipart.support.MissingServletRequestPartException.class,org.springframework.http.converter.HttpMessageNotReadableException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
 public ResponseEntity<Map<String,String>> bad(Exception e){return ResponseEntity.badRequest().body(Collections.singletonMap("message","请求参数不正确"));}
 @ExceptionHandler(Exception.class) public ResponseEntity<Map<String,String>> other(Exception e){log.error("Avatar request failed",e);return ResponseEntity.status(500).body(Collections.singletonMap("message","服务暂时不可用，请检查后端日志"));}
}
