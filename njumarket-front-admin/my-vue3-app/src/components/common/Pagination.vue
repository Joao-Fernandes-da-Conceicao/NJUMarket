<!-- eslint-disable vue/multi-word-component-names -->
<template>
  <div class="pagination-wrapper" v-if="total > 0">
    <div class="pagination-content">
      <span class="pagination-total">共 {{ total }} 条</span>
      <UnifiedSelect
        v-model="localPageSize"
        :options="pageSizeOptions"
        :placeholder="`每页 ${localPageSize} 条`"
        @change="selectPageSize"
      />
      <div class="pagination-buttons">
        <button 
          class="page-btn" 
          :disabled="currentPage <= 1"
          @click="handlePrevPage"
        >上一页</button>
        <div v-if="showPageNumbers" class="page-numbers">
          <button 
            v-for="page in getPageNumbers()" 
            :key="page"
            class="page-number"
            :class="{ active: page === currentPage }"
            @click="page !== '...' && handleJumpToPage(page)"
            :disabled="page === '...'"
          >{{ page }}</button>
        </div>
        <button 
          class="page-btn" 
          :disabled="currentPage >= totalPages"
          @click="handleNextPage"
        >下一页</button>
      </div>
      <div class="page-jumper">
        <span class="jumper-label">跳至</span>
        <input 
          v-model="jumpPageStr" 
          type="number" 
          class="jumper-input"
          :min="1" 
          :max="totalPages"
          @keyup.enter="handleJump"
          @blur="handleJump"
          @click.stop
        />
        <span class="jumper-label">页</span>
        <span class="jumper-label" style="margin-left:8px;">（{{ currentPage }} / {{ totalPages }}）</span>
      </div>
    </div>
  </div>
  
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { defineProps, defineEmits } from 'vue'
import UnifiedSelect from './UnifiedSelect.vue'

const props = defineProps({
  total: { type: Number, required: true },
  currentPage: { type: Number, required: true },
  pageSize: { type: Number, required: true },
  showPageNumbers: { type: Boolean, default: false }
})

const emit = defineEmits(['page-change', 'page-size-change'])

const localPageSize = ref(props.pageSize)
const jumpPageStr = ref('')

const totalPages = computed(() => {
  return Math.ceil(props.total / localPageSize.value)
})

const pageSizeOptions = computed(() => [
  { label: '每页 10 条', value: 10 },
  { label: '每页 20 条', value: 20 },
  { label: '每页 50 条', value: 50 }
])

watch(() => props.currentPage, () => { jumpPageStr.value = '' })
watch(() => props.pageSize, () => { localPageSize.value = props.pageSize })

const selectPageSize = (size) => {
  localPageSize.value = size
  emit('page-size-change', size)
}
const handlePrevPage = () => { if (props.currentPage > 1) emit('page-change', props.currentPage - 1) }
const handleNextPage = () => { if (props.currentPage < totalPages.value) emit('page-change', props.currentPage + 1) }
const handleJumpToPage = (page) => { if (page !== '...' && page !== props.currentPage) emit('page-change', page) }

const getPageNumbers = () => {
  const pages = []
  const current = props.currentPage
  const total = totalPages.value
  if (total <= 7) {
    for (let i = 1; i <= total; i++) pages.push(i)
  } else {
    if (current <= 3) {
      for (let i = 1; i <= 5; i++) pages.push(i)
      pages.push('...'); pages.push(total)
    } else if (current >= total - 2) {
      pages.push(1); pages.push('...')
      for (let i = total - 4; i <= total; i++) pages.push(i)
    } else {
      pages.push(1); pages.push('...')
      for (let i = current - 1; i <= current + 1; i++) pages.push(i)
      pages.push('...'); pages.push(total)
    }
  }
  return pages
}

const handleJump = () => {
  if (jumpPageStr.value === '') return
  const raw = Number(jumpPageStr.value)
  if (!Number.isFinite(raw)) { jumpPageStr.value = ''; return }
  if (raw < 1 || raw > totalPages.value) {
    ElMessage && ElMessage.warning('超出页数范围')
    jumpPageStr.value = ''
    return
  }
  if (raw !== props.currentPage) emit('page-change', raw)
  jumpPageStr.value = ''
}
</script>

<style scoped>
@import '../../styles/pagination.css';
</style>


