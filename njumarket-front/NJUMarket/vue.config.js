const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    port: 8081,
    client: {
      overlay: {
        errors: false, // 禁用运行时错误 overlay（包括 ResizeObserver 等）
        warnings: false,
        runtimeErrors: false // 完全禁用运行时错误显示
      }
    }
  },
  configureWebpack: {
    resolve: {
      fallback: {
        'util': false
      }
    }
  },
  chainWebpack: config => {
    config.plugins.delete('prefetch')
    // 设置页面标题
    config.plugin('html').tap(args => {
      args[0].title = '南大集市 NJUMarket'
      return args
    })
  }
})
