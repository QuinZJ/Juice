package com.krun.juice.repository.annotation;

import com.krun.juice.repository.processor.DefaultParameterProcessor;
import com.krun.juice.repository.processor.RepositoryParameterProcessor;
import com.krun.juice.repository.resolver.DefaultResultResolver;
import com.krun.juice.repository.resolver.RepositoryResultResolver;
import com.krun.juice.repository.statement.DefaultPreparedStatementProvider;
import com.krun.juice.repository.statement.RepositoryStatementProvider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by krun on 2017/9/23.
 *
 * 数据库查询注解
 *
 * 主要是为了保存方法映射的 sql 语句，后期考虑加入 jpa 那种
 * 方法名映射 sql 语句。
 *
 * provider、processor、resolver 是为了在复杂情况下，将处理权力交由用户负责，
 * 是直接在 statement 级别操作的哈哈
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Query {

	String value();

	Class<? extends RepositoryStatementProvider> provider() default DefaultPreparedStatementProvider.class;

	String provideMethod () default "";

	Class<? extends RepositoryParameterProcessor> processor () default DefaultParameterProcessor.class;

	String processMethod () default "";

	Class<? extends RepositoryResultResolver> resolver() default DefaultResultResolver.class;

	String resolveMethod() default "";

}
