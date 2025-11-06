#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
批量统一Service方法的异常处理模式
将 try-catch 返回 Result.fail() 的模式统一为抛出 BusinessException
"""

import re
import os
from pathlib import Path

def process_service_file(file_path):
    """处理单个Service文件"""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    # 1. 确保导入了BusinessException
    if 'import com.njumarket.njumarket.exception.BusinessException;' not in content:
        # 找到最后一个import语句的位置
        import_pattern = r'(import com\.njumarket\.njumarket\.\w+\.\w+;)'
        matches = list(re.finditer(import_pattern, content))
        if matches:
            last_import = matches[-1]
            insert_pos = last_import.end()
            content = content[:insert_pos] + '\nimport com.njumarket.njumarket.exception.BusinessException;' + content[insert_pos:]
    
    # 2. 替换模式：return Result.fail("...") -> throw new BusinessException("...")
    # 但需要先添加日志
    content = re.sub(
        r'if\s*\(currentUser\s*==\s*null\)\s*\{\s*return Result\.fail\("用户未登录"\);',
        r'if (currentUser == null) {\n            log.warn("操作失败（业务异常） - 用户未登录");\n            throw new BusinessException("用户未登录");',
        content
    )
    
    # 3. 替换其他常见的Result.fail模式
    # 注意：这个替换需要更精确的模式匹配，这里只是示例
    
    if content != original_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False

if __name__ == '__main__':
    service_dir = Path('njumarket/src/main/java/com/njumarket/njumarket/service/impl')
    
    if not service_dir.exists():
        print(f"目录不存在: {service_dir}")
        exit(1)
    
    service_files = list(service_dir.glob('*ServiceImpl.java'))
    print(f"找到 {len(service_files)} 个Service文件")
    
    for file_path in service_files:
        if process_service_file(file_path):
            print(f"已处理: {file_path.name}")
        else:
            print(f"跳过: {file_path.name}")

