# 多阶段构建 Dockerfile（通用模板）
# 使用构建参数指定服务名称

# 第一阶段：构建
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# 复制 pom.xml 文件（利用 Docker 缓存层）
COPY pom.xml .
COPY njumarket-common/pom.xml ./njumarket-common/
COPY njumarket-discovery/pom.xml ./njumarket-discovery/
COPY njumarket-config/pom.xml ./njumarket-config/
COPY njumarket-gateway/pom.xml ./njumarket-gateway/
COPY njumarket-service-auth/pom.xml ./njumarket-service-auth/
COPY njumarket-service-commodity/pom.xml ./njumarket-service-commodity/
COPY njumarket-service-order/pom.xml ./njumarket-service-order/
COPY njumarket-service-message/pom.xml ./njumarket-service-message/
COPY njumarket-service-image/pom.xml ./njumarket-service-image/
COPY njumarket-service-admin/pom.xml ./njumarket-service-admin/
COPY njumarket-service-notification/pom.xml ./njumarket-service-notification/

# 下载依赖（利用缓存）
RUN mvn dependency:go-offline -B

# 复制源代码
COPY . .

# 构建参数：服务名称
ARG SERVICE_NAME
ARG SERVICE_PORT

# 构建指定服务（跳过测试以加快构建速度）
RUN mvn clean package -pl ${SERVICE_NAME} -am -DskipTests -B

# 第二阶段：运行
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# ✅ 安装必要工具和时区数据
RUN apk add --no-cache curl tzdata

# ✅ 设置环境变量（确保中文日志正常显示）
# Alpine Linux默认支持C.UTF-8，无需额外安装locale包
ENV LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai

# 创建非 root 用户
RUN addgroup -S spring && adduser -S spring -G spring

# 声明构建参数（需要在 FROM 之后重新声明）
ARG SERVICE_NAME
ARG SERVICE_PORT=8080

# 从构建阶段复制 JAR 文件（需要在 USER 之前，因为 COPY 需要 root 权限）
COPY --from=build /app/${SERVICE_NAME}/target/*.jar app.jar

# 切换到非 root 用户
USER spring:spring

# 暴露端口
EXPOSE ${SERVICE_PORT}

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${SERVICE_PORT}/actuator/health || exit 1

# ✅ 启动应用（显式指定UTF-8编码）
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-Duser.timezone=Asia/Shanghai", "-jar", "app.jar"]
