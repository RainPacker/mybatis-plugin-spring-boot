package com.mybatis.mybatis.plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @description: 忽略自动添加租户条件；Mapper类或指定的方法加了当前方法后，则不再自动添加租户过滤条件。
 * @author shizhiqiang
 * @since 2024/5/15 17:09
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface IgnoreTenant {
}
