package org.crypto.sse;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class TestFuzzy {
	
	public static void main(String[] args) throws Exception {

		BufferedReader keyRead = new BufferedReader(new InputStreamReader(System.in));

		System.out.println("Enter your password :");

		String pass = keyRead.readLine();

		//----Quick test things
		JazzySpellChecker jazzy = new JazzySpellChecker();
		System.out.println(jazzy.getSuggestions(pass));
		System.out.println(Fuzzy.getGode(pass));
		System.out.println(Fuzzy.getCommonMisspellings(pass));
		//----

		List<byte[]> listSK = IEX2Lev.keyGen(256, pass, "salt/salt", 100000);

		System.out.println("Enter the relative path name of the folder that contains the files to make searchable: ");

		String pathName = keyRead.readLine();
		
		if (pathName.equals(""))
		{
			pathName = "/home/ryan/Documents/test/onlysmall";
			System.out.println(pathName);
		}

		ArrayList<File> listOfFile = new ArrayList<File>();
		TextProc.listf(pathName, listOfFile);

		TextProc.TextProc(false, pathName);

		int bigBlock = 1000;
		int smallBlock = 100;
		
		//----Add Fuzzyness to multimaps
		System.out.println("\nFirst multi-map " + TextExtractPar.lp1);
		System.out.println("Second multi-map " + TextExtractPar.lp2);
		
		Multimap<String, String> newLp1 = ArrayListMultimap.create();
		Multimap<String, String> newLp2 = ArrayListMultimap.create();
		
		useCommonMisspellings("m", newLp1, newLp2);
		useSoundex("s", newLp1, newLp2);
		useStemming("t", newLp1, newLp2);
		
		for (String key : TextExtractPar.lp1.keySet()) {
			for (String file : TextExtractPar.lp1.get(key)) {
				newLp1.put(("n" + ":" + key + ":" + key).intern(), file.intern());
				newLp2.put(file.intern(), ("n" + ":" + key + ":" + key).intern());
			}
		}
		
		TextExtractPar.lp1 = newLp1;
		TextExtractPar.lp2 = newLp2;
		
		System.out.println("\nNew First multi-map " + TextExtractPar.lp1);
		System.out.println("New Second multi-map " + TextExtractPar.lp2);

		//Pause to look at multimaps
		keyRead.readLine();
		//-----

		IEX2Lev disj = IEX2Lev.setup(listSK, TextExtractPar.lp1, TextExtractPar.lp2, bigBlock, smallBlock, 0);
		Multimap<String, String> soundex = Fuzzy.makeSoundex(Fuzzy.DICTIONARY);
		Multimap<String, String> stemming = Fuzzy.makeStemmingMap(Fuzzy.DICTIONARY);

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
					//String fixed = jazzy.getSuggestions(key).get(0);
					for (String word : Fuzzy.getCommonMisspellings(key)) {
						output.add("m:" + key + ":" + word);
					}
					
					output.add("n:" + key + ":" + key);
					
					String soundexKey = Fuzzy.getGode(key);
					for (String suggestion : soundex.get(Fuzzy.getGode(key))) {
						output.add("s:" + soundexKey + ":" + suggestion);
					}
					
					String stemmingKey = Fuzzy.getStem(key);
					for (String suggestion : stemming.get(Fuzzy.getStem(key))) {
						output.add("t:" + stemmingKey + ":" + suggestion);
					}
				}
				
				bool[i] = new String[output.size()];
				for (int j = 0; j < output.size(); ++j) {
					System.out.println(output.get(j));
					bool[i][j] = output.get(j);
				}
			}
			
			//----Convert bool from word array to soundex array
			//COMMENT THIS OUT IF USING COMMON MISSPELLINGS ONLY
//			for (int i = 0; i < bool.length; ++i) {
//				for (int j = 0; j < bool[i].length; ++j) {
//					bool[i][j] = Fuzzy.getGode(bool[i][j]);
//				}
//			}
			//----

			TestLocalIEX2Lev.test("log-1.txt", "Test", 1, disj, listSK, bool);
		}
	}
	
	public static void useCommonMisspellings(String prefix, Multimap<String, String> mm1, Multimap<String, String> mm2) {
		
		for (String key : TextExtractPar.lp1.keySet()) {
			
			List<String> fuzzyWords = Fuzzy.getCommonMisspellings(key);
			//fuzzyWords.add(key);
			System.out.println(key);
			for (String file : TextExtractPar.lp1.get(key)) {
				//System.out.println("\t" + file);
				for (String word : fuzzyWords) {
					//System.out.println("\t\t" + word);
					
					//Add Common misspellings
					mm1.put((prefix + ":" + word + ":" + key).intern(), file.intern());
					mm2.put(file.intern(), (prefix + ":" + word + ":" + key).intern());
				}
			}
		}
	}
	
	public static void useSoundex(String prefix, Multimap<String, String> mm1, Multimap<String, String> mm2) {
		
		for (String key : TextExtractPar.lp1.keySet()) {
			
			System.out.println(key);
			for (String file : TextExtractPar.lp1.get(key)) {

				String soundex = Fuzzy.getGode(key);
				System.out.println(key + " soundex: " + soundex + " file: " + file);
				mm1.put((prefix + ":" + soundex + ":" + key).intern(), file.intern());
				mm2.put(file.intern(), (prefix + ":" + soundex + ":" + key).intern());
			}
		}
	}
	
	public static void useStemming(String prefix, Multimap<String, String> mm1, Multimap<String, String> mm2) {
		
		for (String key : TextExtractPar.lp1.keySet()) {
			
			System.out.println(key);
			for (String file : TextExtractPar.lp1.get(key)) {
				
				String stem = Fuzzy.getStem(key);
				mm1.put((prefix + ":" + stem + ":" + key).intern(), file.intern());
				mm2.put(file.intern(), (prefix + ":" + stem + ":" + key).intern());
			}
		}
	}
}