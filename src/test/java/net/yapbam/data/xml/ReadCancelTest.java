package net.yapbam.data.xml;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.junit.Test;

import net.yapbam.data.GlobalData;
import net.yapbam.data.ProgressReport;

public class ReadCancelTest {
	private static class AutoCancelReporter implements ProgressReport {
		private boolean cancelled = false;
		
		@Override
		public boolean isCancelled() {
			return cancelled;
		}

		@Override
		public void setMax(int length) {
			System.out.println ("Max = "+length);
			cancelled = true;
		}

		@Override
		public void reportProgress(int progress) {
			System.out.println (progress);
		}
	}
	
	@Test
	public void test() {
//		test("bugpre0.13.3.xml", null);
		test("pre0.16.0-gti.zip", "gti");
	}

	private void test(String resName, String password) {
		try {
			URL resource = getClass().getResource(resName);
			if (resource==null) fail("Unable to locate "+resName);
			
			InputStream in = resource.openStream();
			try {
				GlobalData data = new Serializer().read(password, in, new AutoCancelReporter());
				assertNull(data);
			} finally {
				in.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
			fail("Get an IOException while processing "+resName);
		}
	}
}
