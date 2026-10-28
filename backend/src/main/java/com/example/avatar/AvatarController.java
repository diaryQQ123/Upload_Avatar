package com.example.avatar;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
@RestController
public class AvatarController {
 private final JdbcTemplate jdbc;private final AdminAuth auth;private final Path storage;
 private static final Set<String> CATEGORIES=new HashSet<>(Arrays.asList("动漫","真人","萌宠","风景","其他"));
 public AvatarController(JdbcTemplate jdbc,AdminAuth auth,@Value("${app.storage}") String storage) throws IOException {this.jdbc=jdbc;this.auth=auth;this.storage=Paths.get(storage).toAbsolutePath().normalize();Files.createDirectories(this.storage);}
 @PostMapping("/api/admin/login") public Map<String,Object> login(@RequestBody Map<String,String> body,HttpServletRequest request){return auth.login(body.get("username"),body.get("password"),request.getRemoteAddr());}
 @PostMapping("/api/admin/logout") public ResponseEntity<Void> logout(HttpServletRequest request){auth.logout(request);return ResponseEntity.noContent().build();}
 @GetMapping("/api/admin/photos") public Map<String,Object> list(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="12") int size,@RequestParam(defaultValue="") String keyword,@RequestParam(defaultValue="") String category){
  if(page<1||page>1000000||size<1||size>60||keyword.length()>120)throw bad("分页或搜索参数不正确");
  String where=" WHERE 1=1";List<Object> params=new ArrayList<>();
  // LOCATE 按字面搜索，避免 % 和 _ 被当作通配符。
  if(!keyword.trim().isEmpty()){where+=" AND LOCATE(?,title)>0";params.add(keyword.trim());}
  if(!category.isEmpty()){where+=" AND category=?";params.add(category);}
  Long total=jdbc.queryForObject("SELECT COUNT(*) FROM avatar_photo"+where,Long.class,params.toArray());
  params.add(size);params.add((page-1)*size);
  List<Map<String,Object>> items=jdbc.query("SELECT * FROM avatar_photo"+where+" ORDER BY id DESC LIMIT ? OFFSET ?",(rs,i)->photo(rs),params.toArray());
  Map<String,Object> result=new HashMap<>();result.put("items",items);result.put("total",total);return result;
 }
 private Map<String,Object> photo(ResultSet rs) throws SQLException {Map<String,Object> p=new LinkedHashMap<>();long id=rs.getLong("id");p.put("id",id);p.put("title",rs.getString("title"));p.put("category",rs.getString("category"));p.put("size",rs.getLong("size"));p.put("createdAt",rs.getTimestamp("created_at").toLocalDateTime().toString());p.put("url","/api/photos/"+id+"/image");return p;}
 @PostMapping("/api/admin/photos") public synchronized Map<String,Object> upload(@RequestParam("file") MultipartFile file,@RequestParam(defaultValue="头像") String title,@RequestParam(defaultValue="其他") String category) throws IOException {
  if(file.isEmpty()||file.getSize()>5L*1024*1024)throw bad("请选择不超过5MB的图片");
  if(!CATEGORIES.contains(category))throw bad("分类不正确");
  String name=title.trim();if(name.isEmpty())name="头像";if(name.length()>120)name=name.substring(0,120);
  String format;
  try(ImageInputStream input=ImageIO.createImageInputStream(file.getInputStream())){
   Iterator<ImageReader> readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw bad("仅支持真实的JPG、PNG、GIF图片");ImageReader reader=readers.next();
   try{reader.setInput(input);format=reader.getFormatName().toLowerCase(Locale.ROOT);if(!Arrays.asList("jpeg","jpg","png","gif").contains(format))throw bad("不支持此图片格式");int w=reader.getWidth(0),h=reader.getHeight(0);if(w<1||h<1||(long)w*h>25000000)throw bad("图片尺寸过大，最多2500万像素");}finally{reader.dispose();}
  }catch(ResponseStatusException e){throw e;}catch(Exception e){throw bad("图片文件损坏或格式无法识别");}
  String ext=format.equals("jpeg")?"jpg":format;String mime=ext.equals("jpg")?"image/jpeg":"image/"+ext;
  String key=UUID.randomUUID().toString()+"."+ext;Path dest=storage.resolve(key);
  try(InputStream in=file.getInputStream()){Files.copy(in,dest);}
  final String finalTitle=name;KeyHolder holder=new GeneratedKeyHolder();
  try{jdbc.update(connection->{PreparedStatement st=connection.prepareStatement("INSERT INTO avatar_photo(title,category,storage_key,mime_type,size) VALUES(?,?,?,?,?)",new String[]{"id"});st.setString(1,finalTitle);st.setString(2,category);st.setString(3,key);st.setString(4,mime);st.setLong(5,file.getSize());return st;},holder);}catch(RuntimeException e){Files.deleteIfExists(dest);throw e;}
  return jdbc.queryForObject("SELECT * FROM avatar_photo WHERE id=?",(rs,i)->photo(rs),Objects.requireNonNull(holder.getKey()).longValue());
 }
 @DeleteMapping("/api/admin/photos/{id}") public synchronized ResponseEntity<Void> delete(@PathVariable long id) throws IOException {
  List<String> keys=jdbc.query("SELECT storage_key FROM avatar_photo WHERE id=?",(rs,i)->rs.getString(1),id);if(keys.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"图片已不存在");
  Path file=storage.resolve(keys.get(0)),trash=storage.resolve(".trash-"+UUID.randomUUID());boolean moved=Files.exists(file);
  if(moved)Files.move(file,trash);
  try{jdbc.update("DELETE FROM avatar_photo WHERE id=?",id);}catch(RuntimeException e){if(moved)Files.move(trash,file);throw e;}
  // 数据库删除成功后再清理文件；清理失败的隐藏垃圾文件可在维护时删除，不对外提供访问。
  try{Files.deleteIfExists(trash);}catch(IOException e){System.err.println("Pending trash cleanup: "+trash.getFileName());}
  return ResponseEntity.noContent().build();
 }
 // 公开预览：符合未登录也能浏览的需求。上传、查询管理列表和删除仍须管理员登录。
 @GetMapping("/api/photos/{id}/image") public ResponseEntity<FileSystemResource> image(@PathVariable long id){
  List<Map<String,Object>> rows=jdbc.queryForList("SELECT storage_key,mime_type FROM avatar_photo WHERE id=?",id);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"图片不存在");
  Map<String,Object> row=rows.get(0);Path file=storage.resolve((String)row.get("storage_key")).normalize();if(!file.startsWith(storage)||!Files.isRegularFile(file))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"图片文件不存在");
  return ResponseEntity.ok().contentType(MediaType.parseMediaType((String)row.get("mime_type"))).header("X-Content-Type-Options","nosniff").cacheControl(CacheControl.noStore()).body(new FileSystemResource(file));
 }
 private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}
