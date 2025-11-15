package com.njumarket.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Spring Cloud Config Server 应用入口
 * 
 * 功能：
 * 1. 集中管理所有微服务的配置
 * 2. 支持配置的版本管理和动态刷新
 * 3. 支持多环境配置（dev, test, prod）
 * 4. 与 Eureka 集成，提供服务发现
 * 
 * 注意：Spring Cloud 2023.0.3+ 版本中，@EnableEurekaClient 已移除
 * 只要添加了 spring-cloud-starter-netflix-eureka-client 依赖，Eureka Client 会自动启用
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConfigServerApplication.class, args);
	}

}

