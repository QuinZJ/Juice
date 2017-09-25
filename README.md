# Juice

## 简介

*Juice* 是一个简易的、尚不完善的基于 *Java* 的`SQL数据库`工具，它提供了对`SQL语句`最大程度的控制，和一点简单的扩展能力。

这些是开发时的一点笔记:
[做个数据库帮助库雏形](https://segmentfault.com/a/1190000011314783)
[做个数据库帮助库雏形2](https://segmentfault.com/a/1190000011319877)

## 使用效果

```java
RepositoryFactory factory = RepositoryFactory.configure(ConnectionConfiguration.builder()
				.driverClass("com.mysql.jdbc.Driver")
				.connectionURL("jdbc:mysql://localhost:3306/hsc")
				.username("gdpi")
				.password("gdpi")
				.build());

StudentRepository repository = factory.get(StudentRepository.class);

List<Student> studentList = repository.findAll();
// LinkedList<Student> size: 56

Student student = repository.getNameById("20152203300");
// {name: "krun", id: null, college: null, ...}

int count = repository.updateGenderById("20152203300", "男");
// 1

Student student2 = repository.findById("20152203300");
// {name: "krun", id: "20152203300", gender: "男",  major: "软件技术", ...}
```

## 功能与使用

使用 *Juice* 只需要简单的几步：



注: 本示例使用 `lombok` 和 `mysql-connector(5.1.44)`



### 数据库连接配置: `ConnectionConfiguration`

当前版本的 *Juice* 只需要以下几个参数用以连接数据库：

* `driverClass`：这个参数用于向驱动管理器注册一个数据库连接驱动。(本示例将使用 `com.mysql.jdbc.Driver`)


*  `connectionURL`： 这个参数用于向驱动管理器获取一个数据库连接，常用的如：`jdbc:mysql://localhost:3306/juice`，您可以附带任何连接语句中允许附加的参数，如字符编码设置等等。
*  `username`: 这个参数是获取数据库连接时所需要的数据库账户名
*  `password`： 这个参数是获取数据库连接时所需要的数据库密码

> 不建议直接在 `connectionURL`中配置连接所需的数据库账户及密码。

在未来的版本中，`Juice`会尝试加入对 `*.properties`文件的支持，如此一来，您可以直接在 `*.properties`文件中设置连接的详细参数。对**MySQL**适用的 `properties`选项请参见[这里](#"https://dev.mysql.com/doc/connector-j/5.1/en/connector-j-reference-configuration-properties.html")。



在 Java SE 环境中，您可以通过 `ConnectionConfiguration.builder()`构造器来构造一个配置：

```java
ConnecetionConfiguration conf = ConnectionConfiguration.builder()
  									.driverClass("com.mysql.jdbc.Driver")
  									.connectionURL("jdbc:mysql://localhost:3306/juice")
  									.username("gdpi")
  									.password("gdpi")
  									.build();
```

如果是类似 *Spring* 这样可以配置 `Bean`实例的环境中，您可以使用类似如下的方式来以`Bean`的方式创建一个配置:

```xml
<bean id="connectionConfiguration"
      class="com.krun.juice.connection.configuration.ConnectionConfiguration"> 
	<constructor-arg name="driverClass" value="com.mysql.jdbc.Driver" /> 
	<constructor-arg name="connectionURL" value="jdbc:mysql://localhost:3306/juice" /> 
	<constructor-arg name="username" value="gdpi" /> 
	<constructor-arg name="password" value="gdpi" /> 
</bean>
```



### 仓库工厂:  `RepositoryFactory`

仓库工厂是创建、管理仓库的地方。*Juice* 允许在一个 Java Application 中存在多个仓库工厂的实例，但由于每个仓库工厂都会持有一个 *数据库连接供应器(ConnectionProvider)* ，因此建议使用默认全局工厂。

>  每个工厂都由一个自己的名字，默认全局工厂的名字为: `global`， 这并不是一个常量值，为了避免某些情况下发生冲突，*Juice* 允许你在创建前修改 `RepositoryFactory.FACTORY_GLOBAL` 的值来更改默认全局工厂的名字。请注意，如果您在创建全局工厂后修改了该值，那么再次使用 *不指定名称的工厂获取方法(`RepositoryFactory.get()`)*将导致重新创建一个以新值命名的全局工厂。

在使用仓库工厂前，需要传入一个 `ConnectionConfiguration`实例，使仓库工厂得以初始化内部的数据库连接供应器。



在 Java SE 环境中，您可以通过下面的方式来配置仓库工厂:

```java
//这里的 conf 即为前一节所创建的数据库连接配置

// 配置全局仓库工厂
RepositoryFactory globalFactory = RepositoryFactory.configure(conf);

// 配置指定名称的仓库工厂
RepositoryFactory fooFactory = RepositoryFactory.configure("foo", conf);

// 请注意，使用第二种方式配置工厂时，使用默认全局工厂名称将抛出错误，因为这会破坏 API 所划分的全局、特定工厂的界限
RepositoryFactory wrongFactory = RepositoyFactory.configure(RepositoryFactory.FACTORY_GLOBAL， conf);
// > RuntimeException
```

如果是类似 *Spring* 这样可以配置 `Bean`实例的环境中，您可以使用类似如下的方式来以`Bean`的方式创建仓库工厂:

```xml
<bean id="globalFactory"
      class="com.krun.juice.repository.factory.RepositoryFactory"> 
	<constructor-arg ref="connectionConfiguration" /> 
</bean>

<bean id="fooFactory"
      class="com.krun.juice.repository.factory.RepositoryFactory"> 
  	<constructor-arg name="name" value="foo" />
	<constructor-arg name="connectionConfiguration" ref="connectionConfiguration" /> 
</bean>
```



在配置仓库工厂后，您可以通过 `RepositoryFactory.get()`和 `RepositoyFactory.get(name)`来获取全局或给定名称的仓库工厂。



### 表模型

*Juice* 可以将您给定的一个 Java 类视为一个表模型，就像下面这样:

```java
@Data
@Entity("student")
public class Student {

   private String id;

   @Column("class")
   private String clazz;

   private int code;
   private String college;
   private String gender;
   private int grade;
   private String major;
   private String name;

}
```

>  `@Data` 注解来自 `lombok`

`@Entity` 注解是一个可选项，它只有一个必填属性: `value`。当配置该注解时，*Juice*将使用该值作为表名；如果您指定了这个类是个表模型，*Juice* 却找不到该注解时，将使用类名的全小写形式作为表名。

`@Column`注解同样是一个可选项，它只有一个必填属性: `value`。当配置该注解时，`Juice`将使用该值作为数据库中此表的字段名，否则使用 Java 类字段名作为数据库中此表的字段名。

### 仓库: `Repository`

`Repository` 是一个注解，它实际上只是一个用于表明某个接口是一个仓库的标记。就像下面这样:

```java
public interface StudentRepository extends Repository<Student, String> {

	@Query (value = "SELECT * FROM %s")
	List<Student> findAll();

	@Query (value = "SELECT * FROM %s WHERE id = ?")
	Student findById(String id);

 	@Query (value = "UPDATE %s SET gender = ? WHERE id = ?",
			processor = StudentChain.class,
			processMethod = "replaceParameterLocation")
 	Integer updateGenderById(String id, String gender);

 	@Query ("SELECT name FROM %s WHERE id = ?")
 	Student getNameById(String id);

}
```

`Repository`需要填入两个泛型信息，第一个是该仓库所操作的表模型，第二个是该表模型的主键类型。

> 注: 事实上到目前为止，*Juice* 并不区分主键和其他字段，只是为了以后完善留下空间。



#### `@Query` 注解

由于到目前为止，*Juice* 短期内不会实现 *解析方法名并映射为一个SQL操作* 这个 feature， 因此需要 `@Query` 注解来标记一个方法，并以此提供一些信息，*Juice* 提供的扩展能力也在这里体现:

`@Query`注解具有以下七个属性：

* `String value`: 这个属性指定了方法所映射的 `SQL`操作，其中有着一些约定：`%s`占位符用于 *Juice* 填充表名，而 `?` 占位符是 `PreparedStatement` 所使用的参数占位符。由于 *Juice* 提供简单的默认实现，这些默认实现使用的就是 `PreparedStatement`，因此如果您使用了不一样的`Statement`实现，您可以使用任何与之配合的占位符。注意：如果您选择了使用 `%*`系列作为占位符，那么请记得第一个 `%s`将会被 *Juice* 用来填充表名。
* `Class<? extends RepositoryStatementProvider> provider`: 这个属性指定了语句供应器所处的类，您可以指定任何实现了`RepositoryStatementProvider`接口的类，默认值为 *Juiec* 提供的`DefaultPreparedStatementProvider`，详细信息请参见下文。
* `String provideMethod`: 这个属性指定了注解所在方法所使用的语句供应器，当`provider` 属性使用默认值时，此属性无效；默认值为注解所在方法的名字或`provide`。
* `Class<? extends RepositoryParameterProcessor> processor`: 这个属性指定了参数处理器所处的类，您可以指定任何实现了 `RepositoryParameterProcessor`接口的类，默认值为 *Juiec* 提供的默认参数处理器 `DefaultParameterProcessor`，详细信息请参见下文。
* `String processMethod`: 这个属性指定了注解所在方法所使用的参数处理器，当`processor`属性使用默认值时，此属性无效；默认值为注解所在方法的名字或 `process`。
* `Class<? extends RepositoryResultResolver> resolver`: 这个属性指定了结果解析器所处的类，您可以指定任何实现了 `RepositoryResultResolver`接口的类，默认值为 *Juiec* 提供的 `DefaultResultResolver`，详细信息请参见下文。
* `String resolveMethod`: 这个属性指定了注解所在方法所使用的结果解析器，当`resolver`属性使用默认值时，此属性无效；默认值为注解所在方法的名字或 `resolve`。

**注意**：

您所指定的 `provideMethod`、`processMethod`、`resolveMethod`都必须是静态方法，这并无太多考量，只是为了减轻 *Juice* 的对象管理成本。



##### 语句供应器 `RepositoryStatementProvider`

一个语句供应器的方法签名应该如下:

```java
public static Statement provideMethodName(Connection connection, String sql)
```

供应器所在的类是 `@Query.provider` 的值，方法名是 `@Query.provideMethod` 的值。

供应器接收一个 `java.sql.connection`和`@Query.value`值，并返回一个 `java.sql.statement`。

> 这里的 `sql` 已经填充了表名

>  这里的`Connection`可以不关闭，它会由仓库工厂进行复用。

注意：供应器只会在仓库工厂第一次创建工厂时调用，而参数处理器和结果解析器将在每次仓库方法被调用时调用。

如果您希望使用项目所特定的、实现了装饰器模式的、特殊的`Statement`实例，可以为方法定义一个、或创建一个全局的语句供应器，并为所有方法指定。

*也许后期会在 factory 中加入替换默认语句供应器、参数处理器、结果解析器的接口。*



默认的语句供应器 `DefaultPreparedStatementProvider.provide`将根据给定 `sql`创建一个 `com.mysql.jdbc.PreparedStatement`实例。



##### 参数处理器 `RepositoryParameterProcessor`

一个参数处理器的方法签名应该类似下面这样(这里对应的是 `StudentRepository.findById`):

```java
public static Statement findById (Statement statement, String id)
```

处理器所在的类是 `@Query.processor`的值，方法名是 `@Query.processMethod` 的值。

处理器接收一个`java.sql.statement`和具体的参数列表，并返回一个`java.sql.statement`。

如果您希望在每次方法调用时都有个地方可以记录日志、进行参数检查，可以为其配置一个参数处理器。

在当前版本的 *Juice* 中，如果您希望处理类似下面这种情况:

```java
public StudentRepository extends Repository<Student, String> {
  
  @Query("INSERT INTO %s (%s) VALUES (%s)")
  Integer insert(Student student);
  
}
```

您需要为其配置一个语句供应器:

```java
public static Statement insert(Connection connection, String sql) {
    return connection.prepareStatement(
      String.format(sql,
		StringUtils.convertObjectFields2StringList(Student.class)));
}
```

和一个参数处理器:

```java
public static Statement insert(Statement statement, Student student) {
    PreparedStatement ps = (PreparedStatement) statement;
  	for (Field field : student.getClass().getDeclaringFields()) {
      field.setAccessable(true);
      ps.setObject(index, field.get(student));
    }
}
```

> 以上均为伪代码



*Juice* 所提供的默认参数处理器 `DefaultParameterProcessor`，只是简单得把参数按顺序填充入`SQL`语句中并返回。因此，类似下面这种情况可能会发生错误:

```java
public StudentRepository extends Repository<Student, String> {
    
  @Query("UPDATE %s SET gender = ? WHERE id = ?")
  Integer setGenderById(String id, String gender);
  
}
```

`setGenderById`的参数列表中，`id`在前，`gender`·在后，这会使得`DefaultParameter.process`输出:

```mysq
UPDATE student SET gender = {id} WHERE id = {gender}
```

显然这是错误的。如果要避免这种情况，可以直接把方法的参数列表按	`SQL`语句中的参数顺序排放；也可以为其指定一个参数处理器用以调整参数填充顺序。

##### 结果解析器 `RepositoryResultResolver`

一个结果解析器的方法签名应该类似下面这样:

```java
public static Object resolve(Statement statement, Class<?> entityClass, Method method)
```

解析器所在的类是 `@Query.resolver` 的值，方法名是`@Query.resolveMethod`的值。

解析器接收一个`java.sql.statement`语句、`Class<?>`表模型的类声明、`Method`触发解析器的仓库方法声明。

> 这里的 `statement` 尚未执行，因为`java.sql.statement.execute`系列接口需要一些额外参数，这导致 *Juice*无法确保一致的行为。因此当您配置了一个结果解析器，语句的执行时机将推迟到这里。

*Juice* 所提供的默认解析器 `DefaultResultResolver`有着很多限制：

* 只支持解析仓库所声明的表模型类型和其`List`形式
* 对于 `INSERT/UPDATE/DELETE`操作，只会返回`Integer`数值用以表示该`SQL操作`影响的行数
* 不支持表模型字段含有其他非`SQL types`类型的递归、嵌套解析

因此，如果您希望能解析复杂的结果，例如将前一节中的 `insert`操作返回插入后的结果并映射为一个`Student`:

```java
public StudentRepository extends Repository<Student, String> {
    
  @Query("UPDATE %s SET gender = ? WHERE id = ?")
  Student setGenderById(String id, String gender);
  
}
```



那么还需要配置一个结果解析器:

```java
public static Student insert(Statement statement, Class<?> entityClass, Method method) {
    // 解析逻辑...
}
```

## 结束

那么， *Juice* 的介绍、使用帮助就到此结束了，感谢您的观看 : )
