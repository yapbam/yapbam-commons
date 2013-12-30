package net.yapbam.date.helpers;

import java.util.Date;
import java.util.GregorianCalendar;

/** This class allows to compute value date for a deferred operation */ 
public class DeferredValueDateComputer extends DateStepper {
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
		gc.add(GregorianCalendar.MONTH, 1);
		gc.add(GregorianCalendar.DAY_OF_MONTH, -1);
		if (debtDay<gc.get(GregorianCalendar.DAY_OF_MONTH)) {
			gc.set(GregorianCalendar.DATE, debtDay);
		}
		return gc.getTime();
	}

	@Override
	public Date getLastDate() {
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if ((obj==null) || !(obj instanceof DeferredValueDateComputer)) {
			return false;
		}
		return (getStopDay()==((DeferredValueDateComputer)obj).getStopDay()) &&
				(getDebtDay()==((DeferredValueDateComputer)obj).getDebtDay());
	}

	@Override
	public int hashCode() {
		return getDebtDay()*100+getStopDay();
	}
}
