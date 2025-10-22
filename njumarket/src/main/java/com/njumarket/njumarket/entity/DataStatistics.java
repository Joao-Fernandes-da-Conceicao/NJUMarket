package com.njumarket.njumarket.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据统计实体类
 * 存储各种统计数据和分析结果
 */
@Entity
@Table(name = "data_statistics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataStatistics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "cycle", length = 20, nullable = false)
    private String cycle; // DAILY, WEEKLY, MONTHLY, YEARLY
    
    @Column(name = "dimension", length = 50, nullable = false)
    private String dimension; // SALES, USER_ACTIVITY, COMMODITY_VIEWS, REVENUE
    
    @Column(name = "value", nullable = false)
    private Double value;
    
    @Column(name = "category", length = 50)
    private String category;
    
    @Column(name = "date_key", length = 20, nullable = false)
    private String dateKey; // 格式：YYYY-MM-DD 或 YYYY-MM 或 YYYY
    
    @CreationTimestamp
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;
    
    @Column(name = "extra_data", columnDefinition = "TEXT")
    private String extraData; // JSON格式存储额外数据
    
    /**
     * 导出Excel文件
     * @return 文件对象
     */
    public Object exportExcel() {
        // 业务逻辑：生成Excel文件
        return null;
    }
    
    /**
     * 生成图表
     * @return 图片对象
     */
    public Object generateChart() {
        // 业务逻辑：生成统计图表
        return null;
    }
    
    /**
     * 获取趋势数据
     * @return 趋势数据列表
     */
    public static List<DataStatistics> getTrendData(String dimension, String cycle, int periods) {
        // 业务逻辑：查询趋势数据
        return null;
    }
}
