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

import java.util.concurrent.TimeUnit;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import redis.clients.jedis.Jedis;
import java.util.Scanner;
import java.util.*;


/**
 * Represents the results of a search query.
 *
 */
public class WikiSearch {

   private static double totalPages = 10000;
	
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
    * Looks up the title of the given url and prints
    *
    * @param url
    */
   public void readTitle( String url ) throws IOException {
      Document doc = Jsoup.connect(url).get();
      System.out.println(doc.title());
   }

   public void writeTagLine( String url ) throws IOException {
      Document doc = Jsoup.connect(url).get();
      String para = doc.select("p").text();
	   String tag;

	   if (para.length() > 140) {
		   tag = para.substring(0, 140);
	   } else {
		   tag = para;
	   }

	   System.out.println(tag + "...\n");
   }

	/**
	 * Prints the contents in order of term frequency.
	 * 
	 * @param map
	 */
	private void print(boolean fullResult) throws IOException, InterruptedException {
    
      //Get the list of entries and the size (# of urls with term
		List<Entry<String, Double>> entries = sort();
      double termPages = entries.size();

      //Calculate the iDF (total Pages / term pages)
      double iDF = Math.abs(Math.log(totalPages / termPages) + 1.0);
      
      //Update the value of each page with the tf-idf ranking
      for( int index = 0; index < entries.size(); index++ ) {
         entries.get(index).setValue(entries.get(index).getValue() * iDF );
      }

      //Sort the new list
		List<Entry<String, Double>> entriesIDF = sort();
      
      int count = 1;
      //Print out in reverse order (highest ranking first)
		for (int index = entriesIDF.size() - 1; index >= 0; index-- ) {
        
         readTitle(entriesIDF.get(index).getKey());
         
         //Print the url and add it to the list of already indexed terms
			System.out.println(entriesIDF.get(index).getKey());

			// print tag
         writeTagLine(entriesIDF.get(index).getKey());
         
         if( count > 20 && fullResult == false ) {
            return;
         }
         
         count++;
         Thread.sleep(500);
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
        
      //Loop through the terms and only add duplicates
      Map<String,Double> andMap = new HashMap<String,Double>();
      List<String> valueList = new LinkedList(map.keySet());

      for( int index = 0; index < valueList.size(); index++ ) {
         String url = valueList.get(index);

         if( that.map.containsKey(url) ) {
            andMap.put( url, new Double( totalRelevance(getRelevance(url),
               that.getRelevance(url) )) );
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
      
      //Loop through the terms and only add non-duplicates
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
      
      //Return list
      List sortedEntry = new LinkedList(map.entrySet());
     
      //Establish the comparator
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


	public static WikiSearch searchTerms(String term, JedisIndex index) {
		WikiSearch search = search(term, index);

		int searchIndex;
		String term1;
		String term2;

      //Or check
		if (term.contains(" or ")) {
			searchIndex = term.indexOf(" or ");
			term1 = term.substring(0, searchIndex);
			term2 = term.substring(searchIndex + 4);
			search = search.or(search(term1, index));
			search = search.or(search(term2, index));
		}

      //And check
		else if (term.contains(" and ")) {
			searchIndex = term.indexOf(" and ");
			term1 = term.substring(0, searchIndex);
			term2 = term.substring(searchIndex + 5);
			search = search.or(search(term1, index));
			search = search.and(search(term2, index));
		}

      //Minus check
		else if (term.contains(" minus ")) {
			searchIndex = term.indexOf(" minus ");
			term1 = term.substring(0, searchIndex);
			term2 = term.substring(searchIndex + 7);
			search = search.or(search(term1, index));
			search = search.minus(search(term2, index));
		}

      else {
         //Split the term up into an array of words
         String[] splitTerm = term.split(" ");

         //Intersection of the searchs
         WikiSearch sentenceIntersect = search(splitTerm[0], index);

         //Union of the searches
         WikiSearch sentenceUnion = search(splitTerm[0], index);
      
         //Loop through the terms in the array and modify the search union and
         //intersection
         for( int itera = 1; itera < splitTerm.length; itera++ ) {
            sentenceIntersect = sentenceIntersect.and(search(splitTerm[itera], index));
            sentenceUnion = sentenceUnion.or(search(splitTerm[itera], index));
         }

         //Add results to the array and return
         search = search.or(sentenceIntersect);
         search = search.or(sentenceUnion);
      } 
		return search;
	}

	public static void main(String[] args) throws IOException, InterruptedException {

		// make a JedisIndex
		Jedis jedis = JedisMaker.make();
		JedisIndex index = new JedisIndex(jedis);
      String term1;
		Scanner keyboard = new Scanner(System.in);
		
      // make a scanner for input
		System.out.println("Enter a search term: ");

      while( keyboard.hasNextLine() == true) {

         term1 = keyboard.nextLine();
         term1 = term1.toLowerCase();

		  // Accounting for lone terms, intersection, union, and minus
		  System.out.println("\nQuery: " + term1);
        WikiSearch search = searchTerms(term1,index);
		  //ArrayList<WikiSearch> alltheseterms = searchTerms(term1, index);
		  //for (WikiSearch search: alltheseterms) {
			  search.print(false);
		  //}

        //Prompt for new input
        System.out.println("\nEnter search term: ");
		   
      }
   }
}

      
