<template>
  <div class="search-box">
    <div class="search-form">
      <div class="fake-search-box">
        <input
          :model-value="keyword"
          :placeholder="placeholder"
          class="transparent-input"
          @input="$emit('update:keyword', $event.target.value)"
          @keyup.enter="$emit('search')"
        />
        <button class="search-button" @click="$emit('search')">
          <el-icon><Search /></el-icon>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
/* eslint-disable no-undef */
import { Search } from '@element-plus/icons-vue'

defineProps({
  keyword: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '搜索商品...'
  }
})

defineEmits(['update:keyword', 'search'])
</script>

<style scoped>
.search-box {
  width: 100%;
  margin-bottom: 20px;
}

.search-form {
  max-width: 600px;
  margin: 0 auto;
}

.fake-search-box {
  position: relative;
  width: 100%;
  height: 50px;
  background: white;
  border: 1px solid var(--primary-color);
  border-radius: 25px;
  display: flex;
  align-items: center;
  padding: 0 25px;
  box-shadow: 0 2px 8px rgba(106, 1, 94, 0.1);
  transition: all 0.3s ease;
}

.fake-search-box:hover {
  border-color: var(--primary-light);
  box-shadow: 0 4px 12px rgba(106, 1, 94, 0.15);
}

.fake-search-box:focus-within {
  border-color: var(--primary-color);
  box-shadow: 0 4px 16px rgba(106, 1, 94, 0.2);
}

.transparent-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 16px;
  color: var(--text-primary);
  padding: 0;
  margin: 0;
  margin-right: 50px;
}

.transparent-input::placeholder {
  color: var(--text-light);
}

.search-button {
  position: absolute;
  right: 5px;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  background: var(--primary-color);
  border: none;
  border-radius: 50%;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.search-button:hover {
  background: var(--primary-light);
}

.search-button:active {
  transform: translateY(-50%) scale(0.95);
}

/* 响应式设计 */
@media (max-width: 900px) {
  .fake-search-box {
    height: 45px;
    padding: 0 20px;
  }
  
  .transparent-input {
    font-size: 14px;
  }
  
  .search-button {
    width: 36px;
    height: 36px;
  }
}
</style>
