

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.yapbam.data.GlobalData;

public class PipeTest {

	public static void main(String[] args) {
		try {
			GlobalData data  = new GlobalData();
			
			PipedOutputStream xmlOutput = new PipedOutputStream();
			PipedInputStream compressorInput = new PipedInputStream(xmlOutput);
			
			PipedOutputStream compressorOutput = new PipedOutputStream();
			PipedInputStream encoderInput = new PipedInputStream(compressorOutput);
			
			ExecutorService service = new ThreadPoolExecutor(0, Integer.MAX_VALUE,0, TimeUnit.SECONDS,
          new SynchronousQueue<Runnable>());;
			
      service.submit(new WriterCallable(data, xmlOutput));
			service.submit(new CompressorCallable(compressorInput, compressorOutput));
			Future<Void> encoder = service.submit(new EncoderCallable(encoderInput, new FileOutputStream("out.txt"), "gti"));

			encoder.get(); // Wait encoding is ended
			System.out.println ("Ended");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}