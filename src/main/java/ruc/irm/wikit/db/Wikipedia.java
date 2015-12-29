/*
 *    Wikipedia.java
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

package ruc.irm.wikit.db;

import com.sleepycat.je.EnvironmentLockedException;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.db.je.WEnvironment;
import ruc.irm.wikit.model.Page;
import ruc.irm.wikit.util.ProgressTracker;

import java.io.File;
import java.io.IOException;


/**
 * Represents a single dump or instance of Wikipedia
 */
public class Wikipedia {
	private static Logger LOG = LoggerFactory.getLogger(Wikipedia.class);
	private WEnvironment env ;

	/**
	 * Initialises a newly created Wikipedia according to the given configuration. 
	 * 
	 * This can be a time consuming process if the given configuration specifies databases that need to be cached to memory.
	 * 
	 * This preparation can be done in a separate thread if required, in which case progress can be tracked using {@link #getProgress()}, {@link #getPreparationTracker()} and {@link #isReady()}.
	 *  
	 * @param conf a configuration that describes where the databases are located, etc. 
	 * @param threadedPreparation true if preparation (connecting to databases, caching data to memory) should be done in a separate thread, otherwise false
	 * @throws EnvironmentLockedException if the underlying database environment is unavailable.
	 */
	public Wikipedia(Conf conf) throws
			EnvironmentLockedException {
		this.env = new WEnvironment(conf) ;
	}

	/**
	 * Returns the environment that this is connected to
	 * 
	 * @return the environment that this is connected to
	 */
	public WEnvironment getEnvironment() {
		return env ;
	}

	/**
	 * Returns true if the preparation work has been completed, otherwise false
	 * 
	 * @return true if the preparation work has been completed, otherwise false
	 */
	public boolean isReady() {
		return env.isReady() ;
		
	}

	/**
	 * Returns the Page referenced by the given id. The page can be cast into the appropriate type for 
	 * more specific functionality. 
	 *  
	 * @param id	the id of the Page to retrieve.
	 * @return the Page referenced by the given id, or null if one does not exist. 
	 */
	public Page getPageById(int id) {
		return Page.createPage(env, id) ;
	}

	/**
	 * Tidily closes the database environment behind this wikipedia instance. This should be done whenever
	 * one is finished using it. 
	 */
	public void close() {
		env.close();
		this.env = null ;
	}

	@Override
	public void finalize() {
		if (this.env != null)
			LOG.warn("Unclosed wikipedia. You may be causing a memory leak.") ;
	}

	public static void main(String[] args) throws ParseException, IOException {
		String helpMsg = "usage: Wikipedia -c config.xml -pid 123";

		HelpFormatter helpFormatter = new HelpFormatter();
		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption(new Option("c", true, "config file"));
		options.addOption(new Option("pid", true, "page id"));

		CommandLine commandLine = parser.parse(options, args);
		if (!commandLine.hasOption("c")) {
			helpFormatter.printHelp(helpMsg, options);
			return;
		}

		Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);
		Wikipedia wikipedia = new Wikipedia(conf);

		if (commandLine.hasOption("pid")) {
			String pid = commandLine.getOptionValue("pid");
			Page page = wikipedia.getPageById(NumberUtils.toInt(pid));
			System.out.println(page);
		}
		boolean overwrite = BooleanUtils.toBoolean(commandLine.getOptionValue("overwrite"));
		WEnvironment.buildEnvironment(conf, overwrite);
		System.out.println("I'm DONE for create WEnvironment!");
	}
}
