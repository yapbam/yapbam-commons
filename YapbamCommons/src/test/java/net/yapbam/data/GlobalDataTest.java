package net.yapbam.data;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Test;

public class GlobalDataTest {

	@Test (expected=IllegalArgumentException.class)
	public void testUnknownAccount() {
		GlobalData data = new GlobalData();
		Date date = new Date();
		Account account = new Account("test",0.0);
		Transaction t = new Transaction(date, null, "test", null, 100, account, Mode.UNDEFINED, Category.UNDEFINED, date, null, null);
		data.add(t);
	}

	@Test
	public void testDuplicated() {
		GlobalData data = new GlobalData();
		Date date = new Date();
		Account account = new Account("test",0.0);
		data.add(account);
		Transaction t = new Transaction(date, null, "test", null, 100, account, Mode.UNDEFINED, Category.UNDEFINED, date, null, null);
		data.add(t);
		assertEquals(1, data.getTransactionsNumber());
		try {
			data.add(t);
			fail("add duplicated succeed !!!");
		} catch (IllegalArgumentException e) {
			assertEquals(1, data.getTransactionsNumber());
		}
		Transaction[] ts = new Transaction[] {
				new Transaction(date, null, "test2", null, 100, account, Mode.UNDEFINED, Category.UNDEFINED, date, null, null),
				t
		};
		try {
			data.add(ts);
			fail("add duplicated succeed !!!");
		} catch (IllegalArgumentException e) {
			assertEquals(1, data.getTransactionsNumber());
		}
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testNullCategoryName() {
		GlobalData data = new GlobalData();
		data.setName(new Category("test"), null);
	}
}
