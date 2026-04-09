package net.yapbam.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.GregorianCalendar;

import net.yapbam.util.DateUtils;

import org.junit.jupiter.api.Test;

class DateUtilsTest {

	@Test
	void test() {
		GregorianCalendar first = new GregorianCalendar(2009, 11, 31, 23, 59, 59);
		Object original1 = first.clone();
		GregorianCalendar last = new GregorianCalendar(2010, 0, 1, 0, 0, 0);
		Object original2 = last.clone();
		assertEquals(1, DateUtils.getMonthlyDistance(first, last));
		assertEquals(original1, first);
		assertEquals(original2, last);
	}

}
