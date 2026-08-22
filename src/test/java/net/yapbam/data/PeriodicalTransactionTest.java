package net.yapbam.data;

import java.util.ArrayList;
import java.util.Date;

import net.yapbam.date.helpers.MonthDateStepper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PeriodicalTransactionTest {
	private Date firstJanuary;
	private Date firstFebruary;
	private Date firstMarch;
	private Date firstApril;
	

	@SuppressWarnings("deprecation")
	@BeforeEach
	void setUp() {
		firstJanuary = new Date(112, 0, 1);
		firstFebruary = new Date(112, 1, 1);
		firstMarch = new Date(112, 2, 1);
		firstApril = new Date(112, 3, 1);
	}

	@Test
	void dateAfterEnd() {
		MonthDateStepper ds = new MonthDateStepper(1, 1, firstFebruary);
		Account account = new Account("test",0.0);
		ArrayList<SubTransaction> subs = new ArrayList<SubTransaction>();
		assertThrows(IllegalArgumentException.class, () -> new PeriodicalTransaction("test", null, 10.0, account, Mode.UNDEFINED, Category.UNDEFINED, subs,
				firstMarch, true, ds));
	}

	@Test
	void nextIsNullAndEnabled() {
		MonthDateStepper ds = new MonthDateStepper(1, 1, firstFebruary);
		Account account = new Account("test",0.0);
		ArrayList<SubTransaction> subs = new ArrayList<SubTransaction>();
		assertThrows(IllegalArgumentException.class, () -> new PeriodicalTransaction("test", null, 10.0, account, Mode.UNDEFINED, Category.UNDEFINED, subs,
				null, true, ds));
	}

	@Test
	void doTest() {
		MonthDateStepper ds = new MonthDateStepper(1, 1, firstMarch);
		
		// Test a cool standard periodical transaction
		PeriodicalTransaction pt = new PeriodicalTransaction("test", null, 10.0, new Account("test",0.0), Mode.UNDEFINED, Category.UNDEFINED, new ArrayList<SubTransaction>(),
				firstFebruary, true, ds);
		assertFalse(pt.hasPendingTransactions(firstJanuary));
		assertTrue(pt.hasPendingTransactions(firstFebruary));
		assertTrue(pt.hasPendingTransactions(firstMarch));
		assertTrue(pt.hasPendingTransactions(firstApril));
	}
}
