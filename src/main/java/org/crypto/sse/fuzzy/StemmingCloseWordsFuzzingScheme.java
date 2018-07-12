package org.crypto.sse.fuzzy;

import java.util.Collection;
import java.util.HashSet;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class StemmingCloseWordsFuzzingScheme extends IFuzzingScheme{

	private Multimap<String, String> edges = ArrayListMultimap.create();
	
	public StemmingCloseWordsFuzzingScheme() {}
	
	public StemmingCloseWordsFuzzingScheme(String prefix) {
		super(prefix);
	}

	@Override
	public void fuzzingScheme(
			String keyword,
			Multimap<String, String> origin,
			Multimap<String, String> mm1,
			Multimap<String, String> mm2) {
		
		Collection<String> codes = new HashSet<String>();
		codes.add(StemmingFuzzingScheme.getStem(keyword));
		for (String misspelling : MisspellingFuzzingScheme.getMisspellings(keyword)) {
			codes.add(StemmingFuzzingScheme.getStem(misspelling));
		}
		
		for (String file : origin.get(keyword)) {
			for (String soundex : codes) {
				insertKeyword(file, keyword, soundex, mm1, mm2);
				edges.put(soundex, keyword);
			}
		}
	}

	@Override
	public Collection<String> getEdges(String word) {
		Collection<String> results = new HashSet<String>();
		String stem = StemmingFuzzingScheme.getStem(word);
		for (String suggestion : edges.get(stem)) {
			insertEdge(results, suggestion, stem);
		}
		return results;
	}
}
