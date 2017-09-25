package com.krun.juice.repository.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by krun on 2017/9/25.
 *
 * 数据库字段属性注解
 *
 * 现在只做了别名映射，后期再加其他吧。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {

	String value();
}
