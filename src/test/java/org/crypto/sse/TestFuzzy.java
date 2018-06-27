package org.crypto.sse;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class TestFuzzy {
	
	private static BufferedReader keyRead;
	final private static int N = 3;
	final private static int MAX = 15;
	
	public static void main(String[] args) throws Exception {

		keyRead = new BufferedReader(new InputStreamReader(System.in));

		System.out.println("Enter your password:");

		String pass = keyRead.readLine();

		//----Quick test things
		JazzySpellChecker jazzy = new JazzySpellChecker();
		System.out.println("Jazzy Suggestions:  " + jazzy.getSuggestions(pass));
		System.out.println("Stem:               " + Fuzzy.getStem(pass));
		System.out.println("Soundex:            " + Fuzzy.getGode(pass));
		System.out.println("Misspellings:       " + Fuzzy.getCommonMisspellings(pass));
		System.out.println(N + "-grams:           " + Fuzzy.getNGrams(pass, N));
		System.out.println("Matching " + N + "-grams:  " + Fuzzy.getNGramCloseWords(pass, N));
		//----

		List<byte[]> listSK = IEX2Lev.keyGen(256, pass, "salt/salt", 100000);

		System.out.println("Enter the relative path name of the folder that contains the files to make searchable: ");

		String pathName = keyRead.readLine();
		
		if (pathName.equals("")) {
			pathName = "/home/ryan/Documents/maildir/bailey-s/inbox";
			//pathName = "/home/ryan/Documents/test/onlysmall";
			System.out.println(pathName);
		}

		ArrayList<File> listOfFile = new ArrayList<File>();
		TextProc.listf(pathName, listOfFile);

		TextProc.TextProc(false, pathName);

		int bigBlock = 1000;
		int smallBlock = 100;
		
		//----Add Fuzzyness to multimaps
		//System.out.println("\nFirst multi-map " + TextExtractPar.lp1);
		//System.out.println("Second multi-map " + TextExtractPar.lp2);
		
		testMM();
		
		//System.out.println("\nNew First multi-map " + TextExtractPar.lp1);
		//System.out.println("New Second multi-map " + TextExtractPar.lp2);
		
		Fuzzy.printMultimap(TextExtractPar.lp2);

		//Pause to look at multimaps
		keyRead.readLine();
		//-----

		IEX2Lev disj = IEX2Lev.setup(listSK, TextExtractPar.lp1, TextExtractPar.lp2, bigBlock, smallBlock, 0);
		//Multimap<String, String> soundex = Fuzzy.makeSoundex(Fuzzy.DICTIONARY);
		//Multimap<String, String> stemming = Fuzzy.makeStemmingMap(Fuzzy.DICTIONARY);

		while (true) {
			System.out.println("How many disjunctions? ");
			int numDisjunctions = Integer.parseInt(keyRead.readLine());

			// Storing the CNF form
			String[][] bool = new String[numDisjunctions][];
			for (int i = 0; i < numDisjunctions; i++) {
				System.out.println("Enter the keywords of the disjunctions ");
				//bool[i] = 
				String[] keys = keyRead.readLine().split(" ");
				List<String> output = new ArrayList<String>();
				
				for (String key : keys) {

					List<String> fixed = Fuzzy.JAZZY.getSuggestions(key);
					
					if (fixed.size() > 0 && fixed.get(0).equals(key)) {//Hacky way to check if the word is in the dictionary
						for (String word : Fuzzy.JAZZY.getSuggestions(key)) {
							output.add("m:" + key + ":" + word);
						}
						
						output.add("t:" + Fuzzy.getStem(key) + ":");
						
//						for (String word : Fuzzy.getNGramCloseWords(key, N)) {
//							output.add("n:" + key + ":" + word);
//						}
					}
					
					output.add("s:" + Fuzzy.getGode(key) + ":");
					
					output.add("r:" + key + ":");
				}
				
				bool[i] = new String[output.size()];
				for (int j = 0; j < output.size(); ++j) {
					System.out.println(output.get(j));
					bool[i][j] = output.get(j);
				}
			}

			TestLocalIEX2Lev.test("log-1.txt", "Test", 1, disj, listSK, bool);
		}
	}
	
	public static void testMM () throws IOException {
		Multimap<String, String> newLp1 = ArrayListMultimap.create();
		Multimap<String, String> newLp2 = ArrayListMultimap.create();
		int test = 0;
		
		for (String key : TextExtractPar.lp1.keySet()) {
			List<String> fixed = Fuzzy.JAZZY.getSuggestions(key);
			
			if (fixed.size() > 0 && fixed.get(0).equals(key)) {//Hacky way to check if the word is in the dictionary
				useCommonMisspellings("m", key, newLp1, newLp2);
				useStemming("t", key, newLp1, newLp2);
			}
			//useNGrams("n", key, newLp1, newLp2);
			useSoundex("s", key, newLp1, newLp2);
			useNormal("r", key, newLp1, newLp2);
			
			if (test++ % 80 == -1) {
				keyRead.readLine();
			}
		}
		
		TextExtractPar.lp1 = newLp1;
		TextExtractPar.lp2 = newLp2;
	}
	
	public static void insertKeyword(String prefix, String file, String keyword, String key, Multimap<String, String> mm1, Multimap<String, String> mm2) {
		String insert = prefix + ":" + keyword + ":" + key;
		mm1.put(insert.intern(), file.intern());
		mm2.put(file.intern(), insert.intern());
	}
	
	public static void useCommonMisspellings(String prefix, String key, Multimap<String, String> mm1, Multimap<String, String> mm2) {
		List<String> fuzzyWords = Fuzzy.getCommonMisspellings(key);
		for (String file : TextExtractPar.lp1.get(key)) {
			for (String word : fuzzyWords) {
				insertKeyword(prefix, file, word, key, mm1, mm2);
			}
		}
	}
	
	public static void useNGrams(String prefix, String key, Multimap<String, String> mm1, Multimap<String, String> mm2) {
		List<String> nGrams = Fuzzy.getNGramCloseWords(key, N, MAX);
		for (String file : TextExtractPar.lp1.get(key)) {
			for (String word : nGrams) {
				insertKeyword(prefix, file, word, key, mm1, mm2);
			}
		}
	}
	
	public static void useSoundex(String prefix, String key, Multimap<String, String> mm1, Multimap<String, String> mm2) {
		for (String file : TextExtractPar.lp1.get(key)) {
			String soundex = Fuzzy.getGode(key);
			insertKeyword(prefix, file, soundex, "", mm1, mm2);
		}
	}
	
	public static void useStemming(String prefix, String key, Multimap<String, String> mm1, Multimap<String, String> mm2) {
		for (String file : TextExtractPar.lp1.get(key)) {
			String stem = Fuzzy.getStem(key);
			insertKeyword(prefix, file, stem, "", mm1, mm2);
		}
	}
	
	public static void useNormal(String prefix, String key, Multimap<String, String> mm1, Multimap<String, String> mm2) {
		for (String file : TextExtractPar.lp1.get(key)) {
			insertKeyword(prefix, file, key, "", mm1, mm2);
		}
	}
}