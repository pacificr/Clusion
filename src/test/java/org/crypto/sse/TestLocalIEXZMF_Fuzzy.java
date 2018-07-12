package org.crypto.sse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.crypto.sse.fuzzy.*;

import com.google.common.collect.Multimap;

public class TestLocalIEXZMF_Fuzzy {
	private static final int falsePosRate = 25;
	private static final int maxLengthOfMask = 20;
	
	public static void main(String[] args) throws Exception {
		
		Printer.addPrinter(new Printer(Printer.LEVEL.EXTRA));

		BufferedReader keyRead = new BufferedReader(new InputStreamReader(System.in));

		System.out.println("Enter your password :");

		String pass = keyRead.readLine();

		List<byte[]> listSK = IEXZMF.keyGen(128, pass, "salt/salt", 100);

		System.out.println("Enter the relative path name of the folder that contains the files to make searchable");

		String pathName = keyRead.readLine();
		
		if (pathName.equals("")) {
			//pathName = "/home/ryan/Documents/maildir/bailey-s/inbox";
			pathName = "/home/ryan/Documents/test/onlysmall";
			//pathName = "/home/ryan/Documents/test/other";
			System.out.println(pathName);
		}

		TextProc.TextProc(false, pathName);

		//----FUZZY
		System.out.println("\nChoose a method:");
		System.out.println("1: Normal");
		System.out.println("2: 3-Gram");
		System.out.println("3: Old");
		
		String method = keyRead.readLine();
		
		Fuzzy fuzzy;
		
		switch(method) {
		default:
			fuzzy = new Fuzzy(new MOutOfNQueryScheme(2));
			fuzzy.addFuzzingScheme(new NaturalFuzzingScheme("r"));
			
			fuzzy.addFuzzingScheme(new StemmingCloseWordsFuzzingScheme("t")
					.addInputFilter(new ValidCharactersFilter()));
			
			fuzzy.addFuzzingScheme(new SoundexCloseWordsFuzzingScheme("s")
					.addInputFilter(new ValidCharactersFilter()));
			
			fuzzy.addFuzzingScheme(new MisspellingFuzzingScheme("m")
					.addInputFilter(new DictionaryFilter())
					.addOutputFilter(new SpellCheckFilter()));
			break;
		case "2":
			fuzzy = new Fuzzy(new MOutOfNQueryScheme(3));
			fuzzy.addFuzzingScheme(new NGramsFuzzingScheme("n", 3));
			break;
		case "3":
			fuzzy = new Fuzzy();
			fuzzy.addFuzzingScheme(new NaturalFuzzingScheme("r"));
			
			fuzzy.addFuzzingScheme(new StemmingFuzzingScheme("t")
					.addInputFilter(new ValidCharactersFilter()));
			
			fuzzy.addFuzzingScheme(new SoundexFuzzingScheme("s")
					.addInputFilter(new ValidCharactersFilter()));
			
			fuzzy.addFuzzingScheme(new MisspellingFuzzingScheme("m")
					.addInputFilter(new DictionaryFilter())
					.addOutputFilter(new SpellCheckFilter()));
			
			fuzzy.addFuzzingScheme(new NGramCloseWordsFuzzingScheme("n", 3)
					.addInputFilter(new DictionaryFilter())
					.addOutputFilter(new EditDistanceFilter(2)));
		}

		long startTime = System.nanoTime();
		
		fuzzy.fuzzMultimaps(TextExtractPar.lp1);
		TextExtractPar.lp1 = fuzzy.getMultimap1();
		TextExtractPar.lp2 = fuzzy.getMultimap2();
		
		Fuzzy.printMultimap(TextExtractPar.lp2);
		
		Printer.statsln("Time to produce fuzzy words: " + (System.nanoTime() - startTime) + " ms");
		keyRead.readLine();
		//----

		long startTime2 = System.nanoTime();
		Printer.debugln("Number of keywords pairs (w. id): " + TextExtractPar.lp1.size());
		Printer.debugln("Number of keywords " + TextExtractPar.lp1.keySet().size());

		Printer.debugln("\n Beginning of global encrypted multi-map construction \n");

		int bigBlock = 1000;
		int smallBlock = 100;
		int dataSize = 0;

		RR2Lev[] localMultiMap = null;
		Multimap<String, Integer> dictionaryForMM = null;
		// Construction by Cash et al NDSS 2014

		for (String keyword : TextExtractPar.lp1.keySet()) {

			if (dataSize < TextExtractPar.lp1.get(keyword).size()) {
				dataSize = TextExtractPar.lp1.get(keyword).size();
			}

		}

		IEX2Lev disj = new IEX2Lev(
				RR2Lev.constructEMMParGMM(listSK.get(1), TextExtractPar.lp1, bigBlock, smallBlock, dataSize),
				localMultiMap, dictionaryForMM);

		Printer.debugln("\n Beginning of local encrypted multi-map construction \n");

		IEXZMF.constructMatryoshkaPar(new ArrayList<String>(TextExtractPar.lp1.keySet()), listSK.get(0), listSK.get(1),
				maxLengthOfMask, falsePosRate);

		long endTime2 = System.nanoTime();

		long totalTime2 = endTime2 - startTime2;

		Printer.statsln("\n*****************************************************************");
		Printer.statsln("\n\t\tSTATS");
		Printer.statsln("\n*****************************************************************");

		Printer.statsln(
				"\nTotal Time elapsed for the local multi-map construction in seconds: " + totalTime2 / 1000000);

		// Beginning of search phase

		while (true) {

			System.out.println("How many disjunctions? ");
			int numDisjunctions = 1;
			try {
				numDisjunctions = Integer.parseInt(keyRead.readLine());
			}catch(Exception e) {
				System.out.println(1);
			}

			// Storing the CNF form
			String[][] bool = new String[numDisjunctions][];
			for (int i = 0; i < numDisjunctions; i++) {
				System.out.println("Enter the keywords of the " + i + "th disjunctions ");
				bool[i] = keyRead.readLine().toLowerCase().split(" ");
			}
			
			bool = fuzzy.fuzzQuery(bool);

			TestLocalIEXZMF.test("logZMF_Fuzzy.txt", "Test", 1, disj, listSK, bool);
		}
	}
}
