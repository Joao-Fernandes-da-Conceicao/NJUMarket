# Image Service 500错误排查指南

## 问题描述
访问图片接口报500错误：`http://localhost:8080/api/images/avatars/20251026_213124_avatar_user_003_db66921a.png`

## 可能原因

### 1. Image Service 未启动或启动失败
**检查方法**：
- 查看 Image Service 的启动日志
- 确认服务已注册到 Eureka（访问 `http://localhost:8761` 查看）
- 直接访问 Image Service：`http://localhost:8095/api/images/avatars/20251026_213124_avatar_user_003_db66921a.png`

### 2. 图片文件路径问题
**当前配置**：
```yaml
app:
  upload:
    avatar-path: ${AVATAR_UPLOAD_PATH:uploads/avatars}
    commodity-path: ${COMMODITY_UPLOAD_PATH:uploads/commodities}
```

**问题**：使用相对路径 `uploads/avatars`，在不同目录下运行可能导致找不到文件。

**解决方案**：
1. 使用绝对路径（推荐）
2. 或确保在项目根目录下运行服务

### 3. 图片文件不存在
**检查方法**：
- 检查 `uploads/avatars/` 目录是否存在
- 检查文件 `20251026_213124_avatar_user_003_db66921a.png` 是否存在

### 4. 权限问题
**检查方法**：
- 确认应用有读取文件的权限
- 检查文件系统权限

## 快速排查步骤

1. **检查 Image Service 是否启动**
   ```bash
   # 查看进程
   jps | grep ImageServiceApplication
   
   # 或查看端口
   netstat -ano | findstr 8095
   ```

2. **检查 Eureka 注册**
   - 访问 `http://localhost:8761`
   - 查看 `njumarket-service-image` 是否已注册

3. **直接访问 Image Service**
   - 访问 `http://localhost:8095/api/images/avatars/20251026_213124_avatar_user_003_db66921a.png`
   - 如果直接访问也报错，说明是 Image Service 的问题
   - 如果直接访问正常，说明是 Gateway 路由的问题

4. **检查图片文件**
   ```bash
   # Windows
   dir uploads\avatars\20251026_213124_avatar_user_003_db66921a.png
   
   # Linux/Mac
   ls -la uploads/avatars/20251026_213124_avatar_user_003_db66921a.png
   ```

5. **查看 Image Service 日志**
   - 查看启动日志，确认是否有错误
   - 查看运行时日志，确认请求是否到达服务

## 建议的修复方案

### 方案1：使用绝对路径（推荐）
修改 `application.yml`：
```yaml
app:
  upload:
    avatar-path: ${AVATAR_UPLOAD_PATH:D:/软工作业/NJUMarket/njumarket/njumarket-service-image/uploads/avatars}
    commodity-path: ${COMMODITY_UPLOAD_PATH:D:/软工作业/NJUMarket/njumarket/njumarket-service-image/uploads/commodities}
```

### 方案2：使用系统属性目录
修改 `ImageController` 和 `ImageServiceImpl`，使用 `user.dir` 或 `java.io.tmpdir`：
```java
String baseDir = System.getProperty("user.dir");
Path uploadDir = Paths.get(baseDir, "uploads", "avatars");
```

### 方案3：使用配置类统一管理路径
创建 `ImageConfig` 类，统一管理图片路径配置。

## 当前状态
- ✅ Spring Security 已排除
- ✅ Gateway 路由配置正确
- ✅ Controller 实现正确
- ⚠️ 需要确认 Image Service 是否正常启动
- ⚠️ 需要确认图片文件路径是否正确

