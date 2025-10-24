-- ========================================
-- 订单商品解耦更新脚本
-- 添加商品快照字段到订单表
-- 
-- 注意：卖家详细信息存储说明
-- 1. 卖家基本信息：users表（user_id, primary_phone等）
-- 2. 卖家详细信息：user_profiles表（nickname, avatar, credit_score等）
-- 3. 卖家联系方式：contact_info表（email, phone等，加密存储）
-- ========================================

-- 添加商品快照字段到订单表
ALTER TABLE `orders` 
ADD COLUMN `commodity_snapshot_title` varchar(200) COMMENT '商品快照-标题',
ADD COLUMN `commodity_snapshot_description` text COMMENT '商品快照-描述',
ADD COLUMN `commodity_snapshot_price` decimal(10,2) COMMENT '商品快照-价格',
ADD COLUMN `commodity_snapshot_location` varchar(200) COMMENT '商品快照-位置',
ADD COLUMN `commodity_snapshot_category` varchar(50) COMMENT '商品快照-分类',
ADD COLUMN `commodity_snapshot_condition_level` varchar(20) COMMENT '商品快照-成色',
ADD COLUMN `commodity_snapshot_images` text COMMENT '商品快照-图片(JSON格式)',
ADD COLUMN `commodity_snapshot_status` varchar(20) COMMENT '商品快照-状态',
ADD COLUMN `commodity_snapshot_seller_name` varchar(100) COMMENT '商品快照-卖家名称',
ADD COLUMN `commodity_snapshot_seller_phone` varchar(20) COMMENT '商品快照-卖家电话',
ADD COLUMN `commodity_snapshot_seller_email` varchar(100) COMMENT '商品快照-卖家邮箱',
ADD COLUMN `commodity_snapshot_time` datetime COMMENT '商品快照时间';

-- 添加索引
ALTER TABLE `orders` 
ADD INDEX `idx_commodity_snapshot_time` (`commodity_snapshot_time`);

-- 更新现有订单的商品快照信息（从商品表联合更新）
-- 注意：卖家详细信息从user_profiles表获取，联系方式从contact_info表获取
UPDATE `orders` o 
JOIN `commodities` c ON o.commodity_id = c.commodity_id
JOIN `users` u ON c.seller_id = u.user_id
LEFT JOIN `user_profiles` up ON u.user_id = up.user_id
LEFT JOIN `contact_info` ci_email ON u.user_id = ci_email.owner_id AND ci_email.type = 'EMAIL'
LEFT JOIN `contact_info` ci_phone ON u.user_id = ci_phone.owner_id AND ci_phone.type = 'PHONE'
SET 
    o.commodity_snapshot_title = c.title,
    o.commodity_snapshot_description = c.description,
    o.commodity_snapshot_price = c.price,
    o.commodity_snapshot_location = c.location,
    o.commodity_snapshot_category = c.category,
    o.commodity_snapshot_condition_level = c.condition_level,
    o.commodity_snapshot_images = NULL, -- 暂时置为NULL，后续可考虑复制图片
    o.commodity_snapshot_status = c.commodity_status,
    o.commodity_snapshot_seller_name = COALESCE(up.nickname, u.user_id), -- 优先使用昵称，否则使用用户ID
    o.commodity_snapshot_seller_phone = COALESCE(ci_phone.value_encrypted, u.primary_phone), -- 优先使用联系方式，否则使用主要手机号
    o.commodity_snapshot_seller_email = ci_email.value_encrypted, -- 从联系方式表获取邮箱
    o.commodity_snapshot_time = o.create_time;

-- 添加注释
ALTER TABLE `orders` COMMENT = '订单表-包含商品快照信息';

-- ========================================
-- 使用说明
-- ========================================
/*
1. 数据库结构说明：
   - users表：存储用户基本信息（user_id, primary_phone等）
   - user_profiles表：存储用户详细信息（nickname, avatar, credit_score等）
   - contact_info表：存储联系方式（email, phone等，加密存储）

2. 商品快照字段说明：
   - commodity_snapshot_seller_name：优先使用user_profiles.nickname，否则使用users.user_id
   - commodity_snapshot_seller_phone：优先使用contact_info中的PHONE类型，否则使用users.primary_phone
   - commodity_snapshot_seller_email：从contact_info表中获取EMAIL类型

3. 后端代码建议：
   - 在OrderServiceImpl中，建议使用新的createCommoditySnapshot重载方法
   - 通过查询user_profiles和contact_info表获取完整的卖家信息
   - 示例代码：
     ```java
     // 获取卖家详细信息
     Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(sellerId);
     Optional<ContactInfo> emailOpt = contactInfoRepository.findByOwnerIdAndType(sellerId, "EMAIL");
     Optional<ContactInfo> phoneOpt = contactInfoRepository.findByOwnerIdAndType(sellerId, "PHONE");
     
     String sellerName = profileOpt.map(UserProfile::getNickname).orElse(user.getUserId());
     String sellerPhone = phoneOpt.map(ContactInfo::getValueEncrypted).orElse(user.getPrimaryPhone());
     String sellerEmail = emailOpt.map(ContactInfo::getValueEncrypted).orElse(null);
     
     order.createCommoditySnapshot(commodity, sellerName, sellerPhone, sellerEmail);
     ```

4. 注意事项：
   - contact_info表中的value_encrypted字段是加密存储的，需要解密后使用
   - 如果联系方式不存在，使用users表中的基本信息作为备选
   - 商品快照时间设置为订单创建时间，确保数据一致性
*/
