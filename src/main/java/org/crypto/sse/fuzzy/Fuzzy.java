package org.crypto.sse.fuzzy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.crypto.sse.JazzySpellChecker;
import org.crypto.sse.Printer;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class Fuzzy {
	
	final public static JazzySpellChecker JAZZY = new JazzySpellChecker();
	final public static List<String> DICTIONARY = new ArrayList<String>();
	
	final public static String DICTIONARY_FILE = "2of12.txt";
	
	private Multimap<String, String> mm1;
	private Multimap<String, String> mm2;

	private List<IFuzzingScheme> fuzzingSchemes = new ArrayList<IFuzzingScheme>();
	private IQueryScheme queryScheme = null;
	
	static {

		BufferedReader fileIn;
		
		try {
			fileIn = new BufferedReader(new FileReader(new File(Fuzzy.DICTIONARY_FILE)));
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
	
	public Fuzzy() {}
	
	public Fuzzy(IQueryScheme queryScheme) {
		this.queryScheme = queryScheme;
	}
	
	public Multimap<String, String> getMultimap1(){
		return mm1;
	}
	
	public Multimap<String, String> getMultimap2(){
		return mm2;
	}
	
	public void addFuzzingScheme(IFuzzingScheme fuzzingScheme) {
		fuzzingSchemes.add(fuzzingScheme);
	}
	
	public void fuzzMultimaps (Multimap<String, String> origin) {
		mm1 = ArrayListMultimap.create();
		mm2 = ArrayListMultimap.create();
		
		for (String key : origin.keySet()) {
			for (IFuzzingScheme fuzzingScheme : fuzzingSchemes) {
				fuzzingScheme.fuzz(key, origin, mm1, mm2);
			}
		}
	}
	
	public String[][] fuzzQuery(String[][] query) {
		List<List<String>> results = new ArrayList<List<String>>();
		
		for (int i = 0; i < query.length; ++i) {
			List<String> disjunction = new ArrayList<String>();
			for (int j = 0; j < query[i].length; ++j) {
				for (IFuzzingScheme fuzzingScheme : fuzzingSchemes) {
					disjunction.addAll(fuzzingScheme.getEdges(query[i][j]));
				}
			}
			
			results.add(disjunction);
		}
		
		if (queryScheme != null) {
			results = queryScheme.modifyQuery(results);
		}
		
		query = new String[results.size()][];
		
		for (int i = 0; i < results.size(); ++i) {
			List<String> disjunction = results.get(i);
			query[i] = new String[disjunction.size()];
			Printer.debugln("(");
			for (int j = 0; j < disjunction.size(); ++j) {
				Printer.debugln("\t"+disjunction.get(j));
				query[i][j] = disjunction.get(j);
			}
			Printer.debug(")");
		}
		
		Printer.debugln("");
		
		return query;
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
		}
		Printer.debugln("Number of Keys: " + numKeys + ". Number of Elements: " + numElements);
	}
	
}
