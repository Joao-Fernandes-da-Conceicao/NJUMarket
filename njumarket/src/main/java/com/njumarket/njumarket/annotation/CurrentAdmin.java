package com.njumarket.njumarket.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解，用于在Controller方法参数中注入当前认证的管理员对象。
 * 结合 {@link com.njumarket.njumarket.resolver.CurrentAdminArgumentResolver} 使用。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentAdmin {
}

