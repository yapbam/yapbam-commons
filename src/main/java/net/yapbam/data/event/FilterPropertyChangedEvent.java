package net.yapbam.data.event;

import net.yapbam.data.Filter;

public class FilterPropertyChangedEvent extends DataEvent {
	public static final String UNSPECIFIED = "unspecified"; //$NON-NLS-1$
	
	private Filter category;

	public FilterPropertyChangedEvent(Object source, Filter category) {
		super (source);
		this.category = category;
	}

	public Filter getFilter() {
		return category;
	}

	public String getProperty() {
		return UNSPECIFIED;
	}

	public Object getOldValue() {
		return null;
	}

	public Object getNewValue() {
		return null;
	}
}
