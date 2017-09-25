package com.krun.juice.util;

import java.lang.reflect.Method;

/**
 * Created by krun on 2017/9/24.
 */
public class MethodUtils {

	public static Method findMethod(Class<?> clazz, String name) {
		if (name == null) throw new NullPointerException();
		if (name.isEmpty()) throw new NullPointerException();
		for (Method method : clazz.getDeclaredMethods()) {
			if (!method.getName().equals(name)) continue;
			return method;
		}
		return null;
	}

}
