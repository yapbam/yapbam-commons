package net.yapbam.data;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.yapbam.data.GlobalData;
import net.yapbam.data.Transaction;

/** A wizard that guesses a value using the transaction history.
 * @param <T> The type of the value we are trying to guess.
 */
public abstract class AbstractTransactionWizard<T> {
	private static final int MILLIS_PER_DAY = 3600000 * 24;
	/** The global data used to guess. */
	protected GlobalData data;
	/** The guessed value. */ 
	protected T value;
	/** True if the guessed value was already computed.*/
	protected boolean inited;
	
	/** Constructor.
	 * @param data The global data.
	 */
	protected AbstractTransactionWizard(GlobalData data) {
		this.data = data;
		this.value = null;
		this.inited = false;
	}

	/** The value associated with a transaction.
	 * @param transaction A transaction.
	 * @return the value of this transaction.
	 */
	protected abstract T getValue(Transaction transaction);
	
	/** Tested whether a transaction should be ignored for the guess.
	 * @param transaction A transaction.
	 * @return false if the transaction should be ignored.
	 */
	protected abstract boolean isValid(Transaction transaction);
	
	/** Gets the ranking of a transaction.
	 * <br>This method is used by the wizard to determine which values are the most probable.
	 * <br>The default implementation is based on the transaction's date. Old transactions have lowest ranking than new ones.
	 * @param transaction The transaction
	 * @return a double, the ranking of the transaction.
	 * @see #getRankingBasedOnDate(long, Transaction)
	 */
	protected double getRanking(Transaction transaction) {
		return getRankingBasedOnDate(System.currentTimeMillis(), transaction);
	}

	/** Gets the ranking of a transaction, based on its date.
	 * This method is used by the default implementation of {link {@link #getRanking(Transaction)}.
	 * @param now The current time (usually System.currentTimeMillis()).
	 * @param transaction The transaction.
	 * @return a double
	 */
	public static double getRankingBasedOnDate(long now, Transaction transaction) {
		// We use a function between 0 (for very, very old ones) and 1 for recent one.
		// Probably this function could be improved ...
		long time = Math.abs(transaction.getDate().getTime() - now) / MILLIS_PER_DAY;
		return 2 / Math.sqrt(time + 4);
	}
	
	/** Gets the key in a map with the highest value.
	 * @param map A map
	 * @param <V> The type of the map keys
	 * @return the key with the highest value.
	 */
	public static <V> V getHeaviest(Map<V, Double> map) {
		V ct = null;
		double max = 0.0;
		for (Iterator<Entry<V, Double>> iterator = map.entrySet().iterator(); iterator.hasNext();) {
			Entry<V, Double> next = iterator.next();
			if (next.getValue() > max) {
				ct = next.getKey();
				max = next.getValue();
			}
		}
		return ct;
	}
	
	/** Get the guessed value.
	 * @return The guessed value or null if nothing is guessed.
	 */
	public T get() {
		if (!inited) {
			HashMap<T, Double> toProbability = new HashMap<T, Double>();
			for (int i = 0; i < data.getTransactionsNumber(); i++) {
				Transaction transaction = data.getTransaction(i);
				if (isValid(transaction)) {
					T transactionValue = getValue(transaction);
					if (transactionValue != null) {
						Double weight = toProbability.get(transactionValue);
						double transactionWeight = getRanking(transaction);
						toProbability.put(transactionValue, transactionWeight + (weight == null ? 0 : weight));
					}
				}
			}
			value = getHeaviest(toProbability);
		}
		return value;
	}
}