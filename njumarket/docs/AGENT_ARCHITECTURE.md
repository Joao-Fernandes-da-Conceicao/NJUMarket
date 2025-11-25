# AI Agent 架构说明

## 当前实现问题

目前的 `AIAgentService` 实现存在以下问题：

1. **简单的关键词匹配**：使用 `containsSearchIntent()` 方法通过关键词匹配来判断是否需要搜索，不够智能
2. **没有工具调用**：没有使用 Spring AI 的 Function Calling 功能，Agent 无法自主决定何时调用工具
3. **固定流程**：搜索流程是硬编码的，不够灵活

## 改进方案：基于 Function Calling 的 Agent

### 核心思想

使用 Spring AI 的 Function Calling 功能，让 Agent 能够：
1. **自主决策**：根据用户消息自主决定是否需要调用搜索工具
2. **工具调用**：通过 Function Calling 机制调用搜索工具
3. **多轮交互**：支持 Agent 多次调用工具，直到获得满意的结果

### 实现步骤

1. **定义搜索工具（Function）**
   - 创建 `searchCommodities` 函数，描述其功能和参数
   - 注册到 Spring AI 的 Function Registry

2. **改进系统提示词**
   - 明确告诉 Agent 可以使用哪些工具
   - 说明何时应该使用工具

3. **处理工具调用**
   - 检测 LLM 返回的工具调用请求
   - 执行工具调用
   - 将结果返回给 LLM
   - 让 LLM 基于结果生成最终回复

### Spring AI Function Calling 示例

```java
// 1. 定义 Function
@Bean
public Function<SearchRequest, SearchResponse> searchCommoditiesFunction() {
    return new Function<>() {
        @Override
        public String getName() {
            return "searchCommodities";
        }
        
        @Override
        public String getDescription() {
            return "搜索商品，根据用户查询返回相关商品列表";
        }
        
        @Override
        public SearchResponse apply(SearchRequest request) {
            // 执行搜索逻辑
            List<Commodity> commodities = aiSearchService.search(
                request.getQuery(), 
                request.getLocation(), 
                request.getLimit()
            );
            return new SearchResponse(commodities);
        }
    };
}

// 2. 在 Prompt 中注册 Function
Prompt prompt = new Prompt(messages, 
    PromptOptions.builder()
        .withFunctions(List.of(searchCommoditiesFunction))
        .build()
);

// 3. 处理工具调用
ChatResponse response = chatModel.call(prompt);
if (response.getResult().getOutput().getToolCalls() != null) {
    // 执行工具调用
    // 将结果添加到消息中
    // 再次调用 LLM
}
```

## 下一步

需要检查 Spring AI 1.0.0-M4 是否支持 Function Calling，如果支持，可以按照上述方案改进。

