package net.yapbam.data;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.yapbam.data.comparator.CategoryComparator;

import org.junit.Test;

public class CategoryComparatorTest {

	@Test
	public void test() {
		Category c1 = new Category("aZBC");
		Category c2 = new Category("ABZC");
		Category c3 = new Category("BZaC");
		Category c4 = new Category("Aa");
		
		Category[] test = new Category[] {c1,c2,c3,c4, Category.UNDEFINED};
		Arrays.sort(test, new CategoryComparator(Locale.FRANCE, 'Z'));
		List<Category> sorted = Arrays.asList(test);
		System.out.println (sorted);
		
		assertEquals(0, sorted.indexOf(Category.UNDEFINED));
		assertEquals(1, sorted.indexOf(c1));
		assertEquals(2, sorted.indexOf(c4));
		assertEquals(3, sorted.indexOf(c2));
		assertEquals(4, sorted.indexOf(c3));
	}

	@Test
	public void test2() {
		Category c1 = new Category("AZBZA");
		Category c2 = new Category("AZB");
		assertEquals(1, new CategoryComparator(Locale.FRANCE, 'Z').compare(c1, c2));
	}

	@Test
	public void test3() {
		Category c1 = new Category("AZBZ");
		Category c2 = new Category("AZBZ");
		assertEquals(0, new CategoryComparator(Locale.FRANCE, 'Z').compare(c1, c2));
	}

	@Test
	public void test4() {
		Category c1 = new Category("AZB");
		Category c2 = new Category("AZBZBZB");
		assertEquals(-1, new CategoryComparator(Locale.FRANCE, 'Z').compare(c1, c2));
		assertEquals(1, new CategoryComparator(Locale.FRANCE, 'Z').compare(c2, c1));
	}
}
