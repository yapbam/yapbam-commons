package net.yapbam.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Multi platform Base64 encoder.
 * <br>Both desktop and android java natively support Base64 encoding. Unfortunately, this support relies on different classes.
 * This class provides a unique end point to encode bytes in Base64.
 * @author Jean-Marc Astesana
 */
public class Base64Encoder {
	/** Encodes bytes.
	 * @param bytes The bytes to encode (If you try to encode strings, be aware that String.getBytes() does not return the same bytes on every platforms.
	 * You should prefer the method String.getBytes(String) that allows you to specify the encoding.
	 * @return The Base64 encoded string without any end char (Android Base64 default encoder adds a char of value 10 at the end of the encoded bytes, this doesn't).
	 */
	public static String encode(byte[] bytes) {
		try {
			try { // Java desktop
				Class<?> class1 = Class.forName("javax.xml.bind.DatatypeConverter");
				Method method = class1.getMethod("printBase64Binary", new Class<?>[]{byte[].class});
				return (String) method.invoke(null, bytes);
			} catch (ClassNotFoundException e) {
				try { // Android
					Class<?> class1 = Class.forName("android.util.Base64");
					Method method = class1.getMethod("encode", new Class<?>[]{byte[].class, int.class});
					int flags = class1.getField("NO_WRAP").getInt(null);
					return new String((byte[])method.invoke(null, bytes, flags));
				} catch (ClassNotFoundException e1) {
					// Unable to perform the operation
					throw new UnsupportedOperationException();
				} catch (NoSuchFieldException e1) {
					// TODO Auto-generated catch block
					throw new UnsupportedOperationException();
				}
			}
		} catch (NoSuchMethodException e) {
			throw new UnsupportedOperationException();
		} catch (IllegalAccessException e) {
			throw new UnsupportedOperationException();
		} catch (InvocationTargetException e) {
			throw new UnsupportedOperationException();
		}
	}
}