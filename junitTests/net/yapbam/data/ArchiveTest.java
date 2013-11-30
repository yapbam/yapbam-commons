package net.yapbam.data;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import net.yapbam.date.helpers.DateStepper;

import org.junit.Test;

public class ArchiveTest {
	
	private static final String CATEGORY_1 = "category 1";
	private static final String CATEGORY_2 = "category 2";
	private static final String CATEGORY_3 = "category 3";
	private static final String ACCOUNT_2_NAME = "account 2";
	private static final String ACCOUNT_1_NAME = "account 1";
	private static final String MODE_1 = "mode 1";
	private static final String MODE_2 = "mode 2";

	@Test
	public void test() {
		GlobalData archive_data  = new GlobalData();
		Account archive_account = new Account(ACCOUNT_1_NAME, 10.0);
		archive_data.add(archive_account);
		Mode archive_mode = new Mode(MODE_1, DateStepper.IMMEDIATE, null, false);
		archive_account.add(archive_mode);
		Date date = new GregorianCalendar(2013, 10, 25).getTime();
		archive_data.add(new Transaction(date, null, "Archived transaction", null, -10, archive_account,
				archive_mode, new Category(CATEGORY_1), date, null, null));
		
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
		
		System.out.println (Arrays.asList(transactionsArray));
		System.out.println (archive_data);

		Account account = archive_data.getAccount(ACCOUNT_1_NAME);
		assertNotEquals(account.getBalanceData().getFinalBalance(),data.getAccount(ACCOUNT_1_NAME));
		assertNull(archive_data.getAccount(ACCOUNT_2_NAME));
		assertEquals(4, data.getTransactionsNumber());
		
		Archiver.archive (archive_data, transactionsArray);
		
		// There should be five statements in archive
		assertEquals(5, archive_data.getTransactionsNumber());
		// There should be two statements in account 1
		assertEquals(2, Statement.getStatements(archive_account).length);
		// Mode 1 must be changed
		Mode archiveMode1 = archive_account.getMode(MODE_1);
		assertEquals(DateStepper.IMMEDIATE, archiveMode1.getExpenseVdc());
		assertTrue(archiveMode1.isUseCheckBook());
		// Mode 2 should exists
		assertNotNull(archive_account.getMode(MODE_2));
		// Account 2 should exists
		Account archive_account2 = archive_data.getAccount(ACCOUNT_2_NAME); 
		assertNotNull(archive_account2);
		// Categorie 2 should exists
		assertNotNull(archive_data.getCategory(CATEGORY_2));
		// Categorie 3 should exists
		assertNotNull(archive_data.getCategory(CATEGORY_3));

		Archiver.remove (data, transactionsArray);
		
		// There should be no more transactions in data
		assertEquals(0, data.getTransactionsNumber());
		// account 1 initial balance should have changed
		assertTrue(GlobalData.AMOUNT_COMPARATOR.compare(data.getAccount(0).getInitialBalance(), -20.0)==0);
		// account 2 initial balance should have changed
		assertTrue(GlobalData.AMOUNT_COMPARATOR.compare(data.getAccount(1).getInitialBalance(), -25.0)==0);
	}

}
