/**
 * 统一日志工具
 * 支持日志级别控制，生产环境自动过滤调试日志
 */

// 从环境变量获取日志级别，默认 warn
const logLevel = import.meta.env.VITE_LOG_LEVEL || 'warn'

// 日志级别优先级
const LEVELS = {
  debug: 0,
  info: 1,
  warn: 2,
  error: 3
}

/**
 * 检查是否应该输出日志
 */
function shouldLog(level) {
  const currentLevel = LEVELS[logLevel.toLowerCase()] || LEVELS.warn
  const targetLevel = LEVELS[level.toLowerCase()] || LEVELS.debug
  return targetLevel >= currentLevel
}

/**
 * 格式化日志消息
 */
function formatMessage(level, ...args) {
  const timestamp = new Date().toISOString()
  const prefix = `[${timestamp}] [${level.toUpperCase()}]`
  return [prefix, ...args]
}

/**
 * 日志工具对象
 */
export const logger = {
  /**
   * 调试日志（仅在开发环境或 logLevel=debug 时输出）
   */
  debug: (...args) => {
    if (shouldLog('debug')) {
      console.log(...formatMessage('debug', ...args))
    }
  },

  /**
   * 信息日志
   */
  info: (...args) => {
    if (shouldLog('info')) {
      console.info(...formatMessage('info', ...args))
    }
  },

  /**
   * 警告日志
   */
  warn: (...args) => {
    if (shouldLog('warn')) {
      console.warn(...formatMessage('warn', ...args))
    }
  },

  /**
   * 错误日志（总是输出）
   */
  error: (...args) => {
    // error 级别总是输出
    console.error(...formatMessage('error', ...args))
  },

  /**
   * 获取当前日志级别
   */
  getLevel: () => logLevel,

  /**
   * 设置日志级别（仅开发环境有效）
   */
  setLevel: (level) => {
    if (import.meta.env.DEV && LEVELS[level.toLowerCase()] !== undefined) {
      // 仅开发环境允许动态设置
      logLevel = level.toLowerCase()
    }
  }
}

// 默认导出
export default logger

