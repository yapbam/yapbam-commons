package net.yapbam.util;

/** Utilities about numbers. 
 * @author Jean-Marc Astesana
 * <BR>License : GPL v3
 */
public final class NumberUtils {
	// Be sure nobody will instantiate this class
	private NumberUtils(){
		super();
	}
	
	/** Adds two numbers.
	 * @param first The first number.
	 * @param second The second number.
	 * @return The sum of the first and the second number.<br>The class of the returned number is the one of the first argument.
	 * @throws UnsupportedOperationException if first is not a Integer, Long, Float or Double
	 */
	public static Number add (Number first, Number second) {
		if (first instanceof Integer) {
			return first.intValue() + second.intValue();
		} else if (first instanceof Long) {
			return first.longValue() + second.longValue();
		} else if (first instanceof Double) {
			return first.doubleValue() + second.doubleValue();
		} else if (first instanceof Float) {
			return first.floatValue() + second.floatValue();
		} else {
			throw new UnsupportedOperationException();
		}
	}
}
