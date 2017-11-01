package net.yapbam.data;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Date;

import org.junit.Test;

import net.yapbam.date.helpers.DateStepper;

public class GlobalDataTest {

	static class TestData {
		Account[] accounts;
		Category[] categories;
		Mode[] modes;
		FilteredData fData;
		Filter[] filters;
		
		TestData() {
			accounts = new Account[]{new Account("Toto", 0.0), new Account("Titi", 0.0), new Account("Tutu", 0.0)};
			categories = new Category[]{new Category("cat0"), new Category("cat1"), new Category("cat2")};
			modes = new Mode[]{new Mode("mode0", DateStepper.IMMEDIATE, DateStepper.IMMEDIATE, false), new Mode("mode1", DateStepper.IMMEDIATE, DateStepper.IMMEDIATE, false)};
			// Build the global data
			GlobalData gData = new GlobalData();
			fData = new FilteredData(gData);
			for (Account account : accounts) {
				gData.add(account);
			}
			for (Category category : categories) {
				gData.add(category);
			}
			for (Mode mode : modes) {
				gData.getAccount(0).add(mode);
			}
			gData.getAccount(2).add(new Mode(modes[0].getName(), modes[0].getReceiptVdc(), modes[0].getExpenseVdc(), modes[0].isUseCheckBook()));
			filters = new Filter[2];
			filters[0] = new Filter();
			filters[0].setName("filter 0");
			filters[0].setValidAccounts(Arrays.asList(accounts[1], accounts[0]));
			filters[0].setValidCategories(Arrays.asList(categories[0], categories[2]));
			filters[1] = new Filter();
			filters[1].setName("filter 1");
			filters[1].setValidModes(Arrays.asList(modes[0].getName()));
			for (Filter filter : filters) {
				gData.add(filter);
			}
		}
	}

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
	
	@Test
	public void testFilterUpdate() {
		TestData testData = new TestData();
		GlobalData data = testData.fData.getGlobalData();

		// Do the tests
		// Test mode renaming
		assertEquals(1, data.getFilter(1).getValidModes().size());
		Mode oldMode = testData.modes[0];
		Mode newMode = new Mode("mode0-bis", oldMode.getReceiptVdc(), oldMode.getExpenseVdc(), oldMode.isUseCheckBook());
		data.setMode(data.getAccount(0), oldMode, newMode);
		assertEquals(2, data.getFilter(1).getValidModes().size());
		// Test mode removing
		data.remove(data.getAccount(0), newMode);
		assertEquals(1, data.getFilter(1).getValidModes().size());
		
		// Test account removing
		assertEquals(2, data.getFilter(0).getValidAccounts().size());
		data.remove(testData.accounts[1]);
		assertEquals(1,data.getFilter(0).getValidAccounts().size());
		data.setName(testData.accounts[0], "Toto_bis");
		data.remove(data.getAccount(0));
		assertNull(data.getFilter(0).getValidAccounts());
		
		// Test category removing
		assertEquals(2, data.getFilter(0).getValidCategories().size());
		data.remove(testData.categories[2]);
		assertEquals(1, data.getFilter(0).getValidCategories().size());
		data.setName(testData.categories[0], "cat0_bis");
		data.remove(data.getCategory("cat0_bis"));
		assertNull(data.getFilter(0).getValidCategories());
	}
}
