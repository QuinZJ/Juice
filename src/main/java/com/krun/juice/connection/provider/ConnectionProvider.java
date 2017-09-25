package com.krun.juice.connection.provider;

import com.krun.juice.connection.configuration.ConnectionConfiguration;
import com.krun.utils.log.LoggerProvider;
import com.mysql.jdbc.jdbc2.optional.MysqlPooledConnection;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Created by krun on 2017/9/23.
 *
 * 连接供应器
 *
 * 并不是一个连接池实现，只是尽可能复用一个连接，推迟连接获取时间。
 */
public class ConnectionProvider {

	private static Logger logger = LoggerProvider.provide(ConnectionProvider.class);

	/**
	 * 主要用于 Java SE 环境中使用 Juice。
	 * @param configuration 数据库连接配置类
	 * @return 一个连接供应器实例
	 */
	public static ConnectionProvider configure(ConnectionConfiguration configuration) {
		return new ConnectionProvider(configuration);
	}

	private final ConnectionConfiguration configuration;

	/**
	 * 用于每次获取链接时检查是否已经加载驱动
	 *
	 * 注：本来想通过保存 Driver 来避免 DriverManager.getConnection(url)，
	 * 因为这种连接获取方式会遍历 Driver 列表，由于本实现是带锁的，每次都遍历也许会
	 */
	private final Driver driver;

	private volatile Connection connection;


	public ConnectionProvider(ConnectionConfiguration configuration) {
		this.configuration = configuration;

		DriverManager.setLogWriter(new PrintWriter(System.out));

		try {
			DriverManager.registerDriver(
					(Driver) Class.forName(this.configuration.getDriverClass())
					.newInstance()
			);
		} catch (SQLException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			throw new RuntimeException("无法加载数据库驱动", e);
		}

		try {
			this.driver = DriverManager.getDriver(this.configuration.getConnectionURL());
		} catch (SQLException e) {
			throw new RuntimeException("获取数据库驱动失败", e);
		}
	}

	private synchronized Connection create() {
		if (this.driver == null)
			throw new RuntimeException("尚未加载数据库驱动.");

		try {
			return DriverManager.getConnection(this.configuration.getConnectionURL(),
							this.configuration.getUsername(), this.configuration.getPassword()
			);
		} catch (SQLException e) {
			throw new RuntimeException("获取数据库连接失败", e);
		}
	}

	/**
	 * 做一层延迟加载
	 * @return
	 * @throws SQLException
	 */
	public synchronized Connection provide() throws SQLException {
		if (this.connection == null) {
			this.connection = create();
		} else if (this.connection.isClosed()) {
			this.connection = create();
		}
		return this.connection;
	}
}
