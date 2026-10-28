# 头像相册管理后台 · uni-app + Spring Boot

这是管理人员使用的后台：上传、预览、搜索、分类、分页、单张/批量删除头像。
管理界面采用 uni-app / Vue 3，优先以 H5 在电脑浏览器中使用，也有移动端布局。
后端采用 Spring Boot 2.7.18 + JDBC，生产配置连接 MySQL 8；兼容 Java 8，验证环境为 Java 17。

## 一、目录

```text
frontend/                     uni-app 管理界面
  src/pages/index/index.vue   页面、样式和交互
  src/common/api.js           后端地址、请求和上传封装
  src/manifest.json          平台配置（微信 AppID 留空）
  package.json               启动和构建命令
backend/                      Spring Boot 后端
  src/main/resources/application.yml  数据库、端口、文件目录配置
  src/main/resources/schema.sql       启动时自动建头像表
  src/main/java/com/example/avatar/   登录、文件、数据库接口
  src/test/                   接口集成测试
```

## 二、先准备环境

1. 安装 Node.js 20 或 22、JDK 8 或 17、Maven 3.9、MySQL 8。
2. 解压整个项目。前后端需要分别启动，不能直接双击 Vue 文件运行。
3. 可以使用 IDEA 打开 `backend/pom.xml`；使用 HBuilderX 或 VS Code 打开 `frontend`。

## 三、创建数据库

在 Navicat 或 MySQL 客户端执行：

```sql
CREATE DATABASE IF NOT EXISTS avatar_admin
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

无需手动建表，后端启动后执行 `schema.sql`。
数据库存储图片名称、分类、文件标识、大小和创建时间；实际图片保存到文件目录。

## 四、启动后端（Windows PowerShell）

打开一个终端，进入解压后的 `backend` 文件夹：

```powershell
$env:DB_USER='root'
$env:DB_PASSWORD='替换成你的MySQL密码'
$env:ADMIN_USERNAME='admin'
$env:ADMIN_PASSWORD='替换成至少12位的管理员密码'
$env:AVATAR_STORAGE='D:/avatar-uploads'
mvn spring-boot:run
```

密码没有内置默认值，必须自行设置；登录时使用上面设置的账号和密码。
看到 `Started AvatarApplication` 后端即已启动，端口为 `9090`。
如果使用 IDEA，在运行配置的 Environment variables 中填入以上环境变量，再运行 `AvatarApplication`。
`AVATAR_STORAGE` 可以自选绝对目录。请保留文件目录和数据库；重启不会清空图片。

macOS/Linux 对应使用：

```bash
export DB_USER=root
export DB_PASSWORD='你的MySQL密码'
export ADMIN_USERNAME=admin
export ADMIN_PASSWORD='替换成至少12位的管理员密码'
export AVATAR_STORAGE='/absolute/path/to/avatar-uploads'
mvn spring-boot:run
```

## 五、启动管理界面

新开一个终端，进入 `frontend` 文件夹：

```bash
npm ci
npm run dev:h5
```

浏览器打开 `http://localhost:5173`，用第四步的管理员账号登录。
如使用 HBuilderX，打开 `frontend` CLI 项目，先完成依赖安装，再运行到浏览器。

使用顺序：

1. 登录后台，点击“上传头像”。
2. 选择分类，再选择图片（每批最多 9 张）。
3. 点击“开始上传”，查看每张图片结果；成功项不会重复上传，失败项可以重试或移除。
4. 点击图库中的图片查看大图。
5. 搜索名称或筛选分类；点击“搜索”更新结果。
6. 单张点“删除”；也可以选中多张后点“删除所选”，确认后才删除。
7. 删除会同步删除数据库记录和实际文件。备份需同时保存数据库和文件目录。

支持 JPG / PNG / GIF，单张最多 5 MB，最多 2500 万像素；GIF 原文件保存，不转成静态图片。
管理端只显示真实后端数据，没有内置演示图片。

## 六、地址和分类在哪里改

- API 地址：`frontend/src/common/api.js` 的 `API_BASE`。
- 端口、数据库、保存目录：`backend/src/main/resources/application.yml`，或相应环境变量。
- 分类：前端 `uploadCategories` 和后端 `AvatarController.CATEGORIES`，两处一起修改。
- 样式：`frontend/src/pages/index/index.vue` 的 `<style scoped>`。
- 分页：前端为每页 12 张，接口上限为 60 张。

真机访问时，`localhost` 指手机自身。将 `API_BASE` 改为电脑局域网 IP，并开放 9090 端口。
H5 开发允许来源默认为 localhost/127.0.0.1 的 5173、8080 端口。如果使用其他访问域名或端口，配置后端，例如：

```powershell
$env:ALLOWED_ORIGINS='http://192.168.1.10:5173,https://admin.example.com'
```

正式部署时前端和后端均使用 HTTPS。构建前修改 `API_BASE` 为正式接口地址，再运行：

```bash
npm run build:h5
```

将 `frontend/dist/build/h5` 部署至静态网页服务器。当前使用 hash 路由。
后台必须设置独立管理员密码；令牌有效期 8 小时，重启后端后需要重新登录。

## 七、与用户端小程序的关系

本项目是管理后台；不包含微信用户登录、收藏、点赞和下载历史，这些属于下一阶段的用户端。
已预留公开图片预览接口，符合用户未登录也能看图的需求。这个版本还没有公开的相册列表接口，也没有做微信用户授权。
目前公开图片地址本身可直接读取。以后若要控制原图下载，应另做缩略图/原图分离和下载鉴权；仅在前端显示登录弹窗不能限制访问原图。
不要把管理员令牌用于普通用户小程序。

包含微信平台编译脚本，但本次交付重点是 H5 后台；如需运行到微信开发者工具，请先在 `src/manifest.json` 配置自己的 AppID，再配置请求/上传/下载合法域名并做真机验证。

## 八、接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/admin/login` | JSON：username、password；返回 token |
| POST | `/api/admin/logout` | 注销当前 token |
| GET | `/api/admin/photos?page=1&size=12&keyword=&category=` | 图片列表，返回 items 和 total |
| POST | `/api/admin/photos` | multipart：file、title、category |
| DELETE | `/api/admin/photos/{id}` | 删除头像记录和文件 |
| GET | `/api/photos/{id}/image` | 公开查看图片 |

除登录和公开图片预览外，接口均要求 `Authorization: Bearer <token>`。
删除后返回 204；未登录返回 401；文件过大返回 413。
批量上传和删除由前端按顺序调用单文件接口，失败时提示实际完成数量。

## 九、验证

```bash
# frontend
npm run build:h5

# backend（测试使用独立 H2 内存库，不需要你的 MySQL）
mvn test
```

集成测试验证未登录拦截、错误密码、伪造图片拒绝、真实 PNG 上传、列表查询、公开预览、删除、删除后不可访问、退出令牌失效。
具体执行结果见 `VERIFICATION.md`。不要把测试用的账号密码用于部署。

## 十、当前实现边界

适合单实例后台原型，单个管理员账号通过环境变量设置；不包含多管理员角色管理、相册分组维护、编辑名称、微信用户管理或对象存储。
图片分类是预设分类。不要删除 `data` 文件夹来“清缓存”。
删除采用先移至隐藏临时文件、再删除数据库记录、最后清理文件的流程；常规数据库错误会移回文件。如果进程在移动文件与数据库操作之间突然崩溃，需从备份或隐藏临时文件恢复，当前没有分布式文件事务。
Spring Boot 2.7 分支是为了沿用既有 Java 8 技术路线；正式长期部署前应评估升级到受维护版本，并按部署环境补充访问控制与备份策略。
