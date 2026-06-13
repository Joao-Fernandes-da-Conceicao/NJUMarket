package com.njumarket.trade.search;

/**
 * 公开商品列表/搜索分页上限：排序与检索均在「前 {@link #MAX_VISIBLE_TOTAL} 条」内完成，
 * 避免深分页与 ES offset 过大；与前端约定一致。
 */
public final class PublicCommodityBrowseLimits {

    public static final int MAX_PAGE = 20;
    public static final int MAX_PAGE_SIZE = 10;
    public static final int MAX_VISIBLE_TOTAL = MAX_PAGE * MAX_PAGE_SIZE;

    private PublicCommodityBrowseLimits() {
    }

    public static int clampPage(Integer page) {
        int p = (page == null || page < 1) ? 1 : page;
        return Math.min(p, MAX_PAGE);
    }

    public static int clampSize(Integer size) {
        int s = (size == null || size < 1) ? MAX_PAGE_SIZE : size;
        return Math.min(s, MAX_PAGE_SIZE);
    }

    /** 返回给前端的「总条数」：不超过 {@link #MAX_VISIBLE_TOTAL} */
    public static long capReportedTotal(long rawTotal) {
        long t = Math.max(rawTotal, 0);
        return Math.min(t, MAX_VISIBLE_TOTAL);
    }

    public static int calculateReportedPages(long cappedTotal, int size) {
        if (size <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) cappedTotal / size);
    }
}
