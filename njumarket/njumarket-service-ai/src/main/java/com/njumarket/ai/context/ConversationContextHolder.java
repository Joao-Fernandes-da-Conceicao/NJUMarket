package com.njumarket.ai.context;

/**
 * 基于 ThreadLocal 的 memoryId 持有者。
 *
 * @deprecated 商品工具状态已改为在 {@code @Tool} 方法上使用 {@link dev.langchain4j.service.MemoryId} 显式注入，
 * 在 Reactor 流式 / 异步线程中 ThreadLocal 不可靠，会导致推荐状态丢失。
 * 此类保留供将来其它非 LangChain4j 工具场景使用。
 */
@Deprecated(since = "0.0.1", forRemoval = false)
public final class ConversationContextHolder {

    private static final ThreadLocal<String> MEMORY_ID = new ThreadLocal<>();

    private ConversationContextHolder() {}

    public static void setMemoryId(String memoryId) {
        MEMORY_ID.set(memoryId);
    }

    public static String getMemoryId() {
        return MEMORY_ID.get();
    }

    public static void clear() {
        MEMORY_ID.remove();
    }
}
