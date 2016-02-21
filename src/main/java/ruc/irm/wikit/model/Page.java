package ruc.irm.wikit.model;

import ruc.irm.wikit.db.je.WEnvironment;
import ruc.irm.wikit.db.je.struct.PageRecord;

/**
 * Represents pages of any type in Wikipedia
 */
public class Page implements Comparable<Page> {
	
	/**
	 * Types that wikipedia pages can be.
	 */
	public enum PageType {
		
		/**
		 * A page that provides informative text about a topic.
		 */
		article, 
		
		/**
		 * A page that hierarchically organises other pages
		 */
		category, 
		
		/**
		 * A page that exists only to connect an alternative title to an article
		 */
		redirect, 
	
		/**
		 * A page that lists possible senses of an ambiguous word
		 */
		disambiguation, 
		
		/**
		 * A page that can be transcluded into other pages
		 */
		template,
		
		/**
		 * A type of page that we don't currently deal with (e.g templates)
		 */
		invalid
	} ;

	protected int id ;
	protected String title ;
	protected PageType type ;
	protected String content;
	protected int depth ;
	protected Double weight = null ;

	protected WEnvironment env ;
	protected boolean detailsSet ;

	//constructor =============================================================

	/**
	 * Initialises a newly created Page so that it represents the page given by <em>id</em> and <em>DbPage</em>.
	 * 
	 * This is the most efficient page constructor as no database lookup is required.
	 * 
	 * @param	env	an active WikipediaEnvironment
	 * @param	id	the unique identifier of the page
	 * @param	pd  details (title, type, etc) of the page	 
	 */
	protected Page(WEnvironment env, int id, PageRecord pd)  {
		this.env = env ;
		this.id = id ;
		setDetails(pd) ;
	}


	/**
	 * Initialises a newly created Page so that it represents the page given by <em>id</em>. This is also an efficient
	 * constructor, since details (page title, type, etc) are only retrieved when requested.
	 * 
	 * @param	env	an active WikipediaEnvironment
	 * @param	id	the unique identifier of the Wikipedia page
	 */
	public Page(WEnvironment env, int id) {
		this.env = env ;
		this.id = id ;
		this.detailsSet = false ;
	}


	//public ==================================================================

	/**
	 * @return the database environment
	 */
	public WEnvironment getEnvironment() {
		return env;
	}


	/**
	 *  @return true if a page with this id is defined in Wikipedia, otherwise false.
	 */
	public boolean exists() {
		if (!detailsSet) 
			setDetails() ;

		return (type != PageType.invalid) ;
	}

	/**
	 * Sets the weight by which this page will be compared to others.
	 * 
	 * @param weight  the weight by which this page will be compared to others.
	 */
	public void setWeight(Double weight) {
		this.weight = weight ;
	}

	/**
	 * @return the weight by which this page is compared to others. (may be null, in which case the page is compared only via id)
	 */	
	public Double getWeight() {
		return weight ;
	}

	
	/**
	 * @param p the page to compare to
	 * @return true if this page has the same id as the given one, otherwise false
	 */
	public boolean equals(Page p) {
		return p.id == id ;
	}

	/**
	 * Compares this page to another. If weights are defined for both pages, then the page with the larger 
	 * weight will be considered smaller (and thus appear earlier in sorted lists). Otherwise, the comparison is made based on their ids. 
	 * 
	 * @param	p	the Page to be compared
	 * @return	see above.
	 */
	public int compareTo(Page p) {

		if (p.id == id)
			return 0 ;
		
		int cmp = 0 ;
		
		if (p.weight != null && weight != null && p.weight != weight)
			cmp =  p.weight.compareTo(weight) ; 
		
		if (cmp == 0)
			cmp = new Integer(id).compareTo(p.id) ;
			
		return cmp ;
		
	}

	/**
	 * Returns a string representation of this page, in the format "<em>id</em>: <em>title</em>".
	 * 
	 * @return a string representation of the page
	 */
	public String toString()  {
		String s = getId() + ": " + getTitle() ;
		return s ;
	}



	/**
	 * @return the unique identifier
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		if (!detailsSet) setDetails() ;

		return title;
	}

	/**
	 * @return the title
	 */
	public String getContent() {
		if (!detailsSet) setDetails() ;

		return content;
	}


	/**
	 * @return	the type of the page
	 */
	public PageType getType() {
		if (!detailsSet) setDetails() ;

		return type;
	}

	/**
	 * @return the length of the shortest path from this page to the root category, or null if no path exists.
	 */
	public Integer getDepth() {
		if (!detailsSet) setDetails() ;

		if (depth < 0)
			return null ;
		else
			return depth ;
	}



	//public static ============================================================


	/**
	 * Instantiates the appropriate subclass of Page given the supplied parameters
	 * 
	 * @param env an active Wikipedia environment
	 * @param id the id of the page
	 * @return the instantiated page, which can be safely cast as appropriate
	 */
	public static Page createPage(WEnvironment env, int id)  {

		PageRecord pd = env.getDbPage().retrieve(id) ;

		if (pd != null)
			return createPage(env, id, pd) ;
		else {
			pd = new PageRecord(0, "Invalid id or excluded via caching", PageType.invalid.ordinal(), "") ;
			return new Page(env, id, pd) ;
		}
	}

	/**
	 * Instantiates the appropriate subclass of Page given the supplied parameters
	 * 
	 * @param env an active Wikipedia environment
	 * @param id the id of the page
	 * @param pd the details of the page
	 * @return the instantiated page, which can be safely cast as appropriate
	 */
	public static Page createPage(WEnvironment env, int id, PageRecord pd) {

		Page p = null ;

		PageType type = PageType.values()[pd.getType()] ;

		switch (type) {
		case article:
			p = new Article(env, id, pd) ;
			break ;
//		case redirect:
//			p = new Redirect(env, id, pd) ;
//			break ;
//		case disambiguation:
//			p = new Disambiguation(env, id, pd) ;
//			break ;
//		case category:
//			p = new Category(env, id, pd) ;
//			break ;
		}

		return p ;
	}


	//protected and private ====================================================

	private void setDetails()  {

		try {
			PageRecord pd = env.getDbPage().retrieve(id) ;

			if (pd == null) {
				throw new Exception() ;
			} else {
				setDetails(pd) ;
			}
		} catch (Exception e) {
			title = null ;
			type = PageType.invalid ;
		}
	}

	private void setDetails(PageRecord pd)  {
		title = pd.getTitle() ;
		type = PageType.values()[pd.getType()] ;
		content = pd.getText();
		detailsSet = true ;
	}

}
