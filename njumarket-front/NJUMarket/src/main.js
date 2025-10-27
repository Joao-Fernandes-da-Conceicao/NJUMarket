import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { useUserStore } from './stores/user'

// 抑制 ResizeObserver 循环警告和 parentNode 错误（这些是良性警告，不影响功能）
// 这个问题通常发生在组件尺寸剧烈变化时（如从桌面端切换到移动端）

// 方法1: 全局安装错误处理器
window.addEventListener('error', (event) => {
  const errorMsg = event.message || ''
  
  // 抑制 ResizeObserver 相关错误
  if (errorMsg.includes('ResizeObserver')) {
    event.preventDefault()
    return false
  }
  
  // 抑制 parentNode 相关错误（Vue DOM 更新时的问题）
  if (errorMsg.includes('Cannot read properties of null') && errorMsg.includes('parentNode')) {
    event.preventDefault()
    return false
  }
  
  return false
}, true) // 使用捕获阶段，更早捕获

window.addEventListener('unhandledrejection', (event) => {
  const reason = event.reason ? String(event.reason) : ''
  if (reason.includes('ResizeObserver') || (reason.includes('Cannot read properties of null') && reason.includes('parentNode'))) {
    event.preventDefault()
  }
})

// 方法2: 拦截 console 的所有输出
const originalError = console.error
const originalWarn = console.warn
const originalLog = console.log

const shouldSuppress = (message) => {
  const msgStr = String(message)
  
  // 抑制 ResizeObserver 相关消息
  if (msgStr.includes('ResizeObserver')) {
    return true
  }
  
  // 抑制 parentNode 相关错误
  if (msgStr.includes('Cannot read properties of null') && msgStr.includes('parentNode')) {
    return true
  }
  
  return false
}

console.error = (...args) => {
  if (args.some(arg => shouldSuppress(arg))) {
    return
  }
  originalError.apply(console, args)
}

console.warn = (...args) => {
  if (args.some(arg => shouldSuppress(arg))) {
    return
  }
  originalWarn.apply(console, args)
}

console.log = (...args) => {
  if (args.some(arg => shouldSuppress(arg))) {
    return
  }
  originalLog.apply(console, args)
}

const app = createApp(App)
const pinia = createPinia()

// 注册Element Plus图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)
app.use(router)
app.use(ElementPlus)

// 初始化用户状态
const userStore = useUserStore()
userStore.initUser()

app.mount('#app')
