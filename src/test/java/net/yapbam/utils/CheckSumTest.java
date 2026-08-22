package net.yapbam.utils;

import static org.junit.jupiter.api.Assertions.*;

import net.yapbam.util.CheckSum;

import org.junit.jupiter.api.Test;

class CheckSumTest {

	@Test
	void test() {
		test (new byte[] {0,127});
		test (new byte[] {127,32});
		test (new byte[] {-1,32});
		test (new byte[] {7,32});
	}

	private void test(byte[] bytes) {
		String toString = CheckSum.toString(bytes);
		byte[] toBytes = CheckSum.toBytes(toString);
		assertArrayEquals(bytes, toBytes);
	}

}
