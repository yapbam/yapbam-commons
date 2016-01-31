package net.yapbam.data;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import net.yapbam.data.Transaction;
import net.yapbam.util.DateUtils;
import net.yapbam.util.NullUtils;

/** A statement.
 */
public class Statement  {
	private String id;
	private double positiveBalance;
	private double negativeBalance;
	private int nbTransactions;
	private long dateSum;
	private double startBalance;
	
	/** Gets the statements of an account.
	 * @param account The account
	 * @return An array of statements ordered from the oldest to the newest.
	 * This sort is made according to medium value date of the transactions in statements.
	 * Note that an empty account always have a statement with the null id.
	 */
	public static Statement[] getStatements(Account account) {
		HashMap<String, Statement> map = new HashMap<String, Statement>();
		BalanceHistory balanceHistory = account.getBalanceData().getBalanceHistory();
		for (int i = 0; i < balanceHistory.getTransactionsNumber(); i++) {
			Transaction transaction = balanceHistory.getTransaction(i);
			if (transaction.getAccount().getName().equals(account.getName())) {
				String statementId = transaction.getStatement();
				Statement statement = map.get(statementId);
				if (statement==null) {
					statement = new Statement(statementId);
					map.put(statementId, statement);
				}
				statement.add(transaction);
			}
		}
		Statement[] statements = map.values().toArray(new Statement[map.size()]);
		Arrays.sort(statements, new Comparator<Statement>() {
			@Override
			public int compare(Statement o2, Statement o1) {
				if ((o2.getId()==null) || (o1.getId()==null)) {
					return NullUtils.compareTo(o2.getId(),o1.getId(),false);
				}
				int result = o2.getMediumDate()-o1.getMediumDate();
				if (result==0) {
					result = NullUtils.compareTo(o2.getId(),o1.getId(),false);
				}
				return result;
			}
		});
		if (statements.length==0) {
			statements = new Statement[]{new Statement(null)}; 
		}
		statements[0].setStartBalance(account.getInitialBalance());
		for (int i = 1; i < statements.length; i++) {
			statements[i].setStartBalance(statements[i-1].getEndBalance());
		}
		return statements;
	}
	
	public Statement(String id, double startBalance) {
		this(id);
		setStartBalance(startBalance);
	}
	
	/** Constructor.
	 * @param id the statement id of the statement.
	 */
	private Statement(String id) {
		super();
		this.id = id;
		this.positiveBalance = 0;
		this.negativeBalance = 0;
		this.nbTransactions = 0;
		this.dateSum = 0;
	}

	private void add(Transaction transaction) {
		this.nbTransactions++;
		double amount = transaction.getAmount();
		if (amount>0) {
			this.positiveBalance += amount;
		} else {
			this.negativeBalance -= amount;
		}
		this.dateSum += transaction.getValueDateAsInteger();
	}
	
	/** Gets the medium value date of the transactions in this statement.
	 * @return an integer. You may convert it to a java.util.date using DateUtils.integerToDate(int)
	 * @see DateUtils#integerToDate(int)
	 */
	public int getMediumDate() {
		return (int) (this.dateSum/this.nbTransactions);
	}

	public String getId() {
		return id;
	}

	public double getBalance() {
		return this.positiveBalance - this.negativeBalance;
	}

	/** Gets the number of transactions in the statement.
	 * @return an integer
	 */
	public int getNbTransactions() {
		return nbTransactions;
	}

	@Override
	public String toString() {
		return this.getId();
	}

	private void setStartBalance(double startBalance) {
		this.startBalance = startBalance;
	}

	public double getPositiveBalance() {
		return positiveBalance;
	}

	public double getNegativeBalance() {
		return negativeBalance;
	}

	public double getStartBalance() {
		return startBalance;
	}
	
	public double getEndBalance() {
		return startBalance + getBalance();
	}
}