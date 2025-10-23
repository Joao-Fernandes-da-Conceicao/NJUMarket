# URL映射冲突修复总结

## 问题描述
应用启动时出现URL映射冲突错误：

```
Ambiguous mapping. Cannot map 'publicController' method
com.njumarket.njumarket.controller.PublicController#getCommoditiesByCategory(String, Integer, Integer)
to {GET [/api/public/commodity/category/{category}]}: There is already 'publicCommodityController' bean method
com.njumarket.njumarket.controller.PublicCommodityController#getCommoditiesByCategory(String, Integer, Integer) mapped.
```

## 问题原因
存在两个Controller都映射到了相同的URL路径：

1. **PublicController** - 映射到 `/api/public`
2. **PublicCommodityController** - 映射到 `/api/public/commodity`

两个Controller都有 `getCommoditiesByCategory` 方法，都映射到：
`GET /api/public/commodity/category/{category}`

## 冲突的URL映射

### PublicController
```java
@RestController
@RequestMapping("/api/public")
public class PublicController {
    
    @GetMapping("/commodity/category/{category}")
    public Result getCommoditiesByCategory(@PathVariable String category,
                                         @RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer size) {
        return commodityService.getCommoditiesByCategory(category, page, size);
    }
}
```

### PublicCommodityController (已删除)
```java
@RestController
@RequestMapping("/api/public/commodity")
public class PublicCommodityController {
    
    @GetMapping("/category/{category}")
    public Result getCommoditiesByCategory(@PathVariable String category,
                                          @RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "10") Integer size) {
        return commodityService.getCommoditiesByCategory(category, page, size);
    }
}
```

## 解决方案
删除重复的 `PublicCommodityController`，保留功能更完整的 `PublicController`。

### 删除的文件
- `njumarket/src/main/java/com/njumarket/njumarket/controller/PublicCommodityController.java`

### 保留的Controller
- `njumarket/src/main/java/com/njumarket/njumarket/controller/PublicController.java`

## PublicController提供的功能

### 1. 商品搜索
- `GET /api/public/commodity/search` - 关键词搜索
- `GET /api/public/commodity/ai-search` - AI语义搜索

### 2. 商品浏览
- `GET /api/public/commodity/{commodityId}` - 商品详情
- `GET /api/public/commodity/hot` - 热门商品
- `GET /api/public/commodity/latest` - 最新商品
- `GET /api/public/commodity/category/{category}` - 按分类获取商品

### 3. 分类管理
- `GET /api/public/commodity/categories` - 获取商品分类

### 4. 推荐和统计
- `GET /api/public/commodity/recommend` - 推荐商品
- `POST /api/public/commodity/{commodityId}/view` - 记录浏览

## 修复后的URL结构

```
/api/public/
├── commodity/
│   ├── search                    # 搜索商品
│   ├── ai-search                 # AI搜索
│   ├── hot                       # 热门商品
│   ├── latest                    # 最新商品
│   ├── categories                # 商品分类
│   ├── category/{category}       # 按分类获取商品
│   ├── recommend                 # 推荐商品
│   ├── {commodityId}             # 商品详情
│   └── {commodityId}/view        # 记录浏览
```

## 优势
1. **避免冲突**: 消除了URL映射冲突
2. **功能完整**: PublicController提供了更丰富的功能
3. **结构清晰**: 统一的URL结构，易于理解和维护
4. **文档完善**: PublicController有完整的Swagger文档注解

## 验证
修复后需要验证：
1. ✅ 应用可以正常启动
2. ✅ 所有公共API接口正常工作
3. ✅ URL映射没有冲突
4. ✅ Swagger文档正确显示

## 总结
通过删除重复的 `PublicCommodityController`，解决了URL映射冲突问题。现在 `PublicController` 作为唯一的公共接口控制器，提供了完整的商品浏览功能，包括搜索、分类、推荐等功能，同时避免了URL冲突。
