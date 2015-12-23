/*
 *    Article.java
 *    Copyright (C) 2007 David Milne, d.n.milne@gmail.com
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package ruc.irm.wikit.model;

import java.util.*;

/**
 * Represents articles in Wikipedia; the pages that contain descriptive text regarding a particular topic. 
 */
public class Article {


	/**
	 * Returns a array of {@link Redirect Redirects}, sorted by id, that point to this article.
	 * 
	 * @return	an array of Redirects, sorted by id
	 */
	public Redirect[] getRedirects()  {

		Redirect[] redirects = new Redirect[0] ;
		return redirects ;	
	}


	//TODO:equivalent categories
	/**
	 * Returns the {@link Category} that relates to the same concept as this article. For instance, calling 
	 * this for "6678: Cat" returns the category "799717: Cats"
	 * 
	 * Note that many articles do not have equivalent categories; they are only used when the article 
	 * describes a general topic for which there are other, more specific, articles. Consequently, 
	 * this method will often return null. 
	 * 
	 * @return	the equivalent Category, or null
	 *//*
	public Category getEquivalentCategory() {

		Category equivalentCategory = null ;

		/*
		Statement stmt = getWikipediaDatabase().createStatement() ;
		ResultSet rs = stmt.executeQuery("SELECT page_id, page_title FROM equivalence, page WHERE page_id=eq_cat AND eq_art=" + id) ;

		if (rs.first()) {
			try {
				equivalentCategory = new Category(database, rs.getInt(1), new String(rs.getBytes(2), "UTF-8")) ;
			} catch (Exception e) {} ;
		}

		rs.close() ;
		stmt.close() ;	

		return equivalentCategory ;
	}*/

	/**
	 * Returns an array of {@link Article Articles} that link to this article. These 
	 * are defined by the internal hyperlinks within article text. If these hyperlinks came via 
	 * redirects, then they are resolved.
	 * 
	 * @return	the array of Articles that link to this article, sorted by id.
	 */
	public Article[] getLinksIn() {
		return null;
	}

	/**
	 * Returns an array of {@link Article}s, sorted by article id, that this article 
	 * links to. These are defined by the internal hyperlinks within article text. 
	 * If these hyperlinks point to redirects, then these are resolved. 
	 * 
	 * @return	an array of Articles that this article links to, sorted by id
	 */
	public Article[] getLinksOut()  {
		return null;
	}

	/**
	 * Returns the title of the article translated into the language given by <em>languageCode</em>
	 * (i.e. fn, jp, de, etc) or null if translation is not available. 
	 * 
	 * @param languageCode	the (generally 2 character) language code.
	 * @return the translated title if it is available; otherwise null.
	 */	
	public String getTranslation(String languageCode)  {		
		return null ;
	}

	/**
	 * Returns a TreeMap associating language code with translated title for all available translations 
	 * 
	 * @return a TreeMap associating language code with translated title.
	 */	
	public TreeMap<String,String> getTranslations() {
		return null;
	}

	/**
	 * @return the total number of links that are made to this article 
	 */
	public int getTotalLinksInCount()  {
		return 0;
	}

	/**
	 * @return the number of distinct articles which contain a link to this article 
	 */
	public int getDistinctLinksInCount()  {
		return 0;
	}

	/**
	 * @return the total number links that this article makes to other articles 
	 */
	public int getTotalLinksOutCount() {
		return 0;
	}

	/**
	 * @return the number of distinct articles that this article links to 
	 */
	public int getDistinctLinksOutCount() {
		return 0 ;
	}

	/**
	 * Returns an array of {@link Label Labels} that have been used to refer to this article.
	 * They are sorted by the number of times each label is used.
	 * 
	 * @return an array of {@link Label Labels} that have been used to refer to this article. 
	 */
	public Label[] getLabels() {
		return null;
	}


	/**
	 * A label that has been used to refer to the enclosing {@link Article}. These are mined from the title of the article, the 
	 * titles of {@link Redirect redirects} that point to the article, and the anchors of links that point to the article.   
	 */
	public class Label {

		private String text ;

		private long linkDocCount ;
		private long linkOccCount ;

		private boolean fromTitle ;
		private boolean fromRedirect ;
		private boolean isPrimary ;

		/**
		 * @return the text of this label (the title of the article or redirect, or the anchor of the link
		 */
		public String getText() {
			return text ;
		}

		/**
		 * @return the number of pages that contain links that associate this label with the enclosing {@link Article}.
		 */
		public long getLinkDocCount() {
			return linkDocCount;
		}

		/**
		 * @return the number of times this label occurs as the anchor text in links that refer to the enclosing {@link Article}.
		 */
		public long getLinkOccCount() {
			return linkOccCount;
		}

		/**
		 * @return true if this label matches the title of the enclosing {@link Article}, otherwise false.
		 */
		public boolean isFromTitle() {
			return fromTitle;
		}

		/**
		 * @return true if there is a {@link Redirect} that associates this label with the enclosing {@link Article}, otherwise false.
		 */
		public boolean isFromRedirect() {
			return fromRedirect;
		}

		/**
		 * @return true if the enclosing {@link Article} is the primary, most common sense for the given label, otherwise false.
		 */
		public boolean isPrimary() {
			return isPrimary;
		}
	}



	//public static ============================================================

	/*
	public static void main(String[] args) throws Exception {


		File databaseDirectory = new File("/research/dmilne/wikipedia/db/en/20100130");



		Wikipedia w = new Wikipedia(databaseDirectory) ;




		Article nzBirds = w.getMostLikelyArticle("Birds of New Zealand", null) ;
		//Article kiwi = w.getMostLikelyArticle("Kiwi", null) ;

		/*
		DbLinkLocationList ll = w.getEnvironment().getDbPageLinkOut().retrieve(kiwi.getId()) ;

		for (DbLinkLocation l:ll.getLinkLocations()) {
			System.out.print(" - " + l.getLinkId() +  ":") ;
			for (Integer s:l.getSentenceIndexes()) 
				System.out.print(" " + s) ;
			System.out.println() ;
		}
	 */

	//System.out.println(kiwi) ;

	/*
		Article nz = w.getMostLikelyArticle("New Zealand", null) ;

		for (Article art:kiwi.getLinksOut()){

			if (art.equals(nz))
				System.out.println(" - link: " + art) ;
		}

		for (Article art:nz.getLinksIn()) {
			if (art.equals(kiwi))
				System.out.println(" - link in: " + art) ;
		}


		ArrayList<Article> arts = new ArrayList<Article>() ;
		arts.add(w.getMostLikelyArticle("Kiwi", null)) ;
		arts.add(w.getMostLikelyArticle("Takahe", null)) ;


		System.out.println(nzBirds.getMarkup()) ;


		for (Article art:arts) {
			System.out.println("retrieving sentences mentioning " + art) ;

			for (int si: nzBirds.getSentenceIndexesMentioning(art)){
				System.out.println(nzBirds.getSentenceMarkup(si)) ;
			}

		}

		System.out.println("retrieving sentences mentioning all") ;

		for (int si: nzBirds.getSentenceIndexesMentioning(arts)){
			System.out.println(nzBirds.getSentenceMarkup(si)) ;
		}



	}*/


	/**
	 * Provides a demo of functionality available to Articles
	 * 
	 * @param args an array of arguments for connecting to a wikipedia database: server and database names at a minimum, and optionally a username and password
	 * @throws Exception if there is a problem with the wikipedia database.
	 *//*
	public static void main(String[] args) throws Exception {

		Wikipedia wikipedia = Wikipedia.getInstanceFromArguments(args) ;

		BufferedReader in = new BufferedReader( new InputStreamReader( System.in ) );	
		DecimalFormat df = new DecimalFormat("0") ;

		while (true) {
			System.out.println("Enter article title (or enter to quit): ") ;
			String title = in.readLine() ;

			if (title == null || title.equals(""))
				break ;

			Article article = wikipedia.getArticleByTitle(title) ;

			if (article == null) {
				System.out.println("Could not find exact match. Searching through anchors instead") ;
				article = wikipedia.getMostLikelyArticle(title, null) ; 
			}

			if (article == null) {
				System.out.println("Could not find exact article. Try again") ;
			} else {
				System.out.println("\n" + article + "\n") ;

				if (wikipedia.getDatabase().isContentImported()) {

					System.out.println(" - first sentence:") ;
					System.out.println("    - " + article.getFirstSentence(null, null)) ;

					System.out.println(" - first paragraph:") ;
					System.out.println("    - " + article.getFirstParagraph()) ;
				}

				//Category eqCategory = article.getEquivalentCategory() ;
				//if (eqCategory != null) {
				//	System.out.println("\n - equivalent category") ;
				//	System.out.println("    - " + eqCategory) ;
				//}

				System.out.println("\n - redirects (synonyms or very small related topics that didn't deserve a seperate article):") ;
				for (Redirect r: article.getRedirects())
					System.out.println("    - " + r);

				//System.out.println("\n - anchors (synonyms and hypernyms):") ;
				//for (AnchorText at:article.getAnchorTexts()) 
				//	System.out.println("    - \"" + at.getText() + "\" (used " + at.getCount() + " times)") ;

				System.out.println("\n - parent categories (hypernyms):") ;
				for (Category c: article.getParentCategories()) 
					System.out.println("    - " + c); 

				System.out.println("\n - language links (translations):") ;
				HashMap<String,String> translations = article.getTranslations() ;
				for (String lang:translations.keySet())
					System.out.println("    - \"" + translations.get(lang) + "\" (" + lang + ")") ;

				//System.out.println("\n - pages that this links to (related concepts):") ;
				//for (Article a: article.getLinksOut()) {
				//	System.out.println("    - " + a + " (" + df.format(article.getRelatednessTo(a)*100) + "% related)"); 
				//}
			}
			System.out.println("") ;
		}

	}*/
}
