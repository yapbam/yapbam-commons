

import java.io.*;

public class PipeTest {

	public static void main(String[] args) {
		try {
			PipedInputStream writeIn = new PipedInputStream();
			PipedOutputStream readOut = new PipedOutputStream(writeIn);

			Thread rt = new CompressorThread(new FileInputStream("in.txt"), readOut);
			Thread wt = new EncoderThread(writeIn, new FileOutputStream("out.txt"), "gti");

			rt.start();
			wt.start();
			
			rt.join();
			wt.join();
			System.out.println ("Ended");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class ReadThread extends Thread implements Runnable {

	InputStream pi;
	OutputStream po;

	ReadThread(String process, InputStream pi, OutputStream po) {
		this.pi = pi;
		this.po = po;
	}

	public void run() {
		byte[] buffer = new byte[512];
		int bytes_read;
		try {
			for (;;) {
				bytes_read = pi.read(buffer);
				if (bytes_read == -1) break;
				po.write(buffer, 0, bytes_read);
			}
			pi.close();
			po.flush();
			po.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
		}
	}

}
