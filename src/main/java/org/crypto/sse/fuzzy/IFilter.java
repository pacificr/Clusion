package org.crypto.sse.fuzzy;

public abstract class IFilter {
	
	private boolean invert = false;

	public IFilter invert() {
		invert = true;
		return this;
	}
	public boolean checkInput(String keyword) {
		return invert ^ inputFilter(keyword);
	}

	public boolean checkOutput(String keyword, String alternative) {
		return invert ^ outputFilter(keyword, alternative);
	}
	
	protected abstract boolean inputFilter(String keyword);
	protected abstract boolean outputFilter(String keyword, String alternative);
	
}
