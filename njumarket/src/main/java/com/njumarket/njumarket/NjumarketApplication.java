package com.njumarket.njumarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // 启用定时任务支持（用于WebSocket连接健康检查）
public class NjumarketApplication {

	public static void main(String[] args) {
		SpringApplication.run(NjumarketApplication.class, args);
	}

}
