<template>
  <div class="ai-engine-page">
    <div class="page-header">
      <h2>AI 引擎管理</h2>
      <p class="page-desc">管理 AI Agent 会话与用户画像，便于排障与重新构建 AI 能力</p>
    </div>

    <!-- AI Conversation Management -->
    <div class="management-section">
      <div class="section-header">
        <div>
          <h3>AI 会话管理</h3>
          <p>查看并维护 AI Agent 的会话记录，可执行删除、恢复等操作</p>
        </div>
        <el-button type="primary" link @click="loadConversationStats">刷新统计</el-button>
      </div>

      <div class="stats-row" v-if="conversationStats">
        <div class="stat-card">
          <div class="stat-label">会话总数</div>
          <div class="stat-value">{{ conversationStats.totalCount ?? 0 }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">活跃会话</div>
          <div class="stat-value">{{ conversationStats.activeCount ?? 0 }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">已删除</div>
          <div class="stat-value">{{ conversationStats.deletedCount ?? 0 }}</div>
        </div>
      </div>

      <el-form :inline="true" :model="conversationParams" class="filter-form" @submit.prevent>
        <el-form-item label="用户ID">
          <el-input
            v-model="conversationParams.userId"
            placeholder="user_xxx"
            clearable
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="conversationParams.status"
            placeholder="全部"
            clearable
            style="width: 160px"
          >
            <el-option label="活跃" value="ACTIVE" />
            <el-option label="已删除" value="DELETED" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="conversationParams.keyword"
            placeholder="搜索标题 / 最后一条消息"
            clearable
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleConversationSearch">查询</el-button>
          <el-button @click="resetConversationFilters">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="table-operations">
        <el-button
          type="danger"
          plain
          :disabled="!conversationSelection.length"
          @click="handleBatchDeleteConversations"
        >
          批量删除
        </el-button>
      </div>

      <el-table
        :data="conversationList"
        border
        stripe
        v-loading="conversationLoading"
        @selection-change="(selection) => (conversationSelection = selection)"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="conversationId" label="会话ID" min-width="160" show-overflow-tooltip />
        <el-table-column prop="userId" label="用户ID" min-width="120" show-overflow-tooltip />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="messageCount" label="消息数" width="90" align="center" />
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
              {{ row.status === 'ACTIVE' ? '活跃' : '已删除' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openConversationDetail(row.conversationId)">
              详情
            </el-button>
            <el-button
              type="danger"
              link
              @click="handleDeleteConversation(row.conversationId)"
              v-if="row.status === 'ACTIVE'"
            >
              删除
            </el-button>
            <el-button
              type="success"
              link
              @click="handleRestoreConversation(row.conversationId)"
              v-else
            >
              恢复
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :current-page="conversationParams.page"
          :page-size="conversationParams.size"
          :page-sizes="[10, 20, 50]"
          :total="conversationTotal"
          @size-change="(size) => handleConversationSizeChange(size)"
          @current-change="(page) => handleConversationPageChange(page)"
        />
      </div>
    </div>

    <!-- AI Profile Management -->
    <div class="management-section" v-if="isSystemAdmin">
      <div class="section-header">
        <div>
          <h3>AI 画像管理</h3>
          <p>管理用户画像向量（类似索引管理），可重建、生成、删除画像</p>
        </div>
        <el-button type="primary" link @click="loadProfileStats">刷新统计</el-button>
      </div>

      <div class="stats-row" v-if="profileStats">
        <div class="stat-card">
          <div class="stat-label">画像总数</div>
          <div class="stat-value">{{ profileStats.totalProfiles ?? 0 }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">24h 内更新</div>
          <div class="stat-value">{{ profileStats.recentUpdated ?? 0 }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">覆盖率</div>
          <div class="stat-value">{{ profileStats.coverage ?? '0%' }}</div>
        </div>
      </div>

      <div class="operation-grid">
        <el-card class="operation-card">
          <template #header>重建所有用户画像</template>
          <p>建议在低峰期执行，将为所有用户重新生成画像向量。</p>
          <el-button
            type="primary"
            :loading="reindexLoading"
            @click="handleReindexProfiles"
          >
            {{ reindexLoading ? '重建中...' : '重建全部画像' }}
          </el-button>
        </el-card>

        <el-card class="operation-card">
          <template #header>生成/更新单个用户画像</template>
          <el-input
            v-model="singleUserId"
            placeholder="请输入用户ID"
            style="margin-bottom: 10px"
          />
          <el-button
            type="primary"
            :disabled="!singleUserId"
            :loading="generateLoading"
            @click="handleGenerateProfile"
          >
            {{ generateLoading ? '生成中...' : '生成/更新画像' }}
          </el-button>
        </el-card>

        <el-card class="operation-card">
          <template #header>批量生成/更新画像</template>
          <el-input
            type="textarea"
            v-model="batchUserIds"
            placeholder="每行一个用户ID"
            :rows="4"
            style="margin-bottom: 10px"
          />
          <el-button
            type="primary"
            :disabled="!batchUserIds"
            :loading="batchGenerateLoading"
            @click="handleBatchGenerateProfiles"
          >
            {{ batchGenerateLoading ? '执行中...' : '批量生成' }}
          </el-button>
        </el-card>

        <el-card class="operation-card">
          <template #header>删除用户画像</template>
          <el-input
            v-model="deleteUserId"
            placeholder="请输入用户ID"
            style="margin-bottom: 10px"
          />
          <el-button
            type="danger"
            plain
            :disabled="!deleteUserId"
            :loading="deleteProfileLoading"
            @click="handleDeleteProfile"
          >
            {{ deleteProfileLoading ? '删除中...' : '删除画像' }}
          </el-button>
        </el-card>
      </div>

      <el-form :inline="true" :model="profileParams" class="filter-form" @submit.prevent>
        <el-form-item label="关键词">
          <el-input
            v-model="profileParams.keyword"
            placeholder="搜索用户ID / 内容片段"
            clearable
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleProfileSearch">查询</el-button>
          <el-button @click="resetProfileFilters">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="profileList" border stripe v-loading="profileLoading">
        <el-table-column prop="userId" label="用户ID" min-width="140" show-overflow-tooltip />
        <el-table-column prop="preview" label="画像概要" min-width="220" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openProfileDetail(row.userId)">详情</el-button>
            <el-button type="danger" link @click="handleDeleteProfile(row.userId)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :current-page="profileParams.page"
          :page-size="profileParams.size"
          :page-sizes="[10, 20, 50]"
          :total="profileTotal"
          @size-change="(size) => handleProfileSizeChange(size)"
          @current-change="(page) => handleProfilePageChange(page)"
        />
      </div>
    </div>

    <!-- Conversation Detail Dialog -->
    <el-dialog
      v-model="conversationDetailVisible"
      title="会话详情"
      width="520px"
      destroy-on-close
    >
      <el-descriptions v-if="conversationDetail" :column="1" border>
        <el-descriptions-item label="会话ID">
          {{ conversationDetail.conversationId }}
        </el-descriptions-item>
        <el-descriptions-item label="用户ID">
          {{ conversationDetail.userId }}
        </el-descriptions-item>
        <el-descriptions-item label="标题">
          {{ conversationDetail.title || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="消息数量">
          {{ conversationDetail.messageCount }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="conversationDetail.status === 'ACTIVE' ? 'success' : 'info'">
            {{ conversationDetail.status === 'ACTIVE' ? '活跃' : '已删除' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ formatTime(conversationDetail.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">
          {{ formatTime(conversationDetail.updatedAt) }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="conversationDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- Profile Detail Dialog -->
    <el-dialog
      v-model="profileDetailVisible"
      title="画像详情"
      width="640px"
      destroy-on-close
    >
      <div v-if="profileDetail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="用户ID">
            {{ profileDetail.userId }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatTime(profileDetail.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间">
            {{ formatTime(profileDetail.updatedAt) }}
          </el-descriptions-item>
        </el-descriptions>
        <div class="profile-raw-block">
          <h4>画像内容</h4>
          <pre>{{ profileDetail.content || '暂无内容' }}</pre>
        </div>
        <div class="profile-raw-block" v-if="profileDetail.metadata">
          <h4>画像元数据</h4>
          <pre>{{ formatMetadata(profileDetail.metadata) }}</pre>
        </div>
      </div>
      <template #footer>
        <el-button @click="profileDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import { aiConversationAPI } from '../api/admin/aiConversations'
import { userProfileVectorAPI } from '../api/admin/userProfileVectors'
import { authAPI } from '../api/admin/auth'

export default {
  name: 'AIEngine',
  data() {
    return {
      // 权限
      isSystemAdmin: false,

      // 会话列表
      conversationParams: {
        page: 1,
        size: 10,
        userId: '',
        status: '',
        keyword: '',
        sortProp: 'updatedAt',
        sortOrder: 'desc'
      },
      conversationList: [],
      conversationTotal: 0,
      conversationSelection: [],
      conversationLoading: false,
      conversationStats: null,
      conversationDetailVisible: false,
      conversationDetail: null,

      // 用户画像
      profileParams: {
        page: 1,
        size: 10,
        keyword: '',
        sortProp: 'updated_at',
        sortOrder: 'desc'
      },
      profileList: [],
      profileTotal: 0,
      profileLoading: false,
      profileStats: null,
      profileDetailVisible: false,
      profileDetail: null,
      reindexLoading: false,
      generateLoading: false,
      batchGenerateLoading: false,
      deleteProfileLoading: false,
      singleUserId: '',
      batchUserIds: '',
      deleteUserId: ''
    }
  },
  methods: {
    async fetchAdminInfo() {
      try {
        const res = await authAPI.me()
        if (res?.success && res.data) {
          const level = (res.data.adminLevel || '').toLowerCase()
          this.isSystemAdmin = level === 'system'
          if (this.isSystemAdmin) {
            this.loadProfileStats()
            this.loadProfileList()
          }
        }
      } catch (error) {
        console.error('获取管理员信息失败:', error)
      }
    },
    formatTime(value) {
      if (!value) return '-'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return value
      return date.toLocaleString()
    },
    formatMetadata(metadata) {
      if (!metadata) return ''
      if (typeof metadata === 'string') return metadata
      return JSON.stringify(metadata, null, 2)
    },
    // 会话处理
    async loadConversationList() {
      this.conversationLoading = true
      try {
        const res = await aiConversationAPI.getConversations({ ...this.conversationParams })
        if (res?.success && res.data) {
          this.conversationList = res.data.list || []
          this.conversationTotal = res.data.total || 0
        }
      } catch (error) {
        console.error('加载会话列表失败:', error)
        ElMessage.error('加载会话列表失败')
      } finally {
        this.conversationLoading = false
      }
    },
    async loadConversationStats() {
      try {
        const res = await aiConversationAPI.getStats()
        if (res?.success) {
          this.conversationStats = res.data || {}
        }
      } catch (error) {
        console.error('加载会话统计失败:', error)
      }
    },
    handleConversationSearch() {
      this.conversationParams.page = 1
      this.loadConversationList()
    },
    resetConversationFilters() {
      this.conversationParams = {
        page: 1,
        size: this.conversationParams.size,
        userId: '',
        status: '',
        keyword: '',
        sortProp: 'updatedAt',
        sortOrder: 'desc'
      }
      this.loadConversationList()
    },
    handleConversationSizeChange(size) {
      this.conversationParams.size = size
      this.conversationParams.page = 1
      this.loadConversationList()
    },
    handleConversationPageChange(page) {
      this.conversationParams.page = page
      this.loadConversationList()
    },
    async handleDeleteConversation(conversationId) {
      try {
        await ElMessageBox.confirm('确定删除该会话吗？删除后可通过“恢复”操作找回。', '提示', {
          type: 'warning'
        })
        const res = await aiConversationAPI.deleteConversation(conversationId)
        if (res?.success) {
          ElMessage.success('删除成功')
          this.loadConversationList()
          this.loadConversationStats()
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除会话失败:', error)
          ElMessage.error('删除会话失败')
        }
      }
    },
    async handleRestoreConversation(conversationId) {
      try {
        const res = await aiConversationAPI.restoreConversation(conversationId)
        if (res?.success) {
          ElMessage.success('恢复成功')
          this.loadConversationList()
          this.loadConversationStats()
        }
      } catch (error) {
        console.error('恢复会话失败:', error)
        ElMessage.error('恢复会话失败')
      }
    },
    async handleBatchDeleteConversations() {
      if (!this.conversationSelection.length) return
      try {
        await ElMessageBox.confirm(
          `确定删除选中的 ${this.conversationSelection.length} 个会话吗？`,
          '批量删除',
          { type: 'warning' }
        )
        const ids = this.conversationSelection.map((item) => item.conversationId)
        const res = await aiConversationAPI.batchDeleteConversations(ids)
        if (res?.success) {
          ElMessage.success('批量删除成功')
          this.loadConversationList()
          this.loadConversationStats()
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('批量删除失败:', error)
          ElMessage.error('批量删除失败')
        }
      }
    },
    async openConversationDetail(conversationId) {
      try {
        const res = await aiConversationAPI.getConversationDetail(conversationId)
        if (res?.success) {
          this.conversationDetail = res.data
          this.conversationDetailVisible = true
        }
      } catch (error) {
        console.error('获取会话详情失败:', error)
        ElMessage.error('获取会话详情失败')
      }
    },

    // 用户画像
    async loadProfileStats() {
      if (!this.isSystemAdmin) return
      try {
        const res = await userProfileVectorAPI.getProfileStatistics()
        if (res?.success) {
          this.profileStats = res.data || {}
        }
      } catch (error) {
        console.error('加载画像统计失败:', error)
      }
    },
    async loadProfileList() {
      if (!this.isSystemAdmin) return
      this.profileLoading = true
      try {
        const res = await userProfileVectorAPI.getProfileList({ ...this.profileParams })
        if (res?.success && res.data) {
          this.profileTotal = res.data.total || 0
          this.profileList = (res.data.list || []).map((item) => ({
            userId: item.user_id || item.userId,
            content: item.content,
            preview: item.content ? item.content.slice(0, 60) + (item.content.length > 60 ? '...' : '') : '',
            metadata: item.metadata,
            createdAt: item.created_at || item.createdAt,
            updatedAt: item.updated_at || item.updatedAt
          }))
        }
      } catch (error) {
        console.error('加载画像列表失败:', error)
        ElMessage.error('加载画像列表失败')
      } finally {
        this.profileLoading = false
      }
    },
    handleProfileSearch() {
      this.profileParams.page = 1
      this.loadProfileList()
    },
    resetProfileFilters() {
      this.profileParams = {
        page: 1,
        size: this.profileParams.size,
        keyword: '',
        sortProp: 'updated_at',
        sortOrder: 'desc'
      }
      this.loadProfileList()
    },
    handleProfileSizeChange(size) {
      this.profileParams.size = size
      this.profileParams.page = 1
      this.loadProfileList()
    },
    handleProfilePageChange(page) {
      this.profileParams.page = page
      this.loadProfileList()
    },
    async handleReindexProfiles() {
      try {
        await ElMessageBox.confirm('确定重建所有用户画像吗？操作耗时且占用资源，建议低峰期执行。', '提示', {
          type: 'warning'
        })
        this.reindexLoading = true
        const res = await userProfileVectorAPI.reindexAll()
        if (res?.success) {
          ElMessage.success(res.data?.message || '重建任务已提交')
          this.loadProfileStats()
          this.loadProfileList()
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('重建画像失败:', error)
          ElMessage.error('重建画像失败')
        }
      } finally {
        this.reindexLoading = false
      }
    },
    async handleGenerateProfile() {
      if (!this.singleUserId) {
        ElMessage.warning('请输入用户ID')
        return
      }
      try {
        this.generateLoading = true
        const res = await userProfileVectorAPI.generateProfile(this.singleUserId.trim())
        if (res?.success) {
          ElMessage.success('生成任务已提交')
          this.singleUserId = ''
          this.loadProfileStats()
          this.loadProfileList()
        }
      } catch (error) {
        console.error('生成画像失败:', error)
        ElMessage.error('生成画像失败')
      } finally {
        this.generateLoading = false
      }
    },
    async handleBatchGenerateProfiles() {
      const ids = this.batchUserIds
        .split(/[\n,]/)
        .map((id) => id.trim())
        .filter(Boolean)
      if (!ids.length) {
        ElMessage.warning('请输入至少一个用户ID')
        return
      }
      try {
        this.batchGenerateLoading = true
        const res = await userProfileVectorAPI.batchGenerateProfiles(ids)
        if (res?.success) {
          ElMessage.success(res.data?.message || '批量任务已提交')
          this.batchUserIds = ''
          this.loadProfileStats()
          this.loadProfileList()
        }
      } catch (error) {
        console.error('批量生成失败:', error)
        ElMessage.error('批量生成失败')
      } finally {
        this.batchGenerateLoading = false
      }
    },
    async handleDeleteProfile(userId) {
      const targetId = typeof userId === 'string' ? userId : this.deleteUserId
      if (!targetId) {
        ElMessage.warning('请输入用户ID')
        return
      }
      try {
        await ElMessageBox.confirm(`确定删除用户 ${targetId} 的画像吗？`, '删除画像', {
          type: 'warning'
        })
        this.deleteProfileLoading = true
        const res = await userProfileVectorAPI.deleteProfile(targetId.trim())
        if (res?.success) {
          ElMessage.success('删除成功')
          if (typeof userId !== 'string') {
            this.deleteUserId = ''
          }
          this.loadProfileStats()
          this.loadProfileList()
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除画像失败:', error)
          ElMessage.error('删除画像失败')
        }
      } finally {
        this.deleteProfileLoading = false
      }
    },
    async openProfileDetail(userId) {
      try {
        const res = await userProfileVectorAPI.getProfileDetail(userId)
        if (res?.success) {
          this.profileDetail = res.data
          this.profileDetailVisible = true
        }
      } catch (error) {
        console.error('获取画像详情失败:', error)
        ElMessage.error('获取画像详情失败')
      }
    }
  },
  mounted() {
    this.loadConversationStats()
    this.loadConversationList()
    this.fetchAdminInfo()
  }
}
</script>

<style scoped>
.ai-engine-page {
  padding: 20px;
}

.page-header {
  margin-bottom: 25px;
}

.page-header h2 {
  margin: 0 0 6px 0;
  font-size: 24px;
  color: #303133;
}

.page-desc {
  color: #909399;
  margin: 0;
}

.management-section {
  margin-bottom: 32px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.section-header h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.section-header p {
  margin: 4px 0 0;
  color: #909399;
  font-size: 13px;
}

.stats-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.stat-card {
  flex: 1;
  min-width: 160px;
  background: #f5f7fa;
  border-radius: 6px;
  padding: 16px;
}

.stat-label {
  color: #909399;
  font-size: 13px;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.filter-form {
  margin-bottom: 10px;
}

.table-operations {
  margin: 10px 0;
  display: flex;
  justify-content: flex-end;
}

.pagination-wrap {
  margin-top: 15px;
  display: flex;
  justify-content: flex-end;
}

.operation-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 15px;
  margin-bottom: 20px;
}

.operation-card {
  min-height: 160px;
}

.operation-card p {
  margin: 0 0 12px;
  color: #606266;
}

.profile-raw-block {
  margin-top: 18px;
}

.profile-raw-block h4 {
  margin: 0 0 8px;
  font-size: 15px;
  color: #303133;
}

.profile-raw-block pre {
  background: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
  font-size: 13px;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}
</style>


