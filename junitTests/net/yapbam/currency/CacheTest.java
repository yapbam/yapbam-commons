package net.yapbam.currency;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.CharBuffer;
import java.security.AccessControlException;

import org.junit.Test;

public class CacheTest {
	private static final CharSequence FIRST = "first";
	private static final CharSequence SECOND = "second";

	@Test
	public void test() throws IOException {
		MemoryCache cache = new MemoryCache();
		assertTrue(cache.isEmpty());
		setCache(cache, FIRST);
		assertFalse(cache.isEmpty());
		assertEquals(FIRST, getCacheContent(cache, true));
		cache.commit();
		assertFalse(cache.isEmpty());
		assertEquals(FIRST, getCacheContent(cache, false));
		setCache(cache, SECOND);
		assertEquals(SECOND, getCacheContent(cache, true));
		assertEquals(FIRST, getCacheContent(cache, false));
	}
	
	@Test (expected = FileNotFoundException.class)
	public void testNoTmp() throws IOException {
		MemoryCache cache = new MemoryCache();
		cache.getReader(true);
	}

	@Test (expected = FileNotFoundException.class)
	public void testNoCommited1() throws IOException {
		MemoryCache cache = new MemoryCache();
		cache.getReader(false);
	}

	@Test (expected = FileNotFoundException.class)
	public void testNoCommited2() throws IOException {
		MemoryCache cache = new MemoryCache();
		setCache(cache, FIRST);
		cache.getReader(false);
	}

	private void setCache(MemoryCache cache, CharSequence content) throws IOException {
		Writer writer = cache.getWriter();
		writer.append(content);
		writer.close();
	}

	private CharSequence getCacheContent(MemoryCache cache, boolean tmp) throws IOException {
		CharBuffer buffer = CharBuffer.allocate(1024);
		Reader reader = cache.getReader(tmp);
		for (int nb = reader.read(buffer); nb<0; nb = reader.read(buffer)) {
			// Read the buffer
		}
		buffer.flip();
		return buffer.toString();
	}
}
