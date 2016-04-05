/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */





package ruc.irm.wikit.app;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Convert the text in each token in the token sequence in the data field to lower case.
   @author Andrew McCallum <a href="mailto:mccallum@cs.umass.edu">mccallum@cs.umass.edu</a>
 */

public class FilterTokenSequence extends Pipe implements Serializable
{
	private Set<String> whiteWords = new HashSet<>();

	public FilterTokenSequence(File featureFile) {
		if(featureFile.exists()) {
			try {
				List<String> lines = Files.readLines(featureFile, Charsets.UTF_8);
				whiteWords.addAll(lines);
			} catch (IOException e) {
				throw new IOError(e);
			}
		}
	}

	private boolean accept(Token token) {
		return whiteWords.isEmpty() || whiteWords.contains(token.getText());
	}

	public Instance pipe (Instance carrier)
	{
		TokenSequence tokens = new TokenSequence();
		TokenSequence ts = (TokenSequence) carrier.getData();
		for (int i = 0; i < ts.size(); i++) {
			Token t = ts.get(i);
			if(accept(t)){
				tokens.add(t);
			}
		}
		carrier.setData(tokens);
		return carrier;
	}

	// Serialization 
	
	private static final long serialVersionUID = 1;
	private static final int CURRENT_SERIAL_VERSION = 0;
	
	private void writeObject (ObjectOutputStream out) throws IOException {
		out.writeInt (CURRENT_SERIAL_VERSION);
	}
	
	private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
		int version = in.readInt ();
	}

}
