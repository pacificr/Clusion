package org.crypto.sse.fuzzy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.Multimap;

public abstract class IFuzzingScheme {
	
	protected String prefix = "";
	private List<IFilter> inputFilters = new ArrayList<IFilter>();
	private List<IFilter> outputFilters = new ArrayList<IFilter>();
	
	public IFuzzingScheme() {}
	
	public IFuzzingScheme(String prefix) {
		this.prefix = prefix;
	}
	
	public IFuzzingScheme addInputFilter(IFilter filter) {
		inputFilters.add(filter);
		return this;
	}
	
	public IFuzzingScheme addOutputFilter(IFilter filter) {
		outputFilters.add(filter);
		return this;
	}
	
	private String getEncoding(String keyword, String alternative) {
		return prefix + ":" + keyword + ":" + alternative;
	}
	
	protected boolean insertKeyword(
			String file,
			String keyword,
			String alternative,
			Multimap<String, String> mm1,
			Multimap<String, String> mm2) {

		boolean canInsert = true;
		for (IFilter filter : outputFilters) {
			if (!filter.checkOutput(keyword, alternative)) {
				canInsert = false;
			}
		}
		
		if (canInsert)
		{
			String insert = getEncoding(keyword, alternative);
			mm1.put(insert.intern(), file.intern());
			mm2.put(file.intern(), insert.intern());
		}
		
		return canInsert;
	}
	
	protected boolean insertEdge(
			Collection<String> edges,
			String suggestion,
			String word) {
		
		boolean canInsert = true;
		for (IFilter filter : outputFilters) {
			if (!filter.checkOutput(suggestion, word)) {
				canInsert = false;
			}
		}
		
		if (canInsert)
		{
			edges.add(getEncoding(suggestion, word));
		}
		
		return canInsert;
	}
	
	public void fuzz(
			String keyword,
			Multimap<String, String> origin,
			Multimap<String, String> mm1,
			Multimap<String, String> mm2) {
		
		boolean canFuzz = true;
		for (IFilter filter : inputFilters) {
			if (!filter.checkInput(keyword)) {
				canFuzz = false;
			}
		}
		
		if (canFuzz) {
			fuzzingScheme(keyword, origin, mm1, mm2);
		}
	}
	
	protected abstract void fuzzingScheme(
			String keyword,
			Multimap<String, String> origin,
			Multimap<String, String> mm1,
			Multimap<String, String> mm2);
	
	public abstract Collection<String> getEdges(String word);
}
