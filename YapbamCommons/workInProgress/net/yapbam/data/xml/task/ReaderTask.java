package net.yapbam.data.xml.task;
import java.io.InputStream;
import java.util.concurrent.Callable;

import net.yapbam.data.GlobalData;
import net.yapbam.data.xml.XMLSerializer;

public class ReaderTask implements Callable<GlobalData> {
	private static boolean TRACE = false;
	private InputStream in;
	private String password;

	public ReaderTask (InputStream in, String password) {
		this.in = in;
		this.password = password;
	}

	@Override
	public GlobalData call() throws Exception {
		try {
			if (TRACE) System.out.println ("Start "+getClass().getName());
			return XMLSerializer.read(password, in, null);
		} finally {
			in.close();
			if (TRACE) System.out.println ("Stop "+getClass().getName());
		}
	}
}
