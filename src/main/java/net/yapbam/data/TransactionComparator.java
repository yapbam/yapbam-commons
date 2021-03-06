package net.yapbam.data;

import java.util.Comparator;

import net.yapbam.util.NullUtils;

abstract class TransactionComparator implements Comparator<Transaction> {
	private TransactionComparator() {
		// To prevent instance from being created
	}
	
	static final TransactionComparator INSTANCE = new TransactionComparator() {
		public int compare(Transaction o1, Transaction o2) {
			int result = o1.getDateAsInteger()-o2.getDateAsInteger();
			if (result == 0) {
				result = o1.getAccount().getName().compareToIgnoreCase(o2.getAccount().getName());
			}
			if (result == 0) {
				result = NullUtils.compareTo(o1.getStatement(),o2.getStatement(), true);
			}
			if (result == 0) {
				result = (int) (Math.signum(o1.getAmount()) - Math.signum(o2.getAmount()));
			}
			if (result == 0) {
				result = Long.signum(o1.getId() - o2.getId());
			}
			if (result == 0) {
				result = o1.getValueDateAsInteger()-o2.getValueDateAsInteger();
			}
			if (result == 0) {
				result = partialCompare(o1, o2);
			}
			return result;
		}
	};
	
	/** A comparator used to compute balance history.
	 * 	Transactions are sorted by value date, then, by amount (receipt should be treated before debts).
	 */
	static final TransactionComparator VALUE_DATE_COMPARATOR = new TransactionComparator() {
		public int compare(Transaction o1, Transaction o2) {
			int result = o1.getValueDateAsInteger()-o2.getValueDateAsInteger();
			if (result == 0) {
				result = (int) (Math.signum(o2.getAmount())-Math.signum(o1.getAmount()));
			}
			if (result == 0) {
				result = o1.getDateAsInteger()-o2.getDateAsInteger();
			}
			if (result == 0) {
				result = NullUtils.compareTo(o1.getStatement(),o2.getStatement(), true);
			}
			if (result == 0) {
				result = Long.signum(o1.getId() - o2.getId());
			}
			if (result == 0) {
				result = partialCompare(o1, o2);
			}
			if (result == 0) {
				result = o1.getAccount().getName().compareToIgnoreCase(o2.getAccount().getName());
			}
			return result;
		}
	};

	private static int partialCompare(Transaction o1, Transaction o2) {
		int result = o1.getMode().getName().compareToIgnoreCase(o2.getMode().getName());
		if (result == 0) {
			result = NullUtils.compareTo(o1.getNumber(),o2.getNumber(), true);
		}
		if (result == 0) {
			result = o1.getCategory().getName().compareToIgnoreCase(o2.getCategory().getName());
		}
		if (result == 0) {
			result = o1.getDescription().compareToIgnoreCase(o2.getDescription());
		}
		if (result == 0) {
			result = (int) Math.signum(o1.getAmount() - o2.getAmount());
		}
		return result;
	}
}
