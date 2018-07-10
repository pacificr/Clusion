package org.crypto.sse.fuzzy;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Multimap;

public class NGramCloseWordsFuzzingScheme extends IFuzzingScheme{
	
	private int n;
	final private static int NGRAM_REQUIREMENT = 3;
	
	public NGramCloseWordsFuzzingScheme(String prefix, int n) {
		super(prefix + n);
		this.n = n;
	}

	@Override
	public void fuzzingScheme(
			String keyword,
			Multimap<String, String> origin,
			Multimap<String, String> mm1,
			Multimap<String, String> mm2) {
		
		List<String> nGrams = getNGramCloseWords(keyword, n);
		for (String file : origin.get(keyword)) {
			for (String word : nGrams) {
				insertKeyword(file, keyword, word, mm1, mm2);
			}
		}
	}

	@Override
	public List<String> getEdges(String word) {
		List<String> edges = new ArrayList<String>();
		
		for (String closeWord : getNGramCloseWords(word, n)) {
			insertEdge(edges, closeWord, word);
		}
		
		return edges;
	}
	
	public static List<String> getNGramCloseWords(String str, int n){
		List<String> results = new ArrayList<String>();
		
		for (String word : Fuzzy.DICTIONARY) {
			int matches = 0;
			for (String gram1 : NGramsFuzzingScheme.getNGrams(word, n)) {
				for (String gram2 : NGramsFuzzingScheme.getNGrams(str, n)) {
					if (gram1.equals(gram2)) {
						matches++;
					}
				}
			}
			if (!str.equals(word)) {
				int minLength = Math.min(word.length(), str.length());
				int maxLength = Math.max(word.length(), str.length());
				
				if (matches > 1 && matches > Math.min(minLength - n, maxLength - n - NGRAM_REQUIREMENT)) {
					results.add(word);
				}
			}
		}
		
		return results;
	}

}
