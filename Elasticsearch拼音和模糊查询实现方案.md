# Elasticsearch 拼音查询和模糊查询实现方案

## 问题分析

**现有功能：**
- ✅ 中文分词（IK Smart / IK Max Word）
- ✅ 多字段搜索（title, description, keywordPayload）
- ✅ 精确匹配、短语匹配

**需要新增：**
- 🔲 拼音查询（如：输入 "shouji" 能搜索到 "手机"）
- 🔲 模糊查询（如：输入 "手几" 能搜索到 "手机"）

## 实现方案

### 方案一：使用 Elasticsearch 插件（推荐）⭐

**优点：**
- 成熟稳定，无需自研
- 性能优秀，在 ES 内部处理
- 维护成本低

**所需插件：**
1. **拼音插件**：`analysis-pinyin`（Elasticsearch 官方推荐）
2. **模糊查询**：ES 原生支持 `fuzzy` 和 `wildcard` 查询

### 方案二：应用层处理（不推荐）

**缺点：**
- 需要维护拼音字典
- 性能较差
- 实现复杂

## 详细实现步骤

### 1. 安装拼音插件

```bash
# 进入 ES 容器
docker exec -it njumarket-elasticsearch bash

# 安装拼音插件
./bin/elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-pinyin/releases/download/v8.13.4/elasticsearch-analysis-pinyin-8.13.4.zip

# 重启 ES 容器
docker restart njumarket-elasticsearch
```

**注意：** 插件版本必须与 ES 版本匹配（当前 ES 8.13.4）

### 2. 更新 ES Settings 配置

修改 `commodity-settings.json`，添加拼音分析器：

```json
{
  "analysis": {
    "analyzer": {
      "zh_smart": {
        "type": "custom",
        "tokenizer": "ik_smart",
        "filter": [
          "lowercase",
          "asciifolding"
        ]
      },
      "zh_max": {
        "type": "custom",
        "tokenizer": "ik_max_word",
        "filter": [
          "lowercase",
          "asciifolding"
        ]
      },
      "pinyin_analyzer": {
        "type": "custom",
        "tokenizer": "ik_smart",
        "filter": [
          "pinyin_filter",
          "lowercase"
        ]
      }
    },
    "filter": {
      "pinyin_filter": {
        "type": "pinyin",
        "keep_first_letter": true,
        "keep_separate_first_letter": false,
        "keep_full_pinyin": true,
        "keep_original": true,
        "limit_first_letter_length": 16,
        "lowercase": true,
        "remove_duplicated_term": true
      }
    }
  }
}
```

### 3. 更新索引文档映射

在 `CommoditySearchDocument` 中，为需要支持拼音的字段添加拼音字段：

```java
@Field(type = FieldType.Text, analyzer = "zh_max", searchAnalyzer = "zh_smart")
private String title;

// 新增：拼音字段（用于拼音搜索）
@Field(type = FieldType.Text, analyzer = "pinyin_analyzer", searchAnalyzer = "pinyin_analyzer")
private String titlePinyin;
```

### 4. 更新查询逻辑

在 `CommoditySearchService` 中，修改查询构建逻辑：

```java
private BoolQuery configureBoolQuery(String keyword, ...) {
    return BoolQuery.of(bool -> {
        if (StringUtils.hasText(keyword)) {
            // 判断是否为拼音（只包含字母和数字）
            boolean isPinyin = keyword.matches("^[a-zA-Z0-9]+$");
            
            if (isPinyin) {
                // 拼音查询：搜索拼音字段
                bool.should(s -> s.multiMatch(mm -> mm
                        .query(keyword)
                        .fields("titlePinyin^4", "descriptionPinyin^2", "keywordPayloadPinyin")
                        .type(TextQueryType.BestFields)));
            } else {
                // 中文查询：原有逻辑 + 模糊查询
                bool.should(s -> s.multiMatch(mm -> mm
                        .query(keyword)
                        .fields("title^4", "description^2", "keywordPayload")
                        .type(TextQueryType.BestFields)
                        .fuzziness(Fuzziness.AUTO)));  // 添加模糊匹配
                
                // 同时搜索拼音字段（支持中英文混合）
                bool.should(s -> s.multiMatch(mm -> mm
                        .query(keyword)
                        .fields("titlePinyin^2", "descriptionPinyin", "keywordPayloadPinyin")
                        .type(TextQueryType.BestFields)));
            }
        } else {
            bool.must(m -> m.matchAll(ma -> ma));
        }
        
        // ... 其他过滤条件
    });
}
```

### 5. 更新数据同步逻辑

在 `CommoditySearchDocument.fromCommodity()` 中，生成拼音字段：

```java
// 需要添加拼音转换工具类
document.setTitle(commodity.getTitle());
document.setTitlePinyin(PinyinUtils.toPinyin(commodity.getTitle()));  // 转换为拼音

document.setDescription(Optional.ofNullable(commodity.getDescription()).orElse(""));
document.setDescriptionPinyin(PinyinUtils.toPinyin(commodity.getDescription()));
```

**拼音转换工具类：**

```java
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;

public class PinyinUtils {
    private static final HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
    
    static {
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
    }
    
    public static String toPinyin(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        StringBuilder pinyin = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (Character.toString(c).matches("[\\u4E00-\\u9FA5]+")) {
                // 中文字符，转换为拼音
                try {
                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, format);
                    if (pinyinArray != null && pinyinArray.length > 0) {
                        pinyin.append(pinyinArray[0]);
                    }
                } catch (Exception e) {
                    // 转换失败，保留原字符
                    pinyin.append(c);
                }
            } else {
                // 非中文字符，直接保留
                pinyin.append(c);
            }
        }
        return pinyin.toString();
    }
}
```

**Maven 依赖：**

```xml
<dependency>
    <groupId>com.belerweb</groupId>
    <artifactId>pinyin4j</artifactId>
    <version>2.5.1</version>
</dependency>
```

## 实现难度评估

| 功能 | 难度 | 工作量 | 是否需要专门团队 |
|------|------|--------|-----------------|
| 拼音查询 | ⭐⭐ 中等 | 2-3天 | ❌ 不需要 |
| 模糊查询 | ⭐ 简单 | 1天 | ❌ 不需要 |
| 中英文混合 | ⭐⭐ 中等 | 1-2天 | ❌ 不需要 |

## 总结

### ✅ 可以实现

1. **拼音查询**：使用 `analysis-pinyin` 插件，成熟稳定
2. **模糊查询**：ES 原生支持 `fuzziness` 参数
3. **中英文混合**：同时搜索中文和拼音字段

### ❌ 不需要专门团队

- ES 插件生态成熟，有现成的解决方案
- 实现相对简单，主要是配置和查询逻辑调整
- 可以在现有项目基础上扩展

### 📋 实施建议

1. **第一阶段**（1-2天）：
   - 安装拼音插件
   - 更新 ES settings
   - 添加拼音字段到索引

2. **第二阶段**（2-3天）：
   - 实现拼音转换工具类
   - 更新数据同步逻辑
   - 更新查询逻辑

3. **第三阶段**（1天）：
   - 测试和优化
   - 性能调优

**总工作量：约 1 周**

## 注意事项

1. **插件版本匹配**：拼音插件版本必须与 ES 版本匹配
2. **索引重建**：修改 settings 后需要重建索引
3. **性能影响**：拼音字段会增加索引大小，但影响不大
4. **数据同步**：需要为现有数据生成拼音字段

