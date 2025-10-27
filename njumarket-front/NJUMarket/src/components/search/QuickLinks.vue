<template>
  <div class="quick-links">
    <span 
      v-for="link in links" 
      :key="link"
      class="quick-link" 
      @click="$emit('click', link)"
    >
      {{ link }}
    </span>
  </div>
</template>

<script setup>
/* eslint-disable no-undef */

defineProps({
  links: {
    type: Array,
    default: () => ['电子产品', '服装配饰', '图书文具', '生活用品']
  }
})

defineEmits(['click'])
</script>

<style scoped>
.quick-links {
  display: flex;
  justify-content: center;
  gap: 24px;
  flex-wrap: wrap;
}

.quick-link {
  padding: 8px 16px;
  background: white;
  border: 1px solid var(--primary-color);
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-weight: normal;
  color: var(--primary-color);
  font-size: 14px;
}

.quick-link:hover {
  background: var(--primary-color);
  color: white;
  transform: translateY(-2px);
}

/* 响应式设计 */
@media (max-width: 900px) {
  .quick-links {
    gap: 16px;
  }
  
  .quick-link {
    padding: 6px 12px;
    font-size: 12px;
  }
}
</style>
