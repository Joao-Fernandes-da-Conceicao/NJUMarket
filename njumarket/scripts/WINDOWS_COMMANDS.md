# Windows命令参考

## 📋 常用Windows命令

### 目录操作

```cmd
# 进入目录（使用反斜杠 \）
cd njumarket\scripts

# 返回上级目录
cd ..

# 查看当前目录
cd

# 列出文件
dir
```

### Python相关

**如果 `pip` 命令不可用，使用 `py -m pip`**：

```cmd
# 安装依赖（推荐方式）
py -m pip install -r requirements.txt

# 或者尝试（如果pip在PATH中）
pip install -r requirements.txt
```

**运行Python脚本**：
```cmd
# 方式1：使用py启动器（推荐）
py batch_create_users_simple.py

# 方式2：如果python在PATH中
python batch_create_users_simple.py
```

### JMeter相关

```cmd
# 启动JMeter（使用完整路径）
D:\apache-jmeter-5.6\bin\jmeter.bat

# 或者先进入目录
cd D:\apache-jmeter-5.6\bin
jmeter.bat

# 命令行模式运行测试
jmeter.bat -n -t "测试脚本.jmx" -l results.jtl -e -o report\
```

---

## 🔄 Windows vs Linux 命令对比

| 操作 | Windows | Linux/Mac |
|------|---------|-----------|
| 进入目录 | `cd folder\subfolder` | `cd folder/subfolder` |
| 路径分隔符 | `\` (反斜杠) | `/` (正斜杠) |
| Python命令 | `python` 或 `py` | `python3` |
| 运行脚本 | `script.bat` | `./script.sh` |
| JMeter启动 | `jmeter.bat` | `jmeter.sh` |

---

## 💡 Windows使用技巧

### 1. 路径中的空格

如果路径包含空格，需要用引号括起来：
```cmd
cd "D:\软工作业\NJUMarket\njumarket\scripts"
```

### 2. Python命令不可用

如果提示 `python` 不是内部或外部命令：
- ✅ **使用 `py` 启动器**（推荐）：
  ```cmd
  py batch_create_users_simple.py
  ```
- 或者添加到环境变量PATH中

### 3. pip命令不可用

如果提示 `pip` 不是内部或外部命令：
- ✅ **使用 `py -m pip`**（推荐）：
  ```cmd
  py -m pip install -r requirements.txt
  ```
- 或者添加到环境变量PATH中

### 3. 快速打开文件夹

在文件资源管理器中：
- 按 `Win + R` 打开运行对话框
- 输入 `cmd` 打开命令提示符
- 在地址栏输入 `cmd` 也可以直接打开该目录的命令提示符

---

## 📝 示例：完整操作流程

### 打开命令提示符并运行脚本

1. **打开命令提示符**：
   - 按 `Win + R`
   - 输入 `cmd`
   - 按回车

2. **进入项目目录**：
   ```cmd
   cd D:\软工作业\NJUMarket\njumarket\scripts
   ```

3. **安装依赖**：
   ```cmd
   py -m pip install -r requirements.txt
   ```

4. **运行脚本**：
   ```cmd
   py batch_create_users_simple.py
   ```

---

**适合人群**：Windows用户  
**最后更新**：2025-01-XX
