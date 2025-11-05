#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
批量创建用户账号脚本
功能：
1. 批量创建100个测试用户账号（username_test_1 到 username_test_100）
2. 批量获取这些账号的token
3. 保存token到CSV文件，供JMeter或其他测试工具使用

注意：注册无需验证码，直接使用手机号和密码注册

使用方法：
python batch_create_users.py

需要安装的库：
pip install requests
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


class UserBatchCreator:
    """批量用户创建器"""
    
    def __init__(self, base_url: str = BASE_URL):
        self.base_url = base_url
        self.api_prefix = API_PREFIX
        self.session = requests.Session()
        self.session.headers.update({
            "Content-Type": "application/json"
        })
    
    def generate_phone(self, index: int) -> str:
        """
        生成手机号
        规则：1380000 + 4位数字（0001-0100）
        例如：13800000001, 13800000002, ..., 13800000100
        """
        # 使用1380000开头，后4位从0001开始
        phone_suffix = str(index).zfill(4)  # 0001-0100
        return f"1380000{phone_suffix}"
    
    def register_user(self, username: str, phone: str, password: str, 
                     confirm_password: str, nickname: Optional[str] = None) -> Optional[Dict]:
        """
        注册用户（无需验证码）
        """
        url = f"{self.base_url}{self.api_prefix}/register-new"
        data = {
            "phone": phone,
            "username": username,
            "password": password,
            "confirmPassword": confirm_password,
            "nickname": nickname or username
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
                    message = result.get('message', '未知错误')
                    print(f"✗ 注册失败 {username}: {message}")
                    return None
            else:
                try:
                    error_result = response.json()
                    error_msg = error_result.get("message", f"HTTP {response.status_code}")
                    print(f"✗ 注册失败 {username}: {error_msg}")
                except:
                    print(f"✗ 注册失败 {username}: HTTP {response.status_code}")
                return None
        except Exception as e:
            print(f"✗ 注册异常 {username}: {str(e)}")
            return None
    
    def login(self, identifier: str, password: str) -> Optional[Dict]:
        """
        用户登录
        identifier可以是手机号或用户名
        """
        url = f"{self.base_url}{self.api_prefix}/login"
        data = {
            "identifier": identifier,
            "password": password
        }
        
        try:
            response = self.session.post(url, json=data, timeout=10)
            if response.status_code == 200:
                result = response.json()
                if result.get("success"):
                    data = result.get("data", {})
                    return {
                        "identifier": identifier,
                        "token": data.get("token"),
                        "refreshToken": data.get("refreshToken"),
                        "userId": data.get("userInfo", {}).get("userId")
                    }
                else:
                    print(f"✗ 登录失败 {identifier}: {result.get('message')}")
                    return None
            else:
                print(f"✗ 登录失败 {identifier}: HTTP {response.status_code}")
                return None
        except Exception as e:
            print(f"✗ 登录异常 {identifier}: {str(e)}")
            return None
    
    def batch_create_users(self, count: int = USER_COUNT) -> List[Dict]:
        """
        批量创建用户（无需验证码）
        
        Args:
            count: 要创建的用户数量
        
        Returns:
            创建成功的用户列表
        """
        created_users = []
        
        print(f"\n开始批量创建 {count} 个用户（无需验证码）...")
        print("=" * 60)
        
        for i in range(1, count + 1):
            username = f"{USERNAME_PREFIX}_{i}"
            phone = self.generate_phone(i)
            nickname = f"测试用户{i}"
            
            print(f"[{i}/{count}] {username} ({phone})", end=" ... ")
            
            # 直接注册用户（无需验证码）
            user_data = self.register_user(
                username=username,
                phone=phone,
                password=PASSWORD,
                confirm_password=PASSWORD,
                nickname=nickname
            )
            
            if user_data:
                created_users.append(user_data)
                print("✓")
            else:
                # 如果注册失败，尝试登录（可能用户已存在）
                login_data = self.login(phone, PASSWORD)
                if login_data:
                    login_data["username"] = username
                    login_data["phone"] = phone
                    created_users.append(login_data)
                    print("✓ (已存在，登录成功)")
                else:
                    print("✗")
            
            # 避免请求过快
            time.sleep(0.1)
        
        print("\n" + "=" * 60)
        print(f"批量创建完成！成功: {len(created_users)}/{count}")
        
        return created_users
    
    def batch_login_users(self, phones: List[str]) -> List[Dict]:
        """
        批量登录用户（用于已存在的用户）
        """
        logged_users = []
        
        print(f"\n开始批量登录 {len(phones)} 个用户...")
        print("=" * 60)
        
        for i, phone in enumerate(phones, 1):
            print(f"[{i}/{len(phones)}] 登录用户: {phone}")
            
            login_data = self.login(phone, PASSWORD)
            if login_data:
                login_data["phone"] = phone
                logged_users.append(login_data)
                print(f"  ✓ 登录成功")
            else:
                print(f"  ✗ 登录失败")
            
            time.sleep(0.1)
        
        print("\n" + "=" * 60)
        print(f"批量登录完成！成功: {len(logged_users)}/{len(phones)}")
        
        return logged_users
    
    def save_to_csv(self, users: List[Dict], filename: str = OUTPUT_CSV):
        """保存用户token到CSV文件（JMeter格式）"""
        if not users:
            print("没有用户数据可保存")
            return
        
        with open(filename, 'w', newline='', encoding='utf-8') as f:
            writer = csv.writer(f)
            # 写入表头
            writer.writerow(['username', 'phone', 'token', 'userId'])
            # 写入数据
            for user in users:
                writer.writerow([
                    user.get('username', ''),
                    user.get('phone', ''),
                    user.get('token', ''),
                    user.get('userId', '')
                ])
        
        print(f"\n✓ Token已保存到: {filename}")
    
    def save_to_json(self, users: List[Dict], filename: str = OUTPUT_JSON):
        """保存用户token到JSON文件"""
        if not users:
            print("没有用户数据可保存")
            return
        
        with open(filename, 'w', encoding='utf-8') as f:
            json.dump(users, f, ensure_ascii=False, indent=2)
        
        print(f"✓ Token已保存到: {filename}")


def main():
    """主函数"""
    print("=" * 60)
    print("批量用户创建脚本")
    print("=" * 60)
    
    creator = UserBatchCreator()
    
    # 选择模式
    print("\n请选择模式：")
    print("1. 批量创建新用户（无需验证码）")
    print("2. 批量登录已存在用户")
    print("3. 只生成手机号列表（用于后续手动操作）")
    
    choice = input("\n请输入选项 (1/2/3): ").strip()
    
    if choice == "1":
        # 批量创建模式（无需验证码）
        users = creator.batch_create_users(count=USER_COUNT)
        
        if users:
            creator.save_to_csv(users)
            creator.save_to_json(users)
            print(f"\n✓ 共创建/获取 {len(users)} 个用户的token")
    
    elif choice == "2":
        # 批量登录模式
        print("\n生成手机号列表...")
        phones = [creator.generate_phone(i) for i in range(1, USER_COUNT + 1)]
        
        users = creator.batch_login_users(phones)
        
        if users:
            creator.save_to_csv(users)
            creator.save_to_json(users)
            print(f"\n✓ 共获取 {len(users)} 个用户的token")
    
    elif choice == "3":
        # 只生成手机号列表
        print("\n生成手机号和用户名列表...")
        users_info = []
        for i in range(1, USER_COUNT + 1):
            users_info.append({
                "username": f"{USERNAME_PREFIX}_{i}",
                "phone": creator.generate_phone(i),
                "password": PASSWORD
            })
        
        filename = "user_list.json"
        with open(filename, 'w', encoding='utf-8') as f:
            json.dump(users_info, f, ensure_ascii=False, indent=2)
        
        print(f"✓ 用户列表已保存到: {filename}")
    
    else:
        print("无效选项")


if __name__ == "__main__":
    main()
