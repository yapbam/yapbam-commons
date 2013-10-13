package net.yapbam.data.xml.task;

import java.io.OutputStream;
import java.util.concurrent.Callable;

import net.yapbam.data.ProgressReport;
import net.yapbam.data.xml.AbstractSerializer;

public class WriterTask<T> implements Callable<Void> {
	private static boolean TRACE = false;
	private AbstractSerializer<T> serializer;
	private OutputStream out;
	private T data;
	private ProgressReport report;

	public WriterTask (AbstractSerializer<T> serializer, T data, OutputStream out, ProgressReport report) {
		this.serializer = serializer;
		this.out = out;
		this.data = data;
		this.report = report;
	}

	@Override
	public Void call() throws Exception {
		try {
			if (TRACE) System.out.println ("Start "+getClass().getName());
			serializer.directWrite(data, out, report);
			return null;
		} finally {
			out.close();
			if (TRACE) System.out.println ("Stop "+getClass().getName());
		}
	}
}
