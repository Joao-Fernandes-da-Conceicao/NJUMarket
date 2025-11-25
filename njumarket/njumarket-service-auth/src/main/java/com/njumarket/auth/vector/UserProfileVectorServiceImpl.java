package com.njumarket.auth.vector;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.njumarket.auth.client.CommodityClient;
import com.njumarket.auth.client.MessageClient;
import com.njumarket.auth.client.OrderClient;
import com.njumarket.auth.entity.User;
import com.njumarket.auth.entity.UserAddress;
import com.njumarket.auth.entity.UserProfile;
import com.njumarket.auth.repository.UserAddressRepository;
import com.njumarket.auth.repository.UserRepository;
import com.njumarket.auth.repository.UserProfileRepository;
import com.njumarket.njumarket.dto.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.pgvector.PGvector;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户画像向量化服务实现
 * 迁移到 Auth 服务，符合领域驱动设计
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileVectorServiceImpl implements UserProfileVectorService {
    
    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserAddressRepository userAddressRepository;
    private final CommodityClient commodityClient;
    private final OrderClient orderClient;
    private final MessageClient messageClient;
    private final ObjectMapper objectMapper;
    
    private static final String VECTOR_TABLE = "nju_market.user_profile_vectors";
    
    @Override
    @Async
    @Transactional
    public void generateAndStoreUserProfileVector(String userId) {
        try {
            if (!StringUtils.hasText(userId)) {
                log.warn("用户ID为空，跳过向量化");
                return;
            }
            
            // 构建用户画像文本内容
            String content = buildUserProfileContent(userId);
            if (!StringUtils.hasText(content)) {
                log.warn("用户画像内容为空，跳过向量化: userId={}", userId);
                return;
            }
            
            // 生成向量 - LangChain4j API
            Embedding embedding = embeddingModel.embed(content).content();
            if (embedding == null || embedding.vector() == null || embedding.vector().length == 0) {
                log.error("向量生成失败: userId={}", userId);
                return;
            }
            // 提取向量数组（LangChain4j 返回 float[]）
            float[] embeddingArray = embedding.vector();
            if (embeddingArray == null || embeddingArray.length == 0) {
                log.error("向量数组为空: userId={}", userId);
                return;
            }
            
            // HNSW 索引最多支持 2000 维，如果向量维度超过 2000，需要截断
            int actualDimension = embeddingArray.length;
            int targetDimension = Math.min(actualDimension, 2000);
            
            // 截断向量数组到目标维度
            float[] truncatedArray = new float[targetDimension];
            System.arraycopy(embeddingArray, 0, truncatedArray, 0, targetDimension);
            
            // 构建元数据
            Map<String, Object> metadata = buildUserMetadata(userId);
            
            // 存储向量到数据库
            storeVector(userId, truncatedArray, content, metadata);
            
            log.info("用户画像向量化成功: userId={}", userId);
        } catch (Exception e) {
            log.error("用户画像向量化失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    @Override
    @Async
    @Transactional
    public void updateUserProfileVector(String userId) {
        generateAndStoreUserProfileVector(userId);
    }
    
    @Override
    @Transactional
    public void deleteUserProfileVector(String userId) {
        try {
            String sql = "DELETE FROM " + VECTOR_TABLE + " WHERE user_id = ?";
            int deleted = jdbcTemplate.update(sql, userId);
            if (deleted > 0) {
                log.info("删除用户画像向量成功: userId={}", userId);
            }
        } catch (Exception e) {
            log.error("删除用户画像向量失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    @Override
    public List<Double> getUserProfileVector(String userId) {
        try {
            String sql = "SELECT embedding FROM " + VECTOR_TABLE + " WHERE user_id = ?";
            String vectorStr = jdbcTemplate.queryForObject(sql, String.class, userId);
            if (vectorStr == null) {
                return null;
            }
            
            // 解析向量字符串
            return Arrays.stream(vectorStr.replaceAll("[\\[\\]]", "").split(","))
                .map(String::trim)
                .map(Double::parseDouble)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取用户画像向量失败: userId={}, error={}", userId, e.getMessage());
            return null;
        }
    }
    
    @Override
    public List<String> searchSimilarUsers(List<Double> queryVector, int limit) {
        try {
            if (queryVector == null || queryVector.isEmpty()) {
                return Collections.emptyList();
            }
            
            // 将 List<Double> 转换为 float[]
            // HNSW 索引最多支持 2000 维，如果向量维度超过 2000，需要截断
            int targetDimension = Math.min(queryVector.size(), 2000);
            float[] vectorArray = new float[targetDimension];
            for (int i = 0; i < targetDimension; i++) {
                vectorArray[i] = queryVector.get(i).floatValue();
            }
            
            // 使用 pgvector 官方 JDBC wrapper 创建 PGvector 对象
            PGvector pgQueryVector = new PGvector(vectorArray);
            
            // 构建SQL查询：使用余弦相似度搜索
            // 注意：LIMIT 不能使用 ? 占位符，需要直接拼接
            // 注意：不要使用 ?::vector，因为 PGvector 对象已经包含了类型信息
            // ORDER BY 使用 similarity DESC 确保最相似的排在前面
            String sql = String.format(
                "SELECT user_id, 1 - (embedding <=> ?) as similarity " +
                "FROM %s ORDER BY similarity DESC LIMIT %d",
                VECTOR_TABLE, limit
            );
            
            // 使用 PreparedStatementCreator 和 PGvector 来处理向量类型
            List<Map<String, Object>> results = jdbcTemplate.query(
                new PreparedStatementCreator() {
                    @Override
                    public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                        // 注册 pgvector 类型（如果还没有注册）
                        PGvector.addVectorType(con);
                        
                        // 设置 search_path 确保能找到 vector 类型
                        try (java.sql.Statement stmt = con.createStatement()) {
                            stmt.execute("SET search_path TO public, nju_market");
                        }
                        
                        PreparedStatement ps = con.prepareStatement(sql);
                        
                        // 直接使用 PGvector 对象绑定参数，使用 Types.OTHER 明确指定类型
                        // 只需要一个参数用于 SELECT 子句中的相似度计算
                        // ORDER BY 使用 similarity DESC，不需要额外的参数
                        ps.setObject(1, pgQueryVector, java.sql.Types.OTHER);
                        // LIMIT 已经在 SQL 中直接拼接，不需要设置参数
                        
                        return ps;
                    }
                },
                (rs, rowNum) -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("user_id", rs.getString("user_id"));
                    row.put("similarity", rs.getDouble("similarity"));
                    return row;
                }
            );
            
            return results.stream()
                .map(row -> (String) row.get("user_id"))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("搜索相似用户失败: error={}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 构建用户画像文本内容
     * 整合多个数据源：用户基本信息、商品、订单、地址、AI聊天记录、用户聊天记录
     */
    private String buildUserProfileContent(String userId) {
        StringBuilder content = new StringBuilder();
        
        try {
            // 1. 用户基本信息
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                content.append("用户名：").append(user.getUsername()).append("。");
            }
            
            // 2. 用户档案信息
            UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
            if (profile != null) {
                if (StringUtils.hasText(profile.getNickname())) {
                    content.append("昵称：").append(profile.getNickname()).append("。");
                }
                if (profile.getVipLevel() != null) {
                    content.append("VIP等级：").append(profile.getVipLevel()).append("。");
                }
                if (profile.getCreditScore() != null) {
                    content.append("信用分：").append(profile.getCreditScore()).append("。");
                }
                if (profile.getBuyerRating() != null) {
                    content.append("买家评分：").append(profile.getBuyerRating()).append("。");
                }
                if (profile.getSellerRating() != null) {
                    content.append("卖家评分：").append(profile.getSellerRating()).append("。");
                }
            }
            
            // 3. 用户地址信息（默认地址或全部地址）
            List<UserAddress> addresses = userAddressRepository.findByUserId(userId);
            if (addresses != null && !addresses.isEmpty()) {
                // 优先使用默认地址
                Optional<UserAddress> defaultAddress = addresses.stream()
                    .filter(addr -> addr.getIsDefault() != null && addr.getIsDefault())
                    .findFirst();
                
                if (defaultAddress.isPresent()) {
                    UserAddress addr = defaultAddress.get();
                    content.append("常用地址：").append(buildAddressText(addr)).append("。");
                } else if (addresses.size() > 0) {
                    // 如果没有默认地址，使用第一个地址
                    content.append("地址：").append(buildAddressText(addresses.get(0))).append("。");
                }
            }
            
            // 4. 用户发布的商品
            String commoditiesText = buildCommoditiesText(userId);
            if (StringUtils.hasText(commoditiesText)) {
                content.append(commoditiesText);
            }
            
            // 5. 用户订单（作为买家和卖家）
            String ordersText = buildOrdersText(userId);
            if (StringUtils.hasText(ordersText)) {
                content.append(ordersText);
            }
            
            // 6. AI聊天记录（从 conversation_vectors 表获取）
            String aiChatText = buildAIChatText(userId);
            if (StringUtils.hasText(aiChatText)) {
                content.append(aiChatText);
            }
            
            // 7. 用户聊天记录（从 message 服务获取）
            String userChatText = buildUserChatText(userId);
            if (StringUtils.hasText(userChatText)) {
                content.append(userChatText);
            }
            
            String result = content.toString().trim();
            if (result.isEmpty()) {
                // 如果所有数据都为空，至少返回基本信息
                result = "用户ID：" + userId + "。";
            }
            
            log.debug("构建用户画像内容完成: userId={}, contentLength={}", userId, result.length());
            return result;
            
        } catch (Exception e) {
            log.error("构建用户画像内容失败: userId={}, error={}", userId, e.getMessage(), e);
            // 即使出错，也返回基本信息
            return "用户ID：" + userId + "。";
        }
    }
    
    /**
     * 构建地址文本
     */
    private String buildAddressText(UserAddress address) {
        StringBuilder addrText = new StringBuilder();
        if (StringUtils.hasText(address.getProvince())) {
            addrText.append(address.getProvince());
        }
        if (StringUtils.hasText(address.getCity())) {
            addrText.append(address.getCity());
        }
        if (StringUtils.hasText(address.getDistrict())) {
            addrText.append(address.getDistrict());
        }
        if (StringUtils.hasText(address.getStreetAddress())) {
            addrText.append(address.getStreetAddress());
        }
        if (StringUtils.hasText(address.getDetailAddress())) {
            addrText.append(address.getDetailAddress());
        }
        return addrText.toString();
    }
    
    /**
     * 构建商品文本
     */
    private String buildCommoditiesText(String userId) {
        try {
            Result result = commodityClient.getUserCommodities(userId, 1, 50, null);
            if (result != null && result.getSuccess() != null && result.getSuccess() && result.getData() != null) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataMap = (Map<String, Object>) result.getData();
                    Object commoditiesObj = dataMap.get("commodities");
                    if (commoditiesObj != null) {
                        List<Map<String, Object>> commodities = objectMapper.convertValue(
                            commoditiesObj, new TypeReference<List<Map<String, Object>>>() {});
                        
                        if (commodities != null && !commodities.isEmpty()) {
                            StringBuilder text = new StringBuilder("发布的商品：");
                            int count = 0;
                            for (Map<String, Object> commodity : commodities) {
                                if (count >= 10) break; // 最多取10个商品
                                String title = (String) commodity.get("title");
                                String category = (String) commodity.get("category");
                                Object priceObj = commodity.get("price");
                                
                                if (StringUtils.hasText(title)) {
                                    text.append(title);
                                    if (StringUtils.hasText(category)) {
                                        text.append("（").append(category).append("）");
                                    }
                                    if (priceObj != null) {
                                        text.append("，价格").append(priceObj).append("元");
                                    }
                                    text.append("；");
                                    count++;
                                }
                            }
                            if (count > 0) {
                                text.append("共").append(commodities.size()).append("个商品。");
                                return text.toString();
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("解析商品数据失败: userId={}, error={}", userId, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.debug("获取用户商品失败（可选）: userId={}, error={}", userId, e.getMessage());
        }
        return "";
    }
    
    /**
     * 构建订单文本
     */
    private String buildOrdersText(String userId) {
        try {
            StringBuilder text = new StringBuilder();
            int buyerCount = 0;
            int sellerCount = 0;
            
            // 作为买家的订单
            try {
                Result buyerResult = orderClient.getUserOrders(userId, "buyer", 1, 50, null);
                if (buyerResult != null && buyerResult.getSuccess() != null && buyerResult.getSuccess() 
                    && buyerResult.getData() != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> buyerData = (Map<String, Object>) buyerResult.getData();
                    Object ordersObj = buyerData.get("orders");
                    if (ordersObj != null) {
                        List<Map<String, Object>> orders = objectMapper.convertValue(
                            ordersObj, new TypeReference<List<Map<String, Object>>>() {});
                        if (orders != null) {
                            buyerCount = orders.size();
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("获取买家订单失败（可选）: userId={}, error={}", userId, e.getMessage());
            }
            
            // 作为卖家的订单
            try {
                Result sellerResult = orderClient.getUserOrders(userId, "seller", 1, 50, null);
                if (sellerResult != null && sellerResult.getSuccess() != null && sellerResult.getSuccess() 
                    && sellerResult.getData() != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> sellerData = (Map<String, Object>) sellerResult.getData();
                    Object ordersObj = sellerData.get("orders");
                    if (ordersObj != null) {
                        List<Map<String, Object>> orders = objectMapper.convertValue(
                            ordersObj, new TypeReference<List<Map<String, Object>>>() {});
                        if (orders != null) {
                            sellerCount = orders.size();
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("获取卖家订单失败（可选）: userId={}, error={}", userId, e.getMessage());
            }
            
            if (buyerCount > 0 || sellerCount > 0) {
                text.append("订单情况：");
                if (buyerCount > 0) {
                    text.append("作为买家").append(buyerCount).append("单");
                }
                if (sellerCount > 0) {
                    if (buyerCount > 0) text.append("，");
                    text.append("作为卖家").append(sellerCount).append("单");
                }
                text.append("。");
                return text.toString();
            }
        } catch (Exception e) {
            log.debug("获取订单信息失败（可选）: userId={}, error={}", userId, e.getMessage());
        }
        return "";
    }
    
    /**
     * 构建AI聊天记录文本
     * 从 conversation_vectors 表中获取用户的AI聊天记录
     */
    private String buildAIChatText(String userId) {
        try {
            Result result = commodityClient.getAIChatHistory(userId, 30);
            if (result != null && result.getSuccess() != null && result.getSuccess() && result.getData() != null) {
                try {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> messages = objectMapper.convertValue(
                        result.getData(), new TypeReference<List<Map<String, Object>>>() {});
                    
                    if (messages != null && !messages.isEmpty()) {
                        StringBuilder text = new StringBuilder("AI聊天偏好：");
                        int userMessageCount = 0;
                        int totalMessages = messages.size();
                        
                        // 提取用户消息（最多5条）
                        for (Map<String, Object> msg : messages) {
                            if (userMessageCount >= 5) break;
                            
                            String role = (String) msg.get("role");
                            String content = (String) msg.get("content");
                            
                            // 只提取用户消息（role = "user"）
                            if ("user".equals(role) && StringUtils.hasText(content)) {
                                // 截断过长的消息
                                String displayContent = content.length() > 50 ? 
                                    content.substring(0, 50) + "..." : content;
                                text.append(displayContent).append("；");
                                userMessageCount++;
                            }
                        }
                        
                        if (userMessageCount > 0) {
                            text.append("共").append(totalMessages).append("条聊天记录。");
                            log.debug("构建AI聊天记录文本成功: userId={}, userMessages={}, total={}", 
                                userId, userMessageCount, totalMessages);
                            return text.toString();
                        } else {
                            // 如果没有用户消息，至少记录有AI聊天记录
                            text.append("有").append(totalMessages).append("条AI聊天记录。");
                            return text.toString();
                        }
                    }
                } catch (Exception e) {
                    log.debug("解析AI聊天记录失败: userId={}, error={}", userId, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.debug("获取AI聊天记录失败（可选）: userId={}, error={}", userId, e.getMessage());
        }
        return "";
    }
    
    /**
     * 构建用户聊天记录文本
     */
    private String buildUserChatText(String userId) {
        try {
            Result result = messageClient.getUserChatHistory(userId, 30);
            if (result != null && result.getSuccess() != null && result.getSuccess() && result.getData() != null) {
                try {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> messages = objectMapper.convertValue(
                        result.getData(), new TypeReference<List<Map<String, Object>>>() {});
                    
                    if (messages != null && !messages.isEmpty()) {
                        StringBuilder text = new StringBuilder("聊天偏好：");
                        int count = 0;
                        for (Map<String, Object> msg : messages) {
                            if (count >= 5) break; // 最多取5条
                            String senderId = (String) msg.get("senderId");
                            String content = (String) msg.get("content");
                            
                            // 只提取用户自己发送的消息
                            if (userId.equals(senderId) && StringUtils.hasText(content)) {
                                if (content.length() > 50) {
                                    content = content.substring(0, 50) + "...";
                                }
                                text.append(content).append("；");
                                count++;
                            }
                        }
                        if (count > 0) {
                            text.append("共").append(messages.size()).append("条聊天记录。");
                            return text.toString();
                        }
                    }
                } catch (Exception e) {
                    log.debug("解析用户聊天记录失败: userId={}, error={}", userId, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.debug("获取用户聊天记录失败（可选）: userId={}, error={}", userId, e.getMessage());
        }
        return "";
    }
    
    /**
     * 构建用户元数据
     */
    private Map<String, Object> buildUserMetadata(String userId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", userId);
        metadata.put("updatedAt", System.currentTimeMillis());
        
        // 可以添加更多元数据
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
        if (profile != null) {
            metadata.put("vipLevel", profile.getVipLevel());
            metadata.put("creditScore", profile.getCreditScore());
        }
        
        return metadata;
    }
    
    /**
     * 存储向量到数据库
     * 使用 pgvector 官方 JDBC wrapper
     */
    private void storeVector(String userId, float[] embedding, String content, Map<String, Object> metadata) {
        // 使用 pgvector 官方 JDBC wrapper 创建 PGvector 对象
        PGvector pgVector = new PGvector(embedding);
        
        String metadataJson = new com.fasterxml.jackson.databind.ObjectMapper()
            .valueToTree(metadata).toString();
        
        // 使用单行 SQL 避免文本块可能的问题
        String sql = String.format(
            "INSERT INTO %s (user_id, embedding, content, metadata, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
            "ON CONFLICT (user_id) DO UPDATE SET " +
            "embedding = EXCLUDED.embedding, " +
            "content = EXCLUDED.content, " +
            "metadata = EXCLUDED.metadata, " +
            "updated_at = CURRENT_TIMESTAMP",
            VECTOR_TABLE
        );
        
        // 使用 PreparedStatementCreator 和 PGvector 来处理 vector 类型
        jdbcTemplate.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                // 注册 pgvector 类型（如果还没有注册）
                PGvector.addVectorType(con);
                
                // 设置 search_path 确保能找到 vector 类型
                try (java.sql.Statement stmt = con.createStatement()) {
                    stmt.execute("SET search_path TO public, nju_market");
                }
                
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setString(1, userId);
                // 直接使用 PGvector 对象绑定参数，使用 Types.OTHER 明确指定类型
                ps.setObject(2, pgVector, java.sql.Types.OTHER);
                ps.setString(3, content);
                ps.setString(4, metadataJson);
                return ps;
            }
        });
    }
}

