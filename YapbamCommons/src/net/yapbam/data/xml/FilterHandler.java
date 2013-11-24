package net.yapbam.data.xml;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.yapbam.data.*;
import net.yapbam.util.ArrayUtils;
import net.yapbam.util.DateUtils;
import net.yapbam.util.TextMatcher;
import net.yapbam.util.TextMatcher.Kind;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class FilterHandler extends DefaultHandler {
	private GlobalData data;
	private Filter filter;
	private TextMatcher descriptionMatcher;
	private TextMatcher commentMatcher;
	private TextMatcher numberMatcher;
	private TextMatcher statementMatcher;
	private int property;
	

	public FilterHandler(GlobalData data) {
		super();
		this.data = data;
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (qName.equals(XMLSerializer.FILTER_TAG)) {
			filter = new Filter();
			Date dateFrom = DateUtils.integerToDate(XMLSerializer.toDate(attributes.getValue(XMLSerializer.FILTER_DATE_FROM_ATTRIBUTE)));
			Date dateTo = DateUtils.integerToDate(XMLSerializer.toDate(attributes.getValue(XMLSerializer.FILTER_DATE_TO_ATTRIBUTE)));
			filter.setDateFilter(dateFrom, dateTo);
			Date valueDateFrom = DateUtils.integerToDate(XMLSerializer.toDate(attributes.getValue(XMLSerializer.FILTER_VALUE_DATE_FROM_ATTRIBUTE)));
			Date valueDateTo = DateUtils.integerToDate(XMLSerializer.toDate(attributes.getValue(XMLSerializer.FILTER_VALUE_DATE_TO_ATTRIBUTE)));
			filter.setValueDateFilter(valueDateFrom, valueDateTo);
			String amountFrom = attributes.getValue(XMLSerializer.FILTER_AMOUNT_FROM_ATTRIBUTE);
			String amountTo = attributes.getValue(XMLSerializer.FILTER_AMOUNT_TO_ATTRIBUTE);
			String filterString = attributes.getValue(XMLSerializer.FILTER_ATTRIBUTE);
			property = filterString==null?Filter.ALL:Integer.parseInt(filterString);
			filter.setAmountFilter(property, amountFrom==null?0.0:Double.parseDouble(amountFrom), amountFrom==null?Double.POSITIVE_INFINITY:Double.parseDouble(amountTo));
			{
				String accountsString = attributes.getValue(XMLSerializer.ACCOUNT_ATTRIBUTE);
				if (accountsString!= null) {
					String[] names = ArrayUtils.parseStringArray(accountsString);
					ArrayList<Account> accounts = new ArrayList<Account>();
					for (String name: names) {
						Account account = data.getAccount(name);
						if (account != null) accounts.add(account);
					}
					if (!accounts.isEmpty()) {
						filter.setValidAccounts(accounts);
					}
				}
			} {
				String categoriesString = attributes.getValue(XMLSerializer.CATEGORY_ATTRIBUTE);
				if (categoriesString!= null) {
					String[] names = ArrayUtils.parseStringArray(categoriesString);
					ArrayList<Category> categories = new ArrayList<Category>();
					for (String name: names) {
						name = name.trim();
						Category category = name.isEmpty()?Category.UNDEFINED:data.getCategory(name);
						if (category != null) categories.add(category);
					}
					if (!categories.isEmpty() && (categories.size()!=data.getCategoriesNumber())) {
						filter.setValidCategories(categories);
					}
				}
			} {
				String modesString = attributes.getValue(XMLSerializer.MODE_ATTRIBUTE);
				if (modesString!= null) {
					String[] names = ArrayUtils.parseStringArray(modesString);
					Set<String> dataNames = getAllValidAccountsModeNames();
					ArrayList<String> modes = new ArrayList<String>();
					for (String name: names) {
						name = name.trim();
						if (dataNames.contains(name)) modes.add(name);
					}
					if (!modes.isEmpty() && (modes.size()!=dataNames.size())) {
						filter.setValidModes(modes);
					}
				}
			}
		} else if (qName.equals(XMLSerializer.TEXT_MATCHER_TAG)) {
			String id = attributes.getValue(XMLSerializer.ID_ATTRIBUTE);
			String kindString = attributes.getValue(XMLSerializer.KIND_ATTRIBUTE);
			Kind kind = null;
			if (kindString.equals(XMLSerializer.CONTAINS)) {
				kind = TextMatcher.Kind.CONTAINS;
			} else if (kindString.equals(XMLSerializer.EQUALS)) {
				kind = TextMatcher.Kind.EQUALS;
			} else if (kindString.equals(XMLSerializer.REGULAR)) {
				kind = TextMatcher.Kind.REGULAR;
			}
			String filter = XMLSerializer.decode(attributes.getValue(XMLSerializer.FILTER_ATTRIBUTE));
			String bString = attributes.getValue(XMLSerializer.CASE_SENSITIVE_ATTRIBUTE);
			boolean caseSensitive = bString==null?false:Boolean.parseBoolean(bString);
			bString = attributes.getValue(XMLSerializer.DIACRITICAL_SENSITIVE_ATTRIBUTE);
			boolean diacriticalSensitive = bString==null?false:Boolean.parseBoolean(bString);
			TextMatcher textMatcher = new TextMatcher(kind, filter, caseSensitive, diacriticalSensitive);
			if (id.equals(XMLSerializer.FILTER_DESCRIPTION_ID)) {
				this.descriptionMatcher = textMatcher;
			} else if (id.equals(XMLSerializer.FILTER_COMMENT_ID)) {
				this.commentMatcher = textMatcher;
			} else if (id.equals(XMLSerializer.FILTER_NUMBER_ID)) {
				this.numberMatcher = textMatcher;
			} else if (id.equals(XMLSerializer.FILTER_STATEMENT_ID)) {
				this.statementMatcher = textMatcher;
			}
		} else {
			throw new IllegalArgumentException ("Unknown tag "+qName); //$NON-NLS-1$
		}
	}
	
	private Set<String> getAllValidAccountsModeNames() {
		HashSet<String> result = new HashSet<String>();
		List<Account> accounts = filter.getValidAccounts();
		if (accounts==null) {
			accounts = new ArrayList<Account>(data.getAccountsNumber());
			for (int i = 0; i < data.getAccountsNumber(); i++) {
				accounts.add(data.getAccount(i));
			}
		}
		for (Account account:accounts) {
			for (int i = 0; i < account.getModesNumber(); i++) {
				Mode mode = account.getMode(i);
				result.add(mode.equals(Mode.UNDEFINED)?"":mode.getName()); //$NON-NLS-1$
			}
		}
		return result;
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (qName.equals(XMLSerializer.FILTER_TAG)) {
			filter.setDescriptionMatcher(this.descriptionMatcher);
			filter.setCommentMatcher(this.commentMatcher);
			filter.setNumberMatcher(this.numberMatcher);
			filter.setStatementFilter(property, this.statementMatcher);
		} else if (qName.equals(XMLSerializer.TEXT_MATCHER_TAG)) {
		} else {
			System.err.println ("Unknown tag "+qName); //$NON-NLS-1$
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		String str = new String(ch, start, length);
		str = str.trim();
		if (str.length()!=0) {
			System.err.println ("strange, characters is called : "+str); //$NON-NLS-1$
		}
	}

	public Filter getFilter() {
		return filter;
	}
}
