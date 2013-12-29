package net.yapbam.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** A Cache.
 * <br>This converter use cache in order to preserve web server resources, and to be able to work with no Internet connection.
 * <br>To improve the cache robustness, the cache may have two levels:<ol>
 * <li>A temporary cache that is used to store the data red from Internet.</li>
 * <li>A persistent cache that saved the temporary one after it was validated by a successful parsing (see {@link #commit()}).</li></ol>
 * @author Jean-Marc Astesana
 */
public interface Cache {
	/** Tests whether the cache is empty or not.
	 * <br>The cache is empty if both temporary and saved cache are not available.
	 * @return
	 */
	public boolean isEmpty();
	
	/** Gets an output stream to the temporary cache.
	 * @return An output stream.
	 * @throws IOException if an error occurs while creating the writer. 
	 */
	public OutputStream getOutputStream() throws IOException;
	
	/** Gets an input stream to the cache.
	 * @param tmp true if the temporary cache is required
	 * @return An input stream.
	 * @throws IOException if an error occurs while creating the reader. 
	 */
	public InputStream getInputStream(boolean tmp) throws IOException;
	
	/** Commits the temporary cache.
	 * <br>This method is called once temporary cache has been successfully parsed.
	 * @exception IOException if the commit fails
	 * @throws IOException 
	 */
	public void commit() throws IOException;
}