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
		
		Printer.addPrinter(new Printer(Printer.LEVEL.STATS));
		Printer.addPrinter(new Printer.FilePrinter(Printer.LEVEL.EXTRA, "data.txt"));

		BufferedReader keyRead = new BufferedReader(new InputStreamReader(System.in));

		Printer.normalln("Enter your password :");

		String pass = keyRead.readLine();
		
		Printer.debugln(pass);

		List<byte[]> listSK = IEXZMF.keyGen(128, pass, "salt/salt", 100);

		Printer.normalln("Enter the relative path name of the folder that contains the files to make searchable");

		String pathName = keyRead.readLine();
		
		if (pathName.equals("")) {
			pathName = "/home/ryan/Documents/maildir/allen-p/inbox";
			//pathName = "/home/ryan/Documents/maildir/bailey-s/inbox";
			//pathName = "/home/ryan/Documents/test/onlysmall";
			//pathName = "/home/ryan/Documents/test/other";
		}
		Printer.debugln(pathName);

		long startTime = System.nanoTime();
		TextProc.TextProc(false, pathName);
		Printer.statsln("\nTime to read files: " + (System.nanoTime() - startTime));
		
		Printer.statsln("Number of files: " + TextExtractPar.lp2.keySet().size());
		Printer.statsln("Number of keywords: " + TextExtractPar.lp1.keySet().size());

		//----FUZZY
		Printer.normalln("\nChoose a method:");
		Printer.normalln("1: Normal");
		Printer.normalln("2: 3-Gram");
		Printer.normalln("3: Old");
		
		String method = keyRead.readLine();
		
		Printer.debugln(method);
		
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
			break;
		}

		startTime = System.nanoTime();
		
		fuzzy.fuzzMultimaps(TextExtractPar.lp1);
		TextExtractPar.lp1 = fuzzy.getMultimap1();
		TextExtractPar.lp2 = fuzzy.getMultimap2();
		
		Printer.statsln("Time to produce fuzzy words: " + (System.nanoTime() - startTime));
		Printer.statsln("Number of fuzzy keywords: " + TextExtractPar.lp1.keySet().size());

		Fuzzy.printMultimap(TextExtractPar.lp2);
		Printer.normalln("Enter to continue...");
		keyRead.readLine();
		//----

		startTime = System.nanoTime();
		//Printer.debugln("Number of keywords pairs (w. id): " + TextExtractPar.lp1.size());
		//Printer.debugln("Number of keywords " + TextExtractPar.lp1.keySet().size());

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

		Printer.statsln(
				"\nTime to construct local multi-maps: " + (System.nanoTime() - startTime));

		// Beginning of search phase

		while (true) {

			Printer.normalln("How many disjunctions?");
			int numDisjunctions = 1;
			try {
				numDisjunctions = Integer.parseInt(keyRead.readLine());
			}catch(Exception e) {
				//Printer.normalln(1);
				break;
			}
			
			Printer.debugln(""+numDisjunctions);

			// Storing the CNF form
			String[][] bool = new String[numDisjunctions][];
			for (int i = 0; i < numDisjunctions; i++) {
				Printer.normalln("Enter the keywords of the " + i + "th disjunctions ");
				String terms = keyRead.readLine();
				Printer.debugln(terms);
				bool[i] = terms.toLowerCase().split(" ");
			}
			
			startTime = System.nanoTime();
			bool = fuzzy.fuzzQuery(bool);
			Printer.statsln("Time to fuzz query: " + (System.nanoTime() - startTime));

			startTime = System.nanoTime();
			TestLocalIEXZMF.test("logZMF_Fuzzy.txt", "Test", 1, disj, listSK, bool);
			Printer.statsln("Time to run query: " + (System.nanoTime() - startTime));
		}
		
		Printer.close();
	}
}
