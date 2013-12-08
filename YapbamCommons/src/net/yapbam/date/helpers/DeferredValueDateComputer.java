package net.yapbam.date.helpers;

import java.text.DateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class allows to compute value date for a deferred operation */ 
public class DeferredValueDateComputer extends DateStepper {
	private static final Logger LOGGER = LoggerFactory.getLogger(DeferredValueDateComputer.class);
	private static final boolean DEBUG = false;

	private int stopDay;
	private int debtDay;

	public DeferredValueDateComputer(int stopDay, int debtDay) {
		super();
		this.stopDay = stopDay;
		this.debtDay = debtDay;
	}

	public int getStopDay() {
		return stopDay;
	}

	public int getDebtDay() {
		return debtDay;
	}

	@Override
	public Date getNextStep(Date date) {
		if (DEBUG) {
			LOGGER.debug("Date: {}", DateFormat.getDateInstance().format(date)); //$NON-NLS-1$
			LOGGER.debug("  StopDay: {}", this.stopDay); //$NON-NLS-1$
			LOGGER.debug("  DebtDay: {}", this.debtDay); //$NON-NLS-1$
		}
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(date);
		int day = gc.get(GregorianCalendar.DATE);
		int month = gc.get(GregorianCalendar.MONTH);
		int year = gc.get(GregorianCalendar.YEAR);
		if (day>this.stopDay) {
			month++;
			if (month>gc.getActualMaximum(GregorianCalendar.MONTH)) {
				year++;
				month = 0;
			}
		}
		if (this.stopDay>this.debtDay){
			month++;
			if (month>gc.getActualMaximum(GregorianCalendar.MONTH)) {
				year++;
				month = 0;
			}
		}
		gc.set(year, month, 1);
		if (DEBUG) {
			LOGGER.debug(" First day of debt month: {}", DateFormat.getDateInstance().format(gc.getTime())); //$NON-NLS-1$
		}
		gc.add(GregorianCalendar.MONTH, 1);
		if (DEBUG) {
			LOGGER.debug(" First day of next month: {}", DateFormat.getDateInstance().format(gc.getTime())); //$NON-NLS-1$
		}
		gc.add(GregorianCalendar.DAY_OF_MONTH, -1);
		if (DEBUG) {
			LOGGER.debug(" Last day of debt month: {}", DateFormat.getDateInstance().format(gc.getTime())); //$NON-NLS-1$
		}
		if (debtDay<gc.get(GregorianCalendar.DAY_OF_MONTH)) {
			gc.set(GregorianCalendar.DATE, debtDay);
		} else if (DEBUG) {
			LOGGER.debug(" Month has less than {} days, we take last day of the month"); //$NON-NLS-1$
		}
		if (DEBUG) {
			LOGGER.debug(" => Debt day: {}", DateFormat.getDateInstance().format(gc.getTime())); //$NON-NLS-1$
			LOGGER.debug("----------------------"); //$NON-NLS-1$
		}
		return gc.getTime();
	}

	@Override
	public Date getLastDate() {
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		boolean result = super.equals(obj);
		if (result) {
			result = (getStopDay()==((DeferredValueDateComputer)obj).getStopDay()) &&
				(getDebtDay()==((DeferredValueDateComputer)obj).getDebtDay());
		}
		return result;
	}

	@Override
	public int hashCode() {
		return getDebtDay()*100+getStopDay();
	}
}
