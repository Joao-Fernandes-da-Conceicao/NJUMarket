package com.njumarket.njumarket.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户内部传输对象（用于服务间通信）
 * 不包含关联对象，只包含必要字段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInternalDTO implements Serializable {
    private String userId;
    private String username;
    private String primaryPhone;
    private String accountStatus;
    private LocalDateTime registerTime;
    // 不包含 UserProfile、Order、Complaint 等关联对象
}

