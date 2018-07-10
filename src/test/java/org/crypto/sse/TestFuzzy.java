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

		Printer.addPrinter(new Printer(Printer.LEVEL.DEBUG));
		
		keyRead = new BufferedReader(new InputStreamReader(System.in));

		System.out.println("Enter your password:");

		String pass = keyRead.readLine();

		//----Quick test things
		JazzySpellChecker jazzy = new JazzySpellChecker();
		Printer.debugln("Jazzy Suggestions:  " + jazzy.getSuggestions(pass));
		Printer.debugln("Stem:               " + FuzzyOld.getStem(pass));
		Printer.debugln("Soundex:            " + FuzzyOld.getGode(pass));
		Printer.debugln("Misspellings:       " + FuzzyOld.getCommonMisspellings(pass));
		Printer.debugln(N + "-grams:           " + FuzzyOld.getNGrams(pass, N));
		Printer.debugln("Matching " + N + "-grams:  " + FuzzyOld.getNGramCloseWords(pass, N));
		//----

		List<byte[]> listSK = IEX2Lev.keyGen(256, pass, "salt/salt", 100000);

		System.out.println("Enter the relative path name of the folder that contains the files to make searchable: ");

		String pathName = keyRead.readLine();
		
		if (pathName.equals("")) {
			//pathName = "/home/ryan/Documents/maildir/bailey-s/inbox";
			pathName = "/home/ryan/Documents/test/onlysmall";
			//pathName = "/home/ryan/Documents/test/other";
			System.out.println(pathName);
		}

		ArrayList<File> listOfFile = new ArrayList<File>();
		TextProc.listf(pathName, listOfFile);

		TextProc.TextProc(false, pathName);

		int bigBlock = 1000;
		int smallBlock = 100;
		
		//----Add Fuzzyness to multimaps
		//Printer.debugln("\nFirst multi-map " + TextExtractPar.lp1);
		//Printer.debugln("Second multi-map " + TextExtractPar.lp2);

		Printer.debugln("Number of keywords before fuzzy: " + TextExtractPar.lp1.keySet().size());
		Printer.debugln("Number of pairs before fuzzy: " + TextExtractPar.lp1.keys().size() + "\n");
		
		testMM();

		Printer.debugln("Number of keywords after fuzzy: " + TextExtractPar.lp1.keySet().size());
		Printer.debugln("Number of pairs after fuzzy: " + TextExtractPar.lp1.keys().size() + "\n");
		
		//Printer.debugln("\nNew First multi-map " + TextExtractPar.lp1);
		//Printer.debugln("New Second multi-map " + TextExtractPar.lp2);
		
		FuzzyOld.printMultimap(TextExtractPar.lp2);

		//Pause to look at multimaps
		System.out.println("Enter to continue...");
		keyRead.readLine();
		//-----

		IEX2Lev disj = IEX2Lev.setup(listSK, TextExtractPar.lp1, TextExtractPar.lp2, bigBlock, smallBlock, 0);
		//Multimap<String, String> soundex = FuzzyOld.makeSoundex(FuzzyOld.DICTIONARY);
		//Multimap<String, String> stemming = FuzzyOld.makeStemmingMap(FuzzyOld.DICTIONARY);

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

					List<String> fixed = FuzzyOld.JAZZY.getSuggestions(key);
					
					if (fixed.size() > 0 && fixed.get(0).equals(key)) {//Hacky way to check if the word is in the dictionary
						for (String word : FuzzyOld.JAZZY.getSuggestions(key)) {
							output.add("m:" + key + ":" + word);
						}
						
						output.add("t:" + FuzzyOld.getStem(key) + ":");
						
						for (String word : FuzzyOld.getNGramCloseWords(key, N)) {
							output.add("n:" + key + ":" + word);
						}
					}
					
					output.add("s:" + FuzzyOld.getGode(key) + ":");
					
					output.add("r:" + key + ":");
				}
				
				bool[i] = new String[output.size()];
				for (int j = 0; j < output.size(); ++j) {
					Printer.debugln(output.get(j));
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
			List<String> fixed = FuzzyOld.JAZZY.getSuggestions(key);
			
			if (fixed.size() > 0 && fixed.get(0).equals(key)) {//Hacky way to check if the word is in the dictionary
				useCommonMisspellings("m", key, newLp1, newLp2);
				useStemming("t", key, newLp1, newLp2);
			}
			useNGrams("n", key, newLp1, newLp2);
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
		List<String> fuzzyWords = FuzzyOld.getCommonMisspellings(key);
		for (String file : TextExtractPar.lp1.get(key)) {
			for (String word : fuzzyWords) {
				insertKeyword(prefix, file, word, key, mm1, mm2);
			}
		}
	}
	
	public static void useNGrams(String prefix, String key, Multimap<String, String> mm1, Multimap<String, String> mm2) {
		List<String> nGrams = FuzzyOld.getNGramCloseWords(key, N, MAX);
		for (String file : TextExtractPar.lp1.get(key)) {
			for (String word : nGrams) {
				insertKeyword(prefix, file, word, key, mm1, mm2);
			}
		}
	}
	
	public static void useSoundex(String prefix, String key, Multimap<String, String> mm1, Multimap<String, String> mm2) {
		for (String file : TextExtractPar.lp1.get(key)) {
			String soundex = FuzzyOld.getGode(key);
			insertKeyword(prefix, file, soundex, "", mm1, mm2);
		}
	}
	
	public static void useStemming(String prefix, String key, Multimap<String, String> mm1, Multimap<String, String> mm2) {
		for (String file : TextExtractPar.lp1.get(key)) {
			String stem = FuzzyOld.getStem(key);
			insertKeyword(prefix, file, stem, "", mm1, mm2);
		}
	}
	
	public static void useNormal(String prefix, String key, Multimap<String, String> mm1, Multimap<String, String> mm2) {
		for (String file : TextExtractPar.lp1.get(key)) {
			insertKeyword(prefix, file, key, "", mm1, mm2);
		}
	}
}