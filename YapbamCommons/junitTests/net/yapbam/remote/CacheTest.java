package net.yapbam.remote;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.CharBuffer;

import net.yapbam.remote.MemoryCache;

import org.junit.Test;

public class CacheTest {
	private static final CharSequence FIRST = "first";
	private static final CharSequence SECOND = "second";

	@Test
	public void test() throws IOException {
		MemoryCache cache = new MemoryCache();
		assertTrue(cache.isEmpty());
		assertTrue(cache.getTimeStamp()<0);
		setCache(cache, FIRST);
		assertTrue(cache.getTimeStamp()<0);
		assertFalse(cache.isEmpty());
		assertEquals(FIRST, getCacheContent(cache, true));
		cache.commit();
		assertTrue(cache.getTimeStamp()>0);
		assertFalse(cache.isEmpty());
		assertEquals(FIRST, getCacheContent(cache, false));
		setCache(cache, SECOND);
		assertEquals(SECOND, getCacheContent(cache, true));
		assertEquals(FIRST, getCacheContent(cache, false));
	}
	
	@Test (expected = FileNotFoundException.class)
	public void testNoTmp() throws IOException {
		MemoryCache cache = new MemoryCache();
		cache.getInputStream(true);
	}

	@Test (expected = FileNotFoundException.class)
	public void testNoCommited1() throws IOException {
		MemoryCache cache = new MemoryCache();
		cache.getInputStream(false);
	}

	@Test (expected = FileNotFoundException.class)
	public void testNoCommited2() throws IOException {
		MemoryCache cache = new MemoryCache();
		setCache(cache, FIRST);
		cache.getInputStream(false);
	}

	private void setCache(MemoryCache cache, CharSequence content) throws IOException {
		Writer writer = new OutputStreamWriter(cache.getOutputStream());
		writer.append(content);
		writer.close();
	}

	private CharSequence getCacheContent(MemoryCache cache, boolean tmp) throws IOException {
		CharBuffer buffer = CharBuffer.allocate(1024);
		Reader reader = new InputStreamReader(cache.getInputStream(tmp));
		for (int nb = reader.read(buffer); nb<0; nb = reader.read(buffer)) {
			// Read the buffer
		}
		buffer.flip();
		return buffer.toString();
	}
}
