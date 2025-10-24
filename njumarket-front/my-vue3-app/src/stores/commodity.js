import { defineStore } from 'pinia'
import { commodityAPI } from '../api'

export const useCommodityStore = defineStore('commodity', {
  state: () => ({
    // 商品列表
    commodities: [],
    // 当前商品详情
    currentCommodity: null,
    // 我的商品
    myCommodities: [],
    // 加载状态
    loading: {
      list: false,
      detail: false,
      myCommodities: false,
      publish: false
    },
    // 分页信息
    pagination: {
      current: 1,
      size: 12,
      total: 0,
      pages: 0
    },
    // 搜索条件
    searchParams: {
      keyword: '',
      category: '',
      minPrice: null,
      maxPrice: null,
      location: ''
    }
  }),
  
  getters: {
    // 获取公开商品（过滤掉非公开的）
    publicCommodities: (state) => {
      return state.commodities.filter(commodity => 
        commodity.commodityStatus === 'ON_SHELF' &&
        commodity.sellerVisibility === 'PUBLIC' &&
        commodity.buyerVisibility === 'PUBLIC'
      )
    },
    
    // 获取我的商品统计
    myCommodityStats: (state) => {
      const stats = {
        total: state.myCommodities.length,
        published: 0,
        draft: 0,
        sold: 0,
        totalViews: 0
      }
      
      state.myCommodities.forEach(commodity => {
        switch (commodity.commodityStatus) {
          case 'ON_SHELF':
            stats.published++
            break
          case 'DRAFT':
            stats.draft++
            break
          case 'SOLD_OUT':
            stats.sold++
            break
        }
        stats.totalViews += commodity.clickCount || 0
      })
      
      return stats
    }
  },
  
  actions: {
    // 搜索商品
    async searchCommodities(params = {}) {
      this.loading.list = true
      try {
        const searchParams = { ...this.searchParams, ...params }
        const response = await commodityAPI.search(searchParams)
        
        if (response.success) {
          this.commodities = response.data.commodities || []
          this.pagination = {
            current: response.data.current || 1,
            size: response.data.size || 12,
            total: response.data.total || 0,
            pages: response.data.pages || 0
          }
        }
        
        return response
      } catch (error) {
        console.error('搜索商品失败:', error)
        throw error
      } finally {
        this.loading.list = false
      }
    },
    
    // 获取商品详情
    async getCommodityDetail(commodityId) {
      this.loading.detail = true
      try {
        const response = await commodityAPI.getDetail(commodityId)
        
        if (response.success) {
          this.currentCommodity = response.data
        }
        
        return response
      } catch (error) {
        console.error('获取商品详情失败:', error)
        throw error
      } finally {
        this.loading.detail = false
      }
    },
    
    // 获取我的商品
    async getMyCommodities(page = 1, size = 12, status = null) {
      this.loading.myCommodities = true
      try {
        const response = await commodityAPI.getMy(page, size, status)
        
        if (response.success) {
          this.myCommodities = response.data.commodities || []
          this.pagination = {
            current: response.data.current || 1,
            size: response.data.size || 12,
            total: response.data.total || 0,
            pages: response.data.pages || 0
          }
        }
        
        return response
      } catch (error) {
        console.error('获取我的商品失败:', error)
        throw error
      } finally {
        this.loading.myCommodities = false
      }
    },
    
    // 发布商品
    async publishCommodity(commodityData) {
      this.loading.publish = true
      try {
        const response = await commodityAPI.publish(commodityData)
        
        if (response.success) {
          // 发布成功后刷新我的商品列表
          await this.getMyCommodities()
        }
        
        return response
      } catch (error) {
        console.error('发布商品失败:', error)
        throw error
      } finally {
        this.loading.publish = false
      }
    },
    
    // 更新商品
    async updateCommodity(commodityId, commodityData) {
      try {
        const response = await commodityAPI.update(commodityId, commodityData)
        
        if (response.success) {
          // 更新成功后刷新相关数据
          await this.getMyCommodities()
          if (this.currentCommodity?.commodityId === commodityId) {
            await this.getCommodityDetail(commodityId)
          }
        }
        
        return response
      } catch (error) {
        console.error('更新商品失败:', error)
        throw error
      }
    },
    
    // 删除商品
    async deleteCommodity(commodityId) {
      try {
        const response = await commodityAPI.delete(commodityId)
        
        if (response.success) {
          // 删除成功后刷新我的商品列表
          await this.getMyCommodities()
          // 如果删除的是当前查看的商品，清空当前商品
          if (this.currentCommodity?.commodityId === commodityId) {
            this.currentCommodity = null
          }
        }
        
        return response
      } catch (error) {
        console.error('删除商品失败:', error)
        throw error
      }
    },
    
    // 上架商品
    async onShelfCommodity(commodityId) {
      try {
        const response = await commodityAPI.shelf(commodityId)
        
        if (response.success) {
          await this.getMyCommodities()
        }
        
        return response
      } catch (error) {
        console.error('上架商品失败:', error)
        throw error
      }
    },
    
    // 下架商品
    async offShelfCommodity(commodityId) {
      try {
        const response = await commodityAPI.unshelf(commodityId)
        
        if (response.success) {
          await this.getMyCommodities()
        }
        
        return response
      } catch (error) {
        console.error('下架商品失败:', error)
        throw error
      }
    },
    
    // 重新上架商品
    async republishCommodity(commodityId) {
      try {
        const response = await commodityAPI.republish(commodityId)
        
        if (response.success) {
          await this.getMyCommodities()
        }
        
        return response
      } catch (error) {
        console.error('重新上架商品失败:', error)
        throw error
      }
    },
    
    // 获取热门商品
    async getHotCommodities(limit = 8) {
      try {
        const response = await commodityAPI.getHot(limit)
        return response
      } catch (error) {
        console.error('获取热门商品失败:', error)
        throw error
      }
    },
    
    // 获取最新商品
    async getLatestCommodities(limit = 8) {
      try {
        const response = await commodityAPI.getLatest(limit)
        return response
      } catch (error) {
        console.error('获取最新商品失败:', error)
        throw error
      }
    },
    
    // 设置搜索参数
    setSearchParams(params) {
      this.searchParams = { ...this.searchParams, ...params }
    },
    
    // 清空搜索参数
    clearSearchParams() {
      this.searchParams = {
        keyword: '',
        category: '',
        minPrice: null,
        maxPrice: null,
        location: ''
      }
    },
    
    // 重置状态
    resetState() {
      this.commodities = []
      this.currentCommodity = null
      this.myCommodities = []
      this.loading = {
        list: false,
        detail: false,
        myCommodities: false,
        publish: false
      }
      this.pagination = {
        current: 1,
        size: 12,
        total: 0,
        pages: 0
      }
      this.clearSearchParams()
    }
  }
})
