package net.yapbam.remote;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/** A Cache.
 * <br>This converter use cache in order to preserve web server resources, and to be able to work with no Internet connection.
 * <br>To improve the cache robustness, the cache may have two levels:<ol>
 * <li>A temporary cache that is used to store the data red from Internet.</li>
 * <li>A persistent cache that saved the temporary after it was validated by a successful parsing (see {@link #commit()}).</li></ol>
 * @author Jean-Marc Astesana
 */
public interface Cache {
	/** Tests whether the cache is empty or not.
	 * <br>The cache is empty if both temporary and saved cache are not available.
	 * @return
	 */
	public boolean isEmpty();
	
	/** Gets a writer to the temporary cache.
	 * @return A writer.
	 * @throws IOException if an error occurs while creating the writer. 
	 */
	public Writer getWriter() throws IOException;
	
	/** Gets a reader to the cache.
	 * @param tmp true if the temporary cache is required
	 * @return A reader.
	 * @throws IOException if an error occurs while creating the reader. 
	 */
	public Reader getReader(boolean tmp) throws IOException;
	
	/** Commits the temporary cache.
	 * <br>This method is called once temporary cache has been successfully parsed.
	 */
	public void commit();
}