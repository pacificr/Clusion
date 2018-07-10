package org.crypto.sse.fuzzy;

import java.util.List;

public class SpellCheckFilter extends IFilter{

	@Override
	protected boolean inputFilter(String keyword) {
		return true;
	}

	@Override
	protected boolean outputFilter(String keyword, String alternative) {
		List<String> suggestions = Fuzzy.JAZZY.getSuggestions(alternative);
		return suggestions.size() > 0 && keyword.equals(suggestions.get(0));
	}

}
