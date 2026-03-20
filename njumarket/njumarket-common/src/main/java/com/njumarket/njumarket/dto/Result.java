package com.njumarket.njumarket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 统一返回结果
 */
@Schema(description = "统一返回结果")
@Data
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class Result {
    @Schema(description = "是否成功", example = "true")
    private Boolean success;

    @Schema(description = "错误信息", example = "操作失败")
    private String errorMsg;

    @Schema(description = "返回数据")
    private Object data;

    @Schema(description = "总数量（分页时使用）", example = "100")
    private Long total;

    public static Result ok(){
        return new Result(true, null, null, null);
    }

    public static Result ok(Object data){
        return new Result(true, null, data, null);
    }

    public static Result ok(String message, Object data){
        return new Result(true, message, data, null);
    }

    public static Result ok(List<?> data, Long total){
        return new Result(true, null, data, total);
    }

    public static Result ok(String message, List<?> data, Long total){
        return new Result(true, message, data, total);
    }

    public static Result fail(String errorMsg){
        return new Result(false, errorMsg, null, null);
    }

    /**
     * 获取消息内容（兼容性方法）
     * 成功时返回"操作成功"，失败时返回错误信息
     */
    public String getMessage() {
        if (success != null && success) {
            return "操作成功";
        }
        return errorMsg != null ? errorMsg : "操作失败";
    }

    /**
     * 设置消息内容（兼容性方法）
     */
    public void setMessage(String message) {
        this.errorMsg = message;
    }
}

