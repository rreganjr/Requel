/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirments
 * Elicitation System.
 *
 * Requel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Requel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package edu.harvard.fas.rregan.uiframework.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * @author ron
 */
public class ReflectUtils {
	private static final Logger log = Logger.getLogger(ReflectUtils.class);

	public static List<Method> getScalarPropertyMethods(Class<?> target,
			String confineToPackagesStartingWith, int displayLevel) {
		List<Method> propertyMethods = new ArrayList<Method>();
		for (Method method : getPropertyMethods(target, confineToPackagesStartingWith, displayLevel)) {
			String name = method.getName();
			if (isScalar(method.getReturnType())) {
				log.debug("Adding method " + name + " of class " + target.getName()
						+ " with return type of " + method.getReturnType());
				propertyMethods.add(method);
			}
		}
		return propertyMethods;
	}

	public static boolean isScalar(Class<?> target) {
		if (target.isArray() || Collection.class.isAssignableFrom(target)
				|| Map.class.isAssignableFrom(target)) {
			return false;
		}
		return true;
	}

	/**
	 * Given a target class, a package filter (confineToPackagesStartingWith),
	 * and display level, return a list of java.lang.reflect.Method objects that
	 * can be used to access properties of objects of the target class type.
	 * Methods are returned for the supplied class and super classes where the
	 * full class name starts with the supplied confineToPackagesStartingWith
	 * string, or if confineToPackagesStartingWith is null the classes full
	 * class name doesn't start with java. Methods returned will obey the
	 * following rules: - The method is not static. - The method takes no
	 * arguments. - The method returns a value (not declared as void). - The
	 * method is annotated with a UIMethodDisplayHint displayLevel that matches
	 * the supplied displayLevel. - The displayLevel is LONG and the method
	 * follows the JavaBean naming convention. - The Class name the method is in
	 * starts with the string confineToPackagesStartingWith or if
	 * confineToPackagesStartingWith is null does not start with "java". NOTE:
	 * the Methods of any class that starts with confineToPackagesStartingWith
	 * in the super class chain will be returned even if an intervening class or
	 * the original class name doesn't start with confineToPackagesStartingWith.
	 * 
	 * @param target -
	 *            the target class
	 * @param confineToPackagesStartingWith -
	 *            String like "edu.harvard.fas.rregan."
	 * @param displayLevel
	 *            UIMethodDisplayHint.SHORT, UIMethodDisplayHint.LONG or
	 *            UIMethodDisplayHint.SHORT_OR_LONG
	 * @return
	 */
	public static List<Method> getPropertyMethods(Class<?> target,
			String confineToPackagesStartingWith, int displayLevel) {
		List<Method> propertyMethods = new ArrayList<Method>();
		for (Method method : getPropertyMethods(target, confineToPackagesStartingWith)) {
			UIMethodDisplayHint annotation = method.getAnnotation(UIMethodDisplayHint.class);
			String name = method.getName();
			if ((annotation != null) && ((annotation.displayLevel() & displayLevel) != 0)) {
				log.debug("Adding method " + name + " of class " + target.getName()
						+ " based on annotation displayLevel = " + annotation.displayLevel());
				propertyMethods.add(method);
			} else if (((displayLevel & UIMethodDisplayHint.LONG) == UIMethodDisplayHint.LONG)
					&& (name.startsWith("get") || name.startsWith("is"))) {
				log.debug("Adding method " + name + " of class " + target.getName()
						+ " based on JavaBean naming convention.");
				// if there isn't a UI based annotation check if the name
				// matches the Bean getter pattern
				propertyMethods.add(method);
			}
		}
		return propertyMethods;
	}

	/**
	 * Given a target class return a list of java.lang.reflect.Method objects
	 * that can be used to access properties of objects of the target class
	 * type. Methods are returned for the supplied class and super classes where
	 * the full class name aren't in a java package and the method names follow
	 * the JavaBean naming convention or are annotated with a displayLevel that
	 * is not IGNORE. Methods returned will obey the following rules: - The
	 * method is not static. - The method takes no arguments. - The method
	 * returns a value (not declared as void). - The method is annotated with a
	 * UIMethodDisplayHint displayLevel that is not IGNORE or the method follows
	 * the JavaBean naming convention. - The Class name the method is in does
	 * not start with "java".
	 * 
	 * @param target -
	 *            the target class
	 * @return
	 */
	public static List<Method> getPropertyMethods(Class<?> target) {
		return getPropertyMethods(target, null);
	}

	/**
	 * Given a target class return a list of java.lang.reflect.Method objects
	 * that can be used to access properties of objects of the target class
	 * type. Methods are returned for the supplied class and super classes where
	 * the full class name aren't in a java package and the method names follow
	 * the JavaBean naming convention or are annotated with a displayLevel that
	 * is not IGNORE. Methods returned will obey the following rules: - The
	 * method is not static. - The method takes no arguments. - The method
	 * returns a value (not declared as void). - The method is annotated with a
	 * UIMethodDisplayHint displayLevel that is not IGNORE or the method follows
	 * the JavaBean naming convention. - The Class name the method is in starts
	 * with the string confineToPackagesStartingWith or if
	 * confineToPackagesStartingWith is null does not start with "java". NOTE:
	 * the Methods of any class that starts with confineToPackagesStartingWith
	 * in the super class chain will be returned even if an intervening class or
	 * the original class name doesn't start with confineToPackagesStartingWith.
	 * 
	 * @param target -
	 *            the target class
	 * @return
	 */
	public static List<Method> getPropertyMethods(Class<?> target,
			String confineToPackagesStartingWith) {
		List<Method> propertyMethods = new ArrayList<Method>();
		if (target.getSuperclass() != null) {
			propertyMethods.addAll(getPropertyMethods(target.getSuperclass(),
					confineToPackagesStartingWith));
		}

		if (((confineToPackagesStartingWith == null) && !target.getName().startsWith("java"))
				|| ((confineToPackagesStartingWith != null) && target.getName().startsWith(
						confineToPackagesStartingWith))) {
			for (Method method : target.getDeclaredMethods()) {
				String name = method.getName();

				// collect non-static public methods with 0 args that return a
				// value and
				// match JavaBean getter names or are annotated
				if (!Modifier.isStatic(method.getModifiers())
						&& (method.getParameterTypes().length == 0)
						&& !void.class.isAssignableFrom(method.getReturnType())) {
					// Annotations may be used to alter the configuration
					// method.getDeclaredAnnotations();
					UIMethodDisplayHint annotation = method
							.getAnnotation(UIMethodDisplayHint.class);
					if ((annotation != null)
							&& (annotation.displayLevel() != UIMethodDisplayHint.IGNORE)) {
						removeMatchingSuperclassMethod(propertyMethods, name);
						log.debug("Adding method " + name + " of class " + target.getName()
								+ " based on annotation displayLevel not IGNORE");
						propertyMethods.add(method);
					} else if ((annotation == null)
							&& (name.startsWith("get") || name.startsWith("is"))) {
						removeMatchingSuperclassMethod(propertyMethods, name);
						log.debug("Adding method " + name + " of class " + target.getName()
								+ " based on JavaBean naming convention.");
						// if there isn't a UI based annotation check if the
						// name matches the Bean getter pattern
						propertyMethods.add(method);
					}
				} else if (log.isDebugEnabled()) {
					StringBuilder logMsg = new StringBuilder();
					logMsg.append("Skipping method ");
					logMsg.append(method.getName());
					logMsg.append(" of class ");
					logMsg.append(target.getName());
					logMsg.append(" because it is");
					if (Modifier.isStatic(method.getModifiers())) {
						logMsg.append(" static");
					}
					if (method.getParameterTypes().length > 0) {
						logMsg.append(" takes parameters");
					}
					if (void.class.isAssignableFrom(method.getReturnType())) {
						logMsg.append(" returns null");
					}
					log.debug(logMsg.toString());
				}
			}
		} else if (log.isDebugEnabled()) {
			if (confineToPackagesStartingWith == null) {
				log
						.debug("Skipping class "
								+ target.getName()
								+ " because confineToPackagesStartingWith is null and it is in a java package");
			} else {
				log.debug("Skipping class " + target.getName() + " because it doesn't start with "
						+ confineToPackagesStartingWith);
			}
		}
		return propertyMethods;
	}

	/**
	 * Return a Method of the supplied object that matches the supplied
	 * propertyName either as a UIMethodDisplayHint annotation propertyName() or
	 * following the JavaBean naming convention and doesn't have a
	 * UIMethodDisplayHint annotation displayLevel() of IGNORE. NOTE: the method
	 * may be in any class in the objects class hierarchy, including classes in
	 * the java packages.
	 * 
	 * @param target
	 * @param propertyName
	 * @return
	 * @throws NoSuchMethodException
	 *             if the target object doesn't have a property named
	 *             propertyName
	 */
	public static Method getPropertyMethod(Object target, String propertyName)
			throws NoSuchMethodException {

		String getName = "get"
				+ (Character.isUpperCase(propertyName.charAt(0)) ? propertyName : Character
						.toUpperCase(propertyName.charAt(0))
						+ propertyName.substring(1));

		String isName = "is"
				+ (Character.isUpperCase(propertyName.charAt(0)) ? propertyName : Character
						.toUpperCase(propertyName.charAt(0))
						+ propertyName.substring(1));

		for (Method method : getPropertyMethods(target.getClass(), "")) {
			UIMethodDisplayHint annotation = method.getAnnotation(UIMethodDisplayHint.class);
			if ((annotation != null) && (annotation.propertyName() != null)
					&& annotation.propertyName().equals(propertyName)) {
				return method;
			} else {
				if (getName.equals(method.getName()) || isName.equals(method.getName())) {
					return method;
				}
			}

		}
		throw new NoSuchMethodException("The class " + target.getClass().getName()
				+ " does not have a property \"" + propertyName + "\"");
	}

	/**
	 * Return a pretty name appropriate for labeling the property of a getter
	 * method. The label may be generated from the method name or via a
	 * UIMethodDisplayHint annotation label(). If the name is generated from the
	 * method name "get" or "is" is stripped off the beginning of the name, the
	 * first character is made upper case, and a space is injected before all
	 * capital letters after the first. If the method returns a boolean value a
	 * "?" is appended to the end of the name.
	 * 
	 * @param method
	 * @return
	 */
	public static String getLabelForMethod(Method method) {
		String label;
		UIMethodDisplayHint annotation = method.getAnnotation(UIMethodDisplayHint.class);
		if ((annotation != null) && (annotation.label() != null)
				&& (annotation.label().length() > 0)) {
			label = annotation.label();
		} else {
			String methodName = method.getName();
			StringBuilder sb = new StringBuilder(methodName.length() + 100);
			int index = (methodName.startsWith("get") ? 3 : (methodName.startsWith("is") ? 2 : 0));
			for (; index < methodName.length(); index++) {
				char ch = methodName.charAt(index);
				if (Character.isUpperCase(ch)) {
					sb.append(" ");
				}
				sb.append(ch);
			}
			if (Boolean.class.isAssignableFrom(method.getReturnType())
					|| boolean.class.isAssignableFrom(method.getReturnType())) {
				sb.append("?");
			}
			label = sb.toString().trim();
		}
		return label;
	}

	public static String getLabelForObject(Object target) {
		String label = null;
		if (target != null) {
			UITypeDisplayHint annotation = target.getClass().getAnnotation(UITypeDisplayHint.class);
			if ((annotation != null) && (annotation.targetProperty() != null)
					&& (annotation.targetProperty().length() > 0)) {
				Method propertyMethod = null;
				try {
					propertyMethod = ReflectUtils.getPropertyMethod(target, annotation
							.targetProperty());
					Object result = propertyMethod.invoke(target);
					label = (result != null ? result.toString() : "(null)");
				} catch (InvocationTargetException e) {
					log.warn("Invoking the method '" + propertyMethod.getGenericReturnType() + " "
							+ propertyMethod.getName() + "()' of the class "
							+ propertyMethod.getDeclaringClass().getName()
							+ " caused the exception: " + e.getCause(), e);
				} catch (IllegalAccessException e) {
					log.error("Invoking the method '" + propertyMethod.getGenericReturnType() + " "
							+ propertyMethod.getName() + "()' of the class "
							+ propertyMethod.getDeclaringClass().getName() + " is not accessible.",
							e);
				} catch (NoSuchMethodException e) {
					log.error(
							"A method for the value of the UITypeDisplayHint.targetProperty() of "
									+ annotation.targetProperty() + " for the object " + target
									+ " doesn't match a method.", e);
				}
			} else {
				label = target.toString();
			}
		}
		return label;
	}

	private static void removeMatchingSuperclassMethod(List<Method> propertyMethods, String name) {

		for (int index = 0; index < propertyMethods.size(); index++) {
			if (name.equals(propertyMethods.get(index).getName())) {
				Method removed = propertyMethods.remove(index);
				log.debug("Removing existing method " + removed.getName() + " of class "
						+ removed.getDeclaringClass().getName());
				break;
			}
		}

	}

}
