/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;

/**
 * Base class for unit tests that extends JUnit 5 assertion coverage to
 * subclasses while preserving the custom byte[]/Collection/Map overloads.
 * <p>
 * The bridge overloads below are required because Java resolves inherited
 * static methods before consulting {@code import static} declarations.  Without
 * them, any call to {@code assertEquals(String, String)} inside a subclass
 * would fail to resolve because the compiler finds the custom
 * {@code assertEquals(byte[], byte[])} overload first and reports a type
 * mismatch rather than falling through to the Assertions import.
 * </p>
 * @author ron
 */
public abstract class TestCase {

    // -----------------------------------------------------------------
    // JUnit 5 bridge overloads — prevents custom overloads from shadowing
    // the standard assertion methods that subclasses expect to inherit.
    // -----------------------------------------------------------------

    public static void assertEquals(Object expected, Object actual) {
        Assertions.assertEquals(expected, actual);
    }
    public static void assertEquals(Object expected, Object actual, String message) {
        Assertions.assertEquals(expected, actual, message);
    }
    public static void assertEquals(long expected, long actual) {
        Assertions.assertEquals(expected, actual);
    }
    public static void assertEquals(long expected, long actual, String message) {
        Assertions.assertEquals(expected, actual, message);
    }
    public static void assertEquals(int expected, int actual) {
        Assertions.assertEquals(expected, actual);
    }
    public static void assertNotEquals(Object unexpected, Object actual) {
        Assertions.assertNotEquals(unexpected, actual);
    }
    public static void assertNotEquals(long unexpected, long actual) {
        Assertions.assertNotEquals(unexpected, actual);
    }
    public static void assertTrue(boolean condition) {
        Assertions.assertTrue(condition);
    }
    public static void assertTrue(boolean condition, String message) {
        Assertions.assertTrue(condition, message);
    }
    public static void assertFalse(boolean condition) {
        Assertions.assertFalse(condition);
    }
    public static void assertFalse(boolean condition, String message) {
        Assertions.assertFalse(condition, message);
    }
    public static void assertNull(Object object) {
        Assertions.assertNull(object);
    }
    public static void assertNull(Object object, String message) {
        Assertions.assertNull(object, message);
    }
    public static void assertNotNull(Object object) {
        Assertions.assertNotNull(object);
    }
    public static void assertNotNull(Object object, String message) {
        Assertions.assertNotNull(object, message);
    }
    public static void assertSame(Object expected, Object actual) {
        Assertions.assertSame(expected, actual);
    }
    public static void assertNotSame(Object unexpected, Object actual) {
        Assertions.assertNotSame(unexpected, actual);
    }
    public static void assertNotSame(Object unexpected, Object actual, String message) {
        Assertions.assertNotSame(unexpected, actual, message);
    }
    public static void fail(String message) {
        Assertions.fail(message);
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
