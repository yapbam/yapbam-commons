package net.yapbam.data;

import static org.junit.Assert.*;

import java.math.BigInteger;

import org.junit.Test;

public class AccountTest {
	@Test
	public void test() {
		Account account = new Account("test", 0.0);
		assertFalse(account.hasRemainingChecksAlert());
		account.setCheckNumberAlertThreshold(0);
		assertTrue(account.hasRemainingChecksAlert());
		Checkbook book = new Checkbook("x", BigInteger.ZERO, 1, BigInteger.ZERO);
		account.add(book);
		assertFalse(account.hasRemainingChecksAlert());
		book.copy(new Checkbook("x", BigInteger.ZERO, 1, null));
		assertTrue(account.hasRemainingChecksAlert());
	}
}
