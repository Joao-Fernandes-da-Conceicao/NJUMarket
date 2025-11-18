<template>
  <div class="address-map-picker">
    <div class="address-search">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索地址（如：南京大学仙林校区）"
        @input="handleSearch"
        clearable
      >
        <template #prefix>
          <el-icon>
            <Search />
          </el-icon>
        </template>
      </el-input>
      <div v-if="searchResults.length" class="search-results">
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

    <div :id="mapContainerId" class="map-container"></div>

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

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: {
    type: Object,
    default: null
  },
  defaultLocation: {
    type: Object,
    default: () => ({
      longitude: 118.959,
      latitude: 32.114
    })
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const mapContainerId = `admin-map-container-${Math.random().toString(36).slice(2, 10)}`

const searchKeyword = ref('')
const searchResults = ref([])
const selectedLocation = ref(null)
let map = null
let marker = null
let geocoder = null
let placeSearch = null

const ensureContainer = () => {
  const container = document.getElementById(mapContainerId)
  if (!container) {
    return null
  }
  const style = window.getComputedStyle(container)
  const rect = container.getBoundingClientRect()

  if (
    style.display === 'none' ||
    style.visibility === 'hidden' ||
    rect.width <= 0 ||
    rect.height <= 0
  ) {
    return null
  }
  return container
}

const updateLocation = (longitude, latitude) => {
  if (!map || !marker) return
  marker.setPosition([longitude, latitude])
  map.setCenter([longitude, latitude])

  geocoder.getAddress([longitude, latitude], (status, result) => {
    if (status === 'complete' && result.info === 'OK') {
      const addressComponent = result.regeocode.addressComponent
      const formattedAddress = result.regeocode.formattedAddress
      const city = addressComponent.city || addressComponent.province || ''
      let district = addressComponent.district || ''
      if (!district && city) {
        district = city
      }

      selectedLocation.value = {
        longitude,
        latitude,
        address: formattedAddress,
        province: addressComponent.province || '',
        city,
        district,
        streetAddress: addressComponent.street || '',
        detailAddress: addressComponent.streetNumber || ''
      }
      emit('update:modelValue', selectedLocation.value)
      emit('change', selectedLocation.value)
    }
  })
}

const initMap = () => {
  if (typeof window.AMap === 'undefined') {
    console.error('高德地图API未加载')
    return false
  }

  const container = ensureContainer()
  if (!container) {
    return false
  }

  if (map) {
    try {
      map.destroy()
    } catch (e) {
      console.warn('销毁旧地图实例失败', e)
    }
    map = null
    marker = null
  }

  try {
    const { longitude, latitude } = props.defaultLocation
    map = new window.AMap.Map(mapContainerId, {
      zoom: 15,
      center: [longitude, latitude],
      viewMode: '3D'
    })

    marker = new window.AMap.Marker({
      position: [longitude, latitude],
      draggable: true
    })
    map.add(marker)

    geocoder = new window.AMap.Geocoder({
      city: '南京市'
    })
    placeSearch = new window.AMap.PlaceSearch({
      city: '南京市',
      citylimit: false
    })

    map.on('click', (e) => {
      const { lng, lat } = e.lnglat
      updateLocation(lng, lat)
    })

    marker.on('dragend', (e) => {
      const { lng, lat } = e.lnglat
      updateLocation(lng, lat)
    })

    map.on('complete', () => {
      setTimeout(() => {
        try {
          map.resize()
        } catch (e) {
          console.warn('调整地图尺寸失败', e)
        }
      }, 120)
    })

    if (props.modelValue && props.modelValue.longitude && props.modelValue.latitude) {
      updateLocation(props.modelValue.longitude, props.modelValue.latitude)
    } else {
      updateLocation(longitude, latitude)
    }
    return true
  } catch (e) {
    console.error('初始化地图失败', e)
    return false
  }
}

const resizeMap = () => {
  setTimeout(() => {
    if (!ensureContainer()) {
      setTimeout(resizeMap, 200)
      return
    }
    if (map) {
      try {
        map.resize()
      } catch (e) {
        initMap()
      }
    } else {
      if (!initMap()) {
        setTimeout(resizeMap, 200)
      }
    }
  }, 100)
}

const handleSearch = () => {
  if (!searchKeyword.value || searchKeyword.value.length < 2) {
    searchResults.value = []
    return
  }
  if (!placeSearch) return
  placeSearch.search(searchKeyword.value, (status, result) => {
    if (status === 'complete' && result.info === 'OK') {
      searchResults.value = result.poiList.pois.map((poi) => ({
        name: poi.name,
        address: poi.address,
        longitude: poi.location.lng,
        latitude: poi.location.lat
      }))
    } else {
      searchResults.value = []
    }
  })
}

const selectSearchResult = (item) => {
  searchKeyword.value = item.name
  searchResults.value = []
  updateLocation(item.longitude, item.latitude)
}

watch(
  () => props.modelValue,
  (newVal) => {
    if (
      newVal &&
      newVal.longitude &&
      newVal.latitude &&
      (!selectedLocation.value ||
        selectedLocation.value.longitude !== newVal.longitude ||
        selectedLocation.value.latitude !== newVal.latitude)
    ) {
      updateLocation(newVal.longitude, newVal.latitude)
    }
  }
)

onMounted(() => {
  setTimeout(() => {
    if (!initMap()) {
      resizeMap()
    }
  }, 400)
})

onUnmounted(() => {
  if (map) {
    map.destroy()
    map = null
  }
})
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
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  max-height: 200px;
  overflow-y: auto;
  z-index: 20;
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
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.result-address {
  font-size: 12px;
  color: #666;
}

.map-container {
  width: 100%;
  height: 360px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  margin-bottom: 10px;
}

.selected-location-info {
  background: #f9f9f9;
  border: 1px solid #eee;
  border-radius: 6px;
  padding: 10px 12px;
  font-size: 13px;
  color: #444;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.location-coords {
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: #666;
}
</style>

