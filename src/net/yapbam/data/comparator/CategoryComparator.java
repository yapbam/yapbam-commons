package net.yapbam.data.comparator;

import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

import net.yapbam.data.Category;
import net.yapbam.data.GlobalData;

public class CategoryComparator implements Comparator<Category> {
	private Collator nameComparator;
	private char categorySeparator;

	public CategoryComparator(Locale locale, char categorySeparator) {
		this.nameComparator = Collator.getInstance(locale);
		this.categorySeparator = categorySeparator;
	}

	@Override
	public int compare(Category cat1, Category cat2) {
		String name1 = cat1.getName();
		String name2 = cat2.getName();
		int index1 = 0;
		int index2 = 0;
		while (index1>=0 || index2>=0) {
			String root1;
			String root2;
			if (index1>=0) {
				int old1 = index1;
				index1 = name1.indexOf(categorySeparator, index1);
				root1 = index1<0 ? name1.substring(old1) : name1.substring(old1, index1);
			} else {
				root1 = "";
			}
			if (index2>=0) {
				int old2 = index2;
				index2 = name2.indexOf(categorySeparator, index2);
				root2 = index2<0 ? name2.substring(old2) : name2.substring(old2, index2);
			} else {
				root2 = "";
			}
			int result = nameComparator.compare(root1, root2);
			if (result!=0) {
				return result;
			}
			if (index1>=0) {
				index1++;
			} else {
				name1 = "";
			}
			if (index2>=0) {
				index2++;
			} else {
				name2 = "";
			}
		}
		return 0;
	}
	
	public static Category[] getSortedCategories(GlobalData data, Locale locale) {
		Category[] categories = new Category[data.getCategoriesNumber()];
		for (int i = 0; i < data.getCategoriesNumber(); i++) {
			categories[i] = data.getCategory(i);
		}
		Arrays.sort(categories, new CategoryComparator(locale, data.getSubCategorySeparator()));
		return categories;
	}
}
