#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
微服务迁移脚本 - 批量迁移Service和Controller
"""
import os
import shutil
import re

# 定义迁移映射
SERVICE_MIGRATIONS = {
    # auth-service
    'AdminServiceImpl.java': ('auth', 'com.njumarket.auth'),
    'UserServiceImpl.java': ('auth', 'com.njumarket.auth'),
    'UserProfileServiceImpl.java': ('auth', 'com.njumarket.auth'),
    
    # commodity-service
    'CommodityServiceImpl.java': ('commodity', 'com.njumarket.commodity'),
    'CommodityQueryServiceImpl.java': ('commodity', 'com.njumarket.commodity'),
    'ImageServiceImpl.java': ('commodity', 'com.njumarket.commodity'),
    'ImageReferenceServiceImpl.java': ('commodity', 'com.njumarket.commodity'),
    'ChangeRecordServiceImpl.java': ('commodity', 'com.njumarket.commodity'),
    
    # order-service
    'OrderServiceImpl.java': ('order', 'com.njumarket.order'),
    'ComplaintServiceImpl.java': ('order', 'com.njumarket.order'),
    
    # message-service
    'MessageServiceImpl.java': ('message', 'com.njumarket.message'),
    'ContactServiceImpl.java': ('message', 'com.njumarket.message'),
    'WebSocketRetryServiceImpl.java': ('message', 'com.njumarket.message'),
}

def migrate_file(src_file, service_name, new_package):
    """迁移单个文件"""
    # 读取文件内容
    with open(src_file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 更新包名
    content = re.sub(
        r'package com\.njumarket\.njumarket\.service\.impl;',
        f'package {new_package}.service.impl;',
        content
    )
    
    # 更新repository导入
    content = re.sub(
        r'import com\.njumarket\.njumarket\.repository\.',
        f'import {new_package}.repository.',
        content
    )
    
    # 更新service导入
    content = re.sub(
        r'import com\.njumarket\.njumarket\.service\.',
        f'import {new_package}.service.',
        content
    )
    
    # 保留common模块的导入
    # (entity, dto, exception, utils已经正确)
    
    # 创建目标目录
    target_dir = f'njumarket-service-{service_name}/src/main/java/{new_package.replace(".", "/")}/service/impl'
    os.makedirs(target_dir, exist_ok=True)
    
    # 写入文件
    target_file = os.path.join(target_dir, os.path.basename(src_file))
    with open(target_file, 'w', encoding='utf-8') as f:
        f.write(content)
    
    print(f'✓ 已迁移: {os.path.basename(src_file)} -> {service_name}-service')
    return True

def main():
    """主函数"""
    base_dir = os.path.dirname(os.path.abspath(__file__))
    src_dir = os.path.join(base_dir, 'src/main/java/com/njumarket/njumarket/service/impl')
    
    print('=== 开始迁移Service实现类 ===\n')
    
    success_count = 0
    failed_count = 0
    
    for filename, (service_name, new_package) in SERVICE_MIGRATIONS.items():
        src_file = os.path.join(src_dir, filename)
        
        if not os.path.exists(src_file):
            print(f'⚠ 文件不存在: {filename}')
            continue
        
        try:
            migrate_file(src_file, service_name, new_package)
            success_count += 1
        except Exception as e:
            print(f'✗ 迁移失败: {filename} - {e}')
            failed_count += 1
    
    print(f'\n=== 迁移完成 ===')
    print(f'成功: {success_count} 个文件')
    print(f'失败: {failed_count} 个文件')

if __name__ == '__main__':
    main()

