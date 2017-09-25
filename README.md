# Juice

## 简介

Juice 是一个练手性质的Java项目，在使用 **Mybatis**等数据库框架时，想更轻松得保留对 `SQL` 的控制，也不需要失去太多编码上的轻松，因此前天开始了 *Juice* 项目。

事实上它只是做了一层 `JDBC`的简单封装，所谓的自定义能力也只时是提供 `Connection`、`Statement` 级别的简易接口。

## 功能与使用

> 注：本文示例使用了 `lombok`，基于`mysql-connector(5.1.44)` 

在当前这个勉强具有可用性的版本中，*Juice*可以通过类似下面的形式来使用：

### 数据库连接参数配置: `ConnectionConfiguration`

数据库连接参数配置使用了构造器模式，因此可以用如下方法获取一个配置:

```java
ConnectionConfiguration configuration = ConnectionConfiguration.builder()
				.driverClass("com.mysql.jdbc.Driver")
				.connectionURL("jdbc:mysql://localhost:3306/hsc")
				.username("gdpi")
				.password("gdpi")
				.build()
```

### 仓库工厂: `RepositoryFactory`

允许在一个 *Java 程序* 中存在多个仓库工厂实例，但是一般而言，只需要一个就行了：

```java
RepositoryFactory factory = RepositoryFactory.configure(connectionConfiguration);
```

这样会创建一个名为 `global`的仓库工厂，你可以这样获取它:

```java
RepositoryFactory factory = RepositoryFactory.get();
```

或者:

```java
RepositoryFactory factory = RepositoryFactory.get(RepositoryFactory.FACTORY_GLOBAL);
```

如你所见，仓库工厂的全局名称通过 `RepositoryFactory.FACTORY_GLOBAL`控制，同时它是非 `final`的，也就是说允许你修改这个值来避免遇到一些麻烦。

你还可以通过这样的方式获取一个用你喜欢的名字命名的仓库工厂：

```java
RepositoryFactory factory = RepositoryFactory.configure("hello", connectionConfiguration);
```



### 表模型

```java
@Entity("student")
@Data
public Student {
  
  private String id;
  @Column("class")
  private String clazz;
  private Sring gender;
  private int grade;
  private String major;
  private String name;
  
}
```

`@Entity`注解是一个 *可选项*：

你可以通过它指定该 POJO 所映射的是哪个表，如果你不指定此注解，那么 *Juice* 会使用该 POJO 类名的全小写形式作为表名。

----

`@Column`注解是一个 *可选项*：

你可以通过它指定其所在的字段在表中的名称，如果你不指定此注解，那么*Juice*会使用该字段名作为表中字段名。

> 注意：*Juice* 的默认结果解析器（`DefaultResultResolver`）的解析策略是 `表字段优先`，即如果POJO中不存在结果集的某个字段，它会引发一个异常；而如果POJO中存在一个结果集不存在的某个字段，那么解析进程会继续。

###仓库: `interface Repository<E,I> `

```java
public interface StudentRepository extends Repository<Student, String> {

	@Query (value = "SELECT * FROM %s")
	List<Student> findAll();

	@Query (value = "SELECT * FROM %s WHERE id = ?")
	Student findById(String id);

 	@Query (value = "UPDATE %s SET gender = ? WHERE id = ?",
			processor = StudentProcessor.class,
			processMethod = "replaceParameterLocation")
 	Integer setGenderById(String id, String gender);

 	@Query ("SELECT name FROM %s WHERE id = ?")
 	Student getNameById(String id);

}
```

如果你想为一个表模型定义一个仓库，只需要创建一个接口，并使其继承 `Repository<表模型，表模型主键类型>`接口，请记得填入泛型信息，*Juice*依赖它们进行工作。

> 事实上当前版本 的*Juice*并不区分一个字段是主键还是普通字段，这样的接口签名只是为了以后的完善而预留的。

`@Query`是一个必填项：

如果你希望为仓库中的某个方法注入 *Juice* 的逻辑，那么请为这个方法添加 `@Query`注解，否则会在`方法扫描`阶段被 *Juice*所忽略。

`@Query`具有以下属性:

|      属性名      |                    类型                    | 是否必须 | 说明                                       |
| :-----------: | :--------------------------------------: | :--: | :--------------------------------------- |
|     value     |                  String                  |  是   | 该属性配置此方法所执行的 `sql `语句，其中有些约定，请参见下文。      |
|   processor   | Class<? extends RepositoryMethodProcessor> |  否   | 该属性配置此方法在参数注入阶段的处理器方法，默认值为 `DefaultMethodProcessor`，它将把参数按顺序填入 `sql`语句。 |
| processMethod |                  String                  |  否   | 该属性配置 processor 的具体方法名称，默认值为 `注解所在方法名`或`process`，你可以在多个方法共用一个处理器时使用相同的处理器名称或`process。` |
|   provider    | Class<? extends RepositoryStatementProvider> |  否   | 该属性配置此方法的 `sql`语句所使用的具体 `Statement`类型，默认值为 `DefaultStatementProvider`，它将为`sql`语句创建一个 `MysqlPreparedStatement`。 |
| provideMethod |                  String                  |  否   | 该属性配置 provider 的具体方法名称，默认值为 `注解所在方法名`或 `provide`，你可以在多个方法共用一个处理器时使用相同的处理器名称或`provide`。 |
|   resolver    | Class<? extends RepositoryResultResolver> |  否   | 该属性配置此方法的执行结果的解析器，默认值为 `DefaultResultResolver`，它有着非常多的限制，请参见下文。 |
| resolveMethod |                  String                  |  否   | 该属性配置 resolver 的具体方法名称，默认值为 `注解所在方法名`或 `resolve`，你可以在多个方法共用一个处理器时使用相同的处理器名称或`resolve`。 |



> 注：无论是 `MethodProcessor`、`StatementProvider` 还是 `ResultResolver`的`**Method` 都必须是静态方法。这并没有太多考量，主要是为了减轻对象创建和缓存的压力。



####`@Query.value`属性值约定

* `%s`占位符用于 *Juice* 填入表模型所映射的表的名名称
* `?` 占位符是沿用 `PreparedStatement` 的做法，因此事实上如果你使用的是其他`Statement`实现，那么除了 `%s`占位符，参数占位符是可以自定义的，这可以通过自定义方法处理器和语句供应器实现。

**注意**

`?`占位符与参数的物理位置相对应，例如

```java
Integer setGenderById(String id, String gender);
```

按照编码习惯，会把 `id`写在前面，但默认的方法处理器只是简单得把参数按顺序注入语句，因此在默认语句供应器中，上述方法在以 `{id = "20152203300", gender = "男"}` 调用时，会生成如下语句:

```mysq
UPDATE student SET gender = '20152203300' WHERE id = '男';
```

这显然是不对的，因此需要为该方法配置一个方法处理器来改变参数注入顺序:

```java
public class StudentProcessor implements RepositoryMethodProcessor {

	public static Statement replaceParameterLocation(PreparedStatement statement, String id, String gender) throws SQLException {
		statement.setString(1, gender);
		statement.setString(2, id);
		return statement;
	}
```



####方法处理器：`interface RepositoryMethodProcessor`

方法处理器用于参数注入阶段，如果想精确记录每次 `SQL`执行的参数情况、或是想做一层参数检查，那么可以自定义一个方法处理器，接受一个 `Statement`(在默认情况下这会是个`MysqlPreparedStatement`，只是因为它实现了`asSql()`方法，可以方便得输出参数注入后的`sql`语句 LOL)和参数列表，只需要将注入参数后的 `statement`返回给 *Juice* 即可。

默认的方法处理器签名如下：

```java
public static Statement process (Statement statement, Object ...args) throws Exception 
```

而自定义的方法处理器签名如下：

```java
public static Statement findAll (Statement statement) throws SQLException

public static Statement findById (Statement statement, String id) throws Exception
```

当前版本尚未完全处理一个问题：如果一个自定义方法处理器想像默认方法处理器那样声明参数列表 (`(Statement statement, Object ...args)`) 这会引发参数类型不匹配的错误。

这是因为 `Object ...args` 这种声明，在反射时对应 `Object[] args`，也就是说它的参数类型会是 一个数组，而自定义方法处理器时基本会给定明确的参数类型，这两种参数列表类型会导致在反射时无法直接通过 `method.invoke(null, statement, args)`来完成，要么引发参数数量不对的错误、要么就是前文所说的参数类型不匹配的错误。

这两天应该会修复这个问题。



####语句供应器: `interface RespositoryStatementProvider`

语句供应器用于方法扫描时创建一个 `statement`实例，如果想使用自定义的特定 `statement`实现，即可自定义过一个语句供应器，接受一个 `Connection`和`SQL`语句，`Connection`的类型由所配置的数据库驱动决定。

语句供应器签名如下：

```java
public static PreparedStatement provide(Connection connection, String sql) throws Exception
```

它将返回一个 `PreparedStatement`，如果使用了`mysql-connector`，那么这个 `statement`将会是 *Juice* 所有默认实现所依赖的 `MysqlPreparedStatement`，如果你使用了其他的数据库驱动和`statment`，那么将无法使用 *Juice*的默认结果解析器，因为它会检查是否使用的时 `MysqlPreparedStatement`。



####结果解析器: `interface RepositoryResultResolver`

结果解析器用于解析 `SQL`执行结果，它接受一个 `statement`和 `表模型类`、`所触发的接口方法`和`该方法的返回值`。 

> 注意：传递给解析器的 `statement`是尚未执行的，因为语句的执行取决于其实现类，而标准的 `Statement.exectue`系列接口都需要额外参数，这是*Juice*无法保证能处理的情况。

默认的结果解析器 `DefaultResultResolver`只接受 `MysqlPreparedStatement`类型，因为它需要这个实现的 `asSql()`方法来输出参数注入后的 `SQL`语句。

同时默认的结果解析器具有相当大的限制：

* 只能解析仓库类所标记的表模型和它的 List 形式
* 默认解析器的`UPDATE/DELETE`类操作只能返回 `getUpdateCount()`的结果，这意味着在 `exist(I id)`系列方法的情况下无法自动映射结果到一个 `boolean` 值。
* 没有类型检查，默认解析器在映射字段值到表模型字段时使用的是 `field.set(entity, resultSet.get(index, field.getType()))`这种做法，这意味着你如果使用默认解析器，那么必须由你自己保证 表模型的字段类型是合适的。
* 由于上一点，导致默认解析器无法解析一个表模型字段是另一个POJO类型的情况

结果解析器签名如下：

```java
public static <E, T> Object resolve(Statement statement, Class<E> entityClass, Method method, Class<T> returnType) throws Exception {
```

#### 获取一个仓库实例

```java
StudentRepository repository = factory.get(StudentRepository.class);
```

###  效果

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

