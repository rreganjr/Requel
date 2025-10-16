/*
 * $Id: TestCase.java,v 1.4 2008/08/24 04:13:42 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package com.rreganjr;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.rreganjr.requel.user.impl.UserImpl;
import junit.framework.AssertionFailedError;

/**
 * @author ron
 */
public class TestCase extends junit.framework.TestCase {

	public TestCase() {
		super();
	}

	public TestCase(String name) {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * Test whether two byte arrays are equal by comparing the byte value of
	 * each array element in the 'expected' array to the coresponding array
	 * element in the 'actual'.
	 * 
	 * @param expected
	 * @param actual
	 * @throws AssertionFailedError
	 */
	public static void assertEquals(byte[] expected, byte[] actual) throws AssertionFailedError {
		if ((expected != null) && (actual != null)) {
			if (expected.length == actual.length) {
				for (int i = 0; i < expected.length; i++) {
					if (expected[i] != actual[i]) {
						throw new AssertionFailedError("Expected " + expected[i] + " but found "
								+ actual[i] + " at position " + i);
					}
				}
				return;
			} else {
				throw new AssertionFailedError("The expected byte array was " + expected.length
						+ " bytes long, but the actual was " + actual.length + " bytes long.");
			}
		} else if ((expected != null) && (actual == null)) {
			throw new AssertionFailedError(
					"The expected byte array was not null, but the actual was null.");
		} else if ((expected == null) && (actual != null)) {
			throw new AssertionFailedError(
					"The expected byte array was null, but the actual was not null.");
		}
	}

	/**
	 * Test whether two collections contain the same values.
	 * 
	 * @param expected
	 * @param actual
	 * @throws AssertionFailedError
	 */
	public static void assertEquals(Collection<?> expected, Collection<?> actual)
			throws AssertionFailedError {
		if ((expected == null) && (actual == null)) {
			return;
		}
		if ((expected == null) || (actual == null)) {
			throw new AssertionFailedError("Expected collection " + expected
					+ " but found collection " + actual);
		}
		if (expected.size() != actual.size()) {
			throw new AssertionFailedError("Expected " + expected.size() + " entries but found "
					+ actual.size() + " entries.");
		}

		for (Object entry : expected) {
			if (!actual.contains(entry)) {
				throw new AssertionFailedError("Expected entry '" + entry
						+ "' but it was not found in collection " + actual);
			}
		}
		return;
	}

	public static void assertEqualsIgnoreWhitespace(String expected, String actual) {
		assertEquals(normalize(expected), normalize(actual));
	}

	public static String normalize(String str) {
		StringBuilder sb = new StringBuilder();

		int state = 0; // 0 begin
		// 1 middle
		// 2 end
		// 3 skipping

		for (int i = 0; i < str.length(); i++) {
			char x = str.charAt(i);

			boolean white = Character.isWhitespace(x);

			switch (state) {
			// doing the beginning
			case 0:
				if (white) {
					continue;
				} else {
					sb.append(x);
					state = 1;
				}
				break;

			// doing the middle
			case 1:
				if (white) {
					state = 3;
					sb.append(' ');
				} else {
					sb.append(x);
				}
				break;

			case 3:
				if (!white) {
					state = 1;
					sb.append(x);
				}
				break;

			default:
				throw new RuntimeException("Unexpected state " + state + " for string: " + str);
			}
		}

		return sb.toString().trim();
	}

	/**
	 * Test whether the actual collection contains all the entries in the
	 * expected collection, although the actual may contain more.
	 * 
	 * @param expected
	 * @param actual
	 * @throws AssertionFailedError
	 */
	public static void assertContains(Collection<?> expected, Collection<?> actual)
			throws AssertionFailedError {
		if ((expected == null) && (actual == null)) {
			return;
		}
		if ((expected == null) || (actual == null)) {
			throw new AssertionFailedError("Expected collection " + expected
					+ " but found collection " + actual);
		}

		for (Object entry : expected) {
			if (!actual.contains(entry)) {
				throw new AssertionFailedError("Expected entry '" + entry
						+ "' but it was not found in collection " + actual);
			}
		}
		return;
	}

	/**
	 * Test whether a collection contains an expected value.
	 * 
	 * @param expected
	 * @param actual
	 * @throws AssertionFailedError
	 */
	public static void assertContains(Object expected, Collection<?> actual)
			throws AssertionFailedError {
		if (!actual.contains(expected)) {
			throw new AssertionFailedError("Expected '" + expected
					+ "' but it was not found in collection " + actual);
		}
	}

	/**
	 * assert that the keys and values in the 'expected' map are exactly equal
	 * to the keys and values in the 'actual' map.
	 * 
	 * @param expected
	 * @param actual
	 * @throws AssertionFailedError
	 */
	public static void assertEquals(Map<?, ?> expected, Map<?, ?> actual)
			throws AssertionFailedError {
		if ((expected == null) && (actual == null)) {
			return;
		}
		if (expected == null) {
			throw new AssertionFailedError("Expected null, but actual was Map<"
					+ actual.getClass().getTypeParameters()[0].getGenericDeclaration().getName()
					+ ","
					+ actual.getClass().getTypeParameters()[1].getGenericDeclaration().getName()
					+ "> " + "but actual was null.");
		} else if (actual == null) {
			throw new AssertionFailedError("Expected Map<"
					+ expected.getClass().getTypeParameters()[0].getGenericDeclaration().getName()
					+ ","
					+ expected.getClass().getTypeParameters()[1].getGenericDeclaration().getName()
					+ "> " + "but actual was null.");
		}
		if (expected.size() != actual.size()) {
			throw new AssertionFailedError("Expected Map<"
					+ expected.getClass().getTypeParameters()[0].getGenericDeclaration().getName()
					+ ","
					+ expected.getClass().getTypeParameters()[1].getGenericDeclaration().getName()
					+ "> " + "size " + expected.size() + "but actual was Map<"
					+ actual.getClass().getTypeParameters()[0].getGenericDeclaration().getName()
					+ ","
					+ actual.getClass().getTypeParameters()[1].getGenericDeclaration().getName()
					+ ">" + "size " + actual.size());
		}

		for (Object key : expected.keySet()) {
			if (!expected.get(key).equals(actual.get(key))) {
				throw new AssertionFailedError("Expected value '" + expected.get(key)
						+ "' for key '" + key + "' but found '" + actual.get(key) + "' instead.");
			}
		}
		return;
	}

	/**
	 * assert that the keys and values in the 'expected' map are also in the
	 * 'actual' map, but the 'actual' map may contain more properties.
	 * 
	 * @param expected
	 * @param actual
	 * @throws AssertionFailedError
	 */
	public static void assertContains(Map<?, ?> expected, Map<?, ?> actual)
			throws AssertionFailedError {
		if ((expected == null) && (actual == null)) {
			return;
		}
		if (expected == null) {
			throw new AssertionFailedError("Expected null, but actual was Map<"
					+ actual.getClass().getTypeParameters()[0].getGenericDeclaration().getName()
					+ ","
					+ actual.getClass().getTypeParameters()[1].getGenericDeclaration().getName()
					+ "> " + "but actual was null.");
		} else if (actual == null) {
			throw new AssertionFailedError("Expected Map<"
					+ expected.getClass().getTypeParameters()[0].getGenericDeclaration().getName()
					+ ","
					+ expected.getClass().getTypeParameters()[1].getGenericDeclaration().getName()
					+ "> " + "but actual was null.");
		}
		if (expected.size() > actual.size()) {
			throw new AssertionFailedError("Expected Map<"
					+ expected.getClass().getTypeParameters()[0].getGenericDeclaration().getName()
					+ ","
					+ expected.getClass().getTypeParameters()[1].getGenericDeclaration().getName()
					+ "> " + "size " + expected.size() + "but actual was Map<"
					+ actual.getClass().getTypeParameters()[0].getGenericDeclaration().getName()
					+ ","
					+ actual.getClass().getTypeParameters()[1].getGenericDeclaration().getName()
					+ ">" + "size " + actual.size());
		}

		for (Object key : expected.keySet()) {
			if (!expected.get(key).equals(actual.get(key))) {
				throw new AssertionFailedError("Expected value '" + expected.get(key)
						+ "' for key '" + key + "' but found '" + actual.get(key) + "' instead.");
			}
		}
		return;
	}

	/**
	 * Test that the supplied map contains the supplied key
	 * 
	 * @param key
	 * @param map
	 * @throws AssertionFailedError
	 */
	public static void assertContainsKey(Object key, Map<?, ?> map) throws AssertionFailedError {
		if (map == null) {
			throw new AssertionFailedError("The supplied map is null.");
		}
		if (!map.containsKey(key)) {
			throw new AssertionFailedError("The supplied map does not contain the expected key "
					+ key);
		}
	}

	public static class ParamTypeAndValue {
		private final Class<?> type;
		private final Object value;

		public <T> ParamTypeAndValue(Class<T> type, T value) {
			this.type = type;
			this.value = value;
		}

		public Class<?> getType() {
			return type;
		}

		public Object getValue() {
			return value;
		}
	}

	/**
	 * Allows calling a private method specifying the type and parameter for each argument. This is needed if the
	 * value being set is null or a narrower type to the argument, for example supplying a String for an Object
	 * parameter.
	 *
	 * @param onObject
	 * @param methodName
	 * @param parameters
	 * @return
	 * @throws Exception
	 */
	public static Object callPrivateMethod(Object onObject, String methodName, ParamTypeAndValue... parameters) throws Exception {
		Method method = onObject.getClass().getDeclaredMethod(methodName, (parameters!=null?Arrays.stream(parameters).map(ParamTypeAndValue::getType).toArray(size ->new Class[size]): null));
		method.setAccessible(true);
		return method.invoke(onObject, (parameters!=null?Arrays.stream(parameters).map(ParamTypeAndValue::getValue).toArray(size ->new Object[size]): null));
	}

	public static Object callPrivateMethod(Object onObject, String methodName, Object... parameters) throws Exception {
		Method method = onObject.getClass().getDeclaredMethod(methodName, (parameters!=null?Arrays.stream(parameters).map(o -> {return (o!=null?o.getClass():null);}).toArray(size ->new Class[size]): null));
		method.setAccessible(true);
		return method.invoke(onObject, parameters);
	}

	public static <T> T getPrivateFieldValue(Object onObject, String fieldName, Class<T> fieldType) throws Exception {
		Field field = onObject.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return fieldType.cast(field.get(onObject));
	}
}
