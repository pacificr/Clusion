package org.crypto.sse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.crypto.sse.fuzzy.*;

import com.google.common.collect.Multimap;
/**
 * 
 * @author Ryan Estes
 */
public class TestLocalIEXZMF_Iris {
	private static final int falsePosRate = 25;
	private static final int maxLengthOfMask = 20;
	
	public static void main(String[] args) throws Exception {
		
		Printer.addPrinter(new Printer(Printer.LEVEL.STATS));
		Printer.addPrinter(new Printer.FilePrinter(Printer.LEVEL.STATS, "data.txt"));

		BufferedReader keyRead = new BufferedReader(new InputStreamReader(System.in));

		Printer.normalln("Enter your password :");

		String pass = "a";//keyRead.readLine();
		
		Printer.debugln(pass);

		List<byte[]> listSK = IEXZMF.keyGen(128, pass, "salt/salt", 100);

		Printer.normalln("Enter the relative path name of the folder that contains the files to make searchable");

		String pathName = "";//keyRead.readLine();
		
		if (pathName.equals("")) {
			pathName = "/home/ryan/Documents/006004504pUOPn3qFM6_code.bin";
		}
		Printer.debugln(pathName);
		
		Printer.normal("K: ");
		int k = 20;//Integer.parseInt(keyRead.readLine());
		
		Printer.normal("S: ");
		int s = Integer.parseInt(keyRead.readLine());
		
		Printer.normal("L: ");
		int l = 1;//Integer.parseInt(keyRead.readLine());
		
		long startTime = System.nanoTime();
		
		long[] subsets = new long[s];
		int[][] locations = new int[s][];
		
		String iris = "";
		
		BufferedReader reader = new BufferedReader(new FileReader(pathName));
		
		reader.skip(1);
		for (int i = 0; i < 32768; ++i) {
			iris += (char)reader.read();
			reader.skip(2);
		}
		
		reader.close();
		
		Random rand = new Random();
		
		for (int i = 0; i < s; ++i) {
			locations[i] = new int[k];
			for (int j = 0; j < k; ++j) {
				locations[i][j] = rand.nextInt(32768);
				subsets[i] |= (iris.charAt(locations[i][j]) - (int)'0') << (k - j - 1);
			}
		}
		
		String[] keywords = new String[s];
		for (int i = 0; i < s; ++i) {
			keywords[i] = "";
			for (int j = 0; j < k; ++j) {
				keywords[i] += locations[i][j] + ":";
			}
			keywords[i] += subsets[i];
			TextExtractPar.lp1.put(keywords[i], "match");
			TextExtractPar.lp2.put("match", keywords[i]);
		}
		
		Printer.statsln("\nTime to read files: " + (System.nanoTime() - startTime));
		
		Printer.statsln("Number of files: " + TextExtractPar.lp2.keySet().size());
		Printer.statsln("Number of keywords: " + TextExtractPar.lp1.keySet().size());

		//----FUZZY
		
		Fuzzy fuzzy = new Fuzzy(new MOutOfNQueryScheme(l));

		startTime = System.nanoTime();
		
		Printer.statsln("Time to produce fuzzy words: " + (System.nanoTime() - startTime));
		Printer.statsln("Number of fuzkeywords[i] = locations[i] + \":\" + subsets[i];\n" + 
				"				TextExtractPar.lp1.put(keywords[i], \"match\");\n" + 
				"				TextExtractPar.lp2.put(\"match\", keywords[i]);zy keywords: " + TextExtractPar.lp1.keySet().size());

		Fuzzy.printMultimap(TextExtractPar.lp2);
//		Printer.normalln("Enter to continue...");
//		keyRead.readLine();
		//----

		startTime = System.nanoTime();
		
		Printer.debugln("\n Beginning of global encrypted multi-map construction \n");

		int bigBlock = 1000;
		int smallBlock = 100;
		int dataSize = 0;

		RR2Lev[] localMultiMap = null;
		Multimap<String, Integer> dictionaryForMM = null;
		
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
		BufferedWriter o = new BufferedWriter(new FileWriter("output.txt", true));
		for (int t = 1; t <= 50; ++t) {
			
			Printer.normalln("L: "+t);
			MOutOfNQueryScheme.temp = t;

			String[][] bool = new String[1][];
			bool[0] = keywords;
//			for (int i = 0; i < s; ++i) {
//				Printer.normalln(""+bool[0][i]);
//			}
			
			startTime = System.nanoTime();
			bool = fuzzy.fuzzQuery(bool);
			//Printer.statsln("Time to fuzz query: " + (System.nanoTime() - startTime));

			startTime = System.nanoTime();
			TestLocalIEXZMF.test("logZMF_Fuzzy.txt", "Test", 1, disj, listSK, bool);
			o.write(""+(System.nanoTime() - startTime) + "\n");
			Printer.statsln("Time to run query: " + (System.nanoTime() - startTime));
			
		}
		
		o.close();
		Printer.close();
	}
}
