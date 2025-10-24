package com.njumarket.njumarket.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品实体类
 * 包含商品的基本信息、状态管理和业务方法
 */
@Entity
@Table(name = "commodities")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Commodity {
    
    @Id
    @Column(name = "commodity_id", length = 50)
    private String commodityId;
    
    @Column(name = "seller_id", length = 50, nullable = false)
    private String sellerId;
    
    @Column(name = "title", length = 200, nullable = false)
    private String title;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "price", nullable = false)
    private Double price;
    
    @Column(name = "stock", nullable = false)
    private Integer stock;
    
    @Column(name = "location", length = 200)
    private String location;
    
    @CreationTimestamp
    @Column(name = "publish_time", nullable = false)
    private LocalDateTime publishTime;
    
    @Column(name = "commodity_status", length = 20, nullable = false)
    private String commodityStatus; // DRAFT, PUBLISHED, ON_SHELF, OFF_SHELF, SOLD_OUT
    
    @Column(name = "seller_visibility", length = 20, nullable = false)
    private String sellerVisibility = "PUBLIC"; // PUBLIC, PRIVATE, HIDDEN
    
    @Column(name = "buyer_visibility", length = 20, nullable = false)
    private String buyerVisibility = "PUBLIC"; // PUBLIC, PRIVATE, HIDDEN
    
    @Column(name = "category", length = 50)
    private String category;
    
    @Column(name = "condition_level", length = 20)
    private String conditionLevel = "GOOD"; // EXCELLENT, GOOD, FAIR, POOR
    
    @Column(name = "images", columnDefinition = "TEXT")
    private String images; // JSON格式的图片URL列表
    
    @Column(name = "click_count", nullable = false)
    private Integer clickCount = 0;
    
    @Column(name = "report_count", nullable = false)
    private Integer reportCount = 0;
    
    // 多对一关系：商品属于某个卖家
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", insertable = false, updatable = false)
    private User seller;
    
    // 一对多关系：商品的订单
    @OneToMany(mappedBy = "commodity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Order> orders;
    
    // 一对多关系：商品的审核记录
    @OneToMany(mappedBy = "commodity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AuditRecord> auditRecords;
    
    /**
     * 检查商品合规性
     * @return 是否合规
     */
    public Boolean checkCompliance() {
        // 业务逻辑：检查商品标题、描述、图片等是否合规
        return true;
    }
    
    /**
     * 更新库存
     * @param num 库存变化数量（正数增加，负数减少）
     * @return 更新是否成功
     */
    public Boolean updateStock(Integer num) {
        if (this.stock + num >= 0) {
            this.stock += num;
            return true;
        }
        return false;
    }
    
    /**
     * 发布商品
     * @return 发布是否成功
     */
    public Boolean publish() {
        if (checkCompliance()) {
            this.commodityStatus = "PUBLISHED";
            return true;
        }
        return false;
    }
    
    /**
     * 下架商品
     * @return 下架是否成功
     */
    public Boolean unpublish() {
        this.commodityStatus = "DRAFT";
        this.sellerVisibility = "PRIVATE";
        this.buyerVisibility = "HIDDEN";
        return true;
    }
    
    /**
     * 设置卖家可见性
     * @param sellerVisibility 卖家可见性状态
     * @return 设置是否成功
     */
    public Boolean setSellerVisibility(String sellerVisibility) {
        if ("PUBLIC".equals(sellerVisibility) || "PRIVATE".equals(sellerVisibility) || "HIDDEN".equals(sellerVisibility)) {
            this.sellerVisibility = sellerVisibility;
            return true;
        }
        return false;
    }
    
    /**
     * 设置买家可见性
     * @param buyerVisibility 买家可见性状态
     * @return 设置是否成功
     */
    public Boolean setBuyerVisibility(String buyerVisibility) {
        if ("PUBLIC".equals(buyerVisibility) || "PRIVATE".equals(buyerVisibility) || "HIDDEN".equals(buyerVisibility)) {
            this.buyerVisibility = buyerVisibility;
            return true;
        }
        return false;
    }
    
    /**
     * 设置可见性（兼容旧接口）
     * @param visibility 可见性状态
     * @return 设置是否成功
     */
    public Boolean setVisibility(String visibility) {
        return setSellerVisibility(visibility) && setBuyerVisibility(visibility);
    }
    
    /**
     * 检查商品对卖家是否可见
     * @return 是否可见
     */
    public Boolean isVisibleToSeller() {
        return "PUBLIC".equals(this.sellerVisibility) && ("PUBLISHED".equals(this.commodityStatus) || "ON_SHELF".equals(this.commodityStatus));
    }
    
    /**
     * 检查商品对买家是否可见
     * @return 是否可见
     */
    public Boolean isVisibleToBuyer() {
        return "PUBLIC".equals(this.buyerVisibility) && "ON_SHELF".equals(this.commodityStatus);
    }
    
    /**
     * 检查商品是否可见（兼容旧接口）
     * @return 是否可见
     */
    public Boolean isVisible() {
        return isVisibleToSeller() && isVisibleToBuyer();
    }
}
