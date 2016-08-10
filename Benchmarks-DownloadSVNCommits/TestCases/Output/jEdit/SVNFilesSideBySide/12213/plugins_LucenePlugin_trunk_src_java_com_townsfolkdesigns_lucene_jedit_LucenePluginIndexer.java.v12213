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
import java.util.Date;
import java.util.List;

import com.townsfolkdesigns.lucene.indexer.FileTypeDelegatingIndexer;
import com.townsfolkdesigns.lucene.jedit.manager.IndexStatsManager;
import com.townsfolkdesigns.lucene.jedit.manager.OptionsManager;
import com.townsfolkdesigns.lucene.parser.DefaultFileDocumentParser;

/**
 * @author elberry
 * 
 */
public class LucenePluginIndexer extends FileTypeDelegatingIndexer {

	private IndexStatsManager indexStatsManager;
	private OptionsManager optionsManager;

	public LucenePluginIndexer() {
	}

	public IndexStatsManager getIndexStatsManager() {
		return indexStatsManager;
	}

	public OptionsManager getOptionsManager() {
		return optionsManager;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.townsfolkdesigns.lucene.indexer.AbstractFileIndexer#index()
	 */
	@Override
	public void index() {
		// get locations from the options manager.
		List<String> directories = getOptionsManager().getDirectories();
		String[] locations = directories.toArray(new String[0]);
		setLocations(locations);
		// index method overridden so that the stats can be saved in the manager.
		indexStatsManager.setIndexStartTime(new Date());
		indexStatsManager.setIndexing(true);
		super.index();
		indexStatsManager.setIndexEndTime(new Date());
		indexStatsManager.setDirectoriesIndexed(getDirectoriesIndexed());
		indexStatsManager.setFilesIndexed(getFilesIndexed());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.townsfolkdesigns.lucene.indexer.FileTypeDelegatingIndexer#init()
	 */
	@Override
	public void init() {
		File indexStoreDir = new LucenePlugin().getIndexStoreDirectory();
		if (!indexStoreDir.exists()) {
			indexStoreDir.mkdirs();
		}
		File indexStoreFile = new File(indexStoreDir, LucenePlugin.class.getName());
		setIndexStoreDirectory(indexStoreFile);
		setDefaultDocumentParser(new DefaultFileDocumentParser());
		setOptionsManager(OptionsManager.getInstance());
		setIndexStatsManager(new IndexStatsManager());
		super.init();
	}

	public void setIndexStatsManager(IndexStatsManager indexStatsManager) {
		this.indexStatsManager = indexStatsManager;
	}

	public void setOptionsManager(OptionsManager optionsManager) {
		this.optionsManager = optionsManager;
	}

}
