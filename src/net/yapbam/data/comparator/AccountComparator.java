package net.yapbam.data.comparator;

import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

import net.yapbam.data.Account;
import net.yapbam.data.GlobalData;

public abstract class AccountComparator {

	private AccountComparator() {
		// Just to prevent instantiation
	}

	public static Account[] getSortedAccounts(final GlobalData data, final Locale locale) {
		Account[] accounts = new Account[data.getAccountsNumber()];
		for (int i = 0; i < accounts.length; i++) {
			accounts[i] = data.getAccount(i);
		}
		Arrays.sort(accounts, new Comparator<Account>() {
			Collator c = Collator.getInstance(locale);
			@Override
			public int compare(Account o1, Account o2) {
				return c.compare(o1.getName(), o2.getName());
			}
		});
		return accounts;
	}
}
