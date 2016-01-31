package net.yapbam.date.helpers;

import java.text.DateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.yapbam.util.DateUtils;

public class DayDateStepper extends DateStepper {
	private static final Logger LOGGER = LoggerFactory.getLogger(DayDateStepper.class);
	private static final boolean DEBUG = false;

	private int nbDays;
	private int lastDate;

	/** Constructor.
	 *  @param nbDays Number of days between the value date and the operation date.
	 *  If this number is negative, the value dates will be before operation dates.
	 *  @param lastDate lastDate or null if there's no time limit.
	 */
	public DayDateStepper(int nbDays, Date lastDate) {
		super();
		this.nbDays = nbDays;
		this.lastDate = lastDate==null?Integer.MAX_VALUE:DateUtils.dateToInteger(lastDate);
	}
	
	@Override
	public Date getNextStep(Date date) {
		if (DEBUG) {
			LOGGER.debug("Date: {}", DateFormat.getDateInstance().format(date)); //$NON-NLS-1$
			LOGGER.debug("  Number of days: {}", this.nbDays); //$NON-NLS-1$
		}
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(date);
		gc.add(GregorianCalendar.DAY_OF_MONTH, this.nbDays);
		Date result = gc.getTime();
		if (DEBUG) {
			LOGGER.debug(" => Debt day: {}", DateFormat.getDateInstance().format(result)); //$NON-NLS-1$
			LOGGER.debug("----------------------"); //$NON-NLS-1$
		}
		if (DateUtils.dateToInteger(result)>this.lastDate) {
			result = null;
		}
		return result;
	}

	public int getStep() {
		return this.nbDays;
	}

	@Override
	public Date getLastDate() {
		return this.lastDate==Integer.MAX_VALUE?null:DateUtils.integerToDate(this.lastDate);
	}

	@Override
	public boolean equals(Object obj) {
		if ((obj==null) || !(obj instanceof DayDateStepper)) {
			return false;
		}
		return (getStep()==((DayDateStepper)obj).getStep()) && (lastDate==((DayDateStepper)obj).lastDate);
	}

	@Override
	public int hashCode() {
		return getStep()+this.lastDate;
	}
}
