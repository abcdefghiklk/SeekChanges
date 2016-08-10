package edu.wm.cs.semeru.corpus.corpusPreprocessor.tests;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import edu.wm.cs.semeru.corpus.corpusPreprocessor.CorpusPreprocessor;

public class SplittingTest extends TestCase 
{
	private CorpusPreprocessor corpusPreprocessor;

	protected void setUp() {
		corpusPreprocessor=new CorpusPreprocessor("TestCases/Input/Corpus-jEdit4.3.corpusRawMethodLevelGranularity","TestCases/Output/");
	}

	public void tearDown() {
		corpusPreprocessor=null;
	}

	public void testSplitting1() throws Exception {
		String originalBuf = "JavaJRE and JEdit and ABcedar and ABceDAr FileVIew";
		String expectedBuf = " java jre and j edit and a bcedar and a bce d ar  file v iew ";
		String actualBuf = corpusPreprocessor.splitIdentifiers(originalBuf,false);
		assertEquals(expectedBuf, actualBuf);
	}

	public void testSplitting2() throws Exception {
		String originalBuf = "AStringWithFTPOrSFTP";
		String expectedBuf = "a string with ftp or sftp ";
		String actualBuf = corpusPreprocessor.splitIdentifiers(originalBuf,false);
		assertEquals(expectedBuf, actualBuf);
	}

	public void testSplitting3() throws Exception {
		String originalBuf = "Test_AndThis_JRE FTP";
		String expectedBuf = " test  and this jre ftp ";
		String actualBuf = corpusPreprocessor.splitIdentifiers(originalBuf,false);
		assertEquals(expectedBuf, actualBuf);
	}
	
	public static Test suite() 
	{
	    return new TestSuite(SplittingTest.class);
	}
}
