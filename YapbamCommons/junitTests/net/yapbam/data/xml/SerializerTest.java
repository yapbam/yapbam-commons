package net.yapbam.data.xml;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.AccessControlException;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;

import net.yapbam.data.Account;
import net.yapbam.data.Category;
import net.yapbam.data.GlobalData;
import net.yapbam.data.Mode;
import net.yapbam.data.Transaction;

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
		Date today = new Date();
		Transaction transaction = new Transaction(today, null, "description", "commentaire", -5.32, account, Mode.UNDEFINED, Category.UNDEFINED,
				today, null, Collections.EMPTY_LIST);
		data.add(transaction);
		
		testInstance(data);
		
		data.setPassword("this is the big password");
		testInstance(data);
		data.setPassword("été is a non ascii password");
		testInstance(data);
	}

	@Test
	public void emptyTest() throws Exception {
		GlobalData data = new GlobalData();		
		testInstance(data);
		data.setPassword("été");
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
		for (int i = 0; i < data.getTransactionsNumber(); i++) {
			//TODO Test if other transaction's fields are the same
			Transaction newOne = other.getTransaction(i);
			Transaction original = data.getTransaction(i);
			assertEquals(original.getDateAsInteger(), newOne.getDateAsInteger());
			assertEquals(original.getValueDateAsInteger(), newOne.getValueDateAsInteger());
			assertEquals(original.getSubTransactionSize(), newOne.getSubTransactionSize());
		}
		assertEquals(data.getPeriodicalTransactionsNumber(), other.getPeriodicalTransactionsNumber());
		//TODO Test if periodical transactions are the same
	}

	private GlobalData reread(GlobalData data) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		Serializer.write(data, os, null, null);
		os.flush();
		os.close();
		
		byte[] serialized = os.toByteArray();
//		System.out.println (new String(serialized));
		
		ByteArrayInputStream is = new ByteArrayInputStream(serialized);
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

	@Test
	public void pre0_16_0() {
		testPwdOk("pre0.16.0.xml", null, true);
		testPwdOk("pre0.16.0.xml", "gti", false);
		testPre0_16_0("pre0.16.0.xml", null);
		
		testPwdOk("pre0.16.0-gti.xml", null, false);
		testPwdOk("pre0.16.0-gti.xml", "gti", true);
		testPre0_16_0("pre0.16.0-gti.xml", "gti");
		
		testPwdOk("pre0.16.0-été.xml", null, false);
		testPwdOk("pre0.16.0-été.xml", "gti", false);
		testPwdOk("pre0.16.0-été.xml", "été", true);
		testPre0_16_0("pre0.16.0-été.xml", "été");
		
		testPwdOk("pre0.16.0.zip", null, true);
		testPwdOk("pre0.16.0.zip", "gti", false);
		testPre0_16_0("pre0.16.0.zip", null);

		testPwdOk("pre0.16.0-gti.zip", null, false);
		testPwdOk("pre0.16.0-gti.zip", "gti", true);
		testPre0_16_0("pre0.16.0-gti.zip", "gti");
		
		testPwdOk("pre0.16.0-été.zip", null, false);
		testPwdOk("pre0.16.0-été.zip", "gti", false);
		testPwdOk("pre0.16.0-été.zip", "été", true);
		testPre0_16_0("pre0.16.0-été.zip", "été");
	}
		
	private void testPwdOk(String resName, String password, boolean expectedResult) {
		try {
			URL resource = getClass().getResource(resName);
			if (resource==null) fail("Unable to locate "+resName);
			
			InputStream in = resource.openStream();
			try {
				assertEquals(expectedResult, Serializer.isPasswordOk(in, password));
			} finally {
				in.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
			fail("Get an IOException while processing "+resName);
		}
	}

	private void testPre0_16_0(String resName, String password) {
		try {
			URL resource = getClass().getResource(resName);
			if (resource==null) fail("Unable to locate "+resName);
			
			InputStream in = resource.openStream();
			try {
				GlobalData data = Serializer.read(password, in, null);
				assertEquals(1, data.getAccountsNumber());
			} finally {
				in.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
			fail("Get an IOException while processing "+resName);
		}
	}
	
	@Test
	public void pre0_16_0WrongPwd_1() {
		// Password is wrong, but should be ignored
		testPre0_16_0("pre0.16.0.xml", "xxx");
	}
	@Test(expected = AccessControlException.class)
	public void pre0_16_0WrongPwd_2() {
		testPre0_16_0("pre0.16.0-gti.xml", null);
	}
	@Test(expected = AccessControlException.class)
	public void pre0_16_0WrongPwd_3() {
		testPre0_16_0("pre0.16.0-été.xml", "ete");
	}
	@Test
	public void pre0_16_0WrongPwd_4() {
		// Password is wrong, but should be ignored
		testPre0_16_0("pre0.16.0.zip", "xxx");
	}
	@Test(expected = AccessControlException.class)
	public void pre0_16_0WrongPwd_5() {
		testPre0_16_0("pre0.16.0-gti.zip", "xxx");
	}
	@Test(expected = AccessControlException.class)
	public void pre0_16_0WrongPwd_6() {
		testPre0_16_0("pre0.16.0-été.zip", null);
	}
	
	@Test
	public void testWithZipFile() throws IOException {
		GlobalData data = new GlobalData();
		String pwd = "gti";
		data.setPassword(pwd);
		File file = File.createTempFile("yapbam", null);
		file.deleteOnExit();
		OutputStream out = new FileOutputStream(file);
		try {
			Serializer.write(data, out, "the entry name", null);
		} finally {
			out.close();
		}
		
		InputStream in = new FileInputStream(file);
		try {
			Serializer.read(pwd, in, null);
		} finally {
			in.close();
		}
	}
}
