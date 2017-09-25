package com.krun.juice.repository.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by krun on 2017/9/25.
 *
 * 表实体注解
 *
 * 现在只做了表名映射，其他的后期再做吧
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Entity {

	String value();

}
