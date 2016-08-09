package com.flatironschool.javacs;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Arrays;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import redis.clients.jedis.Jedis;


public class WikiCrawler {
	// keeps track of where we started
	private final String source;

	// the index where the results go
	private JedisIndex index;

	// queue of URLs to be indexed
	private Queue<String> queue = new LinkedList<String>();
	
	// fetcher used to get pages from Wikipedia
	final static WikiFetcher wf = new WikiFetcher();

	/**
	 * Constructor.
	 * 
	 * @param source
	 * @param index
	 */
	public WikiCrawler(String source, JedisIndex index) {
		this.source = source;
		this.index = index;
		queue.offer(source);
	}

	/**
	 * Returns the number of URLs in the queue.
	 * 
	 * @return
	 */
	public int queueSize() {
		return queue.size();	
	}

	/**
	 * Gets a URL from the queue and indexes it.
	 * @param b 
	 * 
	 * @return Number of pages indexed.
	 * @throws IOException
	 */
	public String crawl(boolean testing) throws IOException {

      //Add the url to the queue
      String url = queue.poll();

      //If the url is already indexed and testing is false
      //don't add to queue
      if( index.isIndexed(url) && !(testing) ) {
            return null;
      }

      //Local variable for the current paragraph of the Wiki page
      Elements paragraph;

      //If testing then read the Wiki page from the cached data
      if( testing ) {
         paragraph = wf.readWikipedia(url);
      }

      //Otherwise get the Wiki page from Wikipedia
      else {
         paragraph = wf.fetchWikipedia(url);
      }

      if( paragraph == null ) {
         return null;
      }

      //Index the page and queue the Internal links
      index.indexPage(url, paragraph);

      if( url.contains("https://en.wikipedia.org/")) {
         queueInternalLinks(paragraph);
      }

		return url;
	}
	
	/**
	 * Parses paragraphs and adds internal links to the queue.
	 * 
	 * @param paragraphs
	 */
	// NOTE: absence of access level modifier means package-level
	void queueInternalLinks(Elements paragraphs) {
      
      //Base url for wikipedia
      String wikiUrl = "/wiki/";

      //Loop through the paragraphs on the Wikipedia page
      for( Element paragraph: paragraphs )
      {
         //Get the urls in the paragraph
         Elements urlList = paragraph.getElementsByTag("a");

         //Loop through the urls in the paragraph
         for( Element urlNode: urlList ) {

            //Get the html url
            String hrefURL = urlNode.attr("href");

            //Check if wiki url and add it to the queue
            if( hrefURL.startsWith(wikiUrl) ) {
               urlNode.setBaseUri("https://en.wikipedia.org/");
               String url = urlNode.attr("abs:href");
               queue.offer(url);
            }

            else {
               String url = urlNode.attr("abs:href");
               
               if( !(url.contains("https://en.wikipedia.org/")) ) {
                  queue.offer(url);
               }
            }
         }
      }
	}

	public static void main(String[] args) throws IOException {
	
		// make a WikiCrawler
		Jedis jedis = JedisMaker.make();
		JedisIndex index = new JedisIndex(jedis); 
      // index.deleteTermCounters();
      // index.deleteURLSets();
		// index.deleteAllKeys();
		//String source = "https://en.wikipedia.org/wiki/Main_Page";
      String source = "https://en.wikipedia.org/wiki/Java_(programming_language)";
		WikiCrawler wc = new WikiCrawler(source, index);
		
		// for testing purposes, load up the queue
		Elements paragraphs = wf.fetchWikipedia(source);
		wc.queueInternalLinks(paragraphs);
      int count = 0;
		// loop until we index a new page
		String res;
		do {
			res = wc.crawl(false);
         
         if( res != null ) {
            count++;
         }
         
         if( count % 1000 == 0 ) {
            System.out.println(count);
         }
            // REMOVE THIS BREAK STATEMENT WHEN crawl() IS WORKING
		} while (count < 1000);
	}
}
