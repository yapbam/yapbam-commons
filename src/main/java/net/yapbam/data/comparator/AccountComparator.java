package net.yapbam.data.comparator;

import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

import net.yapbam.data.Account;
import net.yapbam.data.GlobalData;

/** A comparator used to compare sort account names.
 * <br>Account sorting in this class used a java.text.Collator in order to deal with case and diacritical marks.
 */
public abstract class AccountComparator {
	private static final Object LOCK = new Object(); 
	private static Locale LOCALE;
	private static Comparator<Account> INSTANCE;

	private AccountComparator() {
		// Just to prevent instantiation
	}

	/** Gets the accounts sorted accordingly to a locale. 
	 * @param data The global Yapbam data
	 * @param locale the locale
	 * @return An array of the accounts, sorted accordingly to the java.text.collator of this locale.
	 */
	public static Account[] getSortedAccounts(final GlobalData data, final Locale locale) {
		Account[] accounts = new Account[data.getAccountsNumber()];
		for (int i = 0; i < accounts.length; i++) {
			accounts[i] = data.getAccount(i);
		}
		Arrays.sort(accounts, getInstance(locale));
		return accounts;
	}
	
	/** Gets a comparator that compares accounts accordingly to a localized collator. 
	 * @param locale the locale
	 * @return An account comparator.
	 */
	public static Comparator<Account> getInstance(final Locale locale) {
		synchronized (LOCK) {
			if (!locale.equals(LOCALE)) {
				INSTANCE = new Comparator<Account>() {
					Collator c = Collator.getInstance(locale);
					@Override
					public int compare(Account o1, Account o2) {
						return c.compare(o1.getName(), o2.getName());
					}
				};
				LOCALE = locale;
			}
			return INSTANCE;
		}
	}
}
