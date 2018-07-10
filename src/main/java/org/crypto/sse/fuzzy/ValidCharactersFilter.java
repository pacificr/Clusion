package org.crypto.sse.fuzzy;

public class ValidCharactersFilter extends IFilter{

	private boolean whitelist = true;
	private String list = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

	public ValidCharactersFilter useWhitelist(String validCharacters) {
		whitelist = true;
		list = validCharacters;
		return this;
	}
	
	public ValidCharactersFilter useBlacklist(String invalidCharacters) {
		whitelist = false;
		list = invalidCharacters;
		return this;
	}
	
	@Override
	protected boolean inputFilter(String keyword) {
		
		String test = whitelist ? keyword : list;
		String search = whitelist ? list : keyword;
		
		for (char c : test.toCharArray()) {
			if (whitelist ^ search.contains(""+c)) {
				return false;
			}
		}
		return true;
	}

	@Override
	protected boolean outputFilter(String keyword, String alternative) {
		return inputFilter(alternative);
	}

}
