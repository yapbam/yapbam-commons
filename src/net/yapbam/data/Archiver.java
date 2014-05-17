package net.yapbam.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.yapbam.util.NullUtils;

/** Transaction archiver.
 * <br>This class provides methods to transfer transactions from a GlobalData instance to another.
 * <br>The move has to phases:<ol>
 * <li>First we add the moved transactions to the archive with {@link #archive(GlobalData, Transaction[])} method.</li>
 * <li>Second, we remove the transactions from the Global data instance with {@link #remove(GlobalData, Transaction[])}.</li></ol>
 * This two step process allows the developer to save the archive before destroying transactions from the GlobalData instance.
 * <br>Of course if the archive save fails, it should be a good idea to not remove transaction from their origin.
 */
public final class Archiver {
	private Archiver() {
		super();
	}

	/** Moves some transactions to an archive.
	 * <br>Missing account, modes and categories are created in the archive.
	 * <br>Modified modes are modified in the archive.
	 * @param archiveData The archive data.
	 * @param transactions The transactions to move to the archive.
	 * @throws IllegalArgumentException if archiveData.isArchive() is false
	 * @see GlobalData#isArchive()
	 */
	public static void archive(GlobalData archiveData, Transaction[] transactions) {
		if (!archiveData.isArchive()) {
			throw new IllegalArgumentException();
		}
		archiveData.setEventsEnabled(false);
		Transaction[] archiveTransactions = new Transaction[transactions.length];
		for (int i = 0; i < archiveTransactions.length; i++) {
			archiveTransactions[i] = getArchiveTransaction(archiveData, transactions[i]);
		}
		archiveData.add(archiveTransactions);
		archiveData.setEventsEnabled(true);
	}

	private static Transaction getArchiveTransaction(GlobalData archiveData, Transaction transaction) {
		Account archiveAccount = getArchiveAccount(archiveData, transaction.getAccount());
		Mode archiveMode = getArchiveMode(archiveData, archiveAccount, transaction.getMode());
		Category archiveCategory = getArchiveCategory(archiveData, transaction.getCategory());
		return new Transaction(transaction.getDate(), transaction.getNumber(),
				transaction.getDescription(), transaction.getComment(), transaction.getAmount(), archiveAccount,
				archiveMode, archiveCategory, transaction.getValueDate(), transaction.getStatement(),
				getArchiveSubTransactions(archiveData, transaction));
	}
	
	private static Account getArchiveAccount(GlobalData archiveData, Account account) {
		Account archiveAccount = archiveData.getAccount(account.getName());
		if (archiveAccount==null) {
			archiveAccount = new Account(account.getName(), account.getInitialBalance());
			archiveData.add(archiveAccount);
		}
		return archiveAccount;
	}
	
	private static Mode getArchiveMode(GlobalData archiveData, Account archiveAccount, Mode mode) {
		Mode archiveMode = archiveAccount.getMode(mode.getName());
		if (archiveMode==null) {
			archiveMode = new Mode(mode.getName(), mode.getReceiptVdc(), mode.getExpenseVdc(), mode.isUseCheckBook());
			archiveAccount.add(archiveMode);
		} else {
			if (!NullUtils.areEquals(mode.getReceiptVdc(),archiveMode.getReceiptVdc()) ||
					!NullUtils.areEquals(mode.getExpenseVdc(),archiveMode.getExpenseVdc()) ||
					(mode.isUseCheckBook()!=archiveMode.isUseCheckBook())) {
				Mode oldMode = archiveMode;
				archiveMode = new Mode(mode.getName(), mode.getReceiptVdc(), mode.getExpenseVdc(), mode.isUseCheckBook());
				archiveData.setMode(archiveAccount, oldMode, archiveMode);
			}
		}
		return archiveMode;
	}
	
	private static Category getArchiveCategory(GlobalData archiveData, Category category) {
		Category archiveCategory = archiveData.getCategory(category.getName());
		if (archiveCategory==null) {
			archiveCategory = new Category(category.getName());
			archiveData.add(archiveCategory);
		}
		return archiveCategory;
	}
	
	private static List<SubTransaction> getArchiveSubTransactions(GlobalData archiveData, Transaction transaction) {
		List<SubTransaction> result = new ArrayList<SubTransaction>(transaction.getSubTransactionSize());
		for (int i = 0; i < transaction.getSubTransactionSize(); i++) {
			SubTransaction sb = transaction.getSubTransaction(i);
			result.add(new SubTransaction(sb.getAmount(), sb.getDescription(), getArchiveCategory(archiveData, sb.getCategory())));
		}
		return result;
	}

	/** Removes archived transactions from data.
	 * @param data The data where to remove the transactions
	 * @param transactions the transactions to remove
	 */
	public static void remove(GlobalData data, Transaction[] transactions) {
		// Compute the initial balance of accounts after transactions deletion
		Map<Account, Double> accountToInitialBalance = new HashMap<Account, Double>();
		for (Transaction transaction : transactions) {
			Account account = transaction.getAccount();
			Double initialBalance = accountToInitialBalance.get(account);
			if (initialBalance==null) {
				initialBalance = account.getInitialBalance();
			}
			initialBalance = initialBalance + transaction.getAmount();
			accountToInitialBalance.put(account, initialBalance);
		}
		data.setEventsEnabled(false);
		for (Entry<Account, Double> entry : accountToInitialBalance.entrySet()) {
			Account account = entry.getKey();
			Double initialBalance = entry.getValue();
			data.setInitialBalance(account, initialBalance);
		}
		data.remove(transactions);
		data.setEventsEnabled(true);
	}
}
