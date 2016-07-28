package com.flatironschool.javacs;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import redis.clients.jedis.Jedis;
import java.util.Scanner;


/**
 * Represents the results of a search query.
 *
 */
public class WikiSearch {
	
	// map from URLs that contain the term(s) to relevance score
	private Map<String, Integer> map;

	/**
	 * Constructor.
	 * 
	 * @param map
	 */
	public WikiSearch(Map<String, Integer> map) {
		this.map = map;
	}
	
	/**
	 * Looks up the relevance of a given URL.
	 * 
	 * @param url
	 * @return
	 */
	public Integer getRelevance(String url) {
		Integer relevance = map.get(url);
		return relevance==null ? 0: relevance;
	}
	
	/**
	 * Prints the contents in order of term frequency.
	 * 
	 * @param map
	 */
	private  void print() {
		List<Entry<String, Integer>> entries = sort();
		for (Entry<String, Integer> entry: entries) {
			System.out.println(entry);
		}
	}
	
	/**
	 * Computes the union of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch or(WikiSearch that) {
      Map<String,Integer> unionMap = new HashMap<String,Integer>();
      unionMap.putAll(that.map);
      List<String> valueList = new LinkedList(map.keySet());

      for( int index = 0; index < valueList.size(); index++ ) {
         String url = valueList.get(index);

         if( that.map.containsKey(url) ) {
            unionMap.put( url, new Integer( getRelevance(url) + 
               that.getRelevance(url) ) );
         }

         else {
            unionMap.put( url, getRelevance(url) );
         }
      }

		return new WikiSearch(unionMap);
	}
	
	/**
	 * Computes the intersection of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch and(WikiSearch that) {
        
      Map<String,Integer> andMap = new HashMap<String,Integer>();
      List<String> valueList = new LinkedList(map.keySet());

      for( int index = 0; index < valueList.size(); index++ ) {
         String url = valueList.get(index);

         if( that.map.containsKey(url) ) {
            andMap.put( url, new Integer( getRelevance(url) 
               + that.getRelevance(url) ) );
         }
      }

		return new WikiSearch(andMap);
	}
	
	/**
	 * Computes the intersection of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch minus(WikiSearch that) {

      Map<String,Integer> minusMap = new HashMap<String,Integer>();
      List<String> valueList = new LinkedList(map.keySet());

      for( int index = 0; index < valueList.size(); index++ ) {
         String url = valueList.get(index);

         if( !(that.map.containsKey(url)) ) {
            minusMap.put( url, getRelevance(url) );
         }
      }

		return new WikiSearch(minusMap);
	}
	
	/**
	 * Computes the relevance of a search with multiple terms.
	 * 
	 * @param rel1: relevance score for the first search
	 * @param rel2: relevance score for the second search
	 * @return
	 */
	protected int totalRelevance(Integer rel1, Integer rel2) {
		// simple starting place: relevance is the sum of the term frequencies.
		return rel1 + rel2;
	}

	/**
	 * Sort the results by relevance.
	 * 
	 * @return List of entries with URL and relevance.
	 */
	public List<Entry<String, Integer>> sort() {
      
      List sortedEntry = new LinkedList(map.entrySet());
     
      Comparator<Map.Entry<String, Integer>> comparator = 
         new Comparator<Map.Entry<String, Integer>>() {

         @Override
         public int compare( Map.Entry<String,Integer> node1, 
            Map.Entry<String, Integer> node2) {

            if( node1.getValue() < node2.getValue() ) {
               return -1;
            }

            if( node1.getValue() > node2.getValue() ) {
               return 1;
            }

            return 0;
         }
      };

      Collections.sort(sortedEntry, comparator);

		return sortedEntry;
	}

	/**
	 * Performs a search and makes a WikiSearch object.
	 * 
	 * @param term
	 * @param index
	 * @return
	 */
	public static WikiSearch search(String term, JedisIndex index) {
		Map<String, Integer> map = index.getCounts(term);
		return new WikiSearch(map);
	}

	public static void main(String[] args) throws IOException {

		// make a JedisIndex
		Jedis jedis = JedisMaker.make();
		JedisIndex index = new JedisIndex(jedis); 
      String term1;
      String term2;
		Scanner keyboard = new Scanner(System.in);
		
      // make a scanner for input
		System.out.println("Enter a search term: ");

      while( keyboard.hasNextLine() == true ) {
         
         term1 = keyboard.nextLine();
         term1.toLowerCase();
         //term = term.replaceAll("\\s", "");

         // search for the first term
         System.out.println("Query: " + term1);
         WikiSearch search1 = search(term1, index);
         search1.print();
		
         // prompt for the second term
         System.out.println("\nEnter search term: ");
         
         term2 = keyboard.nextLine();
         term2.toLowerCase();
         //term = term.replaceAll("\\s", "");

         //Second term search
         System.out.println("Query: " + term2);
         WikiSearch search2 = search(term2, index);
         search2.print();
         
         //Intersection
         System.out.println("\nQuery: " + term1 + " AND " + term2);
         WikiSearch intersection = search1.and(search2);
         intersection.print();
         
         //Union
         System.out.println("\nQuery: " + term1 + " OR " + term2);
         WikiSearch unionSearch = search1.or(search2);
         unionSearch.print();

         //Minus
         System.out.println("\nQuery: " + term1 + " MINUS " + term2);
         WikiSearch exclusion = search1.minus(search2);
         exclusion.print();
		   
         //Prompt for new input
         System.out.println("\nEnter search term: ");
      }
   }
}

      
