package net.yapbam.data;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Observable;

public class PeriodicalTransactionSimulationData extends Observable {
	public enum Unit {MONTH(GregorianCalendar.MONTH), YEAR(GregorianCalendar.YEAR);
		private int field;
		
		Unit(int field) {
			this.field = field;
		}
		
		public int getField() {
			return this.field;
		}
	};
	
	private FilteredData data;
	private Date endDate;
	private boolean needRefresh;
	
	public PeriodicalTransactionSimulationData(FilteredData data) {
		this.data = data;
		setEndDate(Unit.YEAR, 1);
	}

	public void setEndDate(Unit unit, int amount) {
		Calendar cal = new GregorianCalendar();
		cal.set(GregorianCalendar.HOUR_OF_DAY, 0);
		cal.set(GregorianCalendar.MINUTE, 0);
		cal.add(unit.getField(), amount);
		Date date = cal.getTime();
		if (!date.equals(this.endDate)) {
			this.endDate = date;
			this.needRefresh = true;
			this.setChanged();
			this.notifyObservers();
		}
	}

}
