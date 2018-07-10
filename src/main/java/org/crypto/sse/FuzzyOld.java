package org.crypto.sse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.tartarus.snowball.ext.PorterStemmer;

import java.util.regex.Matcher;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class FuzzyOld {
	final private static String MISSPELLING_RULES_FILE = "MisspellingRules.txt";
	final private static char[] LETTERS = "abcdefghijklmnopqrstuvwxyz".toCharArray();
	final private static Multimap<Pattern, String> MISSPELLING_RULES = ArrayListMultimap.create();
	final private static List<ArrayList<Pattern>> MISSPELLING_FAMILIES = new ArrayList<ArrayList<Pattern>>();
	final private static PorterStemmer STEMMER = new PorterStemmer();
	final private static List<String> DICTIONARY = new ArrayList<String>();
	final public static JazzySpellChecker JAZZY = new JazzySpellChecker();
	
	final public static String DICTIONARY_FILE = "2of12.txt";

	final private static int NGRAM_REQUIREMENT = 3;
	
	static {

		BufferedReader fileIn;
		
		try {
			fileIn = new BufferedReader(new FileReader(new File(DICTIONARY_FILE)));
			String line;
			while ((line = fileIn.readLine()) != null) {
				DICTIONARY.add(line);
			}
			fileIn.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
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
						results.addAll(applyRules(results.get(i), pattern, str));
					}
					results.addAll(applyRules(str, pattern, str));
					
					break;
				}
			}
		}
		
		return results;
	}
	
	private static List<String> applyRules (String str, Pattern rule, String originalStr) {
		List<String> results = new ArrayList<String>();
		Matcher matcher = rule.matcher(str);
		
		while (matcher.find()) {
			for (String alternative : MISSPELLING_RULES.get(rule))
			{
				String word = str.substring(0, matcher.start()) + alternative + str.substring(matcher.end()).intern();
				List<String> suggestions = JAZZY.getSuggestions(word);
				Printer.extra("Misspeling attempt: " + originalStr + ", " + word + ", " + suggestions);
				if (suggestions.size() > 0 && originalStr.equals(suggestions.get(0))) {
					results.add(word);
					Printer.extra(", yes");
				}
				Printer.extraln("");
			}
		}
		
		return results;
	}
	
	public static List<String> getNGrams(String str, int n) {
		List<String> results = new ArrayList<String>();
		
		for (int i = 0; i < str.length() - n + 1; ++i) {
			results.add(str.substring(i, i+n));
		}
		
		return results;
	}
	
	public static List<String> getNGramCloseWords(String str, int n){
		return getNGramCloseWords(str, n, 1000);
	}
	
	public static List<String> getNGramCloseWords(String str, int n, int max){
		List<String> results = new ArrayList<String>();
		
		for (String word : DICTIONARY) {
			int matches = 0;
			for (String gram1 : getNGrams(word, n)) {
				for (String gram2 : getNGrams(str, n)) {
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
		
		//Limit number of results
		if (results.size() > max) {
			Multimap<Integer, String> occurances = ArrayListMultimap.create();
			int num = 0;
			int tippingPoint = 0;
			int most = 0;
			
			for (String word : results) {
				int editDistance = getEditDistance(str, word);
				occurances.put(editDistance, word);
				if (editDistance > most) {
					most = editDistance;
				}
			}
			
			//printMultimapInt(occurances);
			
			for (int i = 1; num < max ; ++i) {
				num += occurances.get(i).size();
				tippingPoint = i;
			}
			
			for (int i = tippingPoint; i <= most; ++i) {
				for (String word : occurances.get(i)) {
					results.remove(word);
				}
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
	
	//https://www.programcreek.com/2013/12/edit-distance-in-java/
	public static int getEditDistance(String word1, String word2) {
		int len1 = word1.length();
		int len2 = word2.length();
	 
		// len1+1, len2+1, because finally return dp[len1][len2]
		int[][] dp = new int[len1 + 1][len2 + 1];
	 
		for (int i = 0; i <= len1; i++) {
			dp[i][0] = i;
		}
	 
		for (int j = 0; j <= len2; j++) {
			dp[0][j] = j;
		}
	 
		//iterate though, and check last char
		for (int i = 0; i < len1; i++) {
			char c1 = word1.charAt(i);
			for (int j = 0; j < len2; j++) {
				char c2 = word2.charAt(j);
	 
				//if last two chars equal
				if (c1 == c2) {
					//update dp value for +1 length
					dp[i + 1][j + 1] = dp[i][j];
				} else {
					int replace = dp[i][j] + 1;
					int insert = dp[i][j + 1] + 1;
					int delete = dp[i + 1][j] + 1;
	 
					int min = replace > insert ? insert : replace;
					min = delete > min ? min : delete;
					dp[i + 1][j + 1] = min;
				}
			}
		}
	 
		return dp[len1][len2];
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
	
	public static String getStem(String word) {
		STEMMER.setCurrent(word);
		STEMMER.stem();
		return STEMMER.getCurrent();
	}
	
	public static void main(String[] args) {
		Printer.addPrinter(new Printer(Printer.LEVEL.EXTRA));
		Printer.debugln("Test");
		
		Multimap <String, String> test = makeNGramMap(DICTIONARY_FILE);
		//Multimap <String, String> test = makeMisspellingMap(DICTIONARY);
		//Multimap <String, String> test = makeStemmingMap(DICTIONARY);
		//Multimap <String, String> test = makeSoundex(DICTIONARY);
		
		printMultimap(test);
		
	}
	
	public static void printMultimap(Multimap<String,String> mm) {
		int numKeys = 0;
		int numElements = 0;
		for (String key : mm.keySet()) {
			++numKeys;
			Printer.debugln(key);
			for (String word : mm.get(key)) {
				++numElements;
				Printer.debugln("\t" + word);
			}
//			Printer.debugln(System.identityHashCode(key) + "(" + key + ")");
//			for (String word : mm.get(key)) {
//				++numElements;
//				Printer.debugln("\t" + System.identityHashCode(word) + "(" + word + ")");
//			}
		}
		Printer.debugln("Number of Keys: " + numKeys + ". Number of Elements: " + numElements);
	}
	
	public static Multimap<String, String> makeSoundex() {
		Multimap<String, String> output = ArrayListMultimap.create();
		for (String line : DICTIONARY) {
			output.put(getGode(line).intern(), line.intern());
		}
		return output;
	}
	
	public static Multimap<String, String> makeStemmingMap(String in) {
		Multimap<String, String> output = ArrayListMultimap.create();
		PorterStemmer stemmer = new PorterStemmer();
		for (String line : DICTIONARY) {
			stemmer.setCurrent(line);
      stemmer.stem();
			output.put((stemmer.getCurrent()).intern(), line.intern());
		}
		
		return output;
	}
	
	public static Multimap<String, String> makeNGramMap(String in) {
		Multimap<String, String> output = ArrayListMultimap.create();
		List<String> words = new ArrayList<String>();
		int n = 3;
		
		int done = 0;
		for (String line : DICTIONARY) {
			for (String word : words) {
				int matches = 0;
				for (String gram1 : getNGrams(word, n)) {
					for (String gram2 : getNGrams(line, n)) {
						if (gram1.equals(gram2)) {
							matches++;
						}
					}
				}
				int minLength = Math.min(word.length(), line.length());
				int maxLength = Math.max(word.length(), line.length());
				
				if (matches > 1 && matches > Math.min(minLength - n, maxLength - n - NGRAM_REQUIREMENT)) {
					output.put(word.intern(), line.intern());
					output.put(line.intern(), word.intern());
				}
			}
			
			if(done++ > 1000) {
				break;
			}
			words.add(line);
		}
		
		return output;
	}
}
