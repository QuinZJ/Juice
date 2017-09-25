package com.krun.juice.utils.log;

import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by krun on 2017/9/23.
 */
public class LoggerProvider {

	private static final LinkedHashMap<Class<?>, Logger> loggerMap;

	static {
		loggerMap = new LinkedHashMap<Class<?>, Logger>();
	}

	public static Logger provide(Class<?> clazz) {
		synchronized ( loggerMap ) {
			Logger logger = loggerMap.get(clazz);
			if (logger == null) {
				logger = Logger.getLogger(clazz.getSimpleName());
				logger.setLevel(Level.ALL);
				loggerMap.put(clazz, logger);
			}
			return logger;
		}
	}
}
