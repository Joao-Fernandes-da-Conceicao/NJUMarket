# Docker容器中文日志编码配置指南

## 问题背景

在WSL + Docker环境下，如果容器内没有正确配置字符编码，中文日志可能会出现乱码问题（UTF-8 ↔ GBK转换错误）。

## 解决方案

### 1. Dockerfile配置 ✅ 已修复

在Dockerfile中设置：
- 容器locale为 `C.UTF-8`（Alpine Linux默认支持，无需额外安装）
- Java文件编码为UTF-8（通过环境变量和启动参数）
- 时区设置为Asia/Shanghai

**关键配置**：
```dockerfile
ENV LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai

ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-Duser.timezone=Asia/Shanghai", "-jar", "app.jar"]
```

### 2. docker-compose.yml配置 ✅ 已修复

在docker-compose.yml中为每个服务添加环境变量：
- `LANG=C.UTF-8` - 系统locale
- `LC_ALL=C.UTF-8` - 所有locale类别
- `JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai` - Java编码和时区

### 3. WSL终端配置（可选）

如果WSL终端显示仍有问题，可以设置：
```bash
# 在 ~/.bashrc 或 ~/.zshrc 中添加
export LANG=zh_CN.UTF-8
export LC_ALL=zh_CN.UTF-8
```

或者在Windows Terminal中设置：
- 设置 → 外观 → 字体 → 选择支持中文的字体（如 "Cascadia Code" 或 "Microsoft YaHei Mono"）

## 修复内容

✅ **已修复**：
1. `Dockerfile` - 添加了UTF-8编码环境变量和Java启动参数
2. `docker-compose.yml` - 为所有8个服务添加了编码环境变量：
   - discovery
   - gateway
   - auth-service
   - commodity-service
   - order-service
   - message-service
   - image-service
   - admin-service
   - notification-service

## 验证方法

重启容器后，可以通过以下方式验证：

1. **查看容器环境变量**：
```bash
docker exec njumarket-service-notification env | grep -E "LANG|LC_ALL|JAVA_TOOL"
```

2. **测试中文日志**：
在代码中添加中文日志，查看是否正常显示：
```java
log.info("测试中文日志：用户登录成功");
```

3. **查看容器日志**：
```bash
docker logs njumarket-service-notification | grep "测试中文"
```

## 注意事项

1. **Alpine Linux特性**：
   - Alpine Linux默认支持 `C.UTF-8`，无需安装额外的locale包
   - 如果使用其他基础镜像（如 `ubuntu`），可能需要安装 `locales` 包

2. **WSL终端编码**：
   - WSL默认使用UTF-8，通常不需要额外配置
   - 如果仍有乱码，检查Windows Terminal的字体设置

3. **Docker日志驱动**：
   - Docker默认使用 `json-file` 日志驱动，支持UTF-8
   - 如果使用其他日志驱动（如 `syslog`），需要确保驱动支持UTF-8

## 预期效果

配置完成后，中文日志应该能够：
- ✅ 在容器内正常显示
- ✅ 通过 `docker logs` 正常显示
- ✅ 在WSL终端中正常显示
- ✅ 不会出现UTF-8 ↔ GBK乱码问题

