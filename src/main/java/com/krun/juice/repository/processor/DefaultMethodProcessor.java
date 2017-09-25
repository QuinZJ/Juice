package com.krun.juice.repository.processor;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by krun on 2017/9/24.
 *
 * 默认方法处理器
 */
public class DefaultMethodProcessor implements RepositoryMethodProcessor {

	public static final String METHOD_HANDLE = "process";

	public static Statement process (Statement statement, Object ...args) throws SQLException {
		if (!( statement instanceof  PreparedStatement))
			throw new RuntimeException("默认的方法处理器只支持 PreparedStatement.");
		if (args == null) return statement;
		PreparedStatement preparedStatement = (PreparedStatement) statement;
		for (int i = 0; i < args.length; i++) {
			preparedStatement.setObject(i + 1, args[i]);
		}
		return statement;
	}
}
