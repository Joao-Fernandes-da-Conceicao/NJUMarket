package com.njumarket.admin.service.impl;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.vo.PageResultVO;
import com.njumarket.admin.entity.AIConversation;
import com.njumarket.admin.repository.AIConversationRepository;
import com.njumarket.admin.service.AIConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 聊天会话管理服务实现类（管理端）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIConversationServiceImpl implements AIConversationService {
    
    private final AIConversationRepository aiConversationRepository;
    
    @Override
    public Result getConversationList(Integer page, Integer size, String userId, String status, String keyword, String sortProp, String sortOrder) {
        try {
            // 参数验证
            int pageNum = (page == null || page < 1) ? 1 : page;
            int pageSize = (size == null || size < 1) ? 10 : (size > 100 ? 100 : size);
            
            // 排序
            Sort sort = buildSort(sortProp, sortOrder);
            Pageable pageable = PageRequest.of(pageNum - 1, pageSize, sort);
            
            // 查询
            Page<AIConversation> conversationPage;
            
            if (StringUtils.hasText(keyword)) {
                // 关键词搜索（标题）
                conversationPage = aiConversationRepository.findByTitleContaining(keyword.trim(), pageable);
            } else if (StringUtils.hasText(userId) && StringUtils.hasText(status)) {
                conversationPage = aiConversationRepository.findByUserIdAndStatus(userId.trim(), status.trim(), pageable);
            } else if (StringUtils.hasText(userId)) {
                conversationPage = aiConversationRepository.findByUserId(userId.trim(), pageable);
            } else if (StringUtils.hasText(status)) {
                conversationPage = aiConversationRepository.findByStatus(status.trim(), pageable);
            } else {
                conversationPage = aiConversationRepository.findAll(pageable);
            }
            
            // 构建返回结果
            PageResultVO<AIConversation> pageResult = new PageResultVO<>();
            pageResult.setList(conversationPage.getContent());
            pageResult.setTotal(conversationPage.getTotalElements());
            pageResult.setCurrent(pageNum);
            pageResult.setSize(pageSize);
            pageResult.setPages(conversationPage.getTotalPages());
            
            return Result.ok("获取会话列表成功", pageResult);
            
        } catch (Exception e) {
            log.error("获取会话列表失败: {}", e.getMessage(), e);
            return Result.fail("获取会话列表失败: " + e.getMessage());
        }
    }
    
    @Override
    public Result getConversationDetail(String conversationId) {
        try {
            if (!StringUtils.hasText(conversationId)) {
                return Result.fail("会话ID不能为空");
            }
            
            AIConversation conversation = aiConversationRepository.findById(conversationId)
                .orElse(null);
            
            if (conversation == null) {
                return Result.fail("会话不存在");
            }
            
            return Result.ok("获取会话详情成功", conversation);
            
        } catch (Exception e) {
            log.error("获取会话详情失败: conversationId={}, error={}", conversationId, e.getMessage(), e);
            return Result.fail("获取会话详情失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public Result deleteConversation(String conversationId) {
        try {
            if (!StringUtils.hasText(conversationId)) {
                return Result.fail("会话ID不能为空");
            }
            
            AIConversation conversation = aiConversationRepository.findById(conversationId)
                .orElse(null);
            
            if (conversation == null) {
                return Result.fail("会话不存在");
            }
            
            conversation.setStatus("DELETED");
            aiConversationRepository.save(conversation);
            
            log.info("删除会话: conversationId={}", conversationId);
            return Result.ok("删除会话成功");
            
        } catch (Exception e) {
            log.error("删除会话失败: conversationId={}, error={}", conversationId, e.getMessage(), e);
            return Result.fail("删除会话失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public Result batchDeleteConversations(List<String> conversationIds) {
        try {
            if (conversationIds == null || conversationIds.isEmpty()) {
                return Result.fail("会话ID列表不能为空");
            }
            
            List<AIConversation> conversations = aiConversationRepository.findAllById(conversationIds);
            
            for (AIConversation conversation : conversations) {
                conversation.setStatus("DELETED");
            }
            
            aiConversationRepository.saveAll(conversations);
            
            log.info("批量删除会话: count={}", conversations.size());
            return Result.ok("批量删除会话成功，共删除 " + conversations.size() + " 条记录");
            
        } catch (Exception e) {
            log.error("批量删除会话失败: error={}", e.getMessage(), e);
                return Result.fail("批量删除会话失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public Result restoreConversation(String conversationId) {
        try {
            if (!StringUtils.hasText(conversationId)) {
                return Result.fail("会话ID不能为空");
            }
            
            AIConversation conversation = aiConversationRepository.findById(conversationId)
                .orElse(null);
            
            if (conversation == null) {
                return Result.fail("会话不存在");
            }
            
            conversation.setStatus("ACTIVE");
            aiConversationRepository.save(conversation);
            
            log.info("恢复会话: conversationId={}", conversationId);
            return Result.ok("恢复会话成功");
            
        } catch (Exception e) {
            log.error("恢复会话失败: conversationId={}, error={}", conversationId, e.getMessage(), e);
            return Result.fail("恢复会话失败: " + e.getMessage());
        }
    }
    
    @Override
    public Result getConversationStatistics() {
        try {
            Map<String, Object> statistics = new HashMap<>();
            
            long totalCount = aiConversationRepository.count();
            long activeCount = aiConversationRepository.countByStatus("ACTIVE");
            long deletedCount = aiConversationRepository.countByStatus("DELETED");
            
            statistics.put("totalCount", totalCount);
            statistics.put("activeCount", activeCount);
            statistics.put("deletedCount", deletedCount);
            
            return Result.ok("获取统计信息成功", statistics);
            
        } catch (Exception e) {
            log.error("获取统计信息失败: error={}", e.getMessage(), e);
            return Result.fail("获取统计信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 构建排序对象
     */
    private Sort buildSort(String sortProp, String sortOrder) {
        if (!StringUtils.hasText(sortProp)) {
            return Sort.by(Sort.Direction.DESC, "updatedAt");
        }
        
        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) 
            ? Sort.Direction.ASC 
            : Sort.Direction.DESC;
        
        return Sort.by(direction, sortProp);
    }
}

