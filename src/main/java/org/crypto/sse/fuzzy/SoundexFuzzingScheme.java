package org.crypto.sse.fuzzy;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Multimap;

/**
 * Produces pseudo edges between the soundex that a keyword maps to
 * and all keywords that map to that soundex.
 * 
 * https://en.wikipedia.org/wiki/Soundex
 * 
 * @author Ryan Estes
 * @see {@link SoundexCloseWordsFuzzingScheme}
 */
public class SoundexFuzzingScheme extends IFuzzingScheme {
	
	/**
	 * See {@link IFuzzingScheme#IFuzzingScheme()}.
	 */
	public SoundexFuzzingScheme() {}
	
	/**
	 * See {@link IFuzzingScheme#IFuzzingScheme(String)}
	 * 
	 * @param prefix
	 */
	public SoundexFuzzingScheme(String prefix) {
		super(prefix);
	}

	@Override
	public void fuzzingScheme(
			String keyword,
			Multimap<String, String> origin,
			Multimap<String, String> mm1,
			Multimap<String, String> mm2) {
		
		String soundex = getSoundex(keyword);
		for (String file : origin.get(keyword)) {
			insertKeyword(file, "", soundex, mm1, mm2);
		}
	}

	@Override
	public List<String> getEdges(String word) {
		List<String> edges = new ArrayList<String>();
		insertEdge(edges, "", getSoundex(word));
		return edges;
	}
	
	/**
	 * Get the soundex code of a string.
	 * 
	 * https://howtodoinjava.com/algorithm/implement-phonetic-search-using-soundex-algorithm/
	 * 
	 * @param s
	 * @return Soundex of s
	 */
	public static String getSoundex(String s) {
		char[] x = s.toUpperCase().toCharArray();

		char firstLetter = x[0];

		for (int i = 0; i < x.length; i++) {
			switch (x[i]) {
			case 'B':
			case 'F':
			case 'P':
			case 'V': {
				x[i] = '1';
				break;
			}

			case 'C':
			case 'G':
			case 'J':
			case 'K':
			case 'Q':
			case 'S':
			case 'X':
			case 'Z': {
				x[i] = '2';
				break;
			}

			case 'D':
			case 'T': {
				x[i] = '3';
				break;
			}

			case 'L': {
				x[i] = '4'; 
				break;
			}

			case 'M':
			case 'N': {
				x[i] = '5';
				break;
			}

			case 'R': {
				x[i] = '6';
				break;
			}

			default: {
				x[i] = '0';
				break;
			}
			}
		}

		String output = "" + firstLetter;

		for (int i = 1; i < x.length; i++)
			if (x[i] != x[i - 1] && x[i] != '0')
				output += x[i];

		output = output + "0000";
		return output.substring(0, 4);
	}
}
