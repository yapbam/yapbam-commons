package net.yapbam.data;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.yapbam.util.DateUtils;
import net.yapbam.util.NullUtils;

/** A balance history.
 * <br>The balance history is an ordered list of periods, during one of these periods, the balance is constant.
 * These periods are represented by BalanceHistoryElement class.
 * @see BalanceHistoryElement
 * @see BalanceData#getBalanceHistory()
 */
public class BalanceHistory implements Serializable {
	private static final long serialVersionUID = 1L;

	private static final Comparator<Object> COMPARATOR = new Comparator<Object>() {
		public int compare(Object o1, Object o2) {
			if (o1 instanceof BalanceHistoryElement) {
				return -((BalanceHistoryElement) o1).getRelativePosition((Date)o2);
			} else {
				return ((BalanceHistoryElement) o2).getRelativePosition((Date)o1);
			}
		}	
	};
	
	/** True if the min and max do not need to be computed and are the min/max values according to minMaxEnddate. */
	private boolean minMaxCached;
	/** The end data used to compute the min/max values (null for no end date). */
	private Date minMaxEndDate;
	private double minBalance;
	private double maxBalance;
	private List<BalanceHistoryElement> elements;
	private List<Transaction> transactions;
	
	/** Constructor.
	 * @param intialBalance The initial balance (at the beginning of times).
	 */
	public BalanceHistory(double intialBalance) {
		super();
		this.minMaxCached = false;
		this.elements = new ArrayList<BalanceHistoryElement>();
		this.elements.add(new BalanceHistoryElement(intialBalance, null, null));
		this.transactions = new ArrayList<Transaction>();
	}
	
	/** Returns the minimum balance of the history.
	 * @return history's minimum balance 
	 */
	public double getMinBalance() {
		return getMinBalance(null);
	}
	
	/** Returns the minimum balance of the history.
	 * @param endDate All elements after this date will be ignored (null, to ignore nothing).
	 * @return history's minimum balance 
	 */
	public double getMinBalance(Date endDate) {
		refreshMinMax(endDate);
		return this.minBalance;
	}
	
	/** Gets the first date between two dates, when the balance is lower or greater than an amount.
	 * @param from first date or null if the time interval starts at the beginning of times
	 * @param to first date or null if the time interval ends at the end of times
	 * @param alert the alert threshold
	 * @return a long representing the first date under amount.
	 * <br>A negative long if the balance never triggers the alert between from and to,
	 * the date.getTime() of the searched date in other cases.
	 */
	public long getFirstAlertDate(Date from, Date to, AlertThreshold alert) {
		if (alert.isLifeless()) {
			return -1;
		}
		int firstIndex = from==null?0:find(from);
		int lastIndex = to==null?elements.size()-1:find(to);
		for (int i=firstIndex; i<=lastIndex; i++) {
			BalanceHistoryElement element = elements.get(i);
			if (alert.getTrigger(element.getBalance())!=0) {
				Date result = element.getFrom();
				return result==null?0:result.getTime();
			}
		}
		return -1;
	}

	/** Returns the maximum balance of the history.
	 * @return history's maximum balance 
	 */
	public double getMaxBalance() {
		return this.getMaxBalance(null);
	}
	
	/** Returns the maximum balance of the history.
	 * @param endDate All elements after this date will be ignored (null, to ignore nothing).
	 * @return history's maximum balance 
	 */
	public double getMaxBalance(Date endDate) {
		refreshMinMax(endDate);
		return this.maxBalance;
	}

	private void refreshMinMax(Date endDate) {
		if (!minMaxCached || !NullUtils.areEquals(endDate, minMaxEndDate)) {
			this.maxBalance = get(0).getBalance();
			this.minBalance = this.maxBalance;
			for (BalanceHistoryElement element : elements) {
				if ((endDate!=null) && (element.getRelativePosition(endDate)<0)) {
					break;
				}
				double balance = element.getBalance();
				if (this.maxBalance<balance) {
					this.maxBalance = balance;
				} else if (this.minBalance>balance) {
					this.minBalance = balance;
				}
			}
			this.minMaxEndDate = endDate;
			minMaxCached = true;
		}
	}

	/** Returns the number of history elements (period with the same balance)
	 * @return the number of periods.
	 */
	public int size() {
		return this.elements.size();
	}

	/** Gets an element (period of time with constant balance) of this history. 
	 * @param index of the element
	 * @return the element
	 */
	public BalanceHistoryElement get(int index) {
		return this.elements.get(index);
	}

	/** Gets the index of the element that includes a date.
	 * @param date The date to search
	 * @return the index of the element that contains the date
	 */
	public int find(Date date) {
		return Collections.binarySearch(this.elements, date, COMPARATOR);
	}

	/** Gets a specified date's balance. 
	 * @param date the date for which we want to get the balance
	 * @return the balance
	 */
	public double getBalance(Date date) {
		return get(find(date)).getBalance();
	}

	/** Add an amount to the history at a specified date.
	 * @param amount amount to add (may be negative)
	 * @param date date or null if the amount has to be added at the beginning of times
	 *  (ie the initial balance of a newly created account)
	 */
	void add(double amount, Date date) {
		if (date==null) {
			if (minMaxCached) {
				this.minBalance += amount;
				this.maxBalance += amount;
			}
			for (Iterator<BalanceHistoryElement> iterator = elements.iterator(); iterator.hasNext();) {
				iterator.next().add(amount);
			}
		} else {
			int index = find(date);
			BalanceHistoryElement element = get(index);
			if (!date.equals(element.getFrom())) {
				// not at the beginning of the period, need to split the element
				BalanceHistoryElement el2 = new BalanceHistoryElement(element.getBalance(), date, element.getTo());
				element.setTo(date);
				index++;
				this.elements.add(index, el2);
			} else {
				// Maybe the amount is now the same as previous element.
				// In such a case, we have to merge these elements
				BalanceHistoryElement previous = this.elements.get(index-1);
				double future = element.getBalance()+amount;
				// WARNING : a simple test like "previous.getBalance()==future" is wrong because of the lack of precision
				// of double representation. We have to test that the difference is less than the currency precision
				if (GlobalData.AMOUNT_COMPARATOR.compare(previous.getBalance(),future)==0) {
					this.elements.remove(index);
					previous.setTo(element.getTo());
				}
			}
			for (int i = index; i < this.elements.size(); i++) {
				element = this.elements.get(i);
				element.add(amount);
			}
			minMaxCached = false;
		}
	}

	/** Adds a transaction to the history. 
	 * @param transaction the added transaction
	 */
	void add(Transaction transaction) {
		this.add(transaction.getAmount(), transaction.getValueDate());
		int index = -Collections.binarySearch(transactions, transaction, TransactionComparator.VALUE_DATE_COMPARATOR)-1;
		transactions.add(index, transaction);
	}

	/** Removes a transaction from the history.
	 * @param transaction the transaction to be removed
	 */
	void remove(Transaction transaction) {
		this.add(-transaction.getAmount(), transaction.getValueDate());
		int index = Collections.binarySearch(transactions, transaction, TransactionComparator.VALUE_DATE_COMPARATOR);
		if (index>=0) {
			transactions.remove(index);
		}
	}

	/** Gets the number of transactions in the history.
	 * @return an positive or null integer.
	 */
	public int getTransactionsNumber() {
		return transactions.size();
	}
	
	/** Gets a transaction in the history.
	 * @param index The transaction's index
	 * @return a Transaction
	 */
	public Transaction getTransaction(int index) {
		return transactions.get(index);
	}

	public int find(Transaction transaction) {
		return transactions.indexOf(transaction);
	}
	
	/** Gets the index of the first transaction with the the argument value date.
	 * @param valueDate The transaction's date in its integer representation.
	 * @return the transaction's index or a negative number if no transaction has this value date.
	 * @see DateUtils#dateToInteger(Date)
	 */
	private int getFirstIndexOf(int valueDate) {
		if (transactions.isEmpty()) {
			return -1;
		}
		int first = 0;
		int last = transactions.size()-1;
		while (first!=last) {
			if (valueDate<transactions.get(first).getValueDateAsInteger() || valueDate>transactions.get(last).getValueDateAsInteger()) {
				return -1;
			}
			if (last-first==1) {
				if (valueDate==transactions.get(first).getValueDateAsInteger()) {
					return first;
				} else if (valueDate==transactions.get(last).getValueDateAsInteger()) {
					return last;
				} else {
					return -1;
				}
			}
			int middle = (first+last)/2;
			if (valueDate<=transactions.get(middle).getValueDateAsInteger()) {
				last = middle;
			} else {
				first = middle;
			}
		}
		return first;
	}
	
	/** Gets the transactions of a balance history element by its index.
	 * @param index The element's index as in method {@link #get(int)}
	 * @return a transactions list
	 * @throws IndexOutOfBoundsException if the index has no balance history element.
	 */
	public List<Transaction> getTransactions(int index) {
		BalanceHistoryElement element = get(index);
		List<Transaction> result = new ArrayList<Transaction>();
		Date from = element.getFrom();
		int first = from==null ? 0 : getFirstIndexOf(DateUtils.dateToInteger(from));
		Date to = element.getTo();
		int lastDate = to==null ? Integer.MAX_VALUE : DateUtils.dateToInteger(to);
		for (int i=first;i<transactions.size();i++) {
			Transaction t = getTransaction(i);
			if (t.getValueDateAsInteger()<lastDate) {
				result.add(t);
			} else {
				break;
			}
		}
		return result;
	}
}
