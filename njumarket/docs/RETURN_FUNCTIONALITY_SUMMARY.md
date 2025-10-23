# 退货功能实现总结

## 功能概述
实现了完整的退货流程，包括买家申请退货、卖家审批、退货发货确认和退货完成等环节。退货状态集成到订单状态中，实现了基于订单状态的退货逻辑。

## 实现的功能

### 1. 订单状态扩展
- **新增退货相关状态**:
  - `RETURN_REQUESTED`: 退货申请中
  - `RETURN_APPROVED`: 退货已同意
  - `RETURN_REJECTED`: 退货已拒绝
  - `RETURN_COMPLETED`: 退货已完成

### 2. 数据库架构更新
- **文件**: `njumarket/src/main/resources/database/update_add_return_fields.sql`
- **新增字段**:
  - `return_reason`: 退货原因
  - `return_request_time`: 退货申请时间
  - `return_approval_time`: 退货审批时间
  - `return_rejection_reason`: 退货拒绝原因
  - `return_tracking_number`: 退货快递单号
  - `return_completion_time`: 退货完成时间
- **新增表**:
  - `return_records`: 退货记录表（详细记录）
  - `return_reason_types`: 退货原因类型表
  - `v_return_statistics`: 退货统计视图

### 3. 实体类更新

#### 3.1 Order实体
- **新增字段**:
  - `returnReason`: 退货原因
  - `returnRequestTime`: 退货申请时间
  - `returnApprovalTime`: 退货审批时间
  - `returnRejectionReason`: 退货拒绝原因
  - `returnTrackingNumber`: 退货快递单号
  - `returnCompletionTime`: 退货完成时间
- **新增方法**:
  - `requestReturn()`: 申请退货
  - `approveReturnRequest()`: 审批退货申请
  - `confirmReturnShipment()`: 确认退货发货
  - `completeReturn()`: 完成退货
  - `canRequestReturn()`: 检查是否可以申请退货
  - `canApproveReturn()`: 检查是否可以审批退货
  - `canConfirmReturnShipment()`: 检查是否可以确认退货发货
  - `canCompleteReturn()`: 检查是否可以完成退货

### 4. DTO类更新
- **OrderDTO**: 添加了所有退货相关字段
  - `returnReason`: 退货原因
  - `returnRequestTime`: 退货申请时间
  - `returnApprovalTime`: 退货审批时间
  - `returnRejectionReason`: 退货拒绝原因
  - `returnTrackingNumber`: 退货快递单号
  - `returnCompletionTime`: 退货完成时间

### 5. Repository层更新
- **OrderRepository**: 添加了退货相关查询方法
  - `findByBuyerIdAndOrderStatusIn()`: 根据买家ID和订单状态列表查询
  - `findBySellerIdAndOrderStatusIn()`: 根据卖家ID和订单状态列表查询

### 6. Service层更新

#### 6.1 OrderService接口
- **新增方法**:
  - `requestReturn()`: 申请退货（买家功能）
  - `approveReturnRequest()`: 审批退货申请（卖家功能）
  - `confirmReturnShipment()`: 确认退货发货（买家功能）
  - `completeReturn()`: 完成退货（卖家功能）
  - `getReturnRequests()`: 获取退货申请列表（卖家功能）
  - `getMyReturnRecords()`: 获取我的退货记录（买家功能）

#### 6.2 OrderServiceImpl实现
- **权限控制**:
  - 申请退货：只有买家可以申请
  - 审批退货：只有卖家可以审批
  - 确认发货：只有买家可以确认
  - 完成退货：只有卖家可以完成
- **业务逻辑**:
  - 只有已完成的订单可以申请退货
  - 拒绝退货时必须提供拒绝原因
  - 完成退货前必须确认退货发货

### 7. Controller层更新

#### 7.1 UserOrderController
- **新增接口**:
  - `POST /api/user/order/{orderId}/return`: 申请退货
  - `PUT /api/user/order/{orderId}/return/approve`: 审批退货申请
  - `PUT /api/user/order/{orderId}/return/shipment`: 确认退货发货
  - `PUT /api/user/order/{orderId}/return/complete`: 完成退货
  - `GET /api/user/order/returns`: 获取退货申请列表（卖家）
  - `GET /api/user/order/my-returns`: 获取我的退货记录（买家）

## 退货流程

### 1. 退货申请流程
```
已完成订单 → 买家申请退货 → 订单状态：RETURN_REQUESTED
```

### 2. 退货审批流程
```
退货申请中 → 卖家审批 → 订单状态：RETURN_APPROVED 或 RETURN_REJECTED
```

### 3. 退货发货流程
```
退货已同意 → 买家确认发货 → 设置退货快递单号
```

### 4. 退货完成流程
```
退货已同意 + 已确认发货 → 卖家完成退货 → 订单状态：RETURN_COMPLETED
```

## 权限控制规则

### 买家权限
- ✅ 申请退货（仅限已完成的订单）
- ✅ 确认退货发货（仅限已同意的退货）
- ✅ 查看自己的退货记录

### 卖家权限
- ✅ 审批退货申请（仅限自己商品的订单）
- ✅ 完成退货（仅限已确认发货的退货）
- ✅ 查看退货申请列表

### 状态限制
- **申请退货**: 仅限`COMPLETED`状态的订单
- **审批退货**: 仅限`RETURN_REQUESTED`状态的订单
- **确认发货**: 仅限`RETURN_APPROVED`状态的订单
- **完成退货**: 仅限`RETURN_APPROVED`状态且已确认发货的订单

## 退货原因类型

系统预定义了常见的退货原因：
- `QUALITY_ISSUE`: 质量问题
- `DESCRIPTION_MISMATCH`: 描述不符
- `DAMAGED_IN_TRANSIT`: 运输损坏
- `WRONG_ITEM`: 发错商品
- `CHANGE_MIND`: 改变主意
- `SIZE_ISSUE`: 尺寸问题
- `COLOR_ISSUE`: 颜色问题
- `OTHER`: 其他原因

## API使用示例

### 申请退货
```bash
POST /api/user/order/{orderId}/return?returnReason=质量问题
```

### 审批退货申请
```bash
# 同意退货
PUT /api/user/order/{orderId}/return/approve?approved=true

# 拒绝退货
PUT /api/user/order/{orderId}/return/approve?approved=false&rejectionReason=商品无质量问题
```

### 确认退货发货
```bash
PUT /api/user/order/{orderId}/return/shipment?returnTrackingNumber=SF1234567890
```

### 完成退货
```bash
PUT /api/user/order/{orderId}/return/complete
```

### 获取退货申请列表（卖家）
```bash
GET /api/user/order/returns?page=1&size=10&status=RETURN_REQUESTED
```

### 获取我的退货记录（买家）
```bash
GET /api/user/order/my-returns?page=1&size=10&status=RETURN_APPROVED
```

## 数据库更新

执行以下脚本更新数据库：
```sql
-- 执行退货功能脚本
source njumarket/src/main/resources/database/update_add_return_fields.sql
```

## 订单状态流转图

```
CREATED → PAID → SHIPPED → COMPLETED
    ↓         ↓        ↓         ↓
CANCELLED  REFUNDED  RETURN_REQUESTED
                              ↓
                    RETURN_APPROVED → RETURN_COMPLETED
                              ↓
                    RETURN_REJECTED
```

## 业务规则

### 1. 退货时间限制
- 只有已完成的订单可以申请退货
- 建议在订单完成后7天内申请退货

### 2. 退货条件
- 商品必须保持原状
- 包装完整
- 配件齐全

### 3. 退货费用
- 质量问题：卖家承担运费
- 非质量问题：买家承担运费

### 4. 退款处理
- 退货完成后自动触发退款流程
- 退款金额为订单实际支付金额

## 总结

退货功能的实现提供了完整的退货流程管理：

✅ **订单状态集成**: 退货状态作为订单状态的一部分
✅ **完整流程**: 申请→审批→发货→完成
✅ **权限控制**: 精确的角色权限管理
✅ **数据记录**: 完整的退货信息记录
✅ **状态检查**: 严格的状态流转控制
✅ **API接口**: 完整的RESTful API
✅ **数据库支持**: 完整的数据库架构

这个实现为电商平台提供了完善的退货管理功能，确保了退货流程的规范性和可追溯性。
