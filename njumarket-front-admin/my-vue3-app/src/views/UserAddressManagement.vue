<template>
  <div>
    <div class="page-header">
      <h2>用户地址管理</h2>
      <div class="user-info" v-if="userInfo">
        <span>用户ID: {{ userInfo.userId }}</span>
        <span>用户名: {{ userInfo.username }}</span>
        <span v-if="userInfo.profile?.nickname">昵称: {{ userInfo.profile.nickname }}</span>
      </div>
    </div>

    <div class="address-panel">
      <div class="address-panel__header">
        <h3>地址列表</h3>
        <UnifiedButton type="primary" @click="openAddressDialog()">新增地址</UnifiedButton>
      </div>
      <el-table
        :data="addresses"
        stripe
        border
        class="address-table"
        v-loading="addressLoading"
        empty-text="暂无地址信息"
      >
        <el-table-column prop="recipientName" label="收货人" width="120" />
        <el-table-column prop="recipientPhone" label="手机号" width="140" />
        <el-table-column label="地区" min-width="180">
          <template #default="scope">
            {{ formatRegion(scope.row) }}
          </template>
        </el-table-column>
        <el-table-column label="详细地址" min-width="220">
          <template #default="scope">
            {{ scope.row.streetAddress }} {{ scope.row.detailAddress || '' }}
          </template>
        </el-table-column>
        <el-table-column prop="addressLabel" label="标签" width="90" />
        <el-table-column label="状态" width="150">
          <template #default="scope">
            <el-tag size="small" type="success" v-if="scope.row.isDefault">默认</el-tag>
            <el-tag size="small" :type="scope.row.isActive ? 'info' : 'danger'">
              {{ scope.row.isActive ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="scope">
            <el-space wrap size="small">
              <UnifiedButton size="small" @click="openAddressDialog(scope.row)">编辑</UnifiedButton>
              <UnifiedButton
                size="small"
                type="primary"
                :disabled="scope.row.isDefault"
                @click="handleSetDefault(scope.row)"
              >
                设为默认
              </UnifiedButton>
              <UnifiedButton
                size="small"
                type="danger"
                plain
                @click="handleDeleteAddress(scope.row)"
              >
                删除
              </UnifiedButton>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="addressDialogVisible"
      :title="addressDialogTitle"
      width="700px"
      destroy-on-close
    >
      <el-form :model="addressForm" label-width="100px" class="address-form">
        <el-form-item label="收货人">
          <UnifiedInput v-model="addressForm.recipientName" placeholder="请输入收货人" />
        </el-form-item>
        <el-form-item label="手机号">
          <UnifiedInput v-model="addressForm.recipientPhone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="地图标注">
          <AddressMapPicker
            v-model="mapLocation"
            :default-location="mapDefaultLocation"
            @change="handleMapLocationChange"
          />
        </el-form-item>
        <el-form-item label="省 / 市 / 区">
          <div class="region-row">
            <UnifiedInput
              v-model="addressForm.province"
              placeholder="省份"
              @input="handleAddressPartChange"
            />
            <UnifiedInput
              v-model="addressForm.city"
              placeholder="城市"
              @input="handleAddressPartChange"
            />
            <UnifiedInput
              v-model="addressForm.district"
              placeholder="区/县"
              @input="handleAddressPartChange"
            />
          </div>
        </el-form-item>
        <el-form-item label="街道">
          <UnifiedInput
            v-model="addressForm.streetAddress"
            placeholder="街道/镇"
            @input="handleAddressPartChange"
          />
        </el-form-item>
        <el-form-item label="详细地址">
          <UnifiedInput
            v-model="addressForm.detailAddress"
            placeholder="楼栋、门牌等"
            @input="handleAddressPartChange"
          />
        </el-form-item>
        <el-form-item label="完整地址">
          <el-input
            v-model="addressForm.fullAddress"
            type="textarea"
            rows="2"
            placeholder="默认根据上方字段拼接，可手动调整"
            @input="handleFullAddressInput"
          />
        </el-form-item>
        <el-form-item label="标签">
          <UnifiedSelect
            v-model="addressForm.addressLabel"
            :options="addressLabelOptions"
            placeholder="请选择标签"
          />
        </el-form-item>
        <el-form-item label="默认地址">
          <el-switch v-model="addressForm.isDefault" />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="addressForm.isActive" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-space>
          <UnifiedButton @click="addressDialogVisible = false">取消</UnifiedButton>
          <UnifiedButton type="primary" @click="saveAddress" :loading="addressSaving">保存</UnifiedButton>
        </el-space>
      </template>
    </el-dialog>

    <div class="page-footer">
      <UnifiedButton @click="$router.back()">返回</UnifiedButton>
    </div>
  </div>
</template>

<script>
import UnifiedInput from '../components/common/UnifiedInput.vue'
import UnifiedButton from '../components/common/UnifiedButton.vue'
import UnifiedSelect from '../components/common/UnifiedSelect.vue'
import AddressMapPicker from '../components/address/AddressMapPicker.vue'
import { usersAPI } from '../api/admin/users'
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
  name: 'UserAddressManagement',
  components: { UnifiedInput, UnifiedButton, UnifiedSelect, AddressMapPicker },
  data() {
    return {
      userId: '',
      userInfo: null,
      addresses: [],
      addressLoading: false,
      addressDialogVisible: false,
      addressDialogTitle: '新增地址',
      addressSaving: false,
      addressForm: {
        addressId: '',
        recipientName: '',
        recipientPhone: '',
        province: '',
        city: '',
        district: '',
        streetAddress: '',
        detailAddress: '',
        fullAddress: '',
        addressLabel: 'HOME',
        isDefault: false,
        isActive: true
      },
      addressLabelOptions: [
        { label: '家', value: 'HOME' },
        { label: '公司', value: 'COMPANY' },
        { label: '学校', value: 'SCHOOL' },
        { label: '其他', value: 'OTHER' }
      ],
      // 地图位置相关
      mapLocation: null,
      mapDefaultLocation: { longitude: 118.959, latitude: 32.114 }, // 默认南京大学位置
      addressFullManuallyEdited: false,
      addressSyncReady: false
    }
  },
  async mounted() {
    this.userId = this.$route.params.userId
    if (!this.userId) {
      ElMessage.error('缺少用户ID')
      this.$router.back()
      return
    }
    await this.loadUserInfo()
    await this.loadAddresses()
  },
  methods: {
    async loadUserInfo() {
      try {
        const res = await usersAPI.get(this.userId)
        if (res && res.success) {
          this.userInfo = res.data
        }
      } catch (error) {
        console.error('加载用户信息失败:', error)
      }
    },
    async loadAddresses() {
      this.addressLoading = true
      try {
        const res = await usersAPI.listAddresses(this.userId)
        if (res && res.success) {
          this.addresses = res.data || []
        } else {
          ElMessage.error(res?.message || '加载地址列表失败')
        }
      } catch (error) {
        console.error('加载地址列表失败:', error)
        ElMessage.error('加载地址列表失败')
      } finally {
        this.addressLoading = false
      }
    },
    formatRegion(row) {
      const parts = [row.province, row.city, row.district].filter(Boolean)
      return parts.join('') || '-'
    },
    openAddressDialog(address = null) {
      if (address) {
        // 编辑模式
        this.addressDialogTitle = '编辑地址'
        this.addressForm = {
          addressId: address.addressId,
          recipientName: address.recipientName || '',
          recipientPhone: address.recipientPhone || '',
          province: address.province || '',
          city: address.city || '',
          district: address.district || '',
          streetAddress: address.streetAddress || '',
          detailAddress: address.detailAddress || '',
          fullAddress: address.fullAddress || '',
          addressLabel: address.addressLabel || 'HOME',
          isDefault: address.isDefault || false,
          isActive: address.isActive !== undefined ? address.isActive : true
        }
      } else {
        // 新增模式
        this.addressDialogTitle = '新增地址'
        this.addressForm = {
          addressId: '',
          recipientName: '',
          recipientPhone: '',
          province: '',
          city: '',
          district: '',
          streetAddress: '',
          detailAddress: '',
          fullAddress: '',
          addressLabel: 'HOME',
          isDefault: false,
          isActive: true
        }
      }

      // 初始化完整地址
      this.addressFullManuallyEdited = false
      this.updateFullAddress(true)

      // 初始化地图位置
      if (this.addressForm.fullAddress) {
        this.mapLocation = {
          address: this.addressForm.fullAddress
        }
        // 如果有经纬度，也设置
        if (address && address.longitude && address.latitude) {
          this.mapLocation.longitude = address.longitude
          this.mapLocation.latitude = address.latitude
          this.mapDefaultLocation = {
            longitude: address.longitude,
            latitude: address.latitude
          }
        }
      } else {
        this.mapLocation = null
      }

      this.addressSyncReady = true
      this.addressDialogVisible = true
    },
    handleAddressPartChange() {
      this.updateFullAddress()
    },
    handleFullAddressInput() {
      this.addressFullManuallyEdited = true
    },
    updateFullAddress(force = false) {
      if (!force && this.addressFullManuallyEdited) return
      const parts = [
        this.addressForm.province,
        this.addressForm.city,
        this.addressForm.district,
        this.addressForm.streetAddress,
        this.addressForm.detailAddress
      ].map(item => (item || '').trim()).filter(Boolean)
      this.addressForm.fullAddress = parts.join('')
    },
    handleMapLocationChange(location) {
      if (!this.addressSyncReady || !location) return
      this.mapLocation = { ...location }
      if (location.province) this.addressForm.province = location.province
      if (location.city) this.addressForm.city = location.city
      if (location.district) this.addressForm.district = location.district
      if (location.streetAddress) this.addressForm.streetAddress = location.streetAddress
      if (location.detailAddress) this.addressForm.detailAddress = location.detailAddress
      this.addressFullManuallyEdited = false
      if (location.address) {
        this.addressForm.fullAddress = location.address
      } else {
        this.updateFullAddress(true)
      }
    },
    async saveAddress() {
      if (!this.addressForm.recipientName) {
        ElMessage.warning('请输入收货人')
        return
      }
      if (!this.addressForm.recipientPhone) {
        ElMessage.warning('请输入手机号')
        return
      }
      if (!this.addressForm.province || !this.addressForm.city) {
        ElMessage.warning('请至少填写省份和城市')
        return
      }
      if (!this.addressForm.streetAddress) {
        ElMessage.warning('请输入街道地址')
        return
      }

      this.addressSaving = true
      try {
        // 如果完整地址为空，自动拼接
        if (!this.addressForm.fullAddress) {
          this.updateFullAddress(true)
        }

        const payload = {
          recipientName: this.addressForm.recipientName,
          recipientPhone: this.addressForm.recipientPhone,
          province: this.addressForm.province,
          city: this.addressForm.city,
          district: this.addressForm.district || '',
          streetAddress: this.addressForm.streetAddress,
          detailAddress: this.addressForm.detailAddress || '',
          fullAddress: this.addressForm.fullAddress || '',
          addressLabel: this.addressForm.addressLabel,
          isDefault: this.addressForm.isDefault,
          isActive: this.addressForm.isActive
        }

        // 如果有地图位置，添加经纬度
        if (this.mapLocation && this.mapLocation.longitude && this.mapLocation.latitude) {
          payload.longitude = this.mapLocation.longitude
          payload.latitude = this.mapLocation.latitude
        }

        let res
        if (this.addressForm.addressId) {
          // 更新
          res = await usersAPI.updateAddress(this.userId, this.addressForm.addressId, payload)
        } else {
          // 创建
          res = await usersAPI.createAddress(this.userId, payload)
        }

        if (res && res.success) {
          ElMessage.success(this.addressForm.addressId ? '更新成功' : '创建成功')
          this.addressDialogVisible = false
          await this.loadAddresses()
        } else {
          ElMessage.error(res?.message || '保存失败')
        }
      } catch (error) {
        console.error('保存地址失败:', error)
        ElMessage.error('保存地址失败')
      } finally {
        this.addressSaving = false
      }
    },
    async handleSetDefault(address) {
      try {
        const res = await usersAPI.setDefaultAddress(this.userId, address.addressId)
        if (res && res.success) {
          ElMessage.success('设置默认地址成功')
          await this.loadAddresses()
        } else {
          ElMessage.error(res?.message || '设置失败')
        }
      } catch (error) {
        console.error('设置默认地址失败:', error)
        ElMessage.error('设置默认地址失败')
      }
    },
    async handleDeleteAddress(address) {
      try {
        await ElMessageBox.confirm(
          `确定要删除地址"${address.recipientName} - ${this.formatRegion(address)}"吗？`,
          '确认删除',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )

        const res = await usersAPI.deleteAddress(this.userId, address.addressId)
        if (res && res.success) {
          ElMessage.success('删除成功')
          await this.loadAddresses()
        } else {
          ElMessage.error(res?.message || '删除失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除地址失败:', error)
          ElMessage.error('删除地址失败')
        }
      }
    }
  }
}
</script>

<style scoped>
.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 12px 0;
}

.user-info {
  display: flex;
  gap: 16px;
  font-size: 14px;
  color: #666;
}

.address-panel {
  margin-bottom: 20px;
}

.address-panel__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.address-panel__header h3 {
  margin: 0;
  font-size: 16px;
}

.address-table {
  width: 100%;
}

.address-form {
  padding: 0 20px;
}

.region-row {
  display: flex;
  gap: 8px;
}

.region-row :deep(.unified-input) {
  flex: 1;
}

.page-footer {
  margin-top: 20px;
  text-align: right;
}
</style>

