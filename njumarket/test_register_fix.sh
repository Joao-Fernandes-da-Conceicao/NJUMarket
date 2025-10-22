# 测试注册接口403错误修复

echo "测试注册接口..."

# 测试1: 发送验证码
echo "1. 发送验证码..."
curl -X POST "http://localhost:8080/api/user/auth/send-code?phone=13800138004" \
  -H "Content-Type: application/json"

echo -e "\n\n2. 测试注册接口（应该不再返回403）..."
curl -X POST "http://localhost:8080/api/user/auth/register-new" \
  -H "accept: */*" \
  -H "Content-Type: application/json" \
  -d '{
  "phone": "13800138004",
  "username": "user_004",
  "password": "123456",
  "confirmPassword": "123456",
  "code": "123456"
}'

echo -e "\n\n测试完成！"
