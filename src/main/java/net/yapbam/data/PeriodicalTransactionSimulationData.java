package net.yapbam.data;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import net.yapbam.data.event.AccountAddedEvent;
import net.yapbam.data.event.CategoryAddedEvent;
import net.yapbam.data.event.CheckbookAddedEvent;
import net.yapbam.data.event.CheckbookPropertyChangedEvent;
import net.yapbam.data.event.CheckbookRemovedEvent;
import net.yapbam.data.event.DataEvent;
import net.yapbam.data.event.DataListener;
import net.yapbam.data.event.IsArchivedChangedEvent;
import net.yapbam.data.event.IsLockedChangedEvent;
import net.yapbam.data.event.ModeAddedEvent;
import net.yapbam.data.event.NeedToBeSavedChangedEvent;
import net.yapbam.data.event.PasswordChangedEvent;
import net.yapbam.data.event.URIChangedEvent;

public class PeriodicalTransactionSimulationData extends Observable {
	public enum Unit {MONTH(GregorianCalendar.MONTH), YEAR(GregorianCalendar.YEAR);
		private int field;
		
		Unit(int field) {
			this.field = field;
		}
		
		public int getField() {
			return this.field;
		}
	}
	
	private FilteredData data;
	private Date endDate;
	private boolean ignoreFilter;
	private boolean needRefresh;
	private double totalExpenses;
	private double totalReceipts;
	private int nbTransactions;
	
	public PeriodicalTransactionSimulationData(FilteredData data) {
		this.data = data;
		this.ignoreFilter = true;
		setEndDate(Unit.YEAR, 1);
		data.getGlobalData().addListener(new DataListener() {
			@Override
			public void processEvent(DataEvent event) {
				if (hasImpact(event)) {
					invalidate();
				}
			}
		});
		data.getFilter().addObserver(new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				if (!ignoreFilter) {
					invalidate();
				}
			}
		});
	}
	
	private boolean hasImpact(DataEvent event) {
		return !(event instanceof AccountAddedEvent) && !(event instanceof CategoryAddedEvent) && !(event instanceof CheckbookAddedEvent) &&
				!(event instanceof CheckbookPropertyChangedEvent) && !(event instanceof CheckbookRemovedEvent) &&
				!(event instanceof IsArchivedChangedEvent) && !(event instanceof IsLockedChangedEvent) &&
				!(event instanceof ModeAddedEvent) && !(event instanceof NeedToBeSavedChangedEvent) &&
				!(event instanceof PasswordChangedEvent) && !(event instanceof URIChangedEvent);
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

	public void setIgnoreFilter(boolean ignore) {
		if (ignore!=ignoreFilter) {
			ignoreFilter = ignore;
			this.needRefresh = true;
			setChanged();
			notifyObservers();
		}
	}
	private void refresh() {
		if (needRefresh) {
			nbTransactions = 0;
			totalReceipts = 0.0;
			totalExpenses = -0.0;
			for (int i = 0; i < data.getGlobalData().getPeriodicalTransactionsNumber(); i++) {
				PeriodicalTransaction pt = data.getGlobalData().getPeriodicalTransaction(i);
				if (ignoreFilter || data.getFilter().isOk(pt)) {
					List<Transaction> generated = pt.generate(endDate, null);
					nbTransactions += generated.size();
					double total = 0.0;
					for (Transaction transaction : generated) {
						total += transaction.getAmount();
					}
					if (total<0) {
						totalExpenses += total;
					} else {
						totalReceipts += total;
					}
				}
			}
		}
	}

	public double getTotalExpenses() {
		refresh();
		return totalExpenses;
	}

	public double getTotalReceips() {
		refresh();
		return totalReceipts;
	}

	public int getNbTransactions() {
		refresh();
		return nbTransactions;
	}

	private void invalidate() {
		needRefresh = true;
		setChanged();
		notifyObservers();
	}
}
