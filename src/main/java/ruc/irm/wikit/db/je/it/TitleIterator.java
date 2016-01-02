package ruc.irm.wikit.db.je.it;


import org.apache.commons.lang3.tuple.Pair;
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


	WEnvironment env ;
	WIterator<String,Integer> iter ;
	WEntry<String, Integer> nextPair = null;


	public TitleIterator(WEnvironment env, WDatabase.DatabaseType type) {

		this.env = env ;
		if(type== WDatabase.DatabaseType.articlesByTitle) {
			iter = env.getDbArticlesByTitle().getIterator();
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
		} catch (NoSuchElementException e) {
			nextPair = null ;
		}
	}

	public void close() {
		iter.close();
	}
}