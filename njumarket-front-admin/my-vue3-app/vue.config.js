module.exports = {
  publicPath: '/admin/',
  devServer: {
    port: 8082,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: false
      }
    }
  },
  configureWebpack: {
    plugins: [
      {
        apply(compiler) {
          compiler.hooks.compilation.tap('HtmlWebpackPluginHook', (compilation) => {
            const HtmlWebpackPlugin = require('html-webpack-plugin')
            HtmlWebpackPlugin.getHooks(compilation).beforeEmit.tapAsync(
              'InjectTitle',
              (data, cb) => {
                data.html = data.html.replace(/<title>.*<\/title>/, '<title>南大集市管理系统 NJUMarketAdmin</title>')
                cb(null, data)
              }
            )
          })
        }
      }
    ]
  }
}


