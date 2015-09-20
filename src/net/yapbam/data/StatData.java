package net.yapbam.data;

import java.util.Arrays;
import java.util.Observable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import net.yapbam.data.event.TransactionsAddedEvent;
import net.yapbam.data.event.TransactionsRemovedEvent;
import net.yapbam.data.event.URIChangedEvent;

/** Some statistics about FilteredData.
 * @see FilteredData
 */
public class StatData extends Observable {
	private static final Logger LOGGER = LoggerFactory.getLogger(StatData.class);
	
	private int nbReceipts;
	private int nbExpenses;
	private double receipts;
	private double expenses;
	private FilteredData data;
	private boolean needRefresh;
	
	/** Constructor
	 * @param data The data to which the created instance will be linked.
	 */
	public StatData(FilteredData data) {
		this.data = data;
		this.nbReceipts = 0;
		this.nbExpenses = 0;
		this.receipts = 0;
		this.expenses = 0;
		this.needRefresh = false;
		data.addListener(new DataListener() {
			@Override
			public void processEvent(DataEvent event) {
				if (!isNeutral(event)) {
					if (!needRefresh && event instanceof TransactionsAddedEvent) {
						Transaction[] transactions = ((TransactionsAddedEvent)event).getTransactions();
						refresh(Arrays.asList(transactions), true);
						LOGGER.trace("Partial update done on {} transactions", transactions.length);
					} else if (!needRefresh && event instanceof TransactionsRemovedEvent) {
						Transaction[] transactions = ((TransactionsRemovedEvent)event).getTransactions();
						refresh(Arrays.asList(transactions), false);
						LOGGER.trace("Partial update done on {} transactions", transactions.length);
					} else {
						needRefresh = true;
					}
					fireChanged();
				}
			}
		});
	}
	
	/** Tests whether an event may have any impact on the statistics.
	 * <br>For instance, NeedToBeSavedChangedEvent has no effect, but TransactionsAddedEvent has.
	 * @param event The event to test
	 * @return true if the event has no effect on this view
	 */
	private boolean isNeutral(DataEvent event) {
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

	/** Gets the number of receipts that match the filter.
	 * @return an integer.
	 */
	public int getNbReceipts() {
		ensureIsUpdated();
		return nbReceipts;
	}

	/** Gets the number of expenses that match the filter.
	 * @return an integer.
	 */
	public int getNbExpenses() {
		ensureIsUpdated();
		return nbExpenses;
	}
	
	/** Gets the total amount of receipts that match the filter.
	 * @return a double.
	 */
	public double getReceipts() {
		ensureIsUpdated();
		return receipts;
	}

	/** Gets the total amount of expenses that match the filter.
	 * @return an integer.
	 */
	public double getExpenses() {
		ensureIsUpdated();
		return expenses;
	}

	private void refresh() {
		nbReceipts = 0;
		receipts = 0.0;
		nbExpenses = 0;
		expenses = 0.0;
		refresh(data.getTransactions(), true);
		LOGGER.trace("Full stat update done on {} transactions", data.getTransactionsNumber());
		needRefresh = false;
	}
	
	private void refresh(Iterable<Transaction> transactions, boolean add) {
		for (Transaction transaction : transactions) {
			double amount = transaction.getAmount();
			if (amount<0) {
				if (add) {
					nbExpenses++;
					expenses += amount;
				} else {
					nbExpenses--;
					expenses -= amount;
				}
			} else {
				if (add) {
					nbReceipts++;
					receipts += amount;
				} else {
					nbReceipts--;
					receipts -= amount;
				}
			}
		}
	}

	private void fireChanged() {
		setChanged();
		notifyObservers();
	}
}
