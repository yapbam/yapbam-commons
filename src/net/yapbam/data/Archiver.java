package net.yapbam.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.yapbam.util.NullUtils;

/** Transaction archiver.
 * <br>This class provides a method to transfer transactions from a GlobalData instance to an archive and the opposite.
 */
public abstract class Archiver {
	/** Constructor.
	 */
	protected Archiver() {
		super();
	}
	
	/** Moves some transactions to or from an archive.
	 * <br>Missing account, modes and categories are created in the destination GlobalData.
	 * <br>Modified modes are modified in the GlobalData destination if argument {@code toArchive} is true.
	 * <br>The initial balances of accounts in {@code data} are updated.
	 * @param data The GlobalData "standard" instance.
	 * @param archive The archive.
	 * @param transactions The transactions to move.
	 * @param toArchive true to move transactions to archive. False to move them back to "standard" instance.
	 * @return true if the move was made successfully
	 * @throws IllegalArgumentException if archive.isArchive() is false or data.isArchive() is true.
	 * @see GlobalData#isArchive()
	 */
	public final boolean move(GlobalData data, GlobalData archive, Transaction[] transactions, boolean toArchive) {
		// The move has to phases:
		// - First we copy the moved transactions to the destination with {@link #copy(GlobalData, GlobalData, Transaction[], boolean)} method.
		// - Second, we remove the transactions from the source instance with {@link #remove(GlobalData, GlobalData, Transaction[], boolean)}.
		// This two step process allows us to save the destination before destroying transactions from the source.
		// Of course if the archive save fails, we do not remove transactions from their origin.
		copy(data, archive, transactions, toArchive);
		if (save(toArchive?archive:data)) {
			remove(data, archive, transactions, toArchive);
			if (!toArchive) {
				return save(archive);
			}
			return true;
		}
		return false;
	}
	
	/** Saves the data to disk.
	 * @param data The data to save.
	 * @return true if the data was successfully saved.
	 */
	protected abstract boolean save(GlobalData data);
	
	/** Copies some transactions to or from an archive.
	 * <br>Missing account, modes and categories are created in the destination GlobalData.
	 * <br>Modified modes are modified in the GlobalData destination if argument {@code toArchive} is true.
	 * <br>If {@code toArchive} is false, the destination initial balance are updated.
	 * @param data The GlobalData "standard" instance.
	 * @param data The archive.
	 * @param transactions The transactions to move.
	 * @param toArchive true to move transactions to archive. False to move them back to "standard" instance.
	 * @throws IllegalArgumentException if archive.isArchive() is false or data.isArchive() is true.
	 * @see GlobalData#isArchive()
	 */
	private static void copy(GlobalData data, GlobalData archive, Transaction[] transactions, boolean toArchive) {
		if (data.isArchive() || !archive.isArchive()) {
			throw new IllegalArgumentException();
		}
		GlobalData target = toArchive?archive:data;
		target.setEventsEnabled(false);
		copy (target,transactions, toArchive);
		if (!toArchive) {
			// If we recover transactions from archive, we have to update the initial balances of the destination data
			Map<Account, Double> accountToAmount = getAmounts(transactions);
			target.setEventsEnabled(false);
			for (Entry<Account, Double> entry : accountToAmount.entrySet()) {
				// Warning entry points to src data, not target one 
				Account account = target.getAccount(entry.getKey().getName());
				double initialBalance = account.getInitialBalance() - entry.getValue();
				target.setInitialBalance(account, initialBalance);
			}
		}
		target.setEventsEnabled(true);
	}
	
	
	/** Removes transactions from data after they have been copied.
	 * @param data The GlobalData "standard" instance.
	 * @param data The archive.
	 * @param transactions the transactions to remove
	 * @param toArchive true if transactions were moved to archive. False if the were moved them back to "standard" instance.
	 */
	private static void remove(GlobalData data, GlobalData archive, Transaction[] transactions, boolean toArchive) {
		GlobalData source = toArchive?data:archive;
		source.setEventsEnabled(false);
		source.remove(transactions);
		if (toArchive) {
			// Compute the initial balance of accounts after transactions deletion
			Map<Account, Double> accountToAmount = getAmounts(transactions);
			for (Entry<Account, Double> entry : accountToAmount.entrySet()) {
				Account account = entry.getKey();
				Double initialBalance = entry.getValue() + account.getInitialBalance();
				source.setInitialBalance(account, initialBalance);
			}
		} else {
			// Remove empty accounts from archive
			for (int i=archive.getAccountsNumber()-1;i>=0;i--) {
				Account account = archive.getAccount(i);
				if (account.getTransactionsNumber()==0) {
					archive.remove(account);
				}
			}
		}
		source.setEventsEnabled(true);
	}

	private static void copy(GlobalData data, Transaction[] transactions, boolean toArchive) {
		Transaction[] copies = new Transaction[transactions.length];
		for (int i = 0; i < copies.length; i++) {
			copies[i] = getTransaction(data, transactions[i], toArchive);
		}
		data.add(copies);
	}

	private static Transaction getTransaction(GlobalData data, Transaction transaction, boolean toArchive) {
		Account account = getAccount(data, transaction.getAccount());
		Mode mode = getMode(data, account, transaction.getMode(), toArchive);
		Category category = getCategory(data, transaction.getCategory());
		return new Transaction(transaction.getDate(), transaction.getNumber(),
				transaction.getDescription(), transaction.getComment(), transaction.getAmount(), account,
				mode, category, transaction.getValueDate(), transaction.getStatement(),
				getSubTransactions(data, transaction));
	}
	
	private static Account getAccount(GlobalData data, Account account) {
		Account currentAccount = data.getAccount(account.getName());
		if (currentAccount==null) {
			currentAccount = new Account(account.getName(), account.getInitialBalance());
			data.add(currentAccount);
		}
		return currentAccount;
	}
	
	private static Mode getMode(GlobalData data, Account account, Mode mode, boolean toArchive) {
		Mode currentMode = account.getMode(mode.getName());
		if (currentMode==null) {
			currentMode = new Mode(mode.getName(), mode.getReceiptVdc(), mode.getExpenseVdc(), mode.isUseCheckBook());
			account.add(currentMode);
		} else if (toArchive && !NullUtils.areEquals(mode.getReceiptVdc(),currentMode.getReceiptVdc()) ||
				!NullUtils.areEquals(mode.getExpenseVdc(), currentMode.getExpenseVdc())
				|| (mode.isUseCheckBook() != currentMode.isUseCheckBook())) {
			Mode oldMode = currentMode;
			currentMode = new Mode(mode.getName(), mode.getReceiptVdc(), mode.getExpenseVdc(), mode.isUseCheckBook());
			data.setMode(account, oldMode, currentMode);
		}
		return currentMode;
	}
	
	private static Category getCategory(GlobalData data, Category category) {
		Category currentCategory = data.getCategory(category.getName());
		if (currentCategory==null) {
			currentCategory = new Category(category.getName());
			data.add(currentCategory);
		}
		return currentCategory;
	}
	
	private static List<SubTransaction> getSubTransactions(GlobalData data, Transaction transaction) {
		List<SubTransaction> result = new ArrayList<SubTransaction>(transaction.getSubTransactionSize());
		for (int i = 0; i < transaction.getSubTransactionSize(); i++) {
			SubTransaction sb = transaction.getSubTransaction(i);
			result.add(new SubTransaction(sb.getAmount(), sb.getDescription(), getCategory(data, sb.getCategory())));
		}
		return result;
	}

	private static Map<Account, Double> getAmounts(Transaction[] transactions) {
		Map<Account, Double> accountToAmount = new HashMap<Account, Double>();
		for (Transaction transaction : transactions) {
			Account account = transaction.getAccount();
			Double amount = accountToAmount.get(account);
			if (amount==null) {
				amount = 0.0;
			}
			amount = amount + transaction.getAmount();
			accountToAmount.put(account, amount);
		}
		return accountToAmount;
	}
}
