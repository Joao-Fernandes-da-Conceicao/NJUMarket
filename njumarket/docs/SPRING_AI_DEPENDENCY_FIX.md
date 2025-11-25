# Spring AI 依赖问题修复说明

## 问题描述

构建时出现以下错误：
```
Could not find artifact org.springframework.ai:spring-ai-bom:pom:1.0.0-M4 in central
```

## 原因

Spring AI `1.0.0-M4` 是**里程碑版本（Milestone）**，不在 Maven Central 仓库中，需要从 Spring Milestone 仓库下载。

## 解决方案

已在父 POM (`pom.xml`) 中添加了 Spring Milestone 仓库配置：

```xml
<repositories>
    <repository>
        <id>spring-milestones</id>
        <name>Spring Milestones</name>
        <url>https://repo.spring.io/milestone</url>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
    </repository>
</repositories>

<pluginRepositories>
    <pluginRepository>
        <id>spring-milestones</id>
        <name>Spring Milestones</name>
        <url>https://repo.spring.io/milestone</url>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
    </pluginRepository>
</pluginRepositories>
```

## 验证修复

重新构建项目：

```bash
# 清理并重新构建
mvn clean install

# 或仅下载依赖
mvn dependency:resolve
```

如果构建成功，说明问题已解决。

## 备选方案：使用稳定版本（如果可用）

如果 Spring AI 发布了稳定版本（如 `1.0.0`），可以更新版本号：

```xml
<properties>
    <spring-ai.version>1.0.0</spring-ai.version>
</properties>
```

稳定版本通常可以从 Maven Central 获取，不需要额外的仓库配置。

## 相关链接

- [Spring AI 文档](https://docs.spring.io/spring-ai/reference/)
- [Spring Milestone 仓库](https://repo.spring.io/milestone)
- [Spring AI GitHub](https://github.com/spring-projects/spring-ai)

