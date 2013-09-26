package net.yapbam.utils;

import static org.junit.Assert.*;

import org.junit.Test;

public class CryptoTest {

	@Test
	public void test() {
		String pwd = "été";
		String message = "Bientôt fini, et si on teste avec un texte un poil plus long ? Qu'est-ce qui se passe ?";
		assertEquals(message, new Crypto2(true).doFileCycle(message, pwd));
		message = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><DATA version=\"1\" nbAccounts=\"0\" nbCategories=\"1\" subCategorySeparator=\".\" nbPeriodicalTransactions=\"0\" nbTransactions=\"0\"/>";
		assertEquals(message, new Crypto2(true).doCycle(message, pwd));
	}
	

}
