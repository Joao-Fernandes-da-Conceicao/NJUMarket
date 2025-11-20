<template>
  <div class="elasticsearch-management">
    <div class="page-header">
      <h2>Elasticsearch 索引管理</h2>
      <p class="page-desc">管理商品搜索索引，包括重建索引、同步商品等功能</p>
    </div>

    <div class="management-section">
      <div class="section-title">索引操作</div>
      
      <div class="operation-card">
        <div class="operation-header">
          <h3>重建搜索索引</h3>
          <p class="operation-desc">全量重建商品搜索索引，建议在低峰期执行。此操作会重新索引所有商品数据。</p>
        </div>
        <div class="operation-actions">
          <el-button 
            type="primary" 
            :loading="reindexLoading"
            @click="handleReindex"
            :disabled="reindexLoading">
            {{ reindexLoading ? '重建中...' : '重建索引' }}
          </el-button>
        </div>
        <div v-if="reindexResult" class="operation-result">
          <el-alert
            :type="reindexResult.success ? 'success' : 'error'"
            :title="reindexResult.message"
            :description="reindexResult.description"
            show-icon
            :closable="true"
            @close="reindexResult = null">
          </el-alert>
        </div>
      </div>

      <div class="operation-card">
        <div class="operation-header">
          <h3>同步单个商品</h3>
          <p class="operation-desc">将指定商品同步到搜索索引，用于修复单个商品的数据不一致问题。</p>
        </div>
        <div class="operation-actions">
          <el-input
            v-model="syncCommodityId"
            placeholder="请输入商品ID"
            style="width: 300px; margin-right: 10px;"
            :disabled="syncLoading">
          </el-input>
          <el-button 
            type="primary" 
            :loading="syncLoading"
            @click="handleSyncCommodity"
            :disabled="syncLoading || !syncCommodityId">
            {{ syncLoading ? '同步中...' : '同步商品' }}
          </el-button>
        </div>
        <div v-if="syncResult" class="operation-result">
          <el-alert
            :type="syncResult.success ? 'success' : 'error'"
            :title="syncResult.message"
            :description="syncResult.description"
            show-icon
            :closable="true"
            @close="syncResult = null">
          </el-alert>
        </div>
      </div>
    </div>

    <div class="management-section">
      <div class="section-title">索引统计</div>
      <div class="stats-card">
        <el-button @click="loadStats" :loading="statsLoading">刷新统计</el-button>
        <div v-if="stats" class="stats-content">
          <p>{{ stats.message || '统计信息加载中...' }}</p>
        </div>
      </div>
    </div>

    <div class="management-section">
      <div class="section-title">使用说明</div>
      <div class="info-card">
        <ul>
          <li><strong>重建索引</strong>：适用于首次部署、索引损坏、批量数据导入后等情况。此操作会重新索引所有商品，耗时较长，建议在低峰期执行。</li>
          <li><strong>同步商品</strong>：适用于单个商品数据不一致的情况。输入商品ID后点击同步即可。</li>
          <li><strong>注意事项</strong>：
            <ul>
              <li>重建索引期间，搜索功能可能受到影响</li>
              <li>重建索引会消耗较多系统资源，建议在低峰期执行</li>
              <li>只有 system 权限的管理员可以执行这些操作</li>
            </ul>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script>
import { elasticsearchAPI } from '../api/admin/elasticsearch'

export default {
  name: 'Elasticsearch',
  data() {
    return {
      reindexLoading: false,
      syncLoading: false,
      statsLoading: false,
      syncCommodityId: '',
      reindexResult: null,
      syncResult: null,
      stats: null
    }
  },
  methods: {
    async handleReindex() {
      if (!confirm('确定要重建搜索索引吗？此操作会重新索引所有商品，耗时较长，建议在低峰期执行。')) {
        return
      }
      
      this.reindexLoading = true
      this.reindexResult = null
      
      try {
        const res = await elasticsearchAPI.reindex()
        if (res && res.success) {
          const indexed = res.data?.indexed || 0
          this.reindexResult = {
            success: true,
            message: '索引重建成功',
            description: `已成功重建索引，共索引 ${indexed} 条商品数据。`
          }
        } else {
          this.reindexResult = {
            success: false,
            message: '索引重建失败',
            description: res?.message || '未知错误'
          }
        }
      } catch (error) {
        console.error('重建索引失败:', error)
        this.reindexResult = {
          success: false,
          message: '索引重建失败',
          description: error.message || '网络错误，请稍后重试'
        }
      } finally {
        this.reindexLoading = false
      }
    },
    
    async handleSyncCommodity() {
      if (!this.syncCommodityId) {
        this.$message.warning('请输入商品ID')
        return
      }
      
      this.syncLoading = true
      this.syncResult = null
      
      try {
        const res = await elasticsearchAPI.syncCommodity(this.syncCommodityId)
        if (res && res.success) {
          this.syncResult = {
            success: true,
            message: '商品同步成功',
            description: `商品 ${this.syncCommodityId} 已成功同步到搜索索引。`
          }
          this.syncCommodityId = ''
        } else {
          this.syncResult = {
            success: false,
            message: '商品同步失败',
            description: res?.message || '未知错误'
          }
        }
      } catch (error) {
        console.error('同步商品失败:', error)
        this.syncResult = {
          success: false,
          message: '商品同步失败',
          description: error.message || '网络错误，请稍后重试'
        }
      } finally {
        this.syncLoading = false
      }
    },
    
    async loadStats() {
      this.statsLoading = true
      try {
        const res = await elasticsearchAPI.getStats()
        if (res && res.success) {
          this.stats = res.data
        }
      } catch (error) {
        console.error('获取统计信息失败:', error)
        this.$message.error('获取统计信息失败')
      } finally {
        this.statsLoading = false
      }
    }
  }
}
</script>

<style scoped>
.elasticsearch-management {
  padding: 20px;
}

.page-header {
  margin-bottom: 30px;
}

.page-header h2 {
  margin: 0 0 10px 0;
  font-size: 24px;
  color: #303133;
}

.page-desc {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.management-section {
  margin-bottom: 30px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 2px solid #409EFF;
}

.operation-card,
.stats-card,
.info-card {
  background: #fff;
  border: 1px solid #EBEEF5;
  border-radius: 4px;
  padding: 20px;
  margin-bottom: 15px;
}

.operation-header h3 {
  margin: 0 0 10px 0;
  font-size: 16px;
  color: #303133;
}

.operation-desc {
  margin: 0 0 15px 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
}

.operation-actions {
  margin-bottom: 15px;
}

.operation-result {
  margin-top: 15px;
}

.stats-content {
  margin-top: 15px;
  padding: 15px;
  background: #F5F7FA;
  border-radius: 4px;
}

.info-card ul {
  margin: 0;
  padding-left: 20px;
  color: #606266;
  line-height: 1.8;
}

.info-card ul ul {
  margin-top: 10px;
}

.info-card li {
  margin-bottom: 10px;
}

.info-card strong {
  color: #303133;
}
</style>

