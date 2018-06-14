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
		System.out.println("\nNew First mult-map " + TextExtractPar.lp1);
		System.out.println("New Second multi-map " + TextExtractPar.lp2);
		
		//useCommonMisspellings();
		useSoundex();
		
		System.out.println("\nNew First mult-map " + TextExtractPar.lp1);
		System.out.println("New Second multi-map " + TextExtractPar.lp2);

		//Pause to look at multimaps
		keyRead.readLine();
		//-----

		IEX2Lev disj = IEX2Lev.setup(listSK, TextExtractPar.lp1, TextExtractPar.lp2, bigBlock, smallBlock, 0);

		while (true) {
			System.out.println("How many disjunctions? ");
			int numDisjunctions = Integer.parseInt(keyRead.readLine());

			// Storing the CNF form
			String[][] bool = new String[numDisjunctions][];
			for (int i = 0; i < numDisjunctions; i++) {
				System.out.println("Enter the keywords of the disjunctions ");
				bool[i] = keyRead.readLine().split(" ");
			}
			
			//----Convert bool from word array to soundex array
			//COMMENT THIS OUT IF USING COMMON MISSPELLINGS ONLY
			for (int i = 0; i < bool.length; ++i) {
				for (int j = 0; j < bool[i].length; ++j) {
					bool[i][j] = Fuzzy.getGode(bool[i][j]);
				}
			}
			//----

			TestLocalIEX2Lev.test("log-1.txt", "Test", 1, disj, listSK, bool);
		}
	}
	
	public static void useCommonMisspellings() {
		Multimap<String, String> newLp1 = ArrayListMultimap.create();
		Multimap<String, String> newLp2 = ArrayListMultimap.create();
		
		for (String key : TextExtractPar.lp1.keySet()) {
			
			List<String> fuzzyWords = Fuzzy.getCommonMisspellings(key);
			fuzzyWords.add(key);
			System.out.println(key);
			for (String file : TextExtractPar.lp1.get(key)) {
				//System.out.println("\t" + file);
				for (String word : fuzzyWords) {
					//System.out.println("\t\t" + word);
					
					//Add Common misspellings
					newLp1.put(word.intern(), file.intern());
					newLp2.put(file.intern(), word.intern());
				}
			}
		}

		TextExtractPar.lp1 = newLp1;
		TextExtractPar.lp2 = newLp2;
	}
	
	public static void useSoundex() {
		Multimap<String, String> newLp1 = ArrayListMultimap.create();
		Multimap<String, String> newLp2 = ArrayListMultimap.create();
		
		for (String key : TextExtractPar.lp1.keySet()) {
			
			System.out.println(key);
			for (String file : TextExtractPar.lp1.get(key)) {
				//System.out.println("\t" + file);
				
				//Add Soundex entries including those of common misspellings
				String soundex = Fuzzy.getGode(key);
				System.out.println(key + " soundex: " + soundex + " file: " + file);
				newLp1.put(soundex.intern(), file.intern());
				newLp2.put(file.intern(), soundex.intern());
			}
		}
		TextExtractPar.lp1 = newLp1;
		TextExtractPar.lp2 = newLp2;
	}
}