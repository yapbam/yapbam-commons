package net.yapbam.currency;

import java.util.Observable;
import java.util.Observer;

public final class SpyObserver implements Observer {
	private boolean wasCalled;
	
	public SpyObserver() {
		this.wasCalled = false;
	}

	@Override
	public void update(Observable o, Object arg) {
		wasCalled = true;
	}

	public boolean wasCalled() {
		return wasCalled;
	}
}