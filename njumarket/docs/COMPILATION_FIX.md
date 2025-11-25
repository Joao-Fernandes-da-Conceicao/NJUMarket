# 编译错误修复说明

## 问题描述

编译时出现以下错误：
```
cannot find symbol: class EmbeddingClient
location: package org.springframework.ai.embedding
```

## 原因

`njumarket-service-auth` 模块缺少 Spring AI 核心依赖，导致 `EmbeddingClient` 类无法找到。

## 已修复

已在 `njumarket-service-auth/pom.xml` 中添加了以下依赖：

```xml
<!-- Spring AI - Core (包含 EmbeddingClient) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-core</artifactId>
</dependency>
<!-- Spring AI - OpenAI Embedding (用于用户画像向量化) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>
```

## 验证修复

重新构建项目：

```bash
# 清理并重新构建
mvn clean install

# 或仅编译 auth 服务
mvn clean compile -pl njumarket-service-auth -am
```

## 如果问题仍然存在

### 1. 清理 Maven 本地仓库缓存

```bash
# 删除 Spring AI 相关依赖的缓存
rm -rf ~/.m2/repository/org/springframework/ai

# Windows
rmdir /s /q %USERPROFILE%\.m2\repository\org\springframework\ai
```

### 2. 强制更新依赖

```bash
mvn clean install -U
```

### 3. 检查依赖是否正确下载

```bash
mvn dependency:tree -pl njumarket-service-auth | grep spring-ai
```

应该看到：
- `spring-ai-core`
- `spring-ai-openai-spring-boot-starter`

### 4. 验证 Spring Milestone 仓库配置

确保父 POM (`pom.xml`) 中已添加 Spring Milestone 仓库（参考 `SPRING_AI_DEPENDENCY_FIX.md`）。

## 相关文件

- `njumarket-service-auth/pom.xml` - Auth 服务依赖配置
- `pom.xml` - 父 POM，包含 Spring AI BOM 和仓库配置

