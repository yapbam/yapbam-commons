package net.yapbam.data.xml;

import java.io.*;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import net.yapbam.data.*;
import net.yapbam.date.helpers.DateStepper;
import net.yapbam.date.helpers.DayDateStepper;
import net.yapbam.date.helpers.DeferredValueDateComputer;
import net.yapbam.date.helpers.MonthDateStepper;
import net.yapbam.util.ArrayUtils;
import net.yapbam.util.DateUtils;
import net.yapbam.util.TextMatcher;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.sax.*;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

/** The class implements xml yapbam data serialization and deserialization to (or from) a stream.
 */
public class XMLSerializer {
	public static final String UTF8 = "UTF-8"; //$NON-NLS-1$

	//TODO Not sure the xml schema validation allows to read all the previously saved file: Disable it by default
	// This should be investigate by testing with real life data: A interesting way is to randomly test every
	// Successfully read data and try it with validation. If it fails, silently post a message to a web server. 
	private static final boolean SCHEMA_VALIDATION = Boolean.getBoolean("xml.schema.validation"); //$NON-NLS-1$
	private static final boolean SLOW_WRITING = Boolean.getBoolean("slowDataWriting"); //$NON-NLS-1$

	private static final String EMPTY = ""; //$NON-NLS-1$
	private static final String CDATA = "CDATA"; //$NON-NLS-1$
	private static final String DATE_DELIM = "/"; //$NON-NLS-1$
	private static final String TRUE = "true"; //$NON-NLS-1$
	
	/** The current Yapbam file format definition version.
	 * <br>This version should be increment each time a change is made to the input format definition. 
	 */
	static final int CURRENT_VERSION = 1;
	static final String VERSION_ATTRIBUTE = "version"; //$NON-NLS-1$

	static final String SUBCATEGORY_SEPARATOR_ATTRIBUTE = "subCategorySeparator"; //$NON-NLS-1$
	static final String NB_TRANSACTIONS_ATTRIBUTE = "nbTransactions"; //$NON-NLS-1$
	static final String STATEMENT_ATTRIBUTE = "statement"; //$NON-NLS-1$
	static final String VALUE_DATE_ATTRIBUTE = "valueDate"; //$NON-NLS-1$
	static final String CATEGORY_ATTRIBUTE = "category"; //$NON-NLS-1$
	static final String NUMBER_ATTRIBUTE = "number"; //$NON-NLS-1$
	static final String MODE_ATTRIBUTE = "mode"; //$NON-NLS-1$
	static final String AMOUNT_ATTRIBUTE = "amount"; //$NON-NLS-1$
	static final String DATE_ATTRIBUTE = "date"; //$NON-NLS-1$
	static final String DESCRIPTION_ATTRIBUTE = "description"; //$NON-NLS-1$
	static final String COMMENT_ATTRIBUTE = "comment"; //$NON-NLS-1$
	static final String ACCOUNT_ATTRIBUTE = "account"; //$NON-NLS-1$

	static final String DEBT_DAY_ATTRIBUTE = "debtDay"; //$NON-NLS-1$
	static final String STOP_DAY_ATTRIBUTE = "stopDay"; //$NON-NLS-1$
	static final String DAY_ATTRIBUTE = "day"; //$NON-NLS-1$
	static final String PERIOD_ATTRIBUTE = "period"; //$NON-NLS-1$
	static final String KIND_ATTRIBUTE = "kind"; //$NON-NLS-1$
	static final String IMMEDIATE_DATE_STEPPER_KIND = "immediate"; //$NON-NLS-1$
	static final String MONTHLY_DATE_STEPPER_KIND = "monthly"; //$NON-NLS-1$
	static final String DEFERRED_DATE_STEPPER_KIND = "deferred"; //$NON-NLS-1$
	static final String RELATIVE_DATE_STEPPER_KIND = "daily"; //$NON-NLS-1$
	static final String CHECKBOOK_ATTRIBUTE = "checkbook"; //$NON-NLS-1$
	
	static final String PREFIX_ATTRIBUTE = "prefix"; //$NON-NLS-1$
	static final String FIRST_NUMBER_ATTRIBUTE = "first"; //$NON-NLS-1$
	static final String SIZE_ATTRIBUTE = "size"; //$NON-NLS-1$
	static final String NEXT_NUMBER_ATTRIBUTE = "next"; //$NON-NLS-1$

	static final String INITIAL_BALANCE_ATTRIBUTE = "initialBalance"; //$NON-NLS-1$
	static final String ALERT_THRESHOLD_LESS = "alertThresholdLess"; //$NON-NLS-1$
	static final String ALERT_THRESHOLD_MORE = "alertThresholdMore"; //$NON-NLS-1$
	static final String ALERT_IGNORE = "no"; //$NON-NLS-1$
	static final String ID_ATTRIBUTE = "id"; //$NON-NLS-1$
	static final String NEXT_DATE_ATTRIBUTE = "next"; //$NON-NLS-1$
	static final String LAST_DATE_ATTRIBUTE = "last"; //$NON-NLS-1$
	static final String ENABLED_ATTRIBUTE = "enabled"; //$NON-NLS-1$

	static final String GLOBAL_DATA_TAG = "DATA"; //$NON-NLS-1$
	static final String CATEGORY_TAG = "CATEGORY"; //$NON-NLS-1$
	static final String ACCOUNT_TAG = "ACCOUNT"; //$NON-NLS-1$
	static final String MODE_TAG = "MODE"; //$NON-NLS-1$
	static final String CHECKBOOK_TAG = "CHECKBOOK"; //$NON-NLS-1$
	static final String EXPENSE_VDC_TAG = "EXPENSE"; //$NON-NLS-1$
	static final String RECEIPT_VDC_TAG = "RECEIPT"; //$NON-NLS-1$
	static final String PERIODICAL_TAG = "PERIODICAL"; //$NON-NLS-1$
	static final String DATE_STEPPER_TAG = "DATE_STEPPER"; //$NON-NLS-1$
	static final String TRANSACTION_TAG = "TRANSACTION"; //$NON-NLS-1$
	static final String SUBTRANSACTION_TAG = "SUBTRANSACTION"; //$NON-NLS-1$
	
	static final String FILTER_TAG = "FILTER"; //$NON-NLS-1$
	static final String FILTER_DATE_FROM_ATTRIBUTE = "dateFrom"; //$NON-NLS-1$
	static final String FILTER_DATE_TO_ATTRIBUTE = "dateTo"; //$NON-NLS-1$
	static final String FILTER_VALUE_DATE_FROM_ATTRIBUTE = "valueDateFrom"; //$NON-NLS-1$
	static final String FILTER_VALUE_DATE_TO_ATTRIBUTE = "valueDateTo"; //$NON-NLS-1$
	static final String FILTER_AMOUNT_FROM_ATTRIBUTE = "amountFrom"; //$NON-NLS-1$
	static final String FILTER_AMOUNT_TO_ATTRIBUTE = "amountTo"; //$NON-NLS-1$
	static final String FILTER_ATTRIBUTE = "filter"; //$NON-NLS-1$
	static final String FILTER_DESCRIPTION_ID = DESCRIPTION_ATTRIBUTE;
	static final String FILTER_COMMENT_ID = COMMENT_ATTRIBUTE;
	static final String FILTER_NUMBER_ID = NUMBER_ATTRIBUTE;
	static final String FILTER_STATEMENT_ID = STATEMENT_ATTRIBUTE;

	static final String TEXT_MATCHER_TAG = "TEXT_MATCHER"; //$NON-NLS-1$
	static final String CONTAINS = "contains"; //$NON-NLS-1$
	static final String EQUALS = "equals"; //$NON-NLS-1$
	static final String REGULAR = "regular"; //$NON-NLS-1$
	static final String DIACRITICAL_SENSITIVE_ATTRIBUTE = "diacriticalSensitive"; //$NON-NLS-1$
	static final String CASE_SENSITIVE_ATTRIBUTE = "caseSensitive"; //$NON-NLS-1$

	private AttributesImpl atts;
	private TransformerHandler hd;
	private OutputStream os;
	
	/** Creates a new XML Serializer.
	 * <br>The serializer outputs the xml header. After all elements are output, you should call closedocument in order
	 * to close the xml document
	 * @param os The output stream on which to write the xml document
	 * @throws IOException if something wrong happens
	 * @see #closeDocument()
	 */
	public XMLSerializer (OutputStream os) throws IOException {
		try {
			this.os = os;
			StreamResult streamResult = new StreamResult(this.os);
			SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
			hd = tf.newTransformerHandler();
			Transformer serializer = hd.getTransformer();
			serializer.setOutputProperty(OutputKeys.ENCODING, UTF8);
			serializer.setOutputProperty(OutputKeys.INDENT,"yes"); //$NON-NLS-1$
			hd.setResult(streamResult);
			this.atts = new AttributesImpl();
			hd.startDocument();
		} catch (TransformerConfigurationException e) {
			throw new IOException(e);
		} catch (SAXException e) {
			throw new IOException(e);
		}
	}
	
	public void closeDocument() throws IOException {
		try {
			hd.endDocument();
		} catch (SAXException e) {
			throw new IOException(e);
		}
	}
	
	/** Reads global data.
	 * @param in The input stream containing the data
	 * @param report A progress report to observe the progress, or null
	 * @return The data red.
	 * @throws IOException If something goes wrong while reading
	 * @throws UnsupportedFormatException If the format of data in the input stream is not supported
	 */
	public static GlobalData read(InputStream in, ProgressReport report) throws IOException {
		GlobalDataHandler dh = new GlobalDataHandler(SCHEMA_VALIDATION, report);
		try {
			SAXParserFactory saxFactory = SAXParserFactory.newInstance();
			if (SCHEMA_VALIDATION) {
				SchemaFactory schemaFactory = SchemaFactory .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
				Schema schema = schemaFactory.newSchema(XMLSerializer.class.getResource("yapbam.xsd")); //$NON-NLS-1$
				saxFactory.setSchema(schema);
			}
			try {
				saxFactory.newSAXParser().parse(in, dh);
			} catch (RuntimeException e) {
				if (SCHEMA_VALIDATION) {
					throw e;
				} else {
					throw new UnsupportedFormatException(e);
				}
			}
		} catch (SaxUnsupportedFileVersionException e) {
			throw new UnsupportedFileVersionException(Integer.toString(e.getVersion()));
		} catch (SAXParseException e) {
			// The format is invalid
			throw new UnsupportedFormatException(e);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		}
		GlobalData data = dh.getData();
		return data;
	}

	public void serialize (GlobalData data, ProgressReport report) throws IOException {
		try {
			atts.clear();
			atts.addAttribute(EMPTY, EMPTY, VERSION_ATTRIBUTE, CDATA, Integer.toString(CURRENT_VERSION)); //$NON-NLS-1$
			atts.addAttribute(EMPTY, EMPTY, "nbAccounts", CDATA, Integer.toString(data.getAccountsNumber())); //$NON-NLS-1$
			atts.addAttribute(EMPTY, EMPTY, "nbCategories", CDATA, Integer.toString(data.getCategoriesNumber())); //$NON-NLS-1$
			atts.addAttribute(EMPTY, EMPTY, SUBCATEGORY_SEPARATOR_ATTRIBUTE, CDATA, Character.toString(data.getSubCategorySeparator())); //$NON-NLS-1$
			atts.addAttribute(EMPTY, EMPTY, "nbPeriodicalTransactions", CDATA, Integer.toString(data.getPeriodicalTransactionsNumber())); //$NON-NLS-1$
			atts.addAttribute(EMPTY, EMPTY, NB_TRANSACTIONS_ATTRIBUTE, CDATA, Integer.toString(data.getTransactionsNumber()));
			hd.startElement(EMPTY,EMPTY,GLOBAL_DATA_TAG,atts);
			
			// Accounts.
			for (int i=0;i<data.getAccountsNumber();i++)
			{
				serialize(data.getAccount(i));
			}
			// Categories
			for (int i=0;i<data.getCategoriesNumber();i++)
			{
				serialize(data.getCategory(i));
			}
			// Periodical transactions
			for (int i = 0; i < data.getPeriodicalTransactionsNumber(); i++) {
				serialize(data.getPeriodicalTransaction(i));
			}
			if (report!=null) report.setMax(data.getTransactionsNumber());
			//Transactions
			for (int i=0;i<data.getTransactionsNumber();i++) {
				if (SLOW_WRITING) {
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {}
				}
				serialize(data.getTransaction(i));
				if (report!=null) report.reportProgress(i+1);
			}
			hd.endElement(EMPTY,EMPTY,GLOBAL_DATA_TAG);
		} catch (SAXException e) {
			throw new IOException(e);
		}
	}
	
	public void serialize(Filter filter) throws SAXException {
		atts.clear();
		if (filter.getDateFrom()!=null) atts.addAttribute(EMPTY,EMPTY,FILTER_DATE_FROM_ATTRIBUTE,CDATA,toString(filter.getDateFrom()));
		if (filter.getDateTo()!=null) atts.addAttribute(EMPTY,EMPTY,FILTER_DATE_TO_ATTRIBUTE,CDATA,toString(filter.getDateTo()));
		if (filter.getValueDateTo()!=null) atts.addAttribute(EMPTY,EMPTY,FILTER_VALUE_DATE_TO_ATTRIBUTE,CDATA,toString(filter.getValueDateTo()));
		if (filter.getValueDateFrom()!=null) atts.addAttribute(EMPTY,EMPTY,FILTER_VALUE_DATE_FROM_ATTRIBUTE,CDATA,toString(filter.getValueDateFrom()));
		if (filter.getMinAmount()!=0.0) atts.addAttribute(EMPTY,EMPTY,FILTER_AMOUNT_FROM_ATTRIBUTE,CDATA,Double.toString(filter.getMinAmount()));
		if (filter.getMaxAmount()!=Double.POSITIVE_INFINITY) atts.addAttribute(EMPTY,EMPTY,FILTER_AMOUNT_TO_ATTRIBUTE,CDATA,Double.toString(filter.getMaxAmount()));
		List<Account> accounts = filter.getValidAccounts();
		if (accounts!=null) {
			String[] strings = new String[accounts.size()];
			for (int i = 0; i < strings.length; i++) {
				strings[i] = accounts.get(i).getName();
			}
			atts.addAttribute(EMPTY, EMPTY, ACCOUNT_ATTRIBUTE, CDATA, ArrayUtils.toString(strings));
		}
		List<String> modes = filter.getValidModes();
		if (modes!=null) {
			atts.addAttribute(EMPTY, EMPTY, MODE_ATTRIBUTE, CDATA, ArrayUtils.toString(modes.toArray(new String[modes.size()])));
		}
		List<Category> categories = filter.getValidCategories();
		if (categories!=null) {
			String[] strings = new String[categories.size()];
			for (int i = 0; i < strings.length; i++) {
				strings[i] = categories.get(i).equals(Category.UNDEFINED)?EMPTY:categories.get(i).getName();
			}
			atts.addAttribute(EMPTY, EMPTY, CATEGORY_ATTRIBUTE, CDATA, ArrayUtils.toString(strings));
		}
		int mask = 0;
		if (filter.isOk(Filter.RECEIPTS)) mask += Filter.RECEIPTS;
		if (filter.isOk(Filter.EXPENSES)) mask += Filter.EXPENSES;
		if (filter.isOk(Filter.CHECKED)) mask += Filter.CHECKED;
		if (filter.isOk(Filter.NOT_CHECKED)) mask += Filter.NOT_CHECKED;
		if (mask!=(Filter.ALL)) atts.addAttribute(EMPTY, EMPTY, FILTER_ATTRIBUTE, CDATA, Integer.toString(mask));
		hd.startElement(EMPTY, EMPTY, FILTER_TAG, atts);
		if (filter.getDescriptionMatcher()!=null) serialize(filter.getDescriptionMatcher(), FILTER_DESCRIPTION_ID);
		if (filter.getCommentMatcher()!=null) serialize(filter.getCommentMatcher(), FILTER_COMMENT_ID);
		if (filter.getNumberMatcher()!=null) serialize(filter.getNumberMatcher(), FILTER_NUMBER_ID);
		if (filter.getStatementMatcher()!=null) serialize(filter.getStatementMatcher(), FILTER_STATEMENT_ID);
		hd.endElement(EMPTY,EMPTY,FILTER_TAG);
	}

	private void serialize(TextMatcher matcher, String id) throws SAXException {
		atts.clear();
		atts.addAttribute(EMPTY, EMPTY, ID_ATTRIBUTE, CDATA, id);
		String kind = null;
		if (matcher.getKind().equals(TextMatcher.Kind.CONTAINS)) {
			kind = CONTAINS;
		} else if (matcher.getKind().equals(TextMatcher.Kind.EQUALS)) {
			kind = EQUALS;
		} else if (matcher.getKind().equals(TextMatcher.Kind.REGULAR)) {
			kind = REGULAR;
		} else {
			throw new IllegalArgumentException();
		}
		atts.addAttribute(EMPTY, EMPTY, KIND_ATTRIBUTE, CDATA, kind);
		atts.addAttribute(EMPTY, EMPTY, FILTER_ATTRIBUTE, CDATA, encode(matcher.getFilter()));
		if (matcher.isCaseSensitive()) atts.addAttribute(EMPTY, EMPTY, CASE_SENSITIVE_ATTRIBUTE, CDATA, TRUE);
		if (matcher.isDiacriticalSensitive()) atts.addAttribute(EMPTY, EMPTY, DIACRITICAL_SENSITIVE_ATTRIBUTE, CDATA, TRUE);
		hd.startElement(EMPTY,EMPTY,TEXT_MATCHER_TAG, atts);
		hd.endElement(EMPTY,EMPTY,TEXT_MATCHER_TAG);
	}
	
	static String encode(String string) {
		if (string==null) return string;
		try {
			return URLEncoder.encode(string, UTF8);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	static String decode(String string) {
		if (string==null) return string;
		try {
			return URLDecoder.decode(string, UTF8);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private void serialize(Account account) throws SAXException {
		atts.clear();
		atts.addAttribute(EMPTY,EMPTY,ID_ATTRIBUTE,CDATA,account.getName());
		atts.addAttribute(EMPTY,EMPTY,INITIAL_BALANCE_ATTRIBUTE,CDATA,Double.toString(account.getInitialBalance()));
		AlertThreshold alertThreshold = account.getAlertThreshold();
		if (!alertThreshold.equals(AlertThreshold.DEFAULT)) {
			atts.addAttribute(EMPTY, EMPTY, ALERT_THRESHOLD_LESS, CDATA, Double.toString(alertThreshold.getLessThreshold()));
			atts.addAttribute(EMPTY, EMPTY, ALERT_THRESHOLD_MORE, CDATA, Double.toString(alertThreshold.getMoreThreshold()));
		}
		
		hd.startElement(EMPTY,EMPTY,ACCOUNT_TAG,atts);
		String comment = account.getComment();
		if (comment!=null) {
			hd.startCDATA();
			hd.characters(comment.toCharArray(), 0, comment.length());
			hd.endCDATA();
		}
		for (int i = 0; i < account.getModesNumber(); i++) {
			Mode mode = account.getMode(i);
			if (!mode.equals(Mode.UNDEFINED)) serialize(mode);
		}
		for (int i = 0; i < account.getCheckbooksNumber(); i++) {
			serialize(account.getCheckbook(i));
		}
		hd.endElement(EMPTY,EMPTY,ACCOUNT_TAG);
	}
	
	/** Serialize a mode.
	 * @param mode The mode to serialize.
	 * @throws SAXException
	 * @throws IllegalArgumentException if mode is Mode.UNDEFINED
	 */
	private void serialize(Mode mode) throws SAXException {
		if (mode.equals(Mode.UNDEFINED)) throw new IllegalArgumentException();
		atts.clear();
		atts.addAttribute(EMPTY,EMPTY,ID_ATTRIBUTE,CDATA,mode.getName());
		if (mode.isUseCheckBook()) atts.addAttribute(EMPTY, EMPTY, CHECKBOOK_ATTRIBUTE, CDATA, TRUE);
		hd.startElement(EMPTY,EMPTY,MODE_TAG,atts);
		DateStepper expense = mode.getExpenseVdc();
		if (expense!=null) {
			setAttributes(expense);
			hd.startElement(EMPTY, EMPTY, EXPENSE_VDC_TAG, atts);
			hd.endElement(EMPTY, EMPTY, EXPENSE_VDC_TAG);
		}
		DateStepper receipt = mode.getReceiptVdc();
		if (receipt!=null) {
			setAttributes(receipt);
			hd.startElement(EMPTY, EMPTY, RECEIPT_VDC_TAG, atts);
			hd.endElement(EMPTY, EMPTY, RECEIPT_VDC_TAG);
		}
		hd.endElement(EMPTY,EMPTY,MODE_TAG);
	}

	private void serialize(Checkbook book) throws SAXException {
		atts.clear();
		atts.addAttribute(EMPTY,EMPTY,PREFIX_ATTRIBUTE,CDATA,book.getPrefix());
		atts.addAttribute(EMPTY,EMPTY,FIRST_NUMBER_ATTRIBUTE,CDATA,book.getFirst().toString());
		atts.addAttribute(EMPTY,EMPTY,SIZE_ATTRIBUTE,CDATA,Integer.toString(book.size()));
		if (!book.isEmpty()) atts.addAttribute(EMPTY, EMPTY, NEXT_NUMBER_ATTRIBUTE, CDATA, book.getFirst().add(BigInteger.valueOf(book.getUsed())).toString());
		hd.startElement(EMPTY,EMPTY,CHECKBOOK_TAG,atts);
		hd.endElement(EMPTY,EMPTY,CHECKBOOK_TAG);
	}

	private void setAttributes(DateStepper dateStepper) {
		atts.clear();
		String kind;
		if (dateStepper instanceof DayDateStepper) {
			kind = RELATIVE_DATE_STEPPER_KIND;
			atts.addAttribute(EMPTY, EMPTY, PERIOD_ATTRIBUTE, CDATA, Integer.toString(((DayDateStepper)dateStepper).getStep()));
		} else if (dateStepper instanceof DeferredValueDateComputer) {
			kind = DEFERRED_DATE_STEPPER_KIND;
			atts.addAttribute(EMPTY, EMPTY, STOP_DAY_ATTRIBUTE, CDATA, Integer.toString(((DeferredValueDateComputer)dateStepper).getStopDay()));
			atts.addAttribute(EMPTY, EMPTY, DEBT_DAY_ATTRIBUTE, CDATA, Integer.toString(((DeferredValueDateComputer)dateStepper).getDebtDay()));
		} else if (dateStepper.equals(DateStepper.IMMEDIATE)) {
			kind = IMMEDIATE_DATE_STEPPER_KIND;
		} else {
			throw new RuntimeException("Unsupported ValueDateComputer class : "+dateStepper.getClass().getName()); //$NON-NLS-1$
		}
		atts.addAttribute(EMPTY, EMPTY, KIND_ATTRIBUTE, CDATA, kind);
	}

	private void serialize(Category category) throws SAXException {
		if (!category.equals(Category.UNDEFINED)) {
			atts.clear();
			atts.addAttribute(EMPTY,EMPTY,ID_ATTRIBUTE,CDATA,category.getName());
			hd.startElement(EMPTY,EMPTY,CATEGORY_TAG,atts);
			hd.endElement(EMPTY,EMPTY,CATEGORY_TAG);
		}
	}
	
	private void serialize(Transaction transaction) throws SAXException {
		atts.clear();
		atts.addAttribute(EMPTY,EMPTY,ACCOUNT_ATTRIBUTE,CDATA,transaction.getAccount().getName());
		String description = transaction.getDescription();
		if (description!=null) atts.addAttribute(EMPTY,EMPTY,DESCRIPTION_ATTRIBUTE,CDATA,description);
		String comment = transaction.getComment();
		if (comment!=null) atts.addAttribute(EMPTY,EMPTY,COMMENT_ATTRIBUTE,CDATA,comment);
		atts.addAttribute(EMPTY,EMPTY,DATE_ATTRIBUTE,CDATA,toString(transaction.getDate()));
		atts.addAttribute(EMPTY,EMPTY,AMOUNT_ATTRIBUTE,CDATA,Double.toString(transaction.getAmount()));
		Mode mode = transaction.getMode();
		if (!mode.equals(Mode.UNDEFINED)) atts.addAttribute(EMPTY,EMPTY,MODE_ATTRIBUTE,CDATA,mode.getName());
		String number = transaction.getNumber();
		if ((number!=null) && (number.length()>0)) atts.addAttribute(EMPTY,EMPTY,NUMBER_ATTRIBUTE,CDATA,number);
		Category category = transaction.getCategory();
		if (!category.equals(Category.UNDEFINED)) atts.addAttribute(EMPTY,EMPTY,CATEGORY_ATTRIBUTE,CDATA,category.getName());
		atts.addAttribute(EMPTY,EMPTY,VALUE_DATE_ATTRIBUTE,CDATA,toString(transaction.getValueDate()));
		String statement = transaction.getStatement();
		if (statement!=null) atts.addAttribute(EMPTY,EMPTY,STATEMENT_ATTRIBUTE,CDATA,statement);
		hd.startElement(EMPTY,EMPTY,TRANSACTION_TAG,atts);
		for (int i = 0; i < transaction.getSubTransactionSize(); i++) {
			serialize(transaction.getSubTransaction(i));
		}
		hd.endElement(EMPTY,EMPTY,TRANSACTION_TAG);
	}

	private void serialize(SubTransaction subTransaction) throws SAXException {
		atts.clear();
		atts.addAttribute(EMPTY, EMPTY, DESCRIPTION_ATTRIBUTE, CDATA, subTransaction.getDescription());
		atts.addAttribute(EMPTY, EMPTY, AMOUNT_ATTRIBUTE, CDATA, Double.toString(subTransaction.getAmount()));
		Category category = subTransaction.getCategory();
		if (!category.equals(Category.UNDEFINED)) atts.addAttribute(EMPTY, EMPTY, CATEGORY_ATTRIBUTE, CDATA,category.getName());
		hd.startElement(EMPTY,EMPTY,SUBTRANSACTION_TAG,atts);
		hd.endElement(EMPTY,EMPTY,SUBTRANSACTION_TAG);
	}
	
	private void serialize(PeriodicalTransaction periodicalTransaction) throws SAXException {
		atts.clear();
		atts.addAttribute(EMPTY,EMPTY,ACCOUNT_ATTRIBUTE,CDATA,periodicalTransaction.getAccount().getName());
		String description = periodicalTransaction.getDescription();
		if (description!=null) atts.addAttribute(EMPTY,EMPTY,DESCRIPTION_ATTRIBUTE,CDATA,description);
		String comment = periodicalTransaction.getComment();
		if (comment!=null) atts.addAttribute(EMPTY,EMPTY,COMMENT_ATTRIBUTE,CDATA,comment);
		atts.addAttribute(EMPTY,EMPTY,AMOUNT_ATTRIBUTE,CDATA,Double.toString(periodicalTransaction.getAmount()));
		Mode mode = periodicalTransaction.getMode();
		if (!mode.equals(Mode.UNDEFINED)) atts.addAttribute(EMPTY,EMPTY,MODE_ATTRIBUTE,CDATA,mode.getName());
		Category category = periodicalTransaction.getCategory();
		if (!category.equals(Category.UNDEFINED)) atts.addAttribute(EMPTY,EMPTY,CATEGORY_ATTRIBUTE,CDATA,category.getName());
		atts.addAttribute(EMPTY,EMPTY,ENABLED_ATTRIBUTE,CDATA,Boolean.toString(periodicalTransaction.isEnabled()));
		Date nextDate = periodicalTransaction.getNextDate();
		if (nextDate!=null) atts.addAttribute(EMPTY,EMPTY,NEXT_DATE_ATTRIBUTE,CDATA,toString(nextDate));
		hd.startElement(EMPTY,EMPTY,PERIODICAL_TAG,atts);
		DateStepper nextDateBuilder = periodicalTransaction.getNextDateBuilder();
		if (nextDateBuilder!=null) serialize(nextDateBuilder);
		for (int i = 0; i < periodicalTransaction.getSubTransactionSize(); i++) {
			serialize(periodicalTransaction.getSubTransaction(i));
		}
		hd.endElement(EMPTY,EMPTY,PERIODICAL_TAG);
	}

	private void serialize(DateStepper stepper) throws SAXException {
		if (stepper instanceof MonthDateStepper) {
			MonthDateStepper mds = (MonthDateStepper) stepper;
			atts.clear();
			atts.addAttribute(EMPTY, EMPTY, KIND_ATTRIBUTE, CDATA, MONTHLY_DATE_STEPPER_KIND);
			atts.addAttribute(EMPTY, EMPTY, PERIOD_ATTRIBUTE, CDATA, Integer.toString(mds.getPeriod()));
			atts.addAttribute(EMPTY, EMPTY, DAY_ATTRIBUTE, CDATA, Integer.toString(mds.getDay()));
			Date last = mds.getLastDate();
			if (last!=null) atts.addAttribute(EMPTY, EMPTY, LAST_DATE_ATTRIBUTE, CDATA, toString(last));
			hd.startElement(EMPTY,EMPTY,DATE_STEPPER_TAG, atts);
			hd.endElement(EMPTY,EMPTY,DATE_STEPPER_TAG);
		} else if (stepper instanceof DayDateStepper) {
			DayDateStepper dds = (DayDateStepper) stepper;
			atts.addAttribute(EMPTY, EMPTY, KIND_ATTRIBUTE, CDATA, RELATIVE_DATE_STEPPER_KIND);
			atts.addAttribute(EMPTY, EMPTY, PERIOD_ATTRIBUTE, CDATA, Integer.toString(dds.getStep()));
			Date last = dds.getLastDate();
			if (last!=null) atts.addAttribute(EMPTY, EMPTY, LAST_DATE_ATTRIBUTE, CDATA, toString(last));
			hd.startElement(EMPTY,EMPTY,DATE_STEPPER_TAG, atts);
			hd.endElement(EMPTY,EMPTY,DATE_STEPPER_TAG);
			atts.clear();
		} else throw new IllegalArgumentException("This stepper class is not supported : "+stepper.getClass()); //$NON-NLS-1$
	}

	@SuppressWarnings("deprecation")
	private String toString(Date date) {
		int month = date.getMonth()+1;
		int year = date.getYear()+1900;
		return year + DATE_DELIM + month + DATE_DELIM + date.getDate();
	}
	
	static int toDate(String value) {
		if (value==null) return -1;
		StringTokenizer tokens = new StringTokenizer(value,DATE_DELIM);
		int year = Integer.parseInt(tokens.nextToken());
		int month = Integer.parseInt(tokens.nextToken());
		int day = Integer.parseInt(tokens.nextToken());
		return DateUtils.dateToInteger(year, month, day);
	}
}
