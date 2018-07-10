package org.crypto.sse.fuzzy;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class CommonMisspellingFuzzingScheme extends IFuzzingScheme{
	
	final private static String MISSPELLING_RULES_FILE = "MisspellingRules.txt";
	final private static Multimap<Pattern, String> MISSPELLING_RULES = ArrayListMultimap.create();
	final private static List<ArrayList<Pattern>> MISSPELLING_FAMILIES = new ArrayList<ArrayList<Pattern>>();

	public CommonMisspellingFuzzingScheme(String prefix) {
		super(prefix);
	}
	
	@Override
	public void fuzzingScheme(
			String keyword,
			Multimap<String, String> origin,
			Multimap<String, String> mm1,
			Multimap<String, String> mm2) {
		List<String> fuzzyWords = getCommonMisspellings(keyword);
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
			List<Pattern> currentPatterns = new ArrayList<Pattern>();
			List<String> currentStrings = new ArrayList<String>();
			MISSPELLING_FAMILIES.add(new ArrayList<Pattern>());
			
			while ((line = misspelledRulesReader.readLine()) != null) {
				if (line.equals("")) {
					currentPatterns.clear();
					currentStrings.clear();
					MISSPELLING_FAMILIES.add(new ArrayList<Pattern>());
				} else {
					Pattern pattern = Pattern.compile(line.substring(0, line.indexOf(' ')));
					String string = line.substring(line.indexOf(' ') + 1);
					
					for (int i = 0; i < currentPatterns.size(); ++i) {
						MISSPELLING_RULES.put(pattern, currentStrings.get(i).intern());
						MISSPELLING_RULES.put(currentPatterns.get(i), string.intern());
					}
					
					currentPatterns.add(pattern);
					currentStrings.add(string.intern());
					
					MISSPELLING_FAMILIES.get(MISSPELLING_FAMILIES.size() - 1).add(pattern);
				}
			}
			
			misspelledRulesReader.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private List<String> getCommonMisspellings(String str){
		List<String> results = new ArrayList<String>();
		
		for (ArrayList<Pattern> family : MISSPELLING_FAMILIES) {
			for (Pattern pattern : family) {
				
				Matcher matcher = pattern.matcher(str);
				if (matcher.find()) {
					int resultSize = results.size();
					
					for (int i = 0; i < resultSize; ++i) {
						results.addAll(applyRules(results.get(i), pattern, str));
					}
					results.addAll(applyRules(str, pattern, str));
					
					break;
				}
			}
		}
		
		return results;
	}
	
	private List<String> applyRules (String str, Pattern rule, String originalStr) {
		List<String> results = new ArrayList<String>();
		Matcher matcher = rule.matcher(str);
		
		while (matcher.find()) {
			for (String alternative : MISSPELLING_RULES.get(rule))
			{
				results.add(str.substring(0, matcher.start()) + alternative + str.substring(matcher.end()).intern());
			}
		}
		
		return results;
	}
}
