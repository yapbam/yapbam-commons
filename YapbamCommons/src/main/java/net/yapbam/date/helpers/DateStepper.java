package net.yapbam.date.helpers;

import java.util.Date;

/** This abstract class is able to perform a time increment over a date.
 * It could be used, for instance, to convert a transaction date in a value date, or to compute
 * the next date of a periodical transaction
 */
public abstract class DateStepper {

	/** A trivial value date computer, that returns the transaction date as value date.*/
	public static final DateStepper IMMEDIATE = new DateStepper() {
		@Override
		public Date getNextStep(Date date) {
			return date;
		}

		@Override
		public Date getLastDate() {
			return null;
		}
	};
	
	/** Compute the next date
	 * @param date date for which we want a successor (which could be before date, for instance for a backward stepper)
	 * @return the next date or null if the cycle has to stop (if the stepper provide this concept)
	 */
	public abstract Date getNextStep (Date date);
	
	/** returns the end date of this stepper.
	 * @return the last date, or null if the stepper has no time limit.
	 */
	public abstract Date getLastDate();
}
