package com.example.avatar;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:avatar;MODE=MySQL;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","app.admin-password=test-password-123456","app.storage=./target/test-avatars"})
@AutoConfigureMockMvc
class AvatarIntegrationTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;
 @Test void authenticatedUploadBrowseAndDelete() throws Exception {
  mvc.perform(get("/api/admin/photos")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/admin/login").contentType("application/json").content("{\"username\":\"admin\",\"password\":\"wrong\"}")).andExpect(status().isUnauthorized());
  String login=mvc.perform(post("/api/admin/login").contentType("application/json").content("{\"username\":\"admin\",\"password\":\"test-password-123456\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  String auth="Bearer "+json.readTree(login).get("token").asText();
  mvc.perform(multipart("/api/admin/photos").file(new MockMultipartFile("file","fake.png","image/png","not an image".getBytes())).header("Authorization",auth)).andExpect(status().isBadRequest());
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(32,32,BufferedImage.TYPE_INT_RGB),"png",bytes);
  MockMultipartFile photo=new MockMultipartFile("file","test.png","image/png",bytes.toByteArray());
  mvc.perform(multipart("/api/admin/photos").file(photo)).andExpect(status().isUnauthorized());
  String uploaded=mvc.perform(multipart("/api/admin/photos").file(photo).param("title","test-avatar").param("category","动漫").header("Authorization",auth)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  long id=json.readTree(uploaded).get("id").asLong();
  mvc.perform(get("/api/admin/photos").param("keyword","test-avatar").header("Authorization",auth)).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
  mvc.perform(get("/api/photos/"+id+"/image")).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
  mvc.perform(delete("/api/admin/photos/"+id)).andExpect(status().isUnauthorized());
  mvc.perform(delete("/api/admin/photos/"+id).header("Authorization",auth)).andExpect(status().isNoContent());
  mvc.perform(get("/api/photos/"+id+"/image")).andExpect(status().isNotFound());
  mvc.perform(post("/api/admin/logout").header("Authorization",auth)).andExpect(status().isNoContent());
  mvc.perform(get("/api/admin/photos").header("Authorization",auth)).andExpect(status().isUnauthorized());
 }
}
