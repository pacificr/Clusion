package org.crypto.sse.fuzzy;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Multimap;

public class CommonMisspellingFuzzingScheme extends IFuzzingScheme{
	
	final private static String MISSPELLING_RULES_FILE = "MisspellingRules.txt";
	final private static List<Family> FAMILIES = new ArrayList<Family>();
	
	
	public CommonMisspellingFuzzingScheme(String prefix) {
		super(prefix);
	}
	
	@Override
	public void fuzzingScheme(
			String keyword,
			Multimap<String, String> origin,
			Multimap<String, String> mm1,
			Multimap<String, String> mm2) {
		
		List<String> fuzzyWords = new ArrayList<String>();
		
		for (Family family : FAMILIES) {
			family.apply(keyword, fuzzyWords);
		}
		
		for (String file : origin.get(keyword)) {
			for (String word : fuzzyWords) {
				insertKeyword(file, keyword, word, mm1, mm2);
			}
		}
	}

	@Override
	public List<String> getEdges(String word) {
		List<String> edges = new ArrayList<String>();
		
		for (String suggestion : Fuzzy.JAZZY.getSuggestions(word)) {
			if (!suggestion.equals(word)) {
				insertEdge(edges, suggestion, word);
			}
		}
		
		return edges;
	}
	
	static {
		try {
			BufferedReader misspelledRulesReader = new BufferedReader (new FileReader(MISSPELLING_RULES_FILE));
			String line;
			
			FAMILIES.add(new Family());
			Family current = new Family();
			
			while ((line = misspelledRulesReader.readLine()) != null) {
				if (line.equals("")) {
					current.compilePatterns();
					FAMILIES.add(current);
					current = new Family();
				} else if (line.startsWith("!")) {
					current.modify(line);
				} else {
					current.addWord(line);
				}
			}
			
			current.compilePatterns();
			FAMILIES.add(current);
			
			misspelledRulesReader.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static class Family {
		private String frontStr = "";
		private String backStr = "";
		private int frontNum = 0;
		private int backNum = 0;
		private List<Pattern> patterns = new ArrayList<Pattern>();
		private List<String> sequences = new ArrayList<String>();
		
		public void modify(String modification) {
			switch(modification) {
			case "!s":
				frontStr = "\\A";
				break;
			case "!e":
				backStr = "\\z";
				break;
			case "!ns":
				frontNum = 1;
				frontStr = "\\S";
				break;
			case "!ne":
				backNum = 1;
				backStr = "\\S";
				break;
			}
		}
		
		public void addWord(String word) {
			sequences.add(word);
		}
		
		public void compilePatterns() {
			for (String word : sequences) {
				patterns.add(Pattern.compile(frontStr + word + backStr));
			}
		}
		
		public void apply(String str, List<String> results){
			for (int patternIndex = 0; patternIndex < patterns.size(); ++patternIndex) {
				
				Matcher matcher = patterns.get(patternIndex).matcher(str);
				if (matcher.find()) {
					int resultSize = results.size();
					
					for (int i = 0; i < resultSize; ++i) {
						results.addAll(applyRule(results.get(i), patternIndex));
					}
					results.addAll(applyRule(str, patternIndex));
					
					break;
				}
			}
		}
		
		private List<String> applyRule (String str, int index) {
			List<String> results = new ArrayList<String>();
			Matcher matcher = patterns.get(index).matcher(str);
			
			while (matcher.find()) {
				for (int i = 0; i < sequences.size(); ++i)
				{
					if (index != i)
						results.add(
								str.substring(0, matcher.start() + frontNum)
								+ sequences.get(i)
								+ str.substring(matcher.end() - backNum).intern()
						);
				}
			}
			
			return results;
		}
	}
}
