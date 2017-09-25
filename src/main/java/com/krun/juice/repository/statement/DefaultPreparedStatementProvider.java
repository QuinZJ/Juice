package com.krun.juice.repository.statement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by krun on 2017/9/24.
 */
public class DefaultPreparedStatementProvider implements RepositoryStatementProvider {

	public static Statement provide(Connection connection, String sql) throws SQLException {
		return connection.prepareStatement(sql);
	}
}
