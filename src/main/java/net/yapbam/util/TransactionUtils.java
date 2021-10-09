package net.yapbam.util;

import org.apache.commons.text.StringEscapeUtils;

import net.yapbam.data.AbstractTransaction;
import net.yapbam.data.Category;
import net.yapbam.data.GlobalData;

public class TransactionUtils {
	private static final String NEW_INDENTED_LINE = HtmlUtils.NEW_LINE_TAG+HtmlUtils.NON_BREAKING_SPACE+HtmlUtils.NON_BREAKING_SPACE;
	private static final String OPEN = HtmlUtils.START_TAG+HtmlUtils.START_BODY_TAG;
	private static final String END = HtmlUtils.END_TAG+HtmlUtils.END_BODY_TAG;

	public interface WordingProvider {
		String getComplementWording();
	}

	private WordingProvider wordingProvider;
	
	public TransactionUtils(WordingProvider wordingProvider) {
		super();
		this.wordingProvider = wordingProvider;
	}
	
	public String getDescription (AbstractTransaction transaction, boolean spread, boolean mergeComment, boolean withHtmlTags) {
		StringBuilder buf = new StringBuilder();
		if (withHtmlTags) {
			buf.append(OPEN);
		}
		if (spread) {
			buf.append(getDescription(transaction, false, mergeComment, false));
			for (int i = 0; i < transaction.getSubTransactionSize(); i++) {
				buf.append(NEW_INDENTED_LINE).append(StringEscapeUtils.escapeHtml4(transaction.getSubTransaction(i).getDescription()));
			}
			if (transaction.getComplement()!=0) {
				buf.append(NEW_INDENTED_LINE).append(StringEscapeUtils.escapeHtml4(wordingProvider.getComplementWording()));
			}
		} else {
			buf.append (StringEscapeUtils.escapeHtml4(transaction.getDescription()));
			if (mergeComment && (transaction.getComment()!=null)) {
				buf.append(" ("); //$NON-NLS-1$
				buf.append(getComment(transaction));
				buf.append(")"); //$NON-NLS-1$
			}
		}
		if (withHtmlTags) {
			buf.append(END); //$NON-NLS-1$
		}
		return buf.toString().replace(" ", HtmlUtils.NON_BREAKING_SPACE);
	}

	public String getComment(AbstractTransaction transaction) {
		String comment = transaction.getComment();
		return comment==null?"":HtmlUtils.toHtml(comment);
	}
	
	private boolean isExpense (double amount) {
		return GlobalData.AMOUNT_COMPARATOR.compare(amount, 0.0)<=0;
	}

	private boolean isZero (double amount) {
		return GlobalData.AMOUNT_COMPARATOR.compare(amount, 0.0)==0;
	}

	private String getName(Category category) {
		return category.equals(Category.UNDEFINED) ? "" : category.getName(); //$NON-NLS-1$
	}

	public double[] getAmount(AbstractTransaction transaction, boolean spread) {
		if (spread) {
			double complement = transaction.getComplement();
			int numberOfLines = transaction.getSubTransactionSize()+1;
			if (complement!=0) {
				numberOfLines++;
			}
			double[] result = new double[numberOfLines];
			result[0] = transaction.getAmount();
			for (int i = 0; i < transaction.getSubTransactionSize(); i++) {
				result[i+1] = transaction.getSubTransaction(i).getAmount();
			}
			if (complement!=0) {
				result[result.length-1] = complement;
			}
			return result;
		} else {
			return new double[]{transaction.getAmount()};
		}
	}
	
	public double[] getExpenseReceipt(AbstractTransaction transaction, boolean spread, boolean expense) {
		if (spread) {
			double complement = transaction.getComplement();
			int numberOfLines = transaction.getSubTransactionSize()+1;
			if (!isZero(complement)) {
				numberOfLines++;
			}
			double[] result = new double[numberOfLines];
			result[0] = expense==isExpense(transaction.getAmount()) ? transaction.getAmount() : Double.NaN;
			for (int i = 0; i < transaction.getSubTransactionSize(); i++) {
				double amount = transaction.getSubTransaction(i).getAmount();
				result[i+1] = expense==isExpense(amount) ? amount : Double.NaN;
			}
			if (!isZero(complement)) {
				result[result.length-1] = expense==isExpense(complement) ? complement : Double.NaN;
			}
			return result;
		} else {
			return (expense==isExpense(transaction.getAmount())) ? new double[]{transaction.getAmount()} : null;
		}
	}

	public Object getCategory(AbstractTransaction transaction, boolean spread) {
		if (spread) {
			StringBuilder buf = new StringBuilder(OPEN);
			buf.append(StringEscapeUtils.escapeHtml4(getName(transaction.getCategory()))); //$NON-NLS-1$
			for (int i = 0; i < transaction.getSubTransactionSize(); i++) {
				buf.append(NEW_INDENTED_LINE).append(StringEscapeUtils.escapeHtml4(getName(transaction.getSubTransaction(i).getCategory())));
			}
			if (transaction.getComplement()!=0) {
				buf.append(NEW_INDENTED_LINE).append(StringEscapeUtils.escapeHtml4(getName(transaction.getCategory())));
			}
			buf.append(END); //$NON-NLS-1$
			return buf.toString().replace(" ", HtmlUtils.NON_BREAKING_SPACE);
		} else {
			return getName(transaction.getCategory());
		}
	}
}
