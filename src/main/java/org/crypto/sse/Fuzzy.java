package org.crypto.sse;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class Fuzzy {
	final private static String MISSPELLING_RULES_FILE = "MisspellingRules.txt";
	final private static char[] LETTERS = "abcdefghijklmnopqrstuvwxyz".toCharArray();
	final private static Multimap<Pattern, String> MISSPELLING_RULES = ArrayListMultimap.create();
	final private static List<ArrayList<Pattern>> MISSPELLING_FAMILIES = new ArrayList<ArrayList<Pattern>>();
	
	public static void initializeMisspellingRules() {
		if (!MISSPELLING_RULES.isEmpty())
			return;
		
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
	
	public static List<String> getCommonMisspellings(String str){
		initializeMisspellingRules();
		List<String> results = new ArrayList<String>();
		
		for (ArrayList<Pattern> family : MISSPELLING_FAMILIES) {
			for (Pattern pattern : family) {
				
				Matcher matcher = pattern.matcher(str);
				if (matcher.find()) {
					int resultSize = results.size();
					
					for (int i = 0; i < resultSize; ++i) {
						results.addAll(applyRules(results.get(i), pattern));
					}
					results.addAll(applyRules(str, pattern));
					
					break;
				}
			}
		}
		
		return results;
	}
	
	private static List<String> applyRules (String str, Pattern rule) {
		List<String> results = new ArrayList<String>();
		Matcher matcher = rule.matcher(str);
		
		while (matcher.find()) {
			for (String alternative : MISSPELLING_RULES.get(rule))
			{
				results.add(str.substring(0, matcher.start()) + alternative + str.substring(matcher.end()));
			}
		}
		
		return results;
	}
	
	public static List<String> getAllStringsOfEditDistance1(String str) {
		
		List<String> results = new ArrayList<String>();
		
		for (int i = 0; i < str.length(); ++i) {
			//Deletion
			results.add(str.substring(0, i) + str.substring(i + 1));
		}
		
		for (char letter : LETTERS) {
			for (int i = 0; i < str.length(); ++i) {
				//Substitution
				if (letter != str.charAt(i)) {
					results.add(str.substring(0, i) + letter + str.substring(i + 1));
					//Insertion
					results.add(str.substring(0, i) + letter + str.substring(i));
				}
			}
			//Insertion
			results.add(str + letter);
		}
		
		return results;
	}
	
//https://howtodoinjava.com/algorithm/implement-phonetic-search-using-soundex-algorithm/
	public static String getGode(String s) {
		char[] x = s.toUpperCase().toCharArray();

		char firstLetter = x[0];

		// RULE [ 2 ]
		// Convert letters to numeric code
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

		// Remove duplicates
		// RULE [ 1 ]
		String output = "" + firstLetter;

		// RULE [ 3 ]
		for (int i = 1; i < x.length; i++)
			if (x[i] != x[i - 1] && x[i] != '0')
				output += x[i];

		// RULE [ 4 ]
		// Pad with 0's or truncate
		output = output + "0000";
		return output.substring(0, 4);
	}
}
