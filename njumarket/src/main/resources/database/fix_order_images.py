#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
修正订单快照图片字段：只保留第一张图片
将 JSON 数组格式的图片列表改为单个图片 URL

使用方法：
python fix_order_images.py

或者提供数据库连接信息：
python fix_order_images.py --host localhost --port 3306 --database njumarket --user root --password your_password
"""

import json
import sys
import argparse
from urllib.parse import unquote

try:
    import pymysql
except ImportError:
    print("请先安装 pymysql: pip install pymysql")
    sys.exit(1)

def get_first_image(images_str):
    """从逗号分隔的字符串中提取第一张图片"""
    if not images_str or images_str.strip() == '':
        return None
    
    # 按逗号分割，取第一张图片
    images = images_str.split(',')
    if len(images) > 0:
        return images[0].strip()
    
    return None

def fix_order_images(host='localhost', port=3306, database='njumarket', 
                     user='root', password=''):
    """修正订单图片字段"""
    try:
        # 连接数据库
        connection = pymysql.connect(
            host=host,
            port=port,
            user=user,
            password=password,
            database=database,
            charset='utf8mb4'
        )
        
        print(f"已连接到数据库: {database}")
        
        with connection.cursor() as cursor:
            # 查询所有包含图片的订单
            cursor.execute("""
                SELECT order_id, commodity_snapshot_images 
                FROM orders 
                WHERE commodity_snapshot_images IS NOT NULL 
                  AND commodity_snapshot_images != ''
                  AND commodity_snapshot_images LIKE '%,%'
            """)
            
            orders = cursor.fetchall()
            print(f"找到 {len(orders)} 个包含图片的订单")
            
            updated_count = 0
            
            for order_id, images_str in orders:
                # 提取第一张图片
                first_image = get_first_image(images_str)
                
                if first_image and first_image != images_str:
                    # 更新订单
                    cursor.execute("""
                        UPDATE orders 
                        SET commodity_snapshot_images = %s 
                        WHERE order_id = %s
                    """, (first_image, order_id))
                    
                    updated_count += 1
                    print(f"✓ 更新订单 {order_id}")
                    print(f"  原值: {images_str[:50]}...")
                    print(f"  新值: {first_image}")
            
            # 提交事务
            connection.commit()
            print(f"\n总共更新了 {updated_count} 个订单")
            
    except Exception as e:
        print(f"错误: {e}")
        if connection:
            connection.rollback()
        sys.exit(1)
    finally:
        if connection:
            connection.close()

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='修正订单快照图片字段')
    parser.add_argument('--host', default='localhost', help='数据库主机')
    parser.add_argument('--port', type=int, default=3306, help='数据库端口')
    parser.add_argument('--database', default='njumarket', help='数据库名称')
    parser.add_argument('--user', default='root', help='数据库用户')
    parser.add_argument('--password', default='', help='数据库密码')
    
    args = parser.parse_args()
    
    fix_order_images(
        host=args.host,
        port=args.port,
        database=args.database,
        user=args.user,
        password=args.password
    )

