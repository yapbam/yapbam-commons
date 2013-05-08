package net.yapbam.util;

import java.util.logging.Level;
import java.util.logging.Logger;

/** A very simple and limited log, based on java.util.Logger.
 * <br>The main additions of this class upon the original java.util.Logger are:<ul>
 * <li>Messages are tagged by the name of the class of an origin instance.</li>
 * <li>Shorter to write than Logger calls (inspired by android.util.Log)</li>
 * </ul> 
 */
public class Log {
	private static void v(Class<? extends Object> class_, String message) {
		Logger.getLogger(class_.getName()).finest(message);
	}

	public static void v(Object obj, String message) {
		Log.v(obj.getClass(), message);
	}

	public static void w(Object obj, String message) {
		Logger.getLogger(obj.getClass().getName()).log(Level.WARNING, message);
	}

	public static void w(Object obj, String message, Throwable e) {
		Logger.getLogger(obj.getClass().getName()).log(Level.WARNING, message, e);
	}
}
