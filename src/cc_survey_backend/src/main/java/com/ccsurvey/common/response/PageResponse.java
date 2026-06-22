package com.ccsurvey.common.response;

import lombok.Data;

import java.util.List;

/**
 * 分页响应封装
 *
 * @param <T> 数据类型
 */
@Data
public class PageResponse<T> {

    /**
     * 数据列表
     */
    private List<T> list;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 当前页码 (从1开始)
     */
    private int page;

    /**
     * 每页大小
     */
    private int size;

    /**
     * 总页数
     */
    private int totalPages;

    /**
     * 是否有下一页
     */
    private boolean hasNext;

    /**
     * 是否有上一页
     */
    private boolean hasPrevious;

    /**
     * 创建分页响应
     */
    public static <T> PageResponse<T> of(List<T> list, long total, int page, int size) {
        PageResponse<T> response = new PageResponse<>();
        response.setList(list);
        response.setTotal(total);
        response.setPage(page);
        response.setSize(size);
        int totalPages = (int) Math.ceil((double) total / size);
        response.setTotalPages(totalPages);
        response.setHasNext(page < totalPages);
        response.setHasPrevious(page > 1);
        return response;
    }

    /**
     * 空分页响应
     */
    public static <T> PageResponse<T> empty(int page, int size) {
        return of(List.of(), 0, page, size);
    }
}