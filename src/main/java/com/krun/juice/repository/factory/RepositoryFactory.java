package com.krun.juice.repository.factory;

import com.krun.juice.repository.Repository;
import com.krun.juice.connection.configuration.ConnectionConfiguration;
import com.krun.juice.repository.invocation.RepositoryInvocationHandler;
import com.krun.utils.log.LoggerProvider;
import lombok.Getter;

import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

/**
 * Created by krun on 2017/9/23.
 *
 * 仓库工厂
 *
 * 用以获取仓库接口代理类实例，一般来说全局只需要创建一个仓库工厂即可，
 * 在 Java SE 环境下通过 RepositoryFactory.configure(connectionConfiguration) 来获取一个默认全局名称的工厂实例；
 * 在 Java EE 环境下(典型如 Spring)，理论上可以通过配置一个 bean 并注入到想要用的地方即可。
 *
 * 注：默认全局名称不是 final 的，这意味着它是可以修改的。
 */
public class RepositoryFactory {

	private static final Logger logger = LoggerProvider.provide(RepositoryFactory.class);

	/**
	 * 保存了所有工厂实例
	 */
	private static final LinkedHashMap<String, RepositoryFactory> factoryMap;

	/**
	 * 默认全局工厂名称
	 */
	public static String FACTORY_GLOBAL = "global";

	static {
		/*
			做一点 hack，方便一点
		 */
		factoryMap = new LinkedHashMap<String, RepositoryFactory>() {
			@Override
			public RepositoryFactory put (String key, RepositoryFactory value) {
				super.put(key, value);
				return value;
			}
		};
	}

	/**
	 * 一点 hack 来检查是不是自身调用
	 * 原理是检查调用栈，创建异常时，栈顶会是当前方法，而此处场景只需要检查栈顶后两位是否也是位于此类的即可。
	 * @return
	 */
	private static boolean isSelfCall() {
		return RepositoryFactory.class.getName().equals(
				new Exception().getStackTrace()[2].getClassName()
		);
	}

	/**
	 * 配置一个仓库工厂
	 * @param configuration 数据库连接配置
	 * @return 仓库工厂实例
	 */
	public static RepositoryFactory configure(ConnectionConfiguration configuration) {
		return RepositoryFactory.configure(FACTORY_GLOBAL, configuration);
	}

	/**
	 * 配置一个仓库工厂
	 * @param name 仓库工厂名称
	 * @param configuration 数据库连接配置
	 * @return 仓库工厂实例
	 */
	public static RepositoryFactory configure(String name, ConnectionConfiguration configuration) {
		/**
		 * 如果直接以全局名称调用，这会导致全局、特定工厂实例的界限变得模糊，因此要阻止用户使用默认名称来创建工厂
		 */
		if (!isSelfCall())
			throw new RuntimeException(String.format("[%s] 是默认全局仓库工厂的名称，请使用其他名称.", FACTORY_GLOBAL));
		synchronized ( factoryMap ) {
			RepositoryFactory factory = factoryMap.get(name);
			if (factory != null) return factory;
			logger.info(String.format("创建仓库工厂: [%s]", name));
			return factoryMap.put(name,  new RepositoryFactory(name, configuration));
		}
	}

	/**
	 * 获取全局工厂实例，没有做空检查，这意味着当你没有配置任何一个工厂实例时，此方法会返回 null
	 * @return 仓库实例
	 */
	public static RepositoryFactory get() {
		return RepositoryFactory.get(FACTORY_GLOBAL);
	}

	/**
	 * 获取特定名称的工厂实例，这里允许使用默认全局工厂名称，因为"获取"这个操作并不在意全局、特定实例。
	 * 如果并没有配置该名称的工厂，那么此方法同样会返回 null
	 * @param name 想获取的仓库实例的名称
	 * @return 特定名称的仓库实例
	 */
	public static RepositoryFactory get(String name) {
		synchronized ( factoryMap ) {
			RepositoryFactory factory = factoryMap.get(name);
			if (factory != null) {
				logger.info(String.format("获取仓库工厂: [%s]", name));
				return factory;
			}
			return null;
		}
	}

	/**
	 * 数据库连接参数
	 */
	@Getter
	private ConnectionConfiguration connectionConfiguration;
	private final LinkedHashMap<Class<? extends Repository>, Repository> repositoryMap;

	/**
	 * 主要用于初始化操作
	 */
	private RepositoryFactory() {
		this.repositoryMap = new LinkedHashMap<Class<? extends Repository>, Repository>() {
			@Override
			public Repository put (Class<? extends Repository> key, Repository value) {
				super.put(key, value);
				return value;
			}
		};
	}

	public RepositoryFactory(String name, ConnectionConfiguration connectionConfiguration) {
		this();
		/**
		 * 保存自身到 repositoryFactory
		 */
		synchronized ( RepositoryFactory.factoryMap ) {
			RepositoryFactory.factoryMap.put(name, this);
		}
		this.connectionConfiguration = connectionConfiguration;
	}

	/**
	 * 获取一个仓库的代理实例，这个实例提供了 Juice 提供的数据库操作支持,同样是延迟加载，第一次获取时才会创建代理实例。
	 * @param repositoryClass 想要获取的仓库类型
	 * @param <E> 仓库所持有的表实体类型
	 * @param <I> 仓库所使用的主键类型
	 * @param <T> 仓库泛型信息
	 * @return 仓库的代理实例
	 */
	@SuppressWarnings("unchecked")
	public <E, I extends Serializable, T extends Repository<E, I>> T get(Class<T> repositoryClass) {
		synchronized ( repositoryMap ) {
			T repository = (T) this.repositoryMap.get(repositoryClass);
			if (repository != null) {
				logger.info(String.format("获取 仓库[%s] 代理实例", repositoryClass.getName()));
				return repository;
			}
			repository = createProxy(repositoryClass);
			return (T) this.repositoryMap.put(repositoryClass, repository);
		}
	}

	@SuppressWarnings("unchecked")
	private <E, I extends Serializable, T extends Repository<E, I>> T createProxy(Class<T> repositoryClass) {
		logger.info(String.format("为 仓库[%s] 创建代理实例", repositoryClass.getName()));
		return (T) Proxy.newProxyInstance(
				repositoryClass.getClassLoader( ),
				new Class[] { repositoryClass },
				new RepositoryInvocationHandler(this, repositoryClass)
		);
	}

	/**
	 * 将指定仓库从此仓库工厂实例的缓存表中移除
	 * @param repositoryClass
	 */
	public void remove(Class<? extends Repository> repositoryClass) {
		Repository repository = this.repositoryMap.get(repositoryClass);
		if (repository == null) return;
		this.repositoryMap.remove(repositoryClass, this.repositoryMap.get(repositoryClass));
	}
}
