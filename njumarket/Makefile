# NJUMarket Docker 管理 Makefile

.PHONY: help build up down restart logs ps clean

help: ## 显示帮助信息
	@echo "NJUMarket Docker 管理命令"
	@echo ""
	@echo "用法: make [target]"
	@echo ""
	@echo "可用命令:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  %-15s %s\n", $$1, $$2}'

build: ## 构建所有 Docker 镜像
	docker-compose build

build-parallel: ## 并行构建所有 Docker 镜像
	docker-compose build --parallel

up: ## 启动所有服务
	docker-compose up -d

up-logs: ## 启动所有服务并显示日志
	docker-compose up

down: ## 停止所有服务
	docker-compose down

down-volumes: ## 停止所有服务并删除数据卷
	docker-compose down -v

restart: ## 重启所有服务
	docker-compose restart

restart-service: ## 重启指定服务 (使用: make restart-service SERVICE=auth-service)
	docker-compose restart $(SERVICE)

logs: ## 查看所有服务日志
	docker-compose logs -f

logs-service: ## 查看指定服务日志 (使用: make logs-service SERVICE=auth-service)
	docker-compose logs -f $(SERVICE)

ps: ## 查看所有服务状态
	docker-compose ps

health: ## 检查所有服务健康状态
	@echo "检查服务健康状态..."
	@docker-compose ps | grep -E "(health|unhealthy)"

clean: ## 清理未使用的 Docker 资源
	docker system prune -f

clean-all: ## 清理所有未使用的 Docker 资源（包括镜像）
	docker system prune -a -f

rebuild: ## 重新构建并启动所有服务
	make down
	make build
	make up

rebuild-service: ## 重新构建并启动指定服务 (使用: make rebuild-service SERVICE=auth-service)
	docker-compose stop $(SERVICE)
	docker-compose build $(SERVICE)
	docker-compose up -d $(SERVICE)

shell: ## 进入指定服务容器 (使用: make shell SERVICE=auth-service)
	docker-compose exec $(SERVICE) sh

mysql: ## 进入 MySQL 容器
	docker-compose exec mysql bash

redis: ## 进入 Redis 容器
	docker-compose exec redis sh

stats: ## 查看容器资源使用情况
	docker stats

