package net.yapbam.data.xml;

import java.util.Map;

import net.yapbam.data.Account;
import net.yapbam.data.Category;
import net.yapbam.data.GlobalData;
import net.yapbam.data.Mode;

class PartialTransaction {
	Account account;
	double amount;
	String description;
	Mode mode;
	Category category;
	String comment;

	PartialTransaction(GlobalData data, Map<String, String> attributes) {
		String accountId = attributes.get(XMLSerializer.ACCOUNT_ATTRIBUTE);
		account = data.getAccount(accountId);
		if (account == null) {
			throw new IllegalArgumentException("Unknown account id : "+accountId); //$NON-NLS-1$
		}
		amount = Double.parseDouble(attributes.get(XMLSerializer.AMOUNT_ATTRIBUTE));
		description = attributes.get(XMLSerializer.DESCRIPTION_ATTRIBUTE);
		comment = attributes.get(XMLSerializer.COMMENT_ATTRIBUTE);
		String modeId = attributes.get(XMLSerializer.MODE_ATTRIBUTE);
		mode = modeId==null ? Mode.UNDEFINED : account.getMode(modeId.trim());
		String categoryId = attributes.get(XMLSerializer.CATEGORY_ATTRIBUTE);
		category = categoryId==null ? Category.UNDEFINED : data.getCategory(categoryId.trim());
	}
}
