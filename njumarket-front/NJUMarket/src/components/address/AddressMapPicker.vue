<template>
  <div class="address-map-picker">
    <!-- 地址搜索框 -->
    <div class="address-search">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索地址（如：南京大学仙林校区）"
        @input="handleSearch"
        clearable
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <div v-if="searchResults.length > 0" class="search-results">
        <div
          v-for="(item, index) in searchResults"
          :key="index"
          class="search-result-item"
          @click="selectSearchResult(item)"
        >
          <div class="result-name">{{ item.name }}</div>
          <div class="result-address">{{ item.address }}</div>
        </div>
      </div>
    </div>
    
    <!-- 地图容器 -->
    <div id="map-container" class="map-container"></div>
    
    <!-- 选中的地址信息 -->
    <div v-if="selectedLocation" class="selected-location-info">
      <div class="location-address">
        <strong>选中地址：</strong>{{ selectedLocation.address }}
      </div>
      <div class="location-coords">
        <span>经度：{{ selectedLocation.longitude }}</span>
        <span>纬度：{{ selectedLocation.latitude }}</span>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'

export default {
  name: 'AddressMapPicker',
  components: {
    Search
  },
  props: {
    modelValue: {
      type: Object,
      default: null
    },
    defaultLocation: {
      type: Object,
      default: () => ({ longitude: 118.959, latitude: 32.114 }) // 默认：南京大学仙林校区
    }
  },
  emits: ['update:modelValue', 'change'],
  setup(props, { emit }) {
    const searchKeyword = ref('')
    const searchResults = ref([])
    const selectedLocation = ref(null)
    let map = null
    let marker = null
    let geocoder = null
    let placeSearch = null
    
    // 初始化地图
    const initMap = () => {
      // 检查是否已加载高德地图API
      if (typeof window.AMap === 'undefined') {
        console.error('高德地图API未加载，请检查index.html中的script标签')
        return
      }
      
      // 检查容器是否存在且可见
      const container = document.getElementById('map-container')
      if (!container) {
        console.error('地图容器不存在')
        return
      }
      
      // 检查容器是否可见且有有效尺寸
      const containerStyle = window.getComputedStyle(container)
      const containerRect = container.getBoundingClientRect()
      const containerWidth = containerRect.width || container.offsetWidth
      const containerHeight = containerRect.height || container.offsetHeight
      
      // 如果容器被隐藏或尺寸无效，不初始化
      if (containerStyle.display === 'none' || 
          containerStyle.visibility === 'hidden' ||
          containerWidth <= 0 || 
          containerHeight <= 0 ||
          isNaN(containerWidth) || 
          isNaN(containerHeight)) {
        console.warn('地图容器不可见或尺寸无效，等待容器可见后再初始化', {
          display: containerStyle.display,
          visibility: containerStyle.visibility,
          width: containerWidth,
          height: containerHeight
        })
        return false
      }
      
      // 如果地图已经初始化，先销毁
      if (map) {
        try {
          map.destroy()
        } catch (e) {
          console.warn('销毁旧地图实例时出错:', e)
        }
        map = null
        marker = null
      }
      
      try {
        // 创建地图实例
        map = new window.AMap.Map('map-container', {
          zoom: 15,
          center: [props.defaultLocation.longitude, props.defaultLocation.latitude],
          viewMode: '3D'
        })
        
        // 创建标记
        marker = new window.AMap.Marker({
          position: [props.defaultLocation.longitude, props.defaultLocation.latitude],
          draggable: true
        })
        map.add(marker)
        
        // 创建地理编码实例（用于逆地理编码）
        geocoder = new window.AMap.Geocoder({
          city: '南京市'
        })
        
        // 创建地点搜索实例
        placeSearch = new window.AMap.PlaceSearch({
          city: '南京市',
          citylimit: false
        })
        
        // 地图点击事件
        map.on('click', (e) => {
          const { lng, lat } = e.lnglat
          updateLocation(lng, lat)
        })
        
        // 标记拖拽事件
        marker.on('dragend', (e) => {
          const { lng, lat } = e.lnglat
          updateLocation(lng, lat)
        })
        
        // 地图加载完成后，确保地图正确渲染
        map.on('complete', () => {
          // 强制重新计算地图尺寸（解决折叠面板导致的问题）
          setTimeout(() => {
            if (map) {
              try {
                map.getSize()
                map.resize()
              } catch (e) {
                console.warn('调整地图尺寸时出错:', e)
              }
            }
          }, 100)
        })
        
        // 如果有初始值，设置位置
        if (props.modelValue && props.modelValue.longitude && props.modelValue.latitude) {
          updateLocation(props.modelValue.longitude, props.modelValue.latitude)
        }
        
        return true
      } catch (error) {
        console.error('初始化地图失败:', error)
        return false
      }
    }
    
    // 公开方法：用于在容器可见时调用
    const resizeMap = () => {
      // 延迟一下，确保DOM已更新
      setTimeout(() => {
        const container = document.getElementById('map-container')
        if (!container) {
          console.warn('地图容器不存在，无法调整尺寸')
          return
        }
        
        // 检查容器尺寸
        const containerRect = container.getBoundingClientRect()
        const containerWidth = containerRect.width || container.offsetWidth
        const containerHeight = containerRect.height || container.offsetHeight
        
        if (containerWidth <= 0 || containerHeight <= 0 || isNaN(containerWidth) || isNaN(containerHeight)) {
          console.warn('地图容器尺寸无效，等待容器尺寸有效后再初始化', {
            width: containerWidth,
            height: containerHeight
          })
          // 如果尺寸仍然无效，再等一会儿重试
          setTimeout(() => resizeMap(), 200)
          return
        }
        
        if (map) {
          // 地图已初始化，调整尺寸
          try {
            map.getSize()
            map.resize()
          } catch (e) {
            console.warn('调整地图尺寸时出错:', e)
            // 如果调整失败，尝试重新初始化
            initMap()
          }
        } else {
          // 如果地图还没初始化，现在初始化
          const success = initMap()
          if (!success) {
            // 如果初始化失败，再等一会儿重试
            setTimeout(() => resizeMap(), 200)
          }
        }
      }, 100)
    }
    
    // 更新位置
    const updateLocation = (longitude, latitude) => {
      // 更新标记位置
      marker.setPosition([longitude, latitude])
      map.setCenter([longitude, latitude])
      
      // 逆地理编码：根据经纬度获取地址
      geocoder.getAddress([longitude, latitude], (status, result) => {
        if (status === 'complete' && result.info === 'OK') {
          const addressComponent = result.regeocode.addressComponent
          const formattedAddress = result.regeocode.formattedAddress
          
          // 获取城市和区/县
          const city = addressComponent.city || addressComponent.province || ''
          let district = addressComponent.district || ''
          
          // 如果没有三级行政区（如中山、东莞、嘉峪关市），将城市名复制到区/县
          // 类似"上海市上海市黄浦区"的方式，表示为"广东省中山市中山市"
          if (!district && city) {
            district = city
          }
          
          selectedLocation.value = {
            longitude,
            latitude,
            address: formattedAddress,
            province: addressComponent.province || '',
            city: city,
            district: district,
            streetAddress: addressComponent.street || '',
            detailAddress: addressComponent.streetNumber || ''
          }
          
          // 触发更新事件
          emit('update:modelValue', selectedLocation.value)
          emit('change', selectedLocation.value)
        }
      })
    }
    
    // 搜索地址
    const handleSearch = () => {
      if (!searchKeyword.value || searchKeyword.value.length < 2) {
        searchResults.value = []
        return
      }
      
      placeSearch.search(searchKeyword.value, (status, result) => {
        if (status === 'complete' && result.info === 'OK') {
          searchResults.value = result.poiList.pois.map(poi => ({
            name: poi.name,
            address: poi.address,
            location: poi.location,
            longitude: poi.location.lng,
            latitude: poi.location.lat
          }))
        } else {
          searchResults.value = []
        }
      })
    }
    
    // 选择搜索结果
    const selectSearchResult = (item) => {
      searchKeyword.value = item.name
      searchResults.value = []
      updateLocation(item.longitude, item.latitude)
    }
    
    // 监听外部值变化
    watch(() => props.modelValue, (newVal) => {
      if (newVal && newVal.longitude && newVal.latitude && map) {
        if (!selectedLocation.value || 
            selectedLocation.value.longitude !== newVal.longitude ||
            selectedLocation.value.latitude !== newVal.latitude) {
          updateLocation(newVal.longitude, newVal.latitude)
        }
      }
    })
    
    onMounted(() => {
      // 延迟初始化，确保高德地图API已加载
      // 注意：如果地图在折叠面板中，需要等待面板展开后再初始化
      setTimeout(() => {
        initMap()
      }, 500)
    })
    
    onUnmounted(() => {
      if (map) {
        map.destroy()
        map = null
        marker = null
        geocoder = null
        placeSearch = null
      }
    })
    
    // 暴露方法供父组件调用
    return {
      searchKeyword,
      searchResults,
      selectedLocation,
      handleSearch,
      selectSearchResult,
      resizeMap // 暴露给父组件，用于在折叠面板展开时调用
    }
  }
}
</script>

<style scoped>
.address-map-picker {
  width: 100%;
}

.address-search {
  position: relative;
  margin-bottom: 10px;
}

.search-results {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  max-height: 200px;
  overflow-y: auto;
  z-index: 1000;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.search-result-item {
  padding: 10px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
}

.search-result-item:hover {
  background-color: #f5f5f5;
}

.search-result-item:last-child {
  border-bottom: none;
}

.result-name {
  font-weight: bold;
  color: #333;
  margin-bottom: 4px;
}

.result-address {
  font-size: 12px;
  color: #666;
}

.map-container {
  width: 100%;
  height: 400px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  margin-bottom: 10px;
}

.selected-location-info {
  padding: 10px;
  background-color: #f5f5f5;
  border-radius: 8px;
}

.location-address {
  margin-bottom: 5px;
  color: #333;
}

.location-coords {
  display: flex;
  gap: 15px;
  font-size: 12px;
  color: #666;
}
</style>

