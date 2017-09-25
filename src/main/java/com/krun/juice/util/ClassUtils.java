package com.krun.juice.util;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by krun on 2017/9/24.
 */
public class ClassUtils {

	static class ListClass<T> {

		private Class<T> listClass;

		public ListClass (Class<T> listClass) {
			this.listClass = listClass;
		}

		protected Type getListType() {
			return ClassUtils.getInterfaceActualType(this.listClass)[0];
		}
	}

	public static Type[] getInterfaceActualType(Class rawClass) {
		return ((ParameterizedType) rawClass.getGenericInterfaces( )[0])
				.getActualTypeArguments( );
	}

	public static <T extends List> Type getListType(Class<T> listClass) throws IllegalAccessException, InstantiationException {

		return new ListClass<T>(listClass).getListType();
	}

	@SuppressWarnings("unchecked")
	public static <O, T> T getField(O obj, String fieldName, Class<T> tClass) throws NoSuchFieldException, IllegalAccessException {
		Field field = obj.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return (T) field.get(obj);
	}

	public static Class<?> parseClassFromSignature(String signature) throws ClassNotFoundException {
		signature = signature.substring(signature.indexOf('<') + 2, signature.indexOf('>') - 1);
		return Class.forName(signature.replaceAll("/", "."));
	}

}