#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
简化版批量用户创建脚本
无需验证码，直接注册用户

使用方法：
python batch_create_users_simple.py
"""

import requests
import time
import json
import csv
from typing import List, Dict, Optional

# 配置
BASE_URL = "http://localhost:8080"
API_PREFIX = "/api/user/auth"

# 用户配置
USER_COUNT = 100
PASSWORD = "123456"
USERNAME_PREFIX = "username_test"

# 输出文件
OUTPUT_CSV = "user_tokens.csv"
OUTPUT_JSON = "user_tokens.json"


class SimpleUserCreator:
    """简化版用户创建器（无需验证码）"""
    
    def __init__(self, base_url: str = BASE_URL):
        self.base_url = base_url
        self.api_prefix = API_PREFIX
        self.session = requests.Session()
        self.session.headers.update({
            "Content-Type": "application/json"
        })
    
    def generate_phone(self, index: int) -> str:
        """生成手机号：13800000001 到 13800000100"""
        phone_suffix = str(index).zfill(4)
        return f"1380000{phone_suffix}"
    
    def register_user(self, username: str, phone: str) -> Optional[Dict]:
        """注册用户（无需验证码）"""
        url = f"{self.base_url}{self.api_prefix}/register-new"
        data = {
            "phone": phone,
            "username": username,
            "password": PASSWORD,
            "confirmPassword": PASSWORD,
            "nickname": f"测试用户{username.split('_')[-1]}"
        }
        
        try:
            response = self.session.post(url, json=data, timeout=10)
            if response.status_code == 200:
                result = response.json()
                if result.get("success"):
                    data = result.get("data", {})
                    return {
                        "username": username,
                        "phone": phone,
                        "token": data.get("token"),
                        "refreshToken": data.get("refreshToken"),
                        "userId": data.get("userInfo", {}).get("userId")
                    }
                else:
                    message = result.get("message", "未知错误")
                    # 如果用户已存在，尝试登录
                    if "已注册" in message or "已存在" in message:
                        return self.login_user(phone)
                    # 打印错误信息以便调试
                    print(f"  ✗ {message}")
                    return None
            else:
                # 打印HTTP错误信息
                try:
                    error_result = response.json()
                    error_msg = error_result.get("message", f"HTTP {response.status_code}")
                    print(f"  ✗ {error_msg}")
                except:
                    print(f"  ✗ HTTP {response.status_code}")
                return None
        except Exception as e:
            print(f"  ✗ 注册异常: {e}")
            return None
    
    def login_user(self, phone: str) -> Optional[Dict]:
        """登录用户"""
        url = f"{self.base_url}{self.api_prefix}/login"
        data = {
            "identifier": phone,
            "password": PASSWORD
        }
        
        try:
            response = self.session.post(url, json=data, timeout=10)
            if response.status_code == 200:
                result = response.json()
                if result.get("success"):
                    data = result.get("data", {})
                    return {
                        "phone": phone,
                        "token": data.get("token"),
                        "refreshToken": data.get("refreshToken"),
                        "userId": data.get("userInfo", {}).get("userId")
                    }
            return None
        except Exception as e:
            return None
    
    def batch_create(self, count: int = USER_COUNT) -> List[Dict]:
        """批量创建用户（无需验证码）"""
        created_users = []
        
        print(f"\n开始批量创建 {count} 个用户（无需验证码）...")
        print("=" * 60)
        
        for i in range(1, count + 1):
            username = f"{USERNAME_PREFIX}_{i}"
            phone = self.generate_phone(i)
            
            print(f"[{i}/{count}] {username} ({phone})", end=" ... ")
            
            # 注册用户（无需验证码）
            user_data = self.register_user(username, phone)
            
            if user_data:
                if not user_data.get("username"):
                    user_data["username"] = username
                created_users.append(user_data)
                print("✓")
            else:
                print("✗")
            
            # 避免请求过快
            time.sleep(0.1)
        
        print("\n" + "=" * 60)
        print(f"完成！成功: {len(created_users)}/{count}")
        
        return created_users
    
    def save_to_csv(self, users: List[Dict], filename: str = OUTPUT_CSV):
        """保存到CSV"""
        if not users:
            return
        
        with open(filename, 'w', newline='', encoding='utf-8') as f:
            writer = csv.writer(f)
            writer.writerow(['username', 'phone', 'token', 'userId'])
            for user in users:
                writer.writerow([
                    user.get('username', ''),
                    user.get('phone', ''),
                    user.get('token', ''),
                    user.get('userId', '')
                ])
        
        print(f"\n✓ CSV已保存: {filename}")
    
    def save_to_json(self, users: List[Dict], filename: str = OUTPUT_JSON):
        """保存到JSON"""
        if not users:
            return
        
        with open(filename, 'w', encoding='utf-8') as f:
            json.dump(users, f, ensure_ascii=False, indent=2)
        
        print(f"✓ JSON已保存: {filename}")


def main():
    """主函数"""
    print("=" * 60)
    print("简化版批量用户创建脚本（无需验证码）")
    print("=" * 60)
    
    creator = SimpleUserCreator()
    
    # 批量创建
    users = creator.batch_create(count=USER_COUNT)
    
    if users:
        creator.save_to_csv(users)
        creator.save_to_json(users)
        print(f"\n✓ 共获取 {len(users)} 个用户的token")
        print("\n可以用于JMeter测试！")
    else:
        print("\n⚠ 没有成功创建用户，请检查后端服务是否正常运行")


if __name__ == "__main__":
    main()
