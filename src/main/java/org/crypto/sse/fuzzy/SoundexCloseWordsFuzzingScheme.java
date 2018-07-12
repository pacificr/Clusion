package org.crypto.sse.fuzzy;

import java.util.Collection;
import java.util.HashSet;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class SoundexCloseWordsFuzzingScheme extends IFuzzingScheme{
	
	private Multimap<String, String> edges = ArrayListMultimap.create();
	
	public SoundexCloseWordsFuzzingScheme() {}
	
	public SoundexCloseWordsFuzzingScheme(String prefix) {
		super(prefix);
	}

	@Override
	public void fuzzingScheme(
			String keyword,
			Multimap<String, String> origin,
			Multimap<String, String> mm1,
			Multimap<String, String> mm2) {
		
		Collection<String> codes = new HashSet<String>();
		codes.add(SoundexFuzzingScheme.getSoundex(keyword));
		for (String misspelling : MisspellingFuzzingScheme.getMisspellings(keyword)) {
			codes.add(SoundexFuzzingScheme.getSoundex(misspelling));
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
		String soundex = SoundexFuzzingScheme.getSoundex(word);
		for (String suggestion : edges.get(soundex)) {
			insertEdge(results, suggestion, soundex);
		}
		return results;
	}
}
