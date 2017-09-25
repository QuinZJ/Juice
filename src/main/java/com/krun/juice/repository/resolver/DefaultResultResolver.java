package com.krun.juice.repository.resolver;

import com.krun.juice.repository.annotation.Column;
import com.krun.juice.util.ClassUtils;
import com.krun.juice.utils.log.LoggerProvider;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by krun on 2017/9/24.
 */
public class DefaultResultResolver implements RepositoryResultResolver {

	private static Logger logger = LoggerProvider.provide(DefaultResultResolver.class);

	@SuppressWarnings("unchecked")
	public static Object resolve(Statement statement, Class<?> entityClass, Method method)
			throws InstantiationException, IllegalAccessException, SQLException {
		Class<?> returnType = method.getReturnType();
		if (statement instanceof com.mysql.jdbc.PreparedStatement) {
			logger.info(String.format("执行方法 [%s.%s()] 所配置的 sql:\n%s",
					method.getDeclaringClass().getSimpleName(),
					method.getName(),
					((com.mysql.jdbc.PreparedStatement) statement).asSql()));
		} else {
			logger.info("Juice 默认结果解析器只能输出 `com/mysql/jdbc/PreparedStatement` 类型的执行SQL语句。");
		}
		if (!(statement instanceof PreparedStatement))
			throw new RuntimeException("Juice 默认结果解析器只支持 `java/sql/PreparedStatement` 类型");
		if (!((PreparedStatement) statement).execute()) {
			return statement.getUpdateCount();
		}

		if (List.class.isAssignableFrom(returnType)) {
			Class<?> actualType;
			try {
				actualType = ClassUtils.parseClassFromSignature(ClassUtils.getField(method, "signature", String.class));
			} catch (NoSuchFieldException | ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
			if (actualType.equals(entityClass)) {
				return list((PreparedStatement) statement, entityClass);
			}
		} else if (entityClass.equals(returnType)) {
			return single((PreparedStatement) statement, entityClass);
		}
		throw new RuntimeException(String.format(
				"为方法 [%s %s.%s()] 映射结果时，发生类型不匹配: Juice 暂不支持映射表实体以外的类型 [%s]",
				method.getReturnType().getSimpleName(),
				method.getDeclaringClass().getSimpleName(),
				method.getName(),
				ClassUtils.getInterfaceActualType(returnType)[0]
		));
	}

	private static <E> List<E> list (PreparedStatement statement, Class<E> entityClass) throws SQLException {
		List<E> list = new LinkedList<>();
		statement.execute();
		ResultSetMetaData metaData = statement.getMetaData();
		ResultSet resultSet = statement.getResultSet();
		while (resultSet.next()) {
			list.add(parseFromResultSet(metaData, resultSet, entityClass));
		}
		resultSet.close();
		return list;
	}

	private static <E> E single(PreparedStatement statement, Class<E> entityClass) throws IllegalAccessException, InstantiationException, SQLException {
		E entity = entityClass.newInstance();
		statement.execute();
		ResultSetMetaData metaData = statement.getMetaData();
		ResultSet resultSet = statement.getResultSet();

		if (resultSet.next()) {
			entity = parseFromResultSet(metaData, resultSet, entityClass);
		}

		resultSet.close();
		return entity;
	}

	private static <E> E parseFromResultSet(ResultSetMetaData metaData, ResultSet resultSet, Class<E> entityClass) {
		try {
			E entity = entityClass.newInstance();

			try {
				for (int i = 1; i <= metaData.getColumnCount(); i++) {
					setFieldValue(entity, entityClass, metaData.getColumnName(i), resultSet, i, true);
				}
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
			return entity;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private static void setFieldValue(Object entity, Class<?> entityClass, String fieldName, ResultSet resultSet, int index, boolean isContinue) throws SQLException {
		try {
			Field field = entityClass.getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(entity, resultSet.getObject(index, field.getType()));
		} catch (NoSuchFieldException e) {
			for (Field field : entityClass.getDeclaredFields()) {
				Column column = field.getAnnotation(Column.class);
				if (column == null)	continue;
				if (!column.value().equals(fieldName)) continue;
				setFieldValue(entity, entityClass, field.getName(), resultSet, index, false);
			}
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
