package com.krun.juice.connection.configuration;

import lombok.*;

/**
 * Created by krun on 2017/9/23.
 * 数据库连接参数类
 *
 * 屏蔽掉 setter 是为了避免从 RepositoryFactory.get().getConnectionConfiguration() 后修改值，
 * 这可能会导致连接不可用。
 *
 * TO-DO: 加入 properties 支持、读入文件映射 properties 支持。
 */

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ConnectionConfiguration {

	/**
	 * 连接url，目前多数参数配置可以直接写在 url 里，所以没有先做 properties 支持
	 *
	 * properties 列表参见
	 * {@link "https://dev.mysql.com/doc/connector-j/5.1/en/connector-j-reference-configuration-properties.html"}
	 */
	private String connectionURL;

	/**
	 * 驱动类名，事实上由于当前实现可以说是绑定了 com/mysql/jdbc/PreparedStatement
	 * 这个 mysql-connector 包里的语句类型，
	 * 后期估计不会再计划支持其他 sql 实现
	 *
	 * 注：其实要取消对这个类的依赖也行...自己写个装饰器保存 sql 预编译语句，但是这样就很难做一个
	 * 输出编译后语句的效果了。
	 */
	private String driverClass;

	private String username;
	private String password;
}
