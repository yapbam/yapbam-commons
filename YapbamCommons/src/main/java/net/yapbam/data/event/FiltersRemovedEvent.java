package net.yapbam.data.event;

import net.yapbam.data.Filter;

/** This event is sent when one or more filters are deleted.
 */
public class FiltersRemovedEvent extends DataEvent {
	private Filter[] removed;
	
	/** Constructor.
	 * @param source The object that thrown the event
	 * @param removed The removed filters
	 */
	public FiltersRemovedEvent(Object source, Filter[] removed) {
		super(source);
		this.removed = removed;
	}

	/** Gets the removed filters.
	 * @return a filter array.
	 */
	public Filter[] getFilters() {
		return removed;
	}
}
