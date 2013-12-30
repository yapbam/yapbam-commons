package net.yapbam.date.helpers;

import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;

import net.yapbam.date.helpers.DateStepper;
import net.yapbam.date.helpers.DayDateStepper;
import net.yapbam.date.helpers.MonthDateStepper;

import org.junit.Test;

public class DateStepperTest {

	@Test
	public void test() {
		DateStepper stepper = new MonthDateStepper(1, 10, new GregorianCalendar(2012, 1, 15).getTime());
		assertEquals(new GregorianCalendar(2012, 1 , 10).getTime(), stepper.getNextStep(new GregorianCalendar(2012, 0 , 1).getTime()));
		assertNull(stepper.getNextStep(new GregorianCalendar(2012, 1 , 10).getTime()));

		stepper = new DayDateStepper(10, new GregorianCalendar(2012, 0, 15).getTime());
		assertEquals(new GregorianCalendar(2012, 0 , 11).getTime(), stepper.getNextStep(new GregorianCalendar(2012, 0 , 1).getTime()));
		assertNull(stepper.getNextStep(new GregorianCalendar(2012, 0 , 10).getTime()));
	}

	@Test
	public void immediateTest() {
		DateStepper stepper = DateStepper.IMMEDIATE;
		Date date = new Date();
		assertEquals(date, stepper.getNextStep(date));
		assertNull(stepper.getLastDate());
	}
	
	@Test
	public void dayDateStepper() {
		DayDateStepper stepper = new DayDateStepper(10, null);
		assertNull(stepper.getLastDate());
		assertEquals(10, stepper.getStep());
		Date date = new Date();
		GregorianCalendar c = new GregorianCalendar();
		c.setTime(date);
		c.add(Calendar.DATE, 10);
		assertEquals(c.getTime(), stepper.getNextStep(date));
		Set<DateStepper> set = new HashSet<DateStepper>();
		set.add(stepper);
		set.add(DateStepper.IMMEDIATE);
		assertTrue(set.contains(new DayDateStepper(10, null)));
		assertFalse(stepper.equals(DateStepper.IMMEDIATE));
	}

	@Test
	public void monthDateStepper() {
		MonthDateStepper stepper = new MonthDateStepper(3, 15);
		assertNull(stepper.getLastDate());
		assertEquals(15, stepper.getDay());
		assertEquals(3, stepper.getPeriod());
		Set<DateStepper> set = new HashSet<DateStepper>();
		set.add(stepper);
		set.add(DateStepper.IMMEDIATE);
		assertTrue(set.contains(new MonthDateStepper(3, 15)));
		assertFalse(set.contains(new MonthDateStepper(3, 16)));
		assertFalse(stepper.equals(DateStepper.IMMEDIATE));
	}


	@Test
	public void deferedValueDateComputer() {
		DeferredValueDateComputer stepper = new DeferredValueDateComputer(3, 15);
		assertNull(stepper.getLastDate());
		assertEquals(15, stepper.getDebtDay());
		assertEquals(3, stepper.getStopDay());
		Set<DateStepper> set = new HashSet<DateStepper>();
		set.add(stepper);
		set.add(DateStepper.IMMEDIATE);
		assertTrue(set.contains(new DeferredValueDateComputer(3, 15)));
		assertFalse(set.contains(new DeferredValueDateComputer(3, 16)));
		assertFalse(stepper.equals(DateStepper.IMMEDIATE));
	}
}
