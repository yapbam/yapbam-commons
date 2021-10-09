package net.yapbam.data;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** A bank account */
public class Account {
	private String name;
	private double initialBalance;
	private List<Mode> modes;
	private List<Checkbook> checkbooks;
	private int transactionNumber;
	private int unCheckedTransactionNumber;
	private BalanceData balanceData;
	private AlertThreshold alertThreshold;
	private int checkNumberAlertThreshold;
	private String comment;

	/** Constructor.
	 * <br>This constructor creates a new account with the default alerts and one payment mode: Mode.UNDEFINED.
	 * @param name The name of the account
	 * @param initialBalance The initial balance of the account
	 * @see Mode#UNDEFINED
	 * @see AlertThreshold#DEFAULT
	 */
	public Account(String name, double initialBalance) {
		this(name, initialBalance, AlertThreshold.DEFAULT);
	}

	/**
	 * Constructor.
	 * <br>This constructor creates a new account with one payment mode: Mode.UNDEFINED.
	 * @param name The account's name
	 * @param initialBalance The account's initial balance
	 * @param alerts The alerts set on the account.
	 */
	public Account(String name, double initialBalance, AlertThreshold alerts) {
		this(name, initialBalance, alerts, null);
	}

	public Account(String name, double initialBalance, AlertThreshold alerts, String comment) {
		this.name = name;
		this.initialBalance = initialBalance;
		this.alertThreshold = alerts;
		this.modes = new ArrayList<Mode>();
		this.checkbooks = new ArrayList<Checkbook>();
		this.checkNumberAlertThreshold = -1;
		this.balanceData = new BalanceData();
		this.balanceData.clear(initialBalance);
		this.add(Mode.UNDEFINED);
		this.setComment(comment);
	}

	/** Gets the account's name
	 * @return the account's name
	 */
	public String getName() {
		return name;
	}

	/** Gets the account's initial balance.
	 * @return the account's initial balance
	 */
	public double getInitialBalance() {
		return this.initialBalance;
	}

	/** Gets an account's payment mode by its name.
	 * @param name the payment mode's name (an empty string to retrieve the UNDEFINED mode)
	 * @return The payment mode, or null, if no payment mode with that name exists
	 */
	public Mode getMode(String name) {
		for (int i = 0; i < this.modes.size(); i++) {
			if (this.modes.get(i).getName().equalsIgnoreCase(name)) {
				return this.modes.get(i);
			}
		}
		return null;
	}
	
	/** Gets the account's number of checkbooks.
	 * @return an integer
	 */
	public int getCheckbooksNumber() {
		return this.checkbooks.size();
	}
	
	/** Get's an account checkbook
	 * @param index checkbook index
	 * @return a checkbook
	 * @see #getCheckbooksNumber()
	 */
	public Checkbook getCheckbook(int index) {
		return this.checkbooks.get(index);
	}
	
	/** Gets the index of a checkbook in the account.
	 * @param book The checkbook to test.
	 * @return a negative integer if the checkbook is unknown in this account,
	 * or, else, the index of the checkbook.  
	 */
	public int indexOf(Checkbook book) {
		return this.checkbooks.indexOf(book);
	}
	
	void add(Checkbook book) {
		this.checkbooks.add(book);
	}

	void remove(Checkbook book) {
		this.checkbooks.remove(book);
	}
	
	/** Gets the number of transactions in this account.
	 * @return the number of transactions in this account.
	 */
	public int getTransactionsNumber() {
		return transactionNumber;
	}

	/** Gets the number of unchecked transactions in this account.
	 * <br>An unchecked transaction is a transaction assigned to no Statement.
	 * @return the number of unchecked transactions in this account.
	 */
	public int getUncheckedTransactionsNumber() {
		return unCheckedTransactionNumber;
	}

	/** Adds transactions to the account.
	 * @param transactions the transaction to add
	 */
	void add(Transaction[] transactions) {
		transactionNumber += transactions.length;
		for (Transaction transaction : transactions) {
			if (!transaction.isChecked()) {
				this.unCheckedTransactionNumber++;
			}
		}
		this.balanceData.updateBalance(transactions, true);
	}
	
	/** Removes transactions from this account.
	 * @param transactions the transactions to be removed.
	 */
	void remove(Transaction[] transactions) {
		transactionNumber = transactionNumber - transactions.length;
		for (Transaction transaction : transactions) {
			if (!transaction.isChecked()) {
				this.unCheckedTransactionNumber--;
			}
		}
		this.balanceData.updateBalance(transactions, false);
	}

	void add(Mode newMode) {
		if (this.getMode(newMode.getName())!=null) {
			throw new IllegalArgumentException("This account already contains the mode "+newMode.getName()); //$NON-NLS-1$
		}
		this.modes.add(newMode);
	}

	void remove(Mode mode) {
		this.modes.remove(mode);
	}
	

	void replace(Mode oldMode, Mode newMode) {
		// Be aware not to really replace the mode, but update it (transactions have a pointer to their mode).
		oldMode.updateTo(newMode);
	}

	@Override
	public String toString() {
		return MessageFormat.format("{0}[{1,number,currency}]",this.getName(),this.initialBalance); //$NON-NLS-1$
	}
	
	/** Gets this account's total number of payment modes (expense and receipt modes).
	 * @return this account's total number of payment modes
	 */
	public int getModesNumber() {
		return this.modes.size();
	}
	
	/** Gets a payment mode by its index.
	 * @param index the payment mode index.
	 * @return the payment mode.
	 */
	public Mode getMode(int index) {
		return this.modes.get(index);
	}

	/** Gets the index of a payment mode for this account.
	 * @param mode The mode to find
	 * @return a negative number if the mode is unknown, or the index if it was found.
	 */
	public int indexOf(Mode mode) {
		return this.modes.indexOf(mode);
	}
	
	void setName(String name) {
		this.name = name;
	}

	void setInitialBalance(double value) {
		this.balanceData.updateBalance(this.initialBalance, false);
		this.initialBalance = value;
		this.balanceData.updateBalance(this.initialBalance, true);
	}

	/** Gets the alert threshold.
	 * @return an {@link AlertThreshold}
	 */
	public AlertThreshold getAlertThreshold() {
		return this.alertThreshold;
	}

	void setAlertThreshold(AlertThreshold alertThreshold) {
		this.alertThreshold = alertThreshold;
	}
	
	/** Gets the number of available checks under which an alert should be raised.
	 * @return an int. A negative number if no alert is set (which is the default).
	 */
	public int getCheckNumberAlertThreshold() {
		return this.checkNumberAlertThreshold;
	}

	void setCheckNumberAlertThreshold(int alertThreshold) {
		this.checkNumberAlertThreshold = alertThreshold;
	}

	/** Gets this account's balance data.
	 * @return a BalanceData
	 */
	public BalanceData getBalanceData() {
		return this.balanceData;
	}
	
	/** Gets this account's comment.
	 * @return a String or null if no comment is attached to this account
	 */
	public String getComment() {
		return this.comment;
	}
	
	void setComment(String comment) {
		this.comment = comment;
		if (this.comment!=null) {
			this.comment = this.comment.trim();
			if (this.comment.length()==0) {
				this.comment = null;
			}
		}
	}

	/** Gets the first alert on this account between to dates.
	 * @param from first date or null if the time interval starts at the beginning of times.
	 * @param to first date or null if the time interval ends at the end of times.
	 * @return An alert or null if there is no alert on this account in that time frame.
	 */
	public Alert getFirstAlert(Date from, Date to) {
		BalanceHistory balanceHistory = getBalanceData().getBalanceHistory();
		long firstAlertDate = balanceHistory.getFirstAlertDate(from, to, getAlertThreshold());
		if (firstAlertDate>=0) {
			Date date = new Date();
			if (firstAlertDate>0) {
				date.setTime(firstAlertDate);
			}
			return new Alert(date, this, balanceHistory.getBalance(date));
		} else {
			return null;
		}
	}
	
	/** Gets the number of check remaining for this account.
	 * @return a positive or null integer
	 * @see #getCheckNumberAlertThreshold()
	 */
	public int getRemainingChecks() {
		int result = 0;
		for (Checkbook book : this.checkbooks) {
			result+= book.getRemaining();
		}
		return result;
	}
}
