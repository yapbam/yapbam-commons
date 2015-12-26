package net.yapbam.data;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import net.yapbam.date.helpers.DateStepper;

import org.junit.Test;

public class ArchiveTest {
	private static final Archiver ARCHIVER = new Archiver(){
		@Override
		protected boolean save(GlobalData data) {
			return true;
		}};
	
	private static final String CATEGORY_1 = "category 1";
	private static final String CATEGORY_2 = "category 2";
	private static final String CATEGORY_3 = "category 3";
	private static final String ACCOUNT_2_NAME = "account 2";
	private static final String ACCOUNT_1_NAME = "account 1";
	private static final String MODE_1 = "mode 1";
	private static final String MODE_2 = "mode 2";

	@Test
	public void test() {
		GlobalData archiveData = buildArchiveData();
		GlobalData data = buildData();
		
		Account archiveAccount1 = archiveData.getAccount(ACCOUNT_1_NAME);
		Account account = archiveAccount1;
		double account1Diff = account.getBalanceData().getFinalBalance()- data.getAccount(ACCOUNT_1_NAME).getInitialBalance();
		assertTrue(GlobalData.AMOUNT_COMPARATOR.compare(0.0, account1Diff)!=0);
		assertNull(archiveData.getAccount(ACCOUNT_2_NAME));
		assertEquals(4, data.getTransactionsNumber());
		Transaction[] transactions = new Transaction[data.getTransactionsNumber()];
		for (int i = 0; i < transactions.length; i++) {
			transactions[i] = data.getTransaction(i);
		}
		double[] finalBalances = new double[data.getAccountsNumber()];
		for (int i = 0; i < finalBalances.length; i++) {
			finalBalances[i] = data.getAccount(i).getBalanceData().getFinalBalance();
		}
		ARCHIVER.move (data, archiveData, transactions, true);
		
		// There should be five statements in archive
		assertEquals(5, archiveData.getTransactionsNumber());
		// There should be two statements in account 1
		assertEquals(2, Statement.getStatements(archiveAccount1).length);
		// Mode 1 must be changed
		Mode archiveMode1 = archiveAccount1.getMode(MODE_1);
		assertEquals(DateStepper.IMMEDIATE, archiveMode1.getExpenseVdc());
		assertTrue(archiveMode1.isUseCheckBook());
		// Mode 2 should exists
		assertNotNull(archiveAccount1.getMode(MODE_2));
		// Account 2 should exists
		Account archive_account2 = archiveData.getAccount(ACCOUNT_2_NAME); 
		assertNotNull(archive_account2);
		// Categorie 2 should exists
		assertNotNull(archiveData.getCategory(CATEGORY_2));
		// Categorie 3 should exists
		assertNotNull(archiveData.getCategory(CATEGORY_3));

		for (int i = 0; i < finalBalances.length; i++) {
			assertTrue(GlobalData.AMOUNT_COMPARATOR.compare(finalBalances[i], data.getAccount(i).getBalanceData().getFinalBalance())==0);
		}
		// Difference between final archive balance and initial balance of common account should not change
		double finalAccount1Diff = archiveAccount1.getBalanceData().getFinalBalance()-data.getAccount(ACCOUNT_1_NAME).getInitialBalance();
		assertTrue(GlobalData.AMOUNT_COMPARATOR.compare(account1Diff,finalAccount1Diff)==0);
		// There should be no more transactions in data
		assertEquals(0, data.getTransactionsNumber());
		// account 1 initial balance should have changed
		assertTrue(GlobalData.AMOUNT_COMPARATOR.compare(data.getAccount(0).getInitialBalance(), -20.0)==0);
		// account 2 initial balance should have changed
		assertTrue(GlobalData.AMOUNT_COMPARATOR.compare(data.getAccount(1).getInitialBalance(), -25.0)==0);

		// Test move back to data
		List<Transaction> lst = new ArrayList<Transaction>();
		for (int i = 0; i < archiveData.getTransactionsNumber(); i++) {
			Transaction transaction = archiveData.getTransaction(i);
			if (!transaction.getDescription().contains("Archived")) {
				lst.add(transaction);
			}
		}
		lst.toArray(transactions);
		ARCHIVER.move (data, archiveData, transactions, false);
		// Test final balances
		for (int i = 0; i < finalBalances.length; i++) {
			assertTrue(GlobalData.AMOUNT_COMPARATOR.compare(finalBalances[i], data.getAccount(i).getBalanceData().getFinalBalance())==0);
		}
		// There should be four statements in data
		assertEquals(4, data.getTransactionsNumber());
		// There should be one in archive
		assertEquals(1, archiveData.getTransactionsNumber());
		for (int i=0;i<archiveData.getAccountsNumber();i++) {
			Account archiveAccount = archiveData.getAccount(i);
			assertFalse(archiveAccount.getTransactionsNumber()==0);
			// Difference between final archive balance and initial balance of common account should not change
			finalAccount1Diff = archiveAccount.getBalanceData().getFinalBalance()-data.getAccount(archiveAccount.getName()).getInitialBalance();
			assertTrue(GlobalData.AMOUNT_COMPARATOR.compare(account1Diff,finalAccount1Diff)==0);
		}
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testNotAnArchive() {
		GlobalData archiveData = buildArchiveData();
		GlobalData data = buildData();
		Transaction[] transactions = new Transaction[data.getTransactionsNumber()];
		for (int i = 0; i < transactions.length; i++) {
			transactions[i] = data.getTransaction(i);
		}
		archiveData.setArchive(false);
		ARCHIVER.move (data, archiveData, transactions, true);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testArchive() {
		GlobalData archiveData = buildArchiveData();
		GlobalData data = buildData();
		data.setArchive(true);
		Transaction[] transactions = new Transaction[data.getTransactionsNumber()];
		for (int i = 0; i < transactions.length; i++) {
			transactions[i] = data.getTransaction(i);
		}
		ARCHIVER.move (data, archiveData, transactions, true);
	}

	protected GlobalData buildArchiveData() {
		GlobalData archiveData  = new GlobalData();
		archiveData.setArchive(true);
		Account archiveAccount1 = new Account(ACCOUNT_1_NAME, 40.0);
		archiveData.add(archiveAccount1);
		Mode archive_mode = new Mode(MODE_1, DateStepper.IMMEDIATE, null, false);
		archiveAccount1.add(archive_mode);
		Date date = new GregorianCalendar(2013, 10, 25).getTime();
		archiveData.add(new Transaction(date, null, "Archived transaction", null, -10, archiveAccount1,
				archive_mode, new Category(CATEGORY_1), date, null, null));
		return archiveData;
	}

	protected GlobalData buildData() {
		Date date;
		GlobalData data  = new GlobalData();
		Account account1 = new Account(ACCOUNT_1_NAME, 0.0);
		Mode mode = new Mode(MODE_1, DateStepper.IMMEDIATE, DateStepper.IMMEDIATE, true);
		account1.add(mode);
		Mode mode2 = new Mode(MODE_2, DateStepper.IMMEDIATE, DateStepper.IMMEDIATE, false);
		account1.add(mode2);
		data.add(account1);
		Account account2 = new Account(ACCOUNT_2_NAME, 0.0);
		data.add(account2);
		data.add(new Category(CATEGORY_1));
		data.add(new Category(CATEGORY_2));
		
		List<Transaction> transactions = new ArrayList<Transaction>();
		date = new GregorianCalendar(2013, 10, 26).getTime();
		transactions.add(new Transaction(date, null, "transaction 1", null, -10, account1,
				mode, data.getCategory(CATEGORY_1), date, "1", null));
		transactions.add(new Transaction(date, null, "transaction 2", null, -10, account1,
				mode2, data.getCategory(CATEGORY_2), date, "1", null));
		date = new GregorianCalendar(2013, 10, 27).getTime();
		transactions.add(new Transaction(date, null, "transaction 3", null, -10, account2,
				Mode.UNDEFINED, data.getCategory(CATEGORY_1), date, "2", null));
		SubTransaction st = new SubTransaction(-5.0, "sub transaction", new Category(CATEGORY_3));
		transactions.add(new Transaction(date, null, "transaction 3", null, -15, account2,
				Mode.UNDEFINED, Category.UNDEFINED, date, "2", Collections.singletonList(st)));
		Transaction[] transactionsArray = transactions.toArray(new Transaction[transactions.size()]);
		data.add(transactionsArray);
		return data;
	}
}
