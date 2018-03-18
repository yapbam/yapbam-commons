package net.yapbam.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Multi platform Base64 encoder.
 * <br>Both desktop and android java natively support Base64 encoding. Unfortunately, this support relies on different classes.
 * This class provides a unique end point to encode bytes in Base64.
 * @author Jean-Marc Astesana
 */
public abstract class Base64Encoder {
	private Base64Encoder() {
		// To prevent instance from being created
	}

	/** Encodes bytes.
	 * @param bytes The bytes to encode (If you try to encode strings, be aware that String.getBytes() does not return the same bytes on every platforms.
	 * You should prefer the method String.getBytes(String) that allows you to specify the encoding.
	 * @return The Base64 encoded string without any end char (Android Base64 default encoder adds a char of value 10 at the end of the encoded bytes, this doesn't).
	 */
	public static String encode(byte[] bytes) {
		try {
			try {
				return doJava8(bytes);
			} catch (ClassNotFoundException e) { // NOSONAR No need to rethrow this exception, it simply denotes we are not on a java desktop machine
				try {
					return doOldJava(bytes);
				} catch (ClassNotFoundException e1) { // NOSONAR No need to rethrow this exception, it simply denotes we are not on a java desktop machine
					return doAndroid(bytes);
				}
			}
		} catch (NoSuchMethodException e) {
			throw new UnsupportedOperationException(e);
		} catch (IllegalAccessException e) {
			throw new UnsupportedOperationException(e);
		} catch (InvocationTargetException e) {
			throw new UnsupportedOperationException(e);
		}
	}
	
	private static String doJava8(byte[] bytes) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		Class<?> class1 = Class.forName("java.util.Base64");
		Method staticMethod = class1.getMethod("getEncoder");
		Object instance = staticMethod.invoke(null);
		Method method = instance.getClass().getMethod("encode", byte[].class);
		return new String((byte[])method.invoke(instance, bytes));
	}

	private static String doOldJava(byte[] bytes) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		Class<?> class1 = Class.forName("javax.xml.bind.DatatypeConverter");
		Method method = class1.getMethod("printBase64Binary", byte[].class);
		return (String) method.invoke(null, bytes);
	}

	private static String doAndroid(byte[] bytes) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		try {
			Class<?> class1 = Class.forName("android.util.Base64");
			Method method = class1.getMethod("encode", byte[].class, int.class);
			int flags = class1.getField("NO_WRAP").getInt(null);
			return new String((byte[])method.invoke(null, bytes, flags));
		} catch (ClassNotFoundException e1) {
			// Unable to perform the operation
			throw new UnsupportedOperationException(e1);
		} catch (NoSuchFieldException e1) {
			throw new UnsupportedOperationException(e1);
		}
	}
}
