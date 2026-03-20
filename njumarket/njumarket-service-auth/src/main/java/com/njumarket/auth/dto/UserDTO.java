package com.njumarket.auth.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户数据传输对象
 */
@Data
public class UserDTO {
    private String userId;
    private String primaryPhone;
    private String accountStatus;
    private LocalDateTime registerTime;
    private String nickname;
    private String avatar;
}

