package com.fatality;

import java.util.ArrayList;
import java.util.List;
/**
 * 
 * @author Simone Monti and Gianluca Puleri
 *
 */
public class QueryXML {
	public List<String> queries = new ArrayList<String>();
	public List<String> tags = new ArrayList<String>();
	public List<String> solutions = new ArrayList<String>();
	
	/**
	 * 
	 * @return
	 */
	public String[] getQueries() {
		return this.queries.toArray(new String[0]);
	}
	/**
	 * 
	 * @param title
	 */
	public void addQueries(String title) {
		this.queries.add(title);
	}
	/**
	 * 
	 * @return
	 */
	public String[] getTags() {
		return this.tags.toArray(new String[0]);
	}
	/**
	 * 
	 * @param tag
	 */
	public void addTags(String tag) {
		this.tags.add(tag);
	}
	/**
	 * 
	 * @return
	 */
	public String[] getSolutions() {
		return this.solutions.toArray(new String[0]);
	}
	/**
	 * 
	 * @param title
	 */
	public void addSolutions(String title) {
		this.solutions.add(title);
	}
}