package net.yapbam.util;

import org.apache.commons.lang3.StringEscapeUtils;

import net.yapbam.data.AbstractTransaction;
import net.yapbam.data.Category;
import net.yapbam.data.GlobalData;
import net.yapbam.util.HtmlUtils;

public class TransactionUtils {
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
			buf.append("<html><body>");
		}
		if (spread) {
			buf.append(getDescription(transaction, false, mergeComment, false)); //$NON-NLS-1$
			for (int i = 0; i < transaction.getSubTransactionSize(); i++) {
				buf.append("<BR>&nbsp;&nbsp;").append(StringEscapeUtils.escapeHtml3(transaction.getSubTransaction(i).getDescription())); //$NON-NLS-1$
			}
			if (transaction.getComplement()!=0) {
				buf.append("<BR>&nbsp;&nbsp;").append(StringEscapeUtils.escapeHtml3(wordingProvider.getComplementWording())); //$NON-NLS-1$
			}
		} else {
			buf.append (StringEscapeUtils.escapeHtml3(transaction.getDescription()));
			if (mergeComment && (transaction.getComment()!=null)) {
				buf.append(" (");
				buf.append(getComment(transaction));
				buf.append(")");
			}
		}
		if (withHtmlTags) {
			buf.append("</body></html>"); //$NON-NLS-1$
		}
		return buf.toString().replace(" ", "&nbsp;");
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
			StringBuilder buf = new StringBuilder("<html><body>").append(StringEscapeUtils.escapeHtml3(getName(transaction.getCategory()))); //$NON-NLS-1$
			for (int i = 0; i < transaction.getSubTransactionSize(); i++) {
				buf.append("<BR>&nbsp;&nbsp;").append(StringEscapeUtils.escapeHtml3(getName(transaction.getSubTransaction(i).getCategory()))); //$NON-NLS-1$
			}
			if (transaction.getComplement()!=0) {
				buf.append("<BR>&nbsp;&nbsp;").append(StringEscapeUtils.escapeHtml3(getName(transaction.getCategory()))); //$NON-NLS-1$
			}
			buf.append("</body></html>"); //$NON-NLS-1$
			return buf.toString().replace(" ", "&nbsp;");
		} else {
			return getName(transaction.getCategory());
		}
	}
}
