import java.net.Proxy;

import net.yapbam.currency.CurrencyConverter;


public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			System.out.println ("INIT");
			CurrencyConverter converter = new CurrencyConverter(Proxy.NO_PROXY, null) {
				/* (non-Javadoc)
				 * @see net.yapbam.currency.CurrencyConverter#log(java.lang.String)
				 */
				@Override
				protected void log(String message) {
					System.out.println(message);
				}
			};
			System.out.println ("After instanciation: "+converter.getReferenceDate()+" / "+converter.isSynchronized());
			System.out.println ("--------------------");
			System.out.println ("FORCE UPDATE");
			converter.forceUpdate();
			System.out.println ("After forceUpdate: "+converter.getReferenceDate()+" / "+converter.isSynchronized());
			System.out.println ("--------------------");
			System.out.println ("UPDATE");
			converter.update();
			System.out.println ("After update: "+converter.getReferenceDate()+" / "+converter.isSynchronized());
			System.out.println ("--------------------");
			System.out.println ("UPDATE");
			converter.update();
			System.out.println ("After second update: "+converter.getReferenceDate()+" / "+converter.isSynchronized());
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

}
