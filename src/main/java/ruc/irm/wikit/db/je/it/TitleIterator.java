package ruc.irm.wikit.db.je.it;


import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ruc.irm.wikit.db.je.WDatabase;
import ruc.irm.wikit.db.je.WEntry;
import ruc.irm.wikit.db.je.WEnvironment;
import ruc.irm.wikit.db.je.WIterator;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author David Milne
 * 
 * Provides efficient iteration over the labels in Wikipedia
 */
public class TitleIterator implements Iterator<WEntry<String, Integer>>{
	private static final Logger LOG = LoggerFactory.getLogger(TitleIterator.class);

	WEnvironment env ;
	WIterator<String,Integer> iter ;
	WEntry<String, Integer> nextPair = null;


	public TitleIterator(WEnvironment env, WDatabase.DatabaseType type) {

		this.env = env ;
		if(type== WDatabase.DatabaseType.articlesByTitle) {
			iter = env.getDbArticlesByTitle().getIterator();
			System.out.println(iter.next());
		} else if (type == WDatabase.DatabaseType.categoriesByTitle) {
			iter = env.getDbCategoriesByTitle().getIterator();
		}

		queueNext() ;
	}

	@Override
	public boolean hasNext() {
		return (nextPair != null) ;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException() ;
	}

	@Override
	public WEntry<String, Integer> next() {

		if (nextPair == null)
			throw new NoSuchElementException() ;

		WEntry<String, Integer> pair = nextPair ;
		queueNext() ;

		return pair ;
	}

	private void queueNext() {

		try {
			WEntry<String,Integer> nextPair = iter.next();
			iter.hasNext();
		} catch (NoSuchElementException e) {
			LOG.error("queueNext error.", e);
			nextPair = null ;
		}
	}

	public void close() {
		iter.close();
	}
}