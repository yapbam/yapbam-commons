package net.yapbam.data.xml;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessControlException;
import java.util.Currency;
import java.util.Locale;

import net.yapbam.data.Account;
import net.yapbam.data.Category;
import net.yapbam.data.GlobalData;

import org.junit.Test;

public class SerializerTest {
	private static final double doubleAccuracy = Math.pow(10, -Currency.getInstance(Locale.getDefault()).getDefaultFractionDigits())/2;

	@Test
	public void test() throws Exception {
		GlobalData data = new GlobalData();
		Account account = new Account("toto", 50.24);
		data.add(account);
		account = new Account("titi", -10.0);
		data.add(account);
		data.setComment(account, "Un commentaire avec plusieurs lignes\nEt des caractères accentués.");
		
		testInstance(data);
		
		data.setPassword("this is the big password");
		testInstance(data);
	}

	private void testInstance(GlobalData data) throws IOException {
		GlobalData other = reread(data);
		
		assertEquals(data.getCategoriesNumber(), other.getCategoriesNumber());
		for (int i = 0; i < data.getCategoriesNumber(); i++) {
			Category category = data.getCategory(i);
			if (!category.equals(Category.UNDEFINED)) assertNotNull(other.getCategory(category.getName()));
		}
		
		assertEquals(other.getAccountsNumber(), data.getAccountsNumber());
		for (int i = 0; i < data.getAccountsNumber(); i++) {
			Account account = data.getAccount(i);
			Account oAccount = other.getAccount(account.getName());
			assertNotNull(oAccount);
			assertEquals(account.getInitialBalance(), oAccount.getInitialBalance(), doubleAccuracy);
			assertEquals(account.getComment(), oAccount.getComment());

			assertEquals(account.getModesNumber(), oAccount.getModesNumber());
			//TODO Test if modes are the same
			assertEquals(account.getCheckbooksNumber(), oAccount.getCheckbooksNumber());
			//TODO Test if checkbooks are the same
		}
		assertEquals(data.getPassword(), other.getPassword());
		
		assertEquals(data.getTransactionsNumber(), other.getTransactionsNumber());
		//TODO Test if transactions are the same
		assertEquals(data.getPeriodicalTransactionsNumber(), other.getPeriodicalTransactionsNumber());
		//TODO Test if periodical transactions are the same
	}

	private GlobalData reread(GlobalData data) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream(); 
		Serializer serializer = new Serializer(data.getPassword(), os);
		serializer.serialize(data, null);
		serializer.closeDocument(data.getPassword());
		os.flush();
		
		ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
		GlobalData other = new GlobalData();
		other = Serializer.read(data.getPassword(), is, null);
		return other;
	}

	@Test
	public void testInvalidXMLFile() {
		testInvalidXMLFile(new String[]{}, UnsupportedFormatException.class); // An empty file
		testInvalidXMLFile(new String[]{"This is not an XML file"}, UnsupportedFormatException.class); // Not an xml file
		testInvalidXMLFile(new String[]{"<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "<DATA>"}, UnsupportedFormatException.class); // Tag is not closed
		testInvalidXMLFile(new String[]{"<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "<UNKNOWN_TAG/>"}, UnsupportedFormatException.class); // Not contains Yapbam data
		testInvalidXMLFile(new String[]{"<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "<DATA><DATA/></DATA>"}, UnsupportedFormatException.class); // More than one data tag
		testInvalidXMLFile(new String[]{"<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "<DATA/><DATA/>"}, UnsupportedFormatException.class); // More than one data tag
		testInvalidXMLFile(new String[]{"<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "<DATA><ACCOUNT/></DATA>"}, UnsupportedFormatException.class); // Tag do not have mandatory field
		testInvalidXMLFile(new String[]{"<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "<DATA><ACCOUNT id=\"toto\" initialBalance=\"0\" alertThresholdLess=\"Not a number\" alertThresholdMore=\"Not a number\"/></DATA>"}, UnsupportedFormatException.class); // Tag do not have mandatory field
		testInvalidXMLFile(new String[]{"<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "<DATA version=\"12345487\"/>"}, UnsupportedFileVersionException.class); // A not supported version of Yapbam format
	}
	
	private void testInvalidXMLFile(String[] content, Class<? extends UnsupportedFormatException> expectedException) {
		File file;
		try {
			file = File.createTempFile("testYapbam", ".tmp");
			BufferedWriter buf = new BufferedWriter(new FileWriter(file));
			try {
				for (String string : content) {
					buf.write(string);
					buf.newLine();
				}
			} finally {
				buf.close();
			}
			try {
				FileInputStream in = new FileInputStream(file);
				try {
					Serializer.read(null, in, null);
				} finally {
					in.close();
				}
				fail("Parsing of invalid file should fail");
			} catch (AccessControlException e) {
				fail("Should not require password");
			} catch (UnsupportedFormatException e) {
				assertTrue(e.getClass().equals(expectedException));
				// Yeah, this is the right exception 
			} catch (IOException e) {
				fail("Should throw a more specify IOException");
			}
		} catch (IOException e) {
			fail("Fail to build test file");
		}
	}

	@Test
	public void pbPre0_12_0() {
		try {
			InputStream in = getClass().getResource("bugpre0.13.3.xml").openStream();
			try {
				Serializer.read(null, in, null);
			} catch (Exception e) {
				fail("Unable to read pre-0.12.0 file");
			} finally {
				in.close();
			}
		} catch (IOException e) {
			fail("Get an IOException");
		}
	}
}
