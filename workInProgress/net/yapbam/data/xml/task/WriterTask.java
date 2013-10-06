package net.yapbam.data.xml.task;
import java.io.OutputStream;
import java.util.concurrent.Callable;

import net.yapbam.data.GlobalData;
import net.yapbam.data.xml.XMLSerializer;

public class WriterTask implements Callable<Void> {
	private static boolean TRACE = false;
	private OutputStream out;
	private GlobalData data;

	public WriterTask (GlobalData data, OutputStream out) {
		this.out = out;
		this.data = data;
	}

	@Override
	public Void call() throws Exception {
		try {
			if (TRACE) System.out.println ("Start "+getClass().getName());
			XMLSerializer serializer = new XMLSerializer(out);
			serializer.serialize(data, null);
			serializer.closeDocument();
			return null;
		} finally {
			out.close();
			if (TRACE) System.out.println ("Stop "+getClass().getName());
		}
	}
}