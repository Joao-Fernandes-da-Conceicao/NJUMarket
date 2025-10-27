<template>
  <footer class="app-footer">
    <div class="container">
      <div class="footer-content">
        <div class="footer-section">
          <h3 class="footer-title">南大市场</h3>
          <p class="footer-desc">南京大学校园二手交易平台</p>
        </div>
        
        <div class="footer-section">
          <h4 class="footer-subtitle">快速导航</h4>
          <ul class="footer-links">
            <li><router-link to="/">首页</router-link></li>
            <li><router-link to="/commodities">商品浏览</router-link></li>
            <li><router-link to="/publish">发布商品</router-link></li>
          </ul>
        </div>
        
        <div class="footer-section">
          <h4 class="footer-subtitle">用户服务</h4>
          <ul class="footer-links">
            <li><router-link to="/orders">我的订单</router-link></li>
            <li><router-link to="/home">我的主页</router-link></li>
            <li><router-link to="/profile">个人资料</router-link></li>
          </ul>
        </div>
        
        <div class="footer-section">
          <h4 class="footer-subtitle">联系我们</h4>
          <p class="footer-contact">
            邮箱: njumarket@nju.edu.cn<br>
            电话: 025-8968xxxx
          </p>
        </div>
      </div>
      
      <div class="footer-bottom">
        <p>&copy; 2024 南京大学校园二手交易平台. All rights reserved.</p>
      </div>
    </div>
  </footer>
</template>

<script setup>
// 无需额外的逻辑
</script>

<style scoped>
.app-footer {
  background-color: #2c3e50;
  color: white;
  padding: 40px 0 20px;
  margin-top: auto;
}

.footer-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 32px;
  margin-bottom: 32px;
}

.footer-section {
  display: flex;
  flex-direction: column;
}

.footer-title {
  font-size: 24px;
  font-weight: normal;
  margin-bottom: 8px;
  color: #ecf0f1;
}

.footer-subtitle {
  font-size: 16px;
  font-weight: normal;
  margin-bottom: 16px;
  color: #bdc3c7;
}

.footer-desc {
  color: #95a5a6;
  line-height: 1.6;
}

.footer-links {
  list-style: none;
  padding: 0;
  margin: 0;
}

.footer-links li {
  margin-bottom: 8px;
}

.footer-links a {
  color: #95a5a6;
  text-decoration: none;
  transition: color 0.3s ease;
}

.footer-links a:hover {
  color: #ecf0f1;
}

.footer-contact {
  color: #95a5a6;
  line-height: 1.6;
}

.footer-bottom {
  border-top: 1px solid #34495e;
  padding-top: 20px;
  text-align: center;
}

.footer-bottom p {
  color: #95a5a6;
  margin: 0;
}

/* 响应式设计 */
@media (max-width: 900px) {
  .footer-content {
    grid-template-columns: 1fr;
    gap: 24px;
  }
  
  .footer-title {
    font-size: 20px;
  }
}
</style>
