package com.krun.juice.repository.invocation;

import com.krun.juice.connection.provider.ConnectionProvider;
import com.krun.juice.repository.Repository;
import com.krun.juice.repository.annotation.Entity;
import com.krun.juice.repository.annotation.Query;
import com.krun.juice.repository.factory.RepositoryFactory;
import com.krun.juice.repository.processor.DefaultParameterProcessor;
import com.krun.juice.repository.processor.RepositoryParameterProcessor;
import com.krun.juice.repository.resolver.RepositoryResultResolver;
import com.krun.juice.repository.statement.RepositoryStatementProvider;
import com.krun.juice.util.ClassUtils;
import com.krun.juice.util.MethodUtils;
import com.krun.juice.utils.log.LoggerProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

/**
 * Created by krun on 2017/9/23.
 *
 * Juice 的核心部分，实现注解解析、方法和 sql 的映射、sql结果的映射
 */
public class RepositoryInvocationHandler <R extends Repository> implements InvocationHandler {

	private static final Logger logger = LoggerProvider.provide(RepositoryInvocationHandler.class);

	/**
	 * 这部分用于缓存后续会用到的信息，避免重复获取造成额外开销
	 */
	private final RepositoryFactory factory;
	private final Class<R> repositoryClass;
	private final String repositoryClassName;
	private final Class<?> entityClass;
	private final String entityName;

	/**
	 * 用于缓存方法的解析结果
	 */
	private final LinkedHashMap<Method, MethodAnnotationValues> methodMap;

	private final ConnectionProvider connectionProvider;

	/**
	 * 与 ConnectionProvider 配合实现延迟加载
	 */
	private volatile Connection connection;


	/**
	 * 方法解析结果类
	 *
	 * 用以保存仓库类里每个方法所配置的：
	 * 方法处理器、语句供应器、结果解析器
	 */
	@Getter
	@Setter
	@AllArgsConstructor
	private static class MethodAnnotationValues {

		private Method processor;
		private Statement statement;
		private Method resolver;

		public String toString() {
			return "\t>>> processor: " +
					processor.getDeclaringClass( ).getSimpleName( ) +
					"." +
					processor.getName( ) +
					"()\n" +
					"\t>>> statement type: " +
					statement.getClass( ).getSimpleName( ) +
					"\n" +
					"\t>>> resolver: " +
					resolver.getDeclaringClass( ).getSimpleName( ) +
					"." +
					resolver.getName( ) +
					"()\n";
		}

	}

	public RepositoryInvocationHandler (RepositoryFactory factory, Class<R> repositoryClass) {
		this.factory = factory;
		this.repositoryClass = repositoryClass;
		this.repositoryClassName = repositoryClass.getSimpleName( );
		this.entityClass = getEntity( );
		Entity entity = this.entityClass.getAnnotation(Entity.class);
		if (entity == null) this.entityName = this.entityClass.getSimpleName().toLowerCase();
		else this.entityName = entity.value();
		logger.info(String.format("获取表名: [%s]", this.entityName));
		this.methodMap = new LinkedHashMap<>( );
		this.connectionProvider = ConnectionProvider.configure(factory.getConnectionConfiguration( ));

		this.scanMethods( );

		/*
		  输出扫描结果
		 */
		StringBuilder builder = new StringBuilder("\n");
		for (Method key : this.methodMap.keySet()) {
			builder.append(String.format("\t> 配置 %s.%s():\n", this.repositoryClassName, key.getName()))
					.append(this.methodMap.get(key));
		}
		logger.info(builder.toString());
	}

	/**
	 * 获取表实体类型
	 * @return 表实体类型
	 */
	private Class<?> getEntity() {
		return (Class<?>) ClassUtils.getInterfaceActualType(this.repositoryClass)[0];
	}

	private synchronized Connection getConnection ( ) {
		try {
			if (this.connection == null) {
				this.connection = this.connectionProvider.provide( );
			} else if (this.connection.isClosed( )) {
				this.connection = this.connectionProvider.provide( );
			}
			return this.connection;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	private void scanMethods ( ) {
		for (Method method : this.repositoryClass.getMethods( )) {
			/*
			  由于当前实现并没有做`方法名映射sql`支持，因此需要判断方法是否带有 Query 注解
			 */
			if (! method.isAnnotationPresent(Query.class)) {
				logger.info(String.format("方法 %s.%s() 并无 `@Query` 注解，跳过该方法]",
						this.repositoryClassName,
						method.getName( )));
				continue;
			}
			this.methodMap.put(method, new MethodAnnotationValues(
					getProcessor(method),
					getStatement(method),
					getResolver(method)));
		}
	}

	/**
	 * 获取方法配置的方法处理器
	 * @param method 要检查的方法
	 * @return 方法处理器
	 */
	private Method getProcessor (Method method) {

		Query query = method.getAnnotation(Query.class);

		Class<?> processorClass = query.processor( );
		String processorName = query.processMethod( );

		/*
		  如果没有指定一个方法处理器，那么使用默认的。
		 */
		if (processorClass.equals(RepositoryParameterProcessor.class))
			return MethodUtils.findMethod(DefaultParameterProcessor.class, DefaultParameterProcessor.METHOD_HANDLE);

		/*
		 a. 使用给定名称寻找处理器
		 a.1 找到了，返回
		 a.2 找不到，使用方法名寻找处理器
		 a.2.1 找到了，返回
		 a.2.2 找不到，使用默认处理器名称 'process' 寻找处理器
		 a.2.2.1 找到了，返回
		 a.2.2.2 找不到，抛出异常
		 */
		if (processorName.isEmpty( )) processorName = method.getName( );
		Method processor = MethodUtils.findMethod(processorClass, processorName);
		if (processor == null) processor = MethodUtils.findMethod(processorClass, "process");
		if (processor != null) return processor;

		throw new RuntimeException(String.format("方法 %s.%s() 所指定的方法处理器本应使用默认方法( '%s' 或 'process' )，但是并没有找到其中任何一个。",
				this.repositoryClassName,
				method.getName( ),
				method.getName()));
	}

	private Statement getStatement (Method method) {
		Query query = method.getAnnotation(Query.class);
		try {
			Class<? extends RepositoryStatementProvider> statementProvider = query.provider();
			String provideMethod = query.provideMethod();
			if (provideMethod.isEmpty()) provideMethod = method.getName();
			Method m = MethodUtils.findMethod(statementProvider, provideMethod);
			if (m == null) m = MethodUtils.findMethod(statementProvider, "provide");
			if (m == null) throw new RuntimeException(String.format("方法 %s.%s() 所指定的语句提供器本应使用默认方法( '%s' 或 'provide' )，但是并没有找到其中任何一个。",
					this.repositoryClassName,
					method.getName(),
					method.getName()));
			logger.info(String.format("调用语句提供器 [%s.%s()] 生成语句 [%s.%s()]",
					m.getDeclaringClass().getSimpleName(),
					m.getName(),
					this.repositoryClassName,
					method.getName()));
			return (Statement) m.invoke(null, getConnection(), String.format(query.value(), this.entityName));
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private Method getResolver (Method method) {
		Query query = method.getAnnotation(Query.class);
		Class<? extends RepositoryResultResolver> resolverClass = query.resolver();
		String resolverMethod = query.resolveMethod();
		if (resolverMethod.isEmpty()) resolverMethod = method.getName();
		Method m = MethodUtils.findMethod(resolverClass, resolverMethod);
		if (m == null) m = MethodUtils.findMethod(resolverClass, "resolve");
		if (m == null) throw new RuntimeException(String.format("方法 %s.%s() 所指定的解析器本应使用默认方法( '%s' 或 'resolve' )，但是并没有找到其中任何一个。",
				this.repositoryClassName,
				method.getName(),
				method.getName()));
		return m;
	}

	@Override
	public Object invoke (Object proxy, Method method, Object[] args) throws Throwable {

		MethodAnnotationValues value = this.methodMap.get(method);

		if (value == null) {
			logger.info(String.format("无法处理的方法 [%s.%s()]，找不到对应的配置信息",
					this.repositoryClassName,
					method.getName()));
			return null;
		}

		Method processor = value.processor;
		Method resolver = value.resolver;

		Statement statement;

		/*
		  由于默认处理器的方法参数签名是: (Statement statement, Object ...args);
		  而自定义方法处理器的方法参数签名是: (Statement statement, String/Integer/... arg);

		  导致直接通过 method.invoke(null, statement, args) 无法适用两种情况，因此需要分开构造参数列表。
		 */
		logger.info(String.format("调用处理器 [%s.%s()] 处理方法 [%s.%s()]",
				processor.getDeclaringClass().getSimpleName(),
				processor.getName(),
				this.repositoryClassName,
				method.getName()));
		if (processor.getDeclaringClass().equals(DefaultParameterProcessor.class)) {
			statement = (Statement) processor.invoke(null, new Object[] {value.statement, args});
		} else {
			Object[] callArgs = new Object[processor.getParameterCount()];

			for (int i = 0; i < callArgs.length; i ++)
				callArgs[i] = i == 0 ? value.statement : args[i - 1];

			statement = (Statement) processor.invoke(null, callArgs);
		}

		logger.info(String.format("调用解析器 [%s.%s()] 处理方法 [%s.%s()]",
				resolver.getDeclaringClass().getSimpleName(),
				resolver.getName(),
				this.repositoryClassName,
				method.getName()));
		return resolver.invoke(null, statement, this.entityClass, method);
	}
}
