package org.crypto.sse.fuzzy;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Multimap;

public class NGramsFuzzingScheme extends IFuzzingScheme{

	private int n;
	
	public NGramsFuzzingScheme(String prefix, int n) {
		super(prefix + n);
		this.n = n;
	}

	@Override
	protected void fuzzingScheme(
			String keyword,
			Multimap<String, String> origin,
			Multimap<String, String> mm1,
			Multimap<String, String> mm2) {
		
		List<String> nGrams = getNGrams(keyword, n);
		for (String file : origin.get(keyword)) {
			for (String nGram : nGrams) {
				insertKeyword(file, "", nGram, mm1, mm2);
			}
		}
	}

	@Override
	public List<String> getEdges(String word) {
		List<String> edges = new ArrayList<String>();
		
		for (String nGram : getNGrams(word, n)) {
			insertEdge(edges, "", nGram);
		}
		
		return edges;
	}

	public static List<String> getNGrams(String str, int n) {
		List<String> results = new ArrayList<String>();
		
		for (int i = 0; i < str.length() - n + 1; ++i) {
			results.add(str.substring(i, i+n));
		}
		
		return results;
	}
}
