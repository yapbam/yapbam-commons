package net.yapbam.data;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.GregorianCalendar;

import net.yapbam.date.helpers.DateStepper;

import org.junit.Test;

public class ArchiveTest {
	
	private static final String CATEGORY_1 = "category";
	private static final String ACCOUNT_2_NAME = "account 2";
	private static final String ACCOUNT_1_NAME = "account 1";
	private static final String MODE_1 = "mode 1";
	private static final String MODE_2 = "mode 2";

	@Test
	public void test() {
		GlobalData archive_data  = new GlobalData();
		Account archive_account = new Account(ACCOUNT_1_NAME, 0.0);
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
		data.add(account1);
		account1.add(mode2);
		date = new GregorianCalendar(2013, 10, 26).getTime();
		archive_data.add(new Transaction(date, null, "transaction 1", null, -10, account1,
				mode, new Category(CATEGORY_1), date, "1", null));
		Account account2 = new Account(ACCOUNT_2_NAME, 0.0);
		data.add(account2);
		
		Account account = archive_data.getAccount(ACCOUNT_1_NAME);
		assertNotEquals(account.getBalanceData().getFinalBalance(),data.getAccount(ACCOUNT_1_NAME));
		assertNull(archive_data.getAccount(ACCOUNT_2_NAME));
	}

}
