package com.njumarket.message.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 联系方式实体类
 * 存储用户的各种联系方式，支持加密存储
 */
@Entity
@Table(name = "contact_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactInfo {
    
    @Id
    @Column(name = "contact_id", length = 50)
    private String contactId;
    
    @Column(name = "owner_id", length = 50, nullable = false)
    private String ownerId;
    
    @Column(name = "type", length = 20, nullable = false)
    private String type; // PHONE, EMAIL, WECHAT, QQ
    
    @Column(name = "value_encrypted", length = 500, nullable = false)
    private String valueEncrypted;
    
    /**
     * 向指定用户展示联系方式
     * @param userId 请求查看的用户ID
     * @return 是否允许展示
     */
    public Boolean revealTo(String userId) {
        // 业务逻辑：检查权限，解密并返回联系方式
        return true;
    }
}

