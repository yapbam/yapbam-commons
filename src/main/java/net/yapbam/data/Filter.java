package net.yapbam.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.yapbam.util.NullUtils;
import net.yapbam.util.TextMatcher;

/** A data filter.
 */
public class Filter extends Observable {
	private static boolean DEBUG = false;
	public static final int CHECKED=1;
	public static final int NOT_CHECKED=2;
	public static final int EXPENSES=4;
	public static final int RECEIPTS=8;
	public static final int ALL = CHECKED+NOT_CHECKED+EXPENSES+RECEIPTS;

	private String name;
	private int filter;
	private Set<Account> validAccounts;
	private Set<String> validModes;
	private Set<Category> validCategories;
	private Date dateFrom;
	private Date dateTo;
	private Date valueDateFrom;
	private Date valueDateTo;
	private double minAmount;
	private double maxAmount;
	private TextMatcher descriptionMatcher;
	private TextMatcher commentMatcher;
	private TextMatcher numberMatcher;
	private TextMatcher statementMatcher;
	
	private boolean suspended;
	private static final Logger LOGGER = LoggerFactory.getLogger(Filter.class);

	/** Constructor. */
	public Filter() {
		init();
		this.suspended = false;
	}

	public boolean isOk(int property) {
		if (DEBUG) {
			LOGGER.trace("---------- isOK({}) ----------",Integer.toBinaryString(property)); //$NON-NLS-1$
			LOGGER.trace("filter: {}",Integer.toBinaryString(this.filter)); //$NON-NLS-1$
			LOGGER.trace("result: {}",Integer.toBinaryString(property & this.filter)); //$NON-NLS-1$
		}
		return (property & this.filter) != 0;
	}
	
	@Override
	protected void setChanged() {
		super.setChanged();
		if (!suspended) {
			this.notifyObservers();
		}
	}
	
	/** Sets the suspended state of the filter.
	 * When the filter is suspended, the filter changes don't automatically call the filter's observers.
	 * This refresh (and the event) is delayed until this method is called with false argument.
	 * Note that if this method is called with false argument, but no filter change occurs, nothing happens.
	 * @param suspended true to suspend observers notifications, false to restore it.
	 */
	public void setSuspended(boolean suspended) {
		this.suspended = suspended;
		if (!this.suspended && this.hasChanged()) {
			this.notifyObservers();
		}
	}

	/** Gets the valid accounts for this filter.
	 * <br>Note: There's no side effect between this instance and the returned array.
	 * @return the valid accounts (null means all accounts are ok).
	 */
	public List<Account> getValidAccounts() {
		if (validAccounts==null) {
			return null;
		}
		ArrayList<Account> result = new ArrayList<Account>(validAccounts.size());
		for (Account account:validAccounts) {
			result.add(account);
		}
		return result;
	}

	public boolean isOk(Account account) {
		return (validAccounts==null) || (validAccounts.contains(account));
	}	

	/** Sets the valid accounts for this filter.
	 * <br>Note: There's no side effect between this instance and the argument array.
	 * @param accounts the accounts that are allowed (null or the complete list of accounts to allow all accounts).
	 */
	public void setValidAccounts(List<Account> accounts) {
		if (!testEquals(accounts, this.validAccounts)) {
			if (accounts==null) {
				validAccounts = null;
			} else {
				validAccounts = new HashSet<Account>(accounts.size());
				this.validAccounts.addAll(accounts);
			}
			setChanged();
		}
	}
	
	private static <T> boolean testEquals(Collection<T> c1, Collection<T> c2) {
		if ((c1==null) && (c2==null)) {
			return true; 
		}
		if ((c1==null) || (c2==null)) {
			return false;
		}
		// Both are not null if we arrive here
		if (c1.size()!=c2.size()) {
			return false;
		}
		for (T element:c1) {
			if (!c2.contains(element)) {
				return false;
			}
		}
		return true;
	}

	/** Gets the valid modes names for this filter.
	 * <br>There's no side effect between this instance and the returned array.
	 * @return the valid modes names (null means, all modes are ok). Mode.Undefined is identified by an empty String in the returned list 
	 * @see #setValidModes(List)
	 */
	public List<String> getValidModes() {
		if (validModes==null) {
			return null;
		}
		ArrayList<String> result = new ArrayList<String>(validModes.size());
		for (String name:validModes) {
			result.add(name);
		}
		return result;
	}
	
	/** Tests whether a mode is valid or not.
	 * @param mode The mode to test
	 * @return true if the mode is valid
	 * @see #setValidModes(List)
	 */
	public boolean isOk(Mode mode) {
		if (validModes==null) {
			return true;
		}
		String name = mode.equals(Mode.UNDEFINED)?"":mode.getName(); //$NON-NLS-1$
		return validModes.contains(name);
	}

	/** Sets the valid modes names for this filter.
	 * <br>There's no side effect between this instance and the argument of this method.
	 * @param validModes null to enable all modes, or a list of valid mode's names.
	 * Be aware of the Mode.UNDEFINED, as its name depends on the localization, it should be identified not by its name but by an empty string.
	 * @see #getValidModes()
	 */
	public void setValidModes(List<String> validModes) {
		if (!testEquals(validModes, this.validModes)) {
			if (validModes==null) {
				this.validModes = null;
			} else {
				this.validModes = new HashSet<String>(validModes.size());
				this.validModes.addAll(validModes);
			}
			setChanged();
		}
	}

	/** Returns the valid categories for this filter.
	 * There's no side effect between this instance and the returned array.
	 * @return the valid categories (null means, all categories are ok).
	 */
	public List<Category> getValidCategories() {
		if (validCategories==null) {
			return null;
		}
		ArrayList<Category> result = new ArrayList<Category>(validCategories.size());
		for (Category account:validCategories) {
			result.add(account);
		}
		return result;
	}

	public boolean isOk(Category category) {
		return (validCategories==null) || (validCategories.contains(category));
	}

	/** Set the valid categories for this filter.
	 * There's no side effect between this instance and the argument array.
	 * @param validCategories the categories that are allowed (null or the complete list of categories to allow all categories).
	 */
	public void setValidCategories(List<Category> validCategories) {
		if (!testEquals(validCategories, this.validCategories)) {
			if (validCategories==null) {
				this.validCategories = null;
			} else {
				this.validCategories = new HashSet<Category>(validCategories.size());
				this.validCategories.addAll(validCategories);
			}
			setChanged();
		}
	}

	/** Gets the transaction date before which all transactions are rejected.
	 * @return a transaction date or null if there's no time limit. 
	 */
	public Date getDateFrom() {
		return dateFrom;
	}

	/** Gets the transaction date after which all transactions are rejected.
	 * @return a transaction date or null if there's no time limit. 
	 */
	public Date getDateTo() {
		return dateTo;
	}

	/** Sets the filter on transaction date.
	 * @param from transactions strictly before <i>from</i> are rejected. A null date means "beginning of times".
	 * @param to transactions strictly after <i>to</i> are rejected. A null date means "end of times". 
	 */
	public void setDateFilter(Date from, Date to) {
		if (!NullUtils.areEquals(from, this.dateFrom) || !NullUtils.areEquals(to, this.dateTo)) {
			this.dateFrom = from;
			this.dateTo = to;
			this.setChanged();
		}
	}
	
	/** Gets the transaction value date before which all transactions are rejected.
	 * @return a transaction value date or null if there's no time limit. 
	 */
	public Date getValueDateFrom() {
		return valueDateFrom;
	}

	/** Gets the transaction value date after which all transactions are rejected.
	 * @return a transaction value date or null if there's no time limit. 
	 */
	public Date getValueDateTo() {
		return valueDateTo;
	}

	/** Sets the filter on transaction value date.
	 * @param from transactions with value date strictly before <i>from</i> are rejected. A null date means "beginning of times".
	 * @param to transactions with value date strictly after <i>to</i> are rejected. A null date means "end of times". 
	 */
	public void setValueDateFilter(Date from, Date to) {
		if (!NullUtils.areEquals(from, this.valueDateFrom) || !NullUtils.areEquals(to, this.valueDateTo)) {
			this.valueDateFrom = from;
			this.valueDateTo = to;
			this.setChanged();
		}
	}

	/** Gets the transaction minimum amount.
	 * <br>Please note that the minimum amount is always a positive or null number. 
	 * @return the minimum amount (0.0 if there's no low limit).
	 */
	public double getMinAmount() {
		return minAmount;
	}

	/** Gets the transaction maximum amount.
	 * @return the maximum amount (Double.POSITIVE_INFINITY if there's no high limit).
	 */
	public double getMaxAmount() {
		return maxAmount;
	}

	/** Sets the transaction minimum and maximum amounts.
	 * @param property An integer that codes if expenses or receipts, or both are ok.
	 * <br>Note that only EXPENSES, RECEIPTS and EXPENSES+RECEIPTS constants are valid arguments.
	 * Any other integer codes (for instance CHECKED) are ignored.
	 * @param minAmount The minimum amount (a positive or null double).
	 * @param maxAmount The maximum amount (Double.POSITIVE_INFINITY to set no high limit).
	 * @throws IllegalArgumentException if minAmount &gt; maxAmount or if minimum amount is negative
	 */
	public void setAmountFilter(int property, double minAmount, double maxAmount) {
		if ((minAmount>maxAmount) || (minAmount<0)) {
			throw new IllegalArgumentException();
		}
		int mask = Filter.EXPENSES+Filter.RECEIPTS;
		if ((GlobalData.AMOUNT_COMPARATOR.compare(minAmount, this.minAmount) != 0) ||
				(GlobalData.AMOUNT_COMPARATOR.compare(maxAmount, this.maxAmount) != 0) ||
				((property & mask)!=(filter & mask))) {
			this.minAmount = minAmount;
			this.maxAmount = maxAmount;
			filter = (filter & ~mask) | (property & mask);
			if (DEBUG) {
				LOGGER.trace("-> filter: {}",filter); //$NON-NLS-1$
			}
			this.setChanged();
		}
	}
	
	/** Tests whether an amount is ok or not.
	 * @param amount The amount to test
	 * @return true if the amount is ok.
	 */
	public boolean isAmountOk(double amount) {
		// We use the currency comparator to implement amount filtering because double are very tricky to compare.
		if ((GlobalData.AMOUNT_COMPARATOR.compare(amount, 0.0)<0) && (!isOk(EXPENSES))) {
			return false;
		}
		if ((GlobalData.AMOUNT_COMPARATOR.compare(amount, 0.0)>0) && (!isOk(RECEIPTS))) {
			return false;
		}
		amount = Math.abs(amount);
		if (GlobalData.AMOUNT_COMPARATOR.compare(amount, getMinAmount())<0) {
			return false;
		}
		return GlobalData.AMOUNT_COMPARATOR.compare(amount, getMaxAmount())<=0;
	}

	/** Gets the description filter.
	 * @return a TextMatcher or null if there is no description filter
	 */
	public TextMatcher getDescriptionMatcher() {
		return descriptionMatcher;
	}
	
	/** Gets the validity of a string according to the current description filter. 
	 * @param description The string to test
	 * @return true if the description is ok with the filter.
	 */
	public boolean isDescriptionOk(String description) {
		return descriptionMatcher==null?true:descriptionMatcher.matches(description);
	}

	/** Sets the description filter.
	 * @param matcher a TextMatcher instance or null to apply no filter on description
	 */
	public void setDescriptionMatcher(TextMatcher matcher) {
		if (!NullUtils.areEquals(matcher, this.descriptionMatcher)) {
			this.descriptionMatcher = matcher;
			this.setChanged();
		}
	}
	
	public TextMatcher getCommentMatcher() {
		return this.commentMatcher;
	}

	/** Gets the validity of a string according to the current comment filter. 
	 * @param comment The string to test
	 * @return true if the comment is ok with the filter.
	 */
	public boolean isCommentOk(String comment) {
		return commentMatcher==null?true:commentMatcher.matches(comment);
	}

	public void setCommentMatcher(TextMatcher textMatcher) {
		if (!NullUtils.areEquals(textMatcher, this.commentMatcher)) {
			this.commentMatcher = textMatcher;
			this.setChanged();
		}
	}

	public TextMatcher getNumberMatcher() {
		return numberMatcher;
	}

	/** Gets the validity of a string according to the current number filter. 
	 * @param number The string to test
	 * @return true if the number is ok with the filter.
	 */
	public boolean isNumberOk(String number) {
		return numberMatcher==null?true:numberMatcher.matches(number);
	}

	public void setNumberMatcher(TextMatcher numberMatcher) {
		if (!NullUtils.areEquals(numberMatcher, this.numberMatcher)) {
			this.numberMatcher = numberMatcher;
			this.setChanged();
		}
	}

	public TextMatcher getStatementMatcher() {
		return statementMatcher;
	}

	public void setStatementFilter (int property, TextMatcher statementFilter) {
		if (((property & Filter.CHECKED) == 0) && (statementFilter!=null)) {
			throw new IllegalArgumentException();
		}
		int mask = Filter.CHECKED+Filter.NOT_CHECKED;
		if (!NullUtils.areEquals(statementFilter, this.statementMatcher) || ((property & mask)!=(filter & mask))) {
			this.statementMatcher = statementFilter;
			filter = (filter & ~mask) | (property & mask);
			if (DEBUG) {
				LOGGER.trace("-> filter: {}",filter); //$NON-NLS-1$
			}
			this.setChanged();
		}
	}
		
	public boolean isStatementOk(String statement) {
		if (statement==null) {
			// Not checked transaction
			return isOk(Filter.NOT_CHECKED);
		} else {
			// Checked transaction
			if (!isOk(Filter.CHECKED)) {
				return false;
			}
			if (statementMatcher==null) {
				return true;
			}
			return statementMatcher.matches(statement);
		}
	}

	public void clear() {
		if (isActive()) {
			this.setSuspended(true);
			init();
			this.setSuspended(false);
		}
	}
	
	private void init() {
		this.name = null;
		this.setDateFilter(null, null);
		this.setValueDateFilter(null, null);
		this.setValidCategories(null);
		this.setValidModes(null);
		this.setAmountFilter(EXPENSES+RECEIPTS, 0.0, Double.POSITIVE_INFINITY);
		this.setDescriptionMatcher(null);
		this.setCommentMatcher(null);
		this.setNumberMatcher(null);
		this.setStatementFilter(CHECKED+NOT_CHECKED, null);
		this.setValidAccounts(null);
	}
	
	/** Tests whether the filter filters something or not.
	 * @return false if no filter is set. Returns true if a filter is set
	 * even if it doesn't filter anything.
	 */
	public boolean isActive() {
		return (filter!=ALL) || (getDateFrom()!=null) || (getDateTo() != null) || (getValueDateFrom()!=null) || (getValueDateTo() != null) ||
			(getValidCategories() !=null) || (getValidModes() != null) || (getValidAccounts()!=null) ||
			(getMinAmount()!=0.0) || (getMaxAmount()!=Double.POSITIVE_INFINITY) ||
			(getDescriptionMatcher()!=null) || (getCommentMatcher()!=null) || (getNumberMatcher()!=null) || (getStatementMatcher()!=null);
	}
	
	/** Gets a transaction's validity.
	 * Note about subtransactions : A transaction is also valid if one of its subtransactions,
	 *  considered as transaction (completed with transactions's date, statement, etc ...), is valid. 
	 * @param transaction The transaction to test.
	 * @return true if the transaction is valid.
	 */
	public boolean isOk(Transaction transaction) {
		if (!isOk(transaction.getAccount()) || !isOk(transaction.getMode()) ||
				!isStatementOk(transaction.getStatement()) || !isNumberOk(transaction.getNumber()) ||
				!isCommentOk(transaction.getComment())) {
			return false;
		}
		if ((getDateFrom()!=null) && (transaction.getDate().compareTo(getDateFrom())<0)) {
			return false;
		}
		if ((getDateTo()!=null) && (transaction.getDate().compareTo(getDateTo())>0)) {
			return false;
		}
		if ((getValueDateFrom()!=null) && (transaction.getValueDate().compareTo(getValueDateFrom())<0)) {
			return false;
		}
		if ((getValueDateTo()!=null) && (transaction.getValueDate().compareTo(getValueDateTo())>0)) {
			return false;
		}
		if (isOk(transaction.getCategory()) && isAmountOk(transaction.getAmount()) &&
				isDescriptionOk(transaction.getDescription())) {
			return true;
		}
		// The transaction may also be valid if one of its subtransactions is valid 
		for (int i = 0; i < transaction.getSubTransactionSize(); i++) {
			if (isOk(transaction.getSubTransaction(i))) {
				return true;
			}
		}
		// The transaction may also be valid if its subtransactions complement is valid 
		return isComplementOk(transaction);
	}

	/** Gets a periodical transaction's validity.
	 * Note about subtransactions : A transaction is also valid if one of its subtransactions,
	 *  considered as transaction (completed with transactions's date, statement, etc ...), is valid. 
	 * @param transaction The periodical transaction to test.
	 * @return true if the transaction is valid.
	 */
	public boolean isOk(PeriodicalTransaction transaction) {
		if (!isOk(transaction.getAccount()) || !isOk(transaction.getMode()) || !isCommentOk(transaction.getComment())) {
			return false;
		}
		if (isOk(transaction.getCategory()) && isAmountOk(transaction.getAmount()) &&
				isDescriptionOk(transaction.getDescription())) {
			return true;
		}
		// The transaction may also be valid if one of its subtransactions is valid 
		for (int i = 0; i < transaction.getSubTransactionSize(); i++) {
			if (isOk(transaction.getSubTransaction(i))) {
				return true;
			}
		}
		// The transaction may also be valid if its subtransactions complement is valid 
		return isComplementOk(transaction);
	}

	/** Gets a subtransaction validity.
	 * @param subtransaction the subtransaction to test
	 * @return true if the subtransaction is valid according to this filter.
	 * Be aware that no specific fields of the transaction are tested, so the subtransaction may be valid
	 * even if its transaction is not (for instance if its payment mode is not ok). So, usually, you'll have
	 * to also test the transaction.
	 * @see #isOk(Transaction)
	 */
	public boolean isOk(SubTransaction subtransaction) {
		return isOk(subtransaction.getCategory()) && isAmountOk(subtransaction.getAmount()) &&
				isDescriptionOk(subtransaction.getDescription());
	}
	
	/** Gets a transaction complement validity.
	 * @param transaction the transaction to test
	 * @return true if the transaction complement is valid according to this filter.
	 * Be aware that the complement is considered as a subtransaction. So the behavior is the same
	 * than in isOk(Subtransaction) method. No specific fields of the transaction are tested, so the complement
	 * may be valid even if the whole transaction is not (for instance if its payment mode is not ok).
	 * So, usually, you'll have to also test the transaction.
	 * @see #isOk(Transaction)
	 */
	public boolean isComplementOk(AbstractTransaction transaction) {
		double amount = transaction.getComplement();
		if ((transaction.getSubTransactionSize()!=0) && (GlobalData.AMOUNT_COMPARATOR.compare(amount,0.0)==0)) {
			return false;
		}
		return isOk(transaction.getCategory()) && isAmountOk(amount) && isDescriptionOk(transaction.getDescription()) && isCommentOk(transaction.getComment());
	}

	/** Copies a filter in this filter.
	 * <br>If filter is not equal to this, observers are notified of the changes.
	 * @param filter The filter to copy
	 */
	public void copy(Filter filter) {
		this.setSuspended(true);
		this.setAmountFilter(filter.filter, filter.minAmount, filter.maxAmount);
		this.setStatementFilter(filter.filter, filter.statementMatcher);
		this.setValidAccounts(filter.getValidAccounts());
		this.setValidModes(filter.getValidModes());
		this.setValidCategories(filter.getValidCategories());
		this.setDateFilter(filter.getDateFrom(), filter.getDateTo());
		this.setValueDateFilter(filter.getValueDateFrom(), filter.getValueDateTo());
		this.setDescriptionMatcher(filter.getDescriptionMatcher());
		this.setCommentMatcher(filter.getCommentMatcher());
		this.setNumberMatcher(filter.getNumberMatcher());
		this.setName(filter.getName());
		this.setSuspended(false);
	}
	
	/** Gets this filter's name.
	 * @return a string or null if the filter has no name.
	 */
	public String getName(){
		return this.name;
	}

	/** Sets this filter's name.
	 * @param name The new name, null to clear filter's name
	 */
	public void setName(String name){
		if (!NullUtils.areEquals(this.name, name)) {
			this.name = name;
			setChanged();
		}
	}
}