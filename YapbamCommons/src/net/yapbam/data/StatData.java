package net.yapbam.data;

import java.util.Observable;

import net.yapbam.data.event.AccountAddedEvent;
import net.yapbam.data.event.AccountPropertyChangedEvent;
import net.yapbam.data.event.AccountRemovedEvent;
import net.yapbam.data.event.CheckbookAddedEvent;
import net.yapbam.data.event.CheckbookPropertyChangedEvent;
import net.yapbam.data.event.CheckbookRemovedEvent;
import net.yapbam.data.event.DataEvent;
import net.yapbam.data.event.DataListener;
import net.yapbam.data.event.ModeAddedEvent;
import net.yapbam.data.event.ModePropertyChangedEvent;
import net.yapbam.data.event.ModeRemovedEvent;
import net.yapbam.data.event.NeedToBeSavedChangedEvent;
import net.yapbam.data.event.PasswordChangedEvent;
import net.yapbam.data.event.PeriodicalTransactionsAddedEvent;
import net.yapbam.data.event.PeriodicalTransactionsRemovedEvent;
import net.yapbam.data.event.URIChangedEvent;

//TODO Javadoc
public class StatData extends Observable {
	private int nbReceipts;
	private int nbExpenses;
	private double receipts;
	private double expenses;
	private FilteredData data;
	private boolean needRefresh;
	
	public StatData(FilteredData data) {
		this.data = data;
		this.nbReceipts = 0;
		this.nbExpenses = 0;
		this.receipts = 0;
		this.expenses = 0;
		data.addListener(new DataListener() {
			@Override
			public void processEvent(DataEvent event) {
				if (!isNeutral(event)) {
					needRefresh = true;
					fireChanged();
					System.out.println (event);
				} else {
					System.out.println ("Ignored: "+event);
				}
			}
		});
	}
	
	/** Tests whether an event may have any impact on the statistics.
	 * <br>For instance, NeedToBeSavedChangedEvent has no effect, but TransactionsAddedEvent has.
	 * @param event The event to test
	 * @return true if the event has no effect on this view
	 */
	private boolean isNeutral(DataEvent event) { //TODO
		return (event instanceof NeedToBeSavedChangedEvent) || (event instanceof PasswordChangedEvent) || (event instanceof URIChangedEvent) 
			|| (event instanceof AccountAddedEvent) || (event instanceof AccountRemovedEvent) || (event instanceof AccountPropertyChangedEvent)
			|| (event instanceof ModeAddedEvent) || (event instanceof ModeRemovedEvent) || (event instanceof ModePropertyChangedEvent)
			|| (event instanceof CheckbookPropertyChangedEvent) || (event instanceof CheckbookAddedEvent) || (event instanceof CheckbookRemovedEvent)
			|| (event instanceof PeriodicalTransactionsAddedEvent) || (event instanceof PeriodicalTransactionsRemovedEvent);
	}

	private void ensureIsUpdated() {
		if (needRefresh) {
			refresh();
		}
	}

	public int getNbReceipts() {
		ensureIsUpdated();
		return nbReceipts;
	}

	public int getNbExpenses() {
		ensureIsUpdated();
		return nbExpenses;
	}
	public double getReceipts() {
		ensureIsUpdated();
		return receipts;
	}
	public double getExpenses() {
		ensureIsUpdated();
		return expenses;
	}

	private void refresh() {
		nbReceipts = 0;
		receipts = 0.0;
		nbExpenses = 0;
		expenses = 0.0;
		for (int i = 0; i < data.getTransactionsNumber(); i++) {
			double amount = data.getTransaction(i).getAmount();
			if (amount<0) {
				nbExpenses++;
				expenses += amount;
			} else {
				nbReceipts++;
				receipts += amount;
			}
		}
		//TODO
		System.out.println ("Update done");
		needRefresh = false;
	}
	
	private void fireChanged() {
		setChanged();
		notifyObservers();
	}
}
