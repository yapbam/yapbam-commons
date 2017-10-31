package net.yapbam.data.event;

import net.yapbam.data.Filter;

/** This event is sent when one or more filters are added.
 */
public class FiltersAddedEvent extends DataEvent {
	private Filter[] filters;
	
	/** Constructor.
	 * @param source The object that thrown the event
	 * @param filters The added filters
	 */
	public FiltersAddedEvent(Object source, Filter[] filters) {
		super(source);
		this.filters = filters;
	}

	/** Gets the added filters.
	 * @return a filter array.
	 */
	public Filter[] getFilters() {
		return filters;
	}
}
