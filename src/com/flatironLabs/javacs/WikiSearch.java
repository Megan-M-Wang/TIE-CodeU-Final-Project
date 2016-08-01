package com.flatironschool.javacs;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Map.Entry;
import java.lang.Math;

import redis.clients.jedis.Jedis;
import java.util.Scanner;


/**
 * Represents the results of a search query.
 *
 */
public class WikiSearch {
	
	// map from URLs that contain the term(s) to relevance score
	private Map<String, Double> map;
	/**
	 * Constructor.
	 * 
	 * @param map
	 */
	public WikiSearch(Map<String, Double> map) {
		this.map = map;
	}
	
	/**
	 * Looks up the relevance of a given URL.
	 * 
	 * @param url
	 * @return
	 */
	public Double getRelevance(String url) {
		Double relevance = map.get(url);
		return relevance==null ? 0: relevance;
	}
	
	/**
	 * Prints the contents in order of term frequency.
	 * 
	 * @param map
	 */
	private void print(double totalPages) {
     
      //Get the list of entries and the size (# of urls with term
		List<Entry<String, Double>> entries = sort();
      double termPages = entries.size();

      //Calculate the iDF (total Pages / term pages)
      double iDF = Math.abs(Math.log(termPages / totalPages));
      
      //Update the value of each page with the tf-idf ranking
      for( int index = 0; index < entries.size(); index++ ) {
         entries.get(index).setValue(entries.get(index).getValue() * iDF );
      }

      //Sort the new list
		List<Entry<String, Double>> entriesIDF = sort();

      //Print out in reverse order (highest ranking first)
		for (int index = entriesIDF.size() - 1; index >= 0; index-- ) {
			System.out.println(entriesIDF.get(index).getKey());
		}
	}
	
	/**
	 * Computes the union of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch or(WikiSearch that) {
      Map<String,Double> unionMap = new HashMap<String,Double>();
      unionMap.putAll(that.map);
      List<String> valueList = new LinkedList(map.keySet());

      for( int index = 0; index < valueList.size(); index++ ) {
         String url = valueList.get(index);

         if( that.map.containsKey(url) ) {
            unionMap.put( url, new Double( getRelevance(url) + 
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
        
      Map<String,Double> andMap = new HashMap<String,Double>();
      List<String> valueList = new LinkedList(map.keySet());

      for( int index = 0; index < valueList.size(); index++ ) {
         String url = valueList.get(index);

         if( that.map.containsKey(url) ) {
            andMap.put( url, new Double( getRelevance(url) 
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

      Map<String,Double> minusMap = new HashMap<String,Double>();
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
	protected double totalRelevance(Double rel1, Double rel2) {
		// simple starting place: relevance is the sum of the term frequencies.
		return rel1 + rel2;
	}

	/**
	 * Sort the results by relevance.
	 * 
	 * @return List of entries with URL and relevance.
	 */
	public List<Entry<String, Double>> sort() {
      
      List sortedEntry = new LinkedList(map.entrySet());
     
      Comparator<Map.Entry<String, Double>> comparator = 
         new Comparator<Map.Entry<String, Double>>() {

         @Override
         public int compare( Map.Entry<String,Double> node1, 
            Map.Entry<String, Double> node2) {

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
		Map<String, Double> map = index.getCounts(term);
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
         term1 = term1.toLowerCase();

         // search for the first term
         System.out.println("Query: " + term1);
         WikiSearch search1 = search(term1, index);
         search1.print(23.0);
         
         // prompt for the second term
         System.out.println("\nEnter search term: ");
         
         term2 = keyboard.nextLine();
         term2 = term2.toLowerCase();

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
*/		   
         //Prompt for new input
         System.out.println("\nEnter search term: ");
      }
   }
}

      
