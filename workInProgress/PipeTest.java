

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.yapbam.data.GlobalData;
import net.yapbam.data.xml.task.DecrypterTask;
import net.yapbam.data.xml.task.DeflaterTask;
import net.yapbam.data.xml.task.EncrypterTask;
import net.yapbam.data.xml.task.InflaterTask;
import net.yapbam.data.xml.task.ReaderTask;
import net.yapbam.data.xml.task.WriterTask;

public class PipeTest {

	public static void main(String[] args) {
		try {
			String fileName = "out.txt";
			String password = "gti";
			{
				GlobalData data  = new GlobalData();
				
				PipedOutputStream xmlOutput = new PipedOutputStream();
				PipedInputStream compressorInput = new PipedInputStream(xmlOutput);
				
				PipedOutputStream compressorOutput = new PipedOutputStream();
				PipedInputStream encoderInput = new PipedInputStream(compressorOutput);
				
				ExecutorService service = new ThreadPoolExecutor(0, Integer.MAX_VALUE,0, TimeUnit.SECONDS,
	          new SynchronousQueue<Runnable>());;
				

	      List<Future<? extends Object>> futures = new ArrayList<Future<? extends Object>>(3);
	      futures.add(service.submit(new WriterTask(data, xmlOutput)));
	      futures.add(service.submit(new DeflaterTask(compressorInput, compressorOutput)));
	      futures.add(service.submit(new EncrypterTask(encoderInput, new FileOutputStream(fileName), password)));
	
	      // Wait encoding is ended and gets the errors
				for (Future<? extends Object> future : futures) {
					future.get();
				}
				
				System.out.println ("Ouput ended");
			}
			
			PipedOutputStream decoderOutput = new PipedOutputStream();
			PipedInputStream deflaterInput = new PipedInputStream(decoderOutput);
			PipedOutputStream deflaterOutput = new PipedOutputStream();
			PipedInputStream readerInput = new PipedInputStream(deflaterOutput);
			
			ExecutorService service = new ThreadPoolExecutor(0, Integer.MAX_VALUE,0, TimeUnit.SECONDS,
          new SynchronousQueue<Runnable>());;
			
      Future<Void> decrypter = service.submit(new DecrypterTask(new FileInputStream(fileName), decoderOutput, password));
      Future<Void> inflater = service.submit(new InflaterTask(deflaterInput, deflaterOutput));
//      Future<Void> inflater = service.submit(new InflaterTask(deflaterInput, new FileOutputStream("text.xml")));
			Future<GlobalData> reader = service.submit(new ReaderTask(readerInput, password));

			decrypter.get(); // Wait encoding is ended
			inflater.get(); // Wait encoding is ended
			
			GlobalData data = reader.get();
			System.out.println ("Read ended");
			
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}