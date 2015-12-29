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

import ruc.irm.wikit.db.je.WEnvironment;
import ruc.irm.wikit.db.je.struct.DbPage;

import java.util.*;

/**
 * Represents articles in Wikipedia; the pages that contain descriptive text regarding a particular topic. 
 */
public class Article extends Page {



	/**
	 * Initialises a newly created Article so that it represents the article given by <em>id</em>.
	 * 
	 * @param env	an active WEnvironment
	 * @param id	the unique identifier of the article
	 */
	public Article(WEnvironment env, int id) {
		super(env, id) ;
	}

	protected Article(WEnvironment env, int id, DbPage pd) {
		super(env, id, pd) ;
	}

	/**
	 * Returns a array of {@link Redirect Redirects}, sorted by id, that point to this article.
	 * 
	 * @return	an array of Redirects, sorted by id
	 */
	public Redirect[] getRedirects()  {
		return null;
	}

}
