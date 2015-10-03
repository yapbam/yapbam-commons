package net.yapbam.data;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.yapbam.data.event.*;
import net.yapbam.util.NullUtils;

/** The filtered Data (the global data viewed through a filter).
 * <BR>A filter is based on all the attributes of a transaction (amount, category, account, ...).
 * <BR>Please note that a transaction is ok for a filter when, of course, the transaction itself is ok,
 * but also when one of its subtransactions is ok.
 * <BR>For instance if your filter is set to display only receipts of the category x, and you have
 * an expense transaction and category y with an expense subtransaction of category x, the whole
 * transaction would be considered as ok.
 * @see GlobalData
 */
public class FilteredData extends DefaultListenable {
	private GlobalData data;
	private List<Transaction> transactions;
	private Comparator<Transaction> comparator = TransactionComparator.INSTANCE;
	private BalanceData balanceData;
	private Filter filter;
	private Observer filterObserver;
	private Logger logger;
	
	/** Constructor.
	 * @param data The data that is filtered
	 */
	public FilteredData(GlobalData data) {
		this.data = data;
		this.filter = new Filter();
		this.filterObserver = new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				filter();
			}
		};
		this.filter.addObserver(this.filterObserver);
		this.data.addListener(new DataListener() {
			@Override
			public void processEvent(DataEvent event) {
				if (eventImplySorting(event)) {
					Collections.sort(transactions, comparator);
				}
				if (event instanceof EverythingChangedEvent) {
					// If everything changed, reset the filter
					filter.clear();
					filter();
				} else if (event instanceof AccountRemovedEvent) {
					Account account = ((AccountRemovedEvent)event).getRemoved();
					List<Account> validAccounts = filter.getValidAccounts();
					if ((validAccounts==null) || validAccounts.remove(account)) {
						double initialBalance = account.getInitialBalance();
						balanceData.updateBalance(initialBalance, false);
						int index = validAccounts==null?((AccountRemovedEvent) event).getIndex():filter.getValidAccounts().indexOf(account);
						filter.setValidAccounts((validAccounts==null) || validAccounts.isEmpty()?null:validAccounts);
						fireEvent(new AccountRemovedEvent(FilteredData.this, index, account));
					}
				} else if (event instanceof CategoryRemovedEvent) {
					Category category = ((CategoryRemovedEvent)event).getRemoved();
					List<Category> validCategories = filter.getValidCategories();
					if ((validCategories==null) || validCategories.remove(category)) {
						int index = validCategories==null?((CategoryRemovedEvent) event).getIndex():filter.getValidCategories().indexOf(category);
						filter.setValidCategories((validCategories==null) || validCategories.isEmpty()?null:validCategories);
						fireEvent(new CategoryRemovedEvent(FilteredData.this, index, category));
					}
				} else if (event instanceof TransactionsAddedEvent) {
					Transaction[] ts = ((TransactionsAddedEvent)event).getTransactions();
					Collection<Transaction> accountOkTransactions = new ArrayList<Transaction>(ts.length);
					Collection<Transaction> okTransactions = new ArrayList<Transaction>(ts.length);
					double addedAmount = 0.0;
					for (Transaction transaction : ts) {
						if (filter.isOk(transaction.getAccount())) {
							// If the added transaction match with the account filter
							Date valueDate = transaction.getValueDate();
							if (NullUtils.compareTo(valueDate, filter.getValueDateFrom(),true)<0) {
								addedAmount += transaction.getAmount();
							} else {
								accountOkTransactions.add(transaction);
								if (filter.isOk(transaction)) {
									// If the added transaction matches with the whole filter
									okTransactions.add(transaction);
									int index = -Collections.binarySearch(transactions, transaction, comparator)-1;
									transactions.add(index, transaction);
								}
							}
						}
					}
					balanceData.updateBalance(addedAmount, true);
					// If some transactions in a valid account were removed, update the balance data
					if (!accountOkTransactions.isEmpty()) {
						balanceData.updateBalance(accountOkTransactions.toArray(new Transaction[accountOkTransactions.size()]), true);
					}
					// If some valid transactions were removed, fire an event.
					if (!okTransactions.isEmpty()) {
						fireEvent(new TransactionsAddedEvent(FilteredData.this, okTransactions.toArray(new Transaction[okTransactions.size()])));
					}
				} else if (event instanceof TransactionsRemovedEvent) {
					Transaction[] ts = ((TransactionsRemovedEvent)event).getTransactions();
					Collection<Transaction> accountOkTransactions = new ArrayList<Transaction>(ts.length);
					Collection<Transaction> okTransactions = new ArrayList<Transaction>(ts.length);
					double addedAmount = 0.0;
					for (Transaction transaction : ts) {
						if (filter.isOk(transaction.getAccount())) {
							Date valueDate = transaction.getValueDate();
							if (NullUtils.compareTo(valueDate, filter.getValueDateFrom(),true)<0) {
								addedAmount -= transaction.getAmount();
							} else {
								accountOkTransactions.add(transaction);
								if (filter.isOk(transaction)) { // If the added transaction matches with the whole filter
									okTransactions.add(transaction);
									int index = Collections.binarySearch(transactions, transaction, comparator);
									transactions.remove(index);
								}
							}
						}
					}
					balanceData.updateBalance(addedAmount, true);
					// If some transactions in a valid account were removed, update the balance data
					if (!accountOkTransactions.isEmpty()) {
						balanceData.updateBalance(accountOkTransactions.toArray(new Transaction[accountOkTransactions.size()]), false);
					}
					// If some valid transactions were removed, fire an event.
					if (!okTransactions.isEmpty()) {
						fireEvent(new TransactionsRemovedEvent(FilteredData.this, okTransactions.toArray(new Transaction[okTransactions.size()])));
					}
				} else if (event instanceof AccountAddedEvent) {
					Account account = ((AccountAddedEvent)event).getAccount();
					if (filter.isOk(account)) {
						balanceData.updateBalance(account.getInitialBalance(), true);
						if (filter.isOk(Filter.CHECKED)) {
							fireEvent(new AccountAddedEvent(FilteredData.this, account));
						}
					}
				} else if (event instanceof CategoryAddedEvent) {
					Category category = ((CategoryAddedEvent)event).getCategory();
					if (filter.isOk(category)) {
						fireEvent(new CategoryAddedEvent(FilteredData.this, category));
					}
				} else if (event instanceof AccountPropertyChangedEvent) {
					AccountPropertyChangedEvent evt = (AccountPropertyChangedEvent) event;
					if (filter.isOk(evt.getAccount())) {
						if (evt.getProperty().equals(AccountPropertyChangedEvent.INITIAL_BALANCE)) {
							double amount = ((Double)evt.getNewValue())-((Double)evt.getOldValue());
							balanceData.updateBalance(amount, true);
						}
						fireEvent(event);
					}
				} else if (event instanceof CategoryPropertyChangedEvent) {
					CategoryPropertyChangedEvent evt = (CategoryPropertyChangedEvent) event;
					if (filter.isOk(evt.getCategory())) {
						fireEvent(event);
					}
				} else if (event instanceof ModePropertyChangedEvent) {
					ModePropertyChangedEvent evt = (ModePropertyChangedEvent) event;
					if (filter.isOk(evt.getNewMode())) {
						fireEvent(event);
					}
				} else if (event instanceof ModeRemovedEvent) {
					ModeRemovedEvent evt = (ModeRemovedEvent) event;
					List<String> validModes = filter.getValidModes();
					String removedModeName = evt.getMode().getName();
					if ((validModes!=null) && (validModes.remove(removedModeName))) {
						// If the suppressed mode belongs to the filter modes list
						// We have to remove it if it is no more a mode of the one of the valid accounts of the filter.
						boolean needRemoving = true;
						for (int i = 0; i < FilteredData.this.data.getAccountsNumber(); i++) {
							Account account = FilteredData.this.getGlobalData().getAccount(i);
							if (filter.isOk(account) && (account.indexOf(evt.getMode())>=0)) {
								needRemoving = false;
								break;
							}
						}
						if (needRemoving) {
							filter.setValidModes(validModes.isEmpty()?null:validModes);
							fireEvent (event);
						}
					}
				} else if ((event instanceof NeedToBeSavedChangedEvent) || (event instanceof IsLockedChangedEvent) || (event instanceof IsArchivedChangedEvent)) {
					fireEvent(event);
				} else {
					getLogger().debug("Be aware {} is not propagated by the fileredData", event);  //$NON-NLS-1$
				}
			}
		});
		this.balanceData = new BalanceData();
		this.filter();
	}
	
	private Logger getLogger() {
		if (this.logger==null) {
			this.logger = LoggerFactory.getLogger(getClass());
		}
		return this.logger;
	}

	/** Gets the balance data.
	 * <br>The balance data ignores all filters except the one on the accounts.
	 * @return the balance data.
	 */
	public BalanceData getBalanceData() {
		return this.balanceData;
	}
	
	private boolean eventImplySorting (DataEvent event) {
		boolean accountRenamed = (event instanceof AccountPropertyChangedEvent) &&
				((AccountPropertyChangedEvent)event).getProperty().equals(AccountPropertyChangedEvent.NAME) &&
				filter.isOk(((AccountPropertyChangedEvent)event).getAccount());
		boolean categoryRenamed = (event instanceof CategoryPropertyChangedEvent) &&
		filter.isOk(((CategoryPropertyChangedEvent)event).getCategory());
		boolean modeRenamed = (event instanceof ModePropertyChangedEvent) &&
		((((ModePropertyChangedEvent)event).getChanges() & ModePropertyChangedEvent.NAME)!=0) &&
		filter.isOk(((ModePropertyChangedEvent)event).getNewMode());
		return accountRenamed || categoryRenamed || modeRenamed;
	}
	
	/** Gets the filter used in this filtered data.
	 * @return a Filter
	 */
	public Filter getFilter() {
		return this.filter;
	}
	
	private void filter() {
		double initialBalance = 0;
		for (int i = 0; i < this.getGlobalData().getAccountsNumber(); i++) {
			Account account = this.getGlobalData().getAccount(i);
			if (filter.isOk(account)) {
				initialBalance += account.getInitialBalance();
			}
		}
		balanceData.enableEvents(false);
		balanceData.clear(initialBalance);
		this.transactions = new ArrayList<Transaction>();
		Collection<Transaction> balanceTransactions = new ArrayList<Transaction>(data.getTransactionsNumber());
		double addedAmount = 0.0;
		for (int i = 0; i < data.getTransactionsNumber(); i++) {
			Transaction transaction = data.getTransaction(i);
			if (filter.isOk(transaction.getAccount())) {
				Date valueDate = transaction.getValueDate();
				if (NullUtils.compareTo(valueDate, filter.getValueDateFrom(),true)<0) {
					addedAmount += transaction.getAmount();
				} else {
					// Here we have a hard choice to make: 
					// Ignore the transactions with a value date after the upper limit of the filter or not.
					// In the first case, users may be surprised that transactions excluded by the filter are taken into account
					// In the second one, the balance history after the filter upper limit is WRONG, and its probably dangerous !!!
					// Especially, if the end date is before today, the current balance will be false and be displayed false in the transactions panel. 
					// Uncomment the test to implement the second one.
					/*if (NullUtils.compareTo(valueDate, getValueDateTo(), false)<=0)*/ balanceTransactions.add(transaction);
					if (filter.isOk(transaction)) {
						int index = -Collections.binarySearch(transactions, transaction, comparator)-1;
						transactions.add(index, transaction);
					}
				}
			}
		}
		balanceData.updateBalance(addedAmount, true);
		balanceData.updateBalance(balanceTransactions.toArray(new Transaction[balanceTransactions.size()]), true);
		balanceData.enableEvents(true);
		fireEvent(new EverythingChangedEvent(this));
	}

	/** Gets the number of transactions that match the filter. 
	 * @return number of transactions that match the filter
	 */
	public int getTransactionsNumber() {
		return this.transactions.size();
	}

	/** Gets a transaction that matches the filter.
	 * @param index the index of the transaction (between 0 and getTransactionsNumber())
	 * @return the transaction.
	 */
	public Transaction getTransaction(int index) {
		return this.transactions.get(index);
	}
	
	/** Gets a an unmodifiable list of the transactions that match the filter.
	 * @return an unmodifiable list of transactions
	 */
	public List<Transaction> getTransactions() {
		return Collections.unmodifiableList(this.transactions);
	}
	
	/** Find the index of a transaction that matches the filter.
	 * @param transaction the transaction to find
	 * @return a negative integer if the transaction doesn't match ths filter,
	 * the index of the transaction if the transaction matches. 
	 */
	public int indexOf(Transaction transaction) {
		return Collections.binarySearch(transactions, transaction, comparator);
	}

	/** Gets the unfiltered data on which is based this FilteredData.
	 * @return the GlobalData instance
	 */
	public GlobalData getGlobalData() {
		return this.data;
	}
}