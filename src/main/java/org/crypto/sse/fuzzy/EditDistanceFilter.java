package org.crypto.sse.fuzzy;

/**
 * Filters alternatives by comparing them to the keyword they were derived
 * from. Only accepts if the two are within a certain edit distance from
 * eachother.
 * 
 * Only used as an output filter.
 * 
 * https://en.wikipedia.org/wiki/Edit_distance
 * 
 * @author Ryan Estes
 */
public class EditDistanceFilter extends IFilter{

	private int distance;
	
	/**
	 * 
	 * @param distance edit distance
	 */
	public EditDistanceFilter(int distance) {
		this.distance = distance;
	}
	
	@Override
	public boolean inputFilter(String keyword) {
		return true;
	}

	@Override
	public boolean outputFilter(String keyword, String alternative) {
		//https://www.programcreek.com/2013/12/edit-distance-in-java/
		int len1 = keyword.length();
		int len2 = alternative.length();
		 
		int[][] dp = new int[len1 + 1][len2 + 1];
	 
		for (int i = 0; i <= len1; i++) {
			dp[i][0] = i;
		}
	 
		for (int j = 0; j <= len2; j++) {
			dp[0][j] = j;
		}
	 
		for (int i = 0; i < len1; i++) {
			char c1 = keyword.charAt(i);
			for (int j = 0; j < len2; j++) {
				char c2 = alternative.charAt(j);
	 
				if (c1 == c2) {
					dp[i + 1][j + 1] = dp[i][j];
				} else {
					int replace = dp[i][j] + 1;
					int insert = dp[i][j + 1] + 1;
					int delete = dp[i + 1][j] + 1;
	 
					int min = replace > insert ? insert : replace;
					min = delete > min ? min : delete;
					dp[i + 1][j + 1] = min;
				}
			}
		}
	 
		return dp[len1][len2] <= distance;
	}

}
