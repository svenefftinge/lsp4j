/*******************************************************************************
 * Copyright (c) 2016 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.lsp4j.jsonrpc.services;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.function.Consumer;

import com.google.common.base.Strings;

public final class AnnotationUtil {
	private AnnotationUtil() {}
	
	public static void findDelegateSegments(Class<?> clazz, Set<Class<?>> visited, Consumer<Method> acceptor) {
		if (clazz == null || !visited.add(clazz))
			return;
		findDelegateSegments(clazz.getSuperclass(), visited, acceptor);
		for (Class<?> interf : clazz.getInterfaces()) {
			findDelegateSegments(interf, visited, acceptor);
		}
		for (Method method : clazz.getDeclaredMethods()) {
			if (isDelegateMethod(method)) {
				acceptor.accept(method);
			}
		}
	}

	public static boolean isDelegateMethod(Method method) {
		JsonDelegate jsonDelegate = method.getAnnotation(JsonDelegate.class);
		if (jsonDelegate != null) {
			if (!(method.getParameterCount() == 0 && method.getReturnType().isInterface())) {
				throw new IllegalStateException(
						"The method " + method.toString() + " is not a proper @JsonDelegate method.");
			}
			return true;
		}
		return false;
	}
	

	/**
	 * Depth first search for annotated methods in hierarchy.
	 */
	public static void findRpcMethods(Class<?> clazz, Set<Class<?>> visited, Consumer<MethodInfo> acceptor) {
		if (clazz == null || !visited.add(clazz))
			return;
		findRpcMethods(clazz.getSuperclass(), visited, acceptor);
		for (Class<?> interf : clazz.getInterfaces()) {
			findRpcMethods(interf, visited, acceptor);
		}
		JsonSegment jsonSegment = clazz.getAnnotation(JsonSegment.class);
		String prefix = jsonSegment == null ? "" : jsonSegment.value() + "/";
		for (Method method : clazz.getDeclaredMethods()) {
			if (method.getParameterCount() <= 1) {
				MethodInfo methodInfo = new MethodInfo();
				methodInfo.method = method;
				if (method.getParameterCount() > 0) {
					methodInfo.parameterType = method.getParameters()[0].getParameterizedType();
				}
				JsonRequest jsonRequest = method.getAnnotation(JsonRequest.class);
				if (jsonRequest != null) {
					String name = Strings.isNullOrEmpty(jsonRequest.value()) ? method.getName() : jsonRequest.value();
					methodInfo.name = prefix + name;
					methodInfo.isNotification = false;
					acceptor.accept(methodInfo);
				} else {
					JsonNotification jsonNotification = method.getAnnotation(JsonNotification.class);
					if (jsonNotification != null) {
						String name = Strings.isNullOrEmpty(jsonNotification.value()) ? method.getName()
								: jsonNotification.value();
						methodInfo.name = prefix + name;
						methodInfo.isNotification = true;
						acceptor.accept(methodInfo);
					}
				}
			}
		}
	}
	
	static class MethodInfo {
		public String name;
		public Method method;
		public Type parameterType = Void.class;
		public boolean isNotification = false;
	}

	static class DelegateInfo {
		public Method method;
		public Object delegate;
	}
}
