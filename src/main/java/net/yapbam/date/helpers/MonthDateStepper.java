package net.yapbam.date.helpers;

import java.text.DateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.yapbam.util.DateUtils;

public class MonthDateStepper extends  DateStepper {
	private static final Logger LOGGER = LoggerFactory.getLogger(DeferredValueDateComputer.class);
	private static final boolean DEBUG = false;

	private int period;
	private int day;
	private int lastDate;
	
	/**
	 * Constructor.
	 * @param nb Number of months between two dates
	 * @param day day of month (1-&gt;31)
	 * The time limit is the end of times. 
	 * @see #MonthDateStepper(int, int, Date)
	 */
	public MonthDateStepper(int nb, int day) {
		this(nb, day, null);
	}
	
	/**
	 * Constructor
	 * @param nb Number of months between two dates
	 * @param day day of month (1-&gt;31)
	 * If the day is 31 and, for instance, the month of next date would be February,
	 * the day would automatically set to the last day of the month 
	 * @param timeLimit timeLimit or null if there is no time limit
	 */
	public MonthDateStepper(int nb, int day, Date timeLimit) {
		super();
		this.period = nb;
		this.day = day;
		this.lastDate = timeLimit==null?Integer.MAX_VALUE:DateUtils.dateToInteger(timeLimit);
	}
	
	/* (non-Javadoc)
	 * @see net.yapbam.data.helpers.DateIterator#getNextDate(java.util.Date)
	 */
	@Override
	public Date getNextStep(Date date) {
		if (DEBUG) {
			LOGGER.debug("Date: {}", DateFormat.getDateInstance().format(date)); //$NON-NLS-1$
			LOGGER.debug("  Number of months: {}", this.period); //$NON-NLS-1$
		}
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(date);
		gc.set(GregorianCalendar.DAY_OF_MONTH, 1);
		gc.add(GregorianCalendar.MONTH, this.period);
		int max = gc.getActualMaximum(GregorianCalendar.DAY_OF_MONTH);
		gc.set(GregorianCalendar.DAY_OF_MONTH, Math.min(max, this.day));
		if (DEBUG) {
			LOGGER.debug(" => Debt day: {}", DateFormat.getDateInstance().format(gc.getTime())); //$NON-NLS-1$
			LOGGER.debug("----------------------"); //$NON-NLS-1$
		}
		Date result = gc.getTime();
		if (DateUtils.dateToInteger(result)>this.lastDate) {
			result = null;
		}
		return result;
	}

	public int getPeriod() {
		return period;
	}

	public int getDay() {
		return day;
	}

	public Date getLastDate() {
		return this.lastDate==Integer.MAX_VALUE?null:DateUtils.integerToDate(this.lastDate);
	}
	
	@Override
	public boolean equals(Object obj) {
		if ((obj==null) || !(obj instanceof MonthDateStepper)) {
			return false;
		}
		return (period==((MonthDateStepper)obj).period) &&
				(day==((MonthDateStepper)obj).day) &&
				(lastDate==((MonthDateStepper)obj).lastDate);
	}

	@Override
	public int hashCode() {
		return this.day+this.period+this.lastDate;
	}
}
