package net.yapbam.data.xml;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import net.yapbam.data.*;
import net.yapbam.date.helpers.DateStepper;
import net.yapbam.date.helpers.DayDateStepper;
import net.yapbam.date.helpers.DeferredValueDateComputer;
import net.yapbam.date.helpers.MonthDateStepper;
import net.yapbam.util.DateUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

class GlobalDataHandler extends DefaultHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalDataHandler.class);
	private static final boolean SLOW_READING = Boolean.getBoolean("slowDataReading"); //$NON-NLS-1$
	
	private GlobalData data;
	private ProgressReport report;
	// used to save temporary object data
	private Stack<Object> tempData;
	private int currentProgress;
	
	private Collection<Transaction> transactions;
	private Map<String,String> tagToCData;
	private String currentTag;
	private Locator locator;
	private DelegateHandler delegateHandler;
	private boolean schemaValidation;
	
	//this will be called when XML-parser starts reading
	// XML-data; here we save reference to current position in XML:
	@Override
	public void setDocumentLocator(Locator locator) {
		this.locator = locator;
	}
  
	@Override
	public void error(SAXParseException e) throws SAXException {
		throw e;
	}

	GlobalDataHandler(boolean schemaValidation, ProgressReport report) {
		super();
		this.schemaValidation = schemaValidation;
		this.report = report;
		this.data = new GlobalData();
		this.tempData = new Stack<Object>();
		this.transactions = new ArrayList<Transaction>();
		this.tagToCData = new HashMap<String, String>();
		if (report!=null) {
			report.setMax(-1);
		}
	}
	
	private Map<String, String> buildMap(Attributes attributes) {
		Map<String, String> result = new HashMap<String, String>();
		for (int i=0;i<attributes.getLength();i++) {
			result.put(attributes.getLocalName(i), attributes.getValue(i));
		}
		return result;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if ((report!=null) && report.isCancelled()) {
			throw new ParsingCancelledException();
		}
		if (delegateHandler!=null) {
			delegateHandler.startElement(uri, localName, qName, attributes);
			return;
		}
		if (!this.schemaValidation) {
			// A very basic alternative to schema validation. We just verify root tag is GLOBAL_DATA_TAG and is not duplicated
			// If root tag is not GLOBAL_DATA_TAG, the file is not a Yapbam file
			if ((currentTag==null) && !qName.equals(XMLSerializer.GLOBAL_DATA_TAG)) {
				throw new SAXParseException(XMLSerializer.GLOBAL_DATA_TAG+" expected as root tag", locator);
			}
			// If there's more than one GLOBAL_DATA_TAG there's a problem 
			if ((currentTag!=null) && qName.equals(XMLSerializer.GLOBAL_DATA_TAG)) {
				throw new SAXParseException(XMLSerializer.GLOBAL_DATA_TAG+" expected as root tag", locator);
			}
		}
		this.currentTag = qName;
		if (XMLSerializer.GLOBAL_DATA_TAG.equals(qName)) {
			try {
				if (SLOW_READING) {
					Thread.sleep(1000);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			// Verify that the version is ok
			String dummy = attributes.getValue(XMLSerializer.VERSION_ATTRIBUTE);
			int version = dummy==null?0:Integer.parseInt(dummy);
			if (version>XMLSerializer.CURRENT_VERSION) {
				throw new SaxUnsupportedFileVersionException(locator, version);
			}
			if (report!=null) {
				String attr = attributes.getValue(XMLSerializer.NB_TRANSACTIONS_ATTRIBUTE);
				if (attr!=null) {
					report.setMax(Integer.parseInt(attr));
					this.currentProgress = 0;
					report.reportProgress(this.currentProgress);
				} else {
					// If the file has an old formar, initialize the current progress to a value which mean that progress should stay unchanged (as max was not set)
					this.currentProgress = -1;
				}
			}
			String attr = attributes.getValue(XMLSerializer.SUBCATEGORY_SEPARATOR_ATTRIBUTE);
			if (attr!=null) {
				data.setSubCategorySeparator(attr.charAt(0));
			}
			attr = attributes.getValue(XMLSerializer.LOCKED_ATTRIBUTE);
			if (attr!=null) {
				data.setLocked(true);
			}
			attr = attributes.getValue(XMLSerializer.ARCHIVE_ATTRIBUTE);
			if (attr!=null) {
				data.setArchive(true);
			}
		} else if (qName.equals(XMLSerializer.ACCOUNT_TAG)) {
			String id = attributes.getValue(XMLSerializer.ID_ATTRIBUTE);
			double balance = Double.parseDouble(attributes.getValue(XMLSerializer.INITIAL_BALANCE_ATTRIBUTE));
			Account account = new Account(id, balance);
			this.data.add(account);
			AlertThreshold alertThreshold;
			String lessAttribute = attributes.getValue(XMLSerializer.ALERT_THRESHOLD_LESS);
			if (lessAttribute==null) {
				alertThreshold = AlertThreshold.DEFAULT;
			} else {
				try {
					alertThreshold = new AlertThreshold(Double.parseDouble(lessAttribute), parseDouble(attributes, XMLSerializer.ALERT_THRESHOLD_MORE, Double.POSITIVE_INFINITY));
				} catch (NumberFormatException e) {
					throw new SAXParseException("Expecting double here", locator);
				}
			}
			this.data.setAlertThreshold(account, alertThreshold);
			this.tempData.push(account);
		} else if (qName.equals(XMLSerializer.CATEGORY_TAG)) {
			String id = attributes.getValue(XMLSerializer.ID_ATTRIBUTE).trim();
			if (data.getCategory(id)==null) {
				// In version before 0.9.8, it was possible to create categories ending (or starting) with a space
				// So, it was possible to have two categories named "x" and "x ".
				// Now, the category's name is trimmed, so we have to merge those equivalent categories. 
				Category cat = new Category(id);
				this.data.add(cat);
			}
		} else if (qName.equals(XMLSerializer.MODE_TAG)) {
			this.tempData.push(attributes.getValue(XMLSerializer.ID_ATTRIBUTE));
			this.tempData.push(attributes.getValue(XMLSerializer.CHECKBOOK_ATTRIBUTE)!=null?true:false);
			this.tempData.push(new DateStepper[2]);
		} else if (qName.equals(XMLSerializer.CHECKBOOK_TAG)) {
			String prefix = attributes.getValue(XMLSerializer.PREFIX_ATTRIBUTE);
			BigInteger first = new BigInteger(attributes.getValue(XMLSerializer.FIRST_NUMBER_ATTRIBUTE));
			String value = attributes.getValue(XMLSerializer.NEXT_NUMBER_ATTRIBUTE);
			BigInteger next = value==null?null:new BigInteger(value);
			int size = Integer.parseInt(attributes.getValue(XMLSerializer.SIZE_ATTRIBUTE));
			this.tempData.push (new Checkbook(prefix, first, size, next));
		} else if (qName.equals(XMLSerializer.EXPENSE_VDC_TAG) || qName.equals(XMLSerializer.RECEIPT_VDC_TAG)) {
			DateStepper vdc;
			String kind = attributes.getValue(XMLSerializer.KIND_ATTRIBUTE);
			if ((kind == null) || kind.equals(XMLSerializer.IMMEDIATE_DATE_STEPPER_KIND)) {
				vdc = DateStepper.IMMEDIATE;
			} else if (kind.equals(XMLSerializer.RELATIVE_DATE_STEPPER_KIND)) {
				int delay = Integer.parseInt(attributes.getValue(XMLSerializer.PERIOD_ATTRIBUTE));
				vdc = new DayDateStepper(delay, null);
			} else if (kind.equals(XMLSerializer.DEFERRED_DATE_STEPPER_KIND)) {
				int stopDay = Integer.parseInt(attributes.getValue(XMLSerializer.STOP_DAY_ATTRIBUTE));
				int debtDay = Integer.parseInt(attributes.getValue(XMLSerializer.DEBT_DAY_ATTRIBUTE));
				vdc = new DeferredValueDateComputer(stopDay, debtDay);
			} else {
				throw new RuntimeException("Invalid ValueDateComputer kind : "+kind); //$NON-NLS-1$
			}
			DateStepper[] vdcs = (DateStepper[]) this.tempData.peek();
			int index = qName.equals(XMLSerializer.EXPENSE_VDC_TAG) ? 0 : 1;
			if (vdcs[index]!=null) {
				LOGGER.warn("Too much value date computer"); //$NON-NLS-1$
			}
			vdcs[index] = vdc;
		} else if (qName.equals(XMLSerializer.FILTER_TAG)) {
			delegateHandler = new FilterHandler(data);
			delegateHandler.startElement(uri, localName, qName, attributes);
		} else if (qName.equals(XMLSerializer.TRANSACTION_TAG)) {
			//We can't directly push the attributes because SAX may reuse the same instance to store next element's attributes.
			this.tempData.push(buildMap(attributes));
			// Put a null in the stack. This place will contains a list of subtransactions, if any exists, or null.
			this.tempData.push(null);
		} else if (qName.equals(XMLSerializer.SUBTRANSACTION_TAG)) {
			double amount = Double.parseDouble(attributes.getValue(XMLSerializer.AMOUNT_ATTRIBUTE));
			String description = attributes.getValue(XMLSerializer.DESCRIPTION_ATTRIBUTE);
			String categoryId = attributes.getValue(XMLSerializer.CATEGORY_ATTRIBUTE);
			if (categoryId!=null) {
				categoryId = categoryId.trim();
			}
			Category category = this.data.getCategory(categoryId);
			SubTransaction sub = new SubTransaction(amount, description, category);
			List<SubTransaction> lst = (ArrayList<SubTransaction>) this.tempData.peek();
			if (lst==null) {
				// If no subtransactions were already found, create the subtransactions list
				lst = new ArrayList<SubTransaction>();
				this.tempData.pop();
				this.tempData.push(lst);
			}
			lst.add(sub);
		} else if (qName.equals(XMLSerializer.PERIODICAL_TAG)) {
			//We can't directly push the attributes because SAX may reuse the same instance to store next element's attributes.
			this.tempData.push(buildMap(attributes));
			// Reserve a place in the stack to store the date stepper
			this.tempData.push(null);
			this.tempData.push(new ArrayList<SubTransaction>());
		} else if (qName.equals(XMLSerializer.DATE_STEPPER_TAG)) {
			String kind = attributes.getValue(XMLSerializer.KIND_ATTRIBUTE);
			DateStepper stepper;
			if (kind.equals(XMLSerializer.MONTHLY_DATE_STEPPER_KIND)) {
				int period = Integer.parseInt(attributes.getValue(XMLSerializer.PERIOD_ATTRIBUTE));
				if (period<=0) {
					throw new IllegalArgumentException();
				}
				int day = Integer.parseInt(attributes.getValue(XMLSerializer.DAY_ATTRIBUTE));
				String dummy =  attributes.getValue(XMLSerializer.LAST_DATE_ATTRIBUTE);
				Date lastDate = dummy==null?null:DateUtils.integerToDate(XMLSerializer.toDate(dummy));
				stepper = new MonthDateStepper(period, day, lastDate);
			} else if (kind.equals(XMLSerializer.RELATIVE_DATE_STEPPER_KIND)) {
				int period = Integer.parseInt(attributes.getValue(XMLSerializer.PERIOD_ATTRIBUTE));
				String dummy =  attributes.getValue(XMLSerializer.LAST_DATE_ATTRIBUTE);
				Date lastDate = dummy==null?null:DateUtils.integerToDate(XMLSerializer.toDate(dummy));
				stepper = new DayDateStepper(period, lastDate);
			} else {
				throw new IllegalArgumentException("Unknown date stepper : "+kind); //$NON-NLS-1$
			}
			// The subtransaction list, will be returned in the stack in a few lines
			Object obj = this.tempData.pop();
			Object old = this.tempData.pop();
			this.tempData.push(stepper);
			this.tempData.push(obj);
			if (old!=null) {
				// Hu ! there are two date steppers !!!
				throw new IllegalStateException("Two date steppers found"); //$NON-NLS-1$
			}
		} else {
			// Simply ignore unknown tags (Maybe we're using a previous Yapbam version)
			LOGGER.warn("Unknown tag {}", qName); //$NON-NLS-1$
		}
	}

	private double parseDouble(Attributes attributes, String attrName, double defaultValue) throws SAXParseException {
		try {
			// WARNING: xml schema allows string attributes (instead of double), because INFINITY is a valid
			// value and schema verification fails on such values in double attributes.
			// So, we have to verify here that attribute really contains a double.
			String value = attributes.getValue(attrName);
			return value==null?defaultValue:Double.parseDouble(value);
		} catch (NumberFormatException e) {
			throw new SAXParseException("Expecting double here", locator);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if ((report!=null) && report.isCancelled()) {
			throw new ParsingCancelledException();
		}
		if (delegateHandler!=null) {
			delegateHandler.endElement(uri, localName, qName);
			if (delegateHandler.isEndTag(qName)) {
				if (XMLSerializer.FILTER_TAG.equals(qName)) {
					data.add(((FilterHandler)delegateHandler).getFilter());
				}
				delegateHandler = null;
			}
			return;
		}
		if (qName.equals(XMLSerializer.GLOBAL_DATA_TAG)) {
			// NOTE: The following line took a very long time before being greatly optimized.
			// This was a problem because this extra time occurred at parsing end and was not reported to the ProgressReport
			// So, on slow devices, the parsing seemed to pause some seconds after the end of parsing :-(
			// Be aware that further modifications in the following method wause make the prblem to occur again.
			this.data.add(this.transactions.toArray(new Transaction[this.transactions.size()]));
		} else if (qName.equals(XMLSerializer.ACCOUNT_TAG)) {
			// remove the tag we added in the stack
			Account account = (Account) this.tempData.pop();
			String lastCData = this.tagToCData.get(qName);
			if (lastCData!=null) {
				this.data.setComment(account, lastCData);
				this.tagToCData.remove(qName);
			}
		} else if (qName.equals(XMLSerializer.MODE_TAG)) {
			DateStepper[] vdcs = (DateStepper[]) this.tempData.pop();
			boolean useCheckbook = (Boolean) this.tempData.pop();
			String id = (String) this.tempData.pop();
			id = id.trim();
			Account account = (Account) this.tempData.peek();
			if (account.getMode(id)==null) {
				// In Yapbam versions before 0.9.8, it was possible to create modes ending (or starting) with a space
				// So, it was possible to have two modes named "x" and "x ".
				// Now, the mode's name is trimed, so we have to merge those equivalent modes.
				// In order to keep the code simple, we will not really merge the modes, but ignore the last one.
				Mode mode = new Mode(id, vdcs[1], vdcs[0], useCheckbook);
				this.data.add(account, mode);
			}
		} else if (qName.equals(XMLSerializer.CHECKBOOK_TAG)) {
			Checkbook book = (Checkbook) this.tempData.pop();
			Account account = (Account) this.tempData.peek();
			this.data.add(account, book);
		} else if (qName.equals(XMLSerializer.TRANSACTION_TAG)) {
			List<SubTransaction> lst = (List<SubTransaction>) this.tempData.pop();
			Map<String, String> attributes = (Map<String, String>) this.tempData.pop();
			PartialTransaction p = new PartialTransaction(this.data, attributes);		
			int date = XMLSerializer.toDate(attributes.get(XMLSerializer.DATE_ATTRIBUTE));
			String number = attributes.get(XMLSerializer.NUMBER_ATTRIBUTE);
			int valueDate = XMLSerializer.toDate(attributes.get(XMLSerializer.VALUE_DATE_ATTRIBUTE));
			String statement = attributes.get(XMLSerializer.STATEMENT_ATTRIBUTE);
			this.transactions.add(new Transaction(date, number, p.description, p.comment, p.amount, p.account, p.mode, p.category, valueDate, statement, lst));
			if (report!=null) {
				if (this.currentProgress>=0) {
					this.currentProgress++;
					report.reportProgress(currentProgress);
				}
				if (SLOW_READING) {
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						// Do nothing
						Thread.currentThread().interrupt();
					}
				}
			}
		} else if (qName.equals(XMLSerializer.PERIODICAL_TAG)) {
			ArrayList<SubTransaction> lst = (ArrayList<SubTransaction>) this.tempData.pop();
			DateStepper stepper = (DateStepper) this.tempData.pop();
			Map<String, String> attributes = (Map<String, String>) this.tempData.pop();
			PartialTransaction p = new PartialTransaction(this.data, attributes);
			String attribute = attributes.get(XMLSerializer.NEXT_DATE_ATTRIBUTE);
			Date nextDate = attribute==null?null:DateUtils.integerToDate(XMLSerializer.toDate(attribute));
			boolean enabled = Boolean.parseBoolean(attributes.get(XMLSerializer.ENABLED_ATTRIBUTE));
			// In previous Yapbam versions, next date could be after end date. Now, it would launch an IllegalArgumentException
			if (nextDate!=null && stepper!=null && stepper.getLastDate()!=null && stepper.getLastDate().compareTo(nextDate)<0) {
				// If next date is after end
				// Set the next date to "no next date"
				nextDate = null;
			}
			// In previous Yapbam versions, next date could also be null on enabled periodical transactions
			// Now, it would launch an IllegalArgumentException
			if (nextDate==null) {
				enabled = false;
			}
			this.data.add(new PeriodicalTransaction(p.description, p.comment, p.amount, p.account, p.mode, p.category, lst, nextDate, enabled, stepper));
		} else if (qName.equals(XMLSerializer.CATEGORY_TAG) || qName.equals(XMLSerializer.EXPENSE_VDC_TAG) ||
				qName.equals(XMLSerializer.RECEIPT_VDC_TAG) || qName.equals(XMLSerializer.SUBTRANSACTION_TAG) ||
				qName.equals(XMLSerializer.DATE_STEPPER_TAG)) {
			// Nothing to do when closing these tags (evrything is done in startElement)
		} else {
			// Simply ignore unknown tags. Maybe we're using a previous Yapbam version
			// The startElement method is in charge of logging these tags
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		String str = new String(ch, start, length).trim();
		if (str.length()>0) {
			this.tagToCData.put(currentTag, str);
		}
	}

	@Override
	public void startDocument() throws SAXException {
		this.data.clear();
		super.startDocument();
	}
	
	public GlobalData getData() {
		return this.data;
	}
}
