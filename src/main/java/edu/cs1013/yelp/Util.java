package edu.cs1013.yelp;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Miscellaneous utility functions.
 *
 * @author Jack O'Sullivan
 */
public class Util {
	public static final DateFormat SQL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	public static final DateTimeFormatter YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

	public static List<Field> getAllFields(Class clazz) {
		List<Field> fields = new ArrayList<>();
		while (clazz != null) {
			fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
			clazz = clazz.getSuperclass();
		}

		return fields;
	}
	public static String toSqlSearch(String term) {
		String escaped = term.replace("!", "!!").replace("%", "!%")
				.replace("_", "!_").replace("[", "![");
		return "%"+escaped+"%";
	}
	public static <K, V> Map<K, V> singleEntryMap(K key, V value) {
		return new SingleEntryMap<>(key, value);
	}
	public static <E> Set<E> singleElementSet(E element) {
		return new SingleElementSet<>(element);
	}
	@SuppressWarnings("unchecked")
	public static <A, B> B[] castArray(A[] array, Class<B> bType) {
		B[] casted = (B[])Array.newInstance(bType, array.length);
		for (int i = 0; i < array.length; i++) {
			casted[i] = (B)array[i];
		}

		return casted;
	}
	public static String[] numericLabels(int start, int end, boolean descending) {
		String[] labels = new String[Math.abs(end - start)];
		for (int i = 0; i < labels.length; i++) {
			labels[i] = String.valueOf(descending ? start - i : start + i);
		}

		return labels;
	}
	public static Date dateFromString(String date) {
		try {
			return SQL_DATE_FORMAT.parse(date);
		} catch (ParseException | NumberFormatException ex) {
			return null;
		}
	}
	public static String dateToString(Date date) {
		if (date == null) {
			return "0000-00-00";
		}

		return SQL_DATE_FORMAT.format(date);
	}
	public static int dateToYearMonth(LocalDate date) {
		return Integer.parseInt(YEAR_MONTH_FORMAT.format(date));
	}

	private static class SingleEntryMap<K, V> extends AbstractMap<K, V> {
		private Set<Entry<K, V>> entrySet;
		public SingleEntryMap() {
			entrySet = new SingleElementSet<>();
		}
		public SingleEntryMap(K key, V value) {
			entrySet = new SingleElementSet<>(new AbstractMap.SimpleEntry<>(key, value));
		}

		@Override
		public Set<Entry<K, V>> entrySet() {
			return entrySet;
		}
	}
	private static class SingleElementSet<E> extends AbstractSet<E> {
		private boolean hasItem;
		private E element;
		public SingleElementSet() {}
		public SingleElementSet(E element) {
			hasItem = true;
			this.element = element;
		}
		@Override
		public Iterator iterator() {
			return new Iterator();
		}
		@Override
		public int size() {
			return hasItem ? 1 : 0;
		}

		private class Iterator implements java.util.Iterator<E> {
			private boolean iterated;

			@Override
			public boolean hasNext() {
				return !iterated;
			}
			@Override
			public E next() {
				if (iterated) {
					throw new NoSuchElementException();
				}

				iterated = true;
				return element;
			}
		}
	}
}
