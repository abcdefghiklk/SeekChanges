/*
 * Copyright (c) 2008 Eric Berry <elberry@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
/**
 * 
 */
package com.townsfolkdesigns.lucene.jedit;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.gjt.sp.util.Log;

import com.townsfolkdesigns.lucene.indexer.FileTypeDelegatingIndexer;
import com.townsfolkdesigns.lucene.jedit.manager.IndexStatsManager;
import com.townsfolkdesigns.lucene.jedit.manager.OptionsManager;
import com.townsfolkdesigns.lucene.parser.DefaultFileDocumentParser;

/**
 * @author elberry
 * 
 */
public class LucenePluginIndexer extends FileTypeDelegatingIndexer {

	private OptionsManager optionsManager;
	private IndexStatsManager indexStatsManager;

	public LucenePluginIndexer() {
		File indexStoreDir = new LucenePlugin().getIndexStoreDirectory();
		if (!indexStoreDir.exists()) {
			indexStoreDir.mkdirs();
		}
		File indexStoreFile = new File(indexStoreDir, LucenePlugin.class.getName());
		setIndexStore(indexStoreFile.getPath());
		setDefaultDocumentParser(new DefaultFileDocumentParser());
		setOptionsManager(OptionsManager.getInstance());
		setIndexStatsManager(new IndexStatsManager());
		try {
			setIndexWriter(new IndexWriter(indexStoreFile, new StandardAnalyzer()));
		} catch (Exception e) {
			Log.log(Log.ERROR, this, "Error creating index writer", e);
		}
	}

	@Override
	public void run() {
		// get locations from the options manager.
		List<String> directories = getOptionsManager().getDirectories();
		String[] locations = directories.toArray(new String[0]);
		setLocations(locations);
		// run method overridden so that the stats can be saved in the manager.
		indexStatsManager.setIndexStartTime(new Date());
		indexStatsManager.setIndexing(true);
		super.run();
		indexStatsManager.setIndexEndTime(new Date());
		indexStatsManager.setDirectoriesIndexed(getDirectoriesIndexed());
		indexStatsManager.setFilesIndexed(getFilesIndexed());
	}

	public OptionsManager getOptionsManager() {
		return optionsManager;
	}

	public void setOptionsManager(OptionsManager optionsManager) {
		this.optionsManager = optionsManager;
	}

	public IndexStatsManager getIndexStatsManager() {
		return indexStatsManager;
	}

	public void setIndexStatsManager(IndexStatsManager indexStatsManager) {
		this.indexStatsManager = indexStatsManager;
	}

}
