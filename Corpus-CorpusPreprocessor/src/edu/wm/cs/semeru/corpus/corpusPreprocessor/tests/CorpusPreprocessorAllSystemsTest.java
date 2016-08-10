package edu.wm.cs.semeru.corpus.corpusPreprocessor.tests;

import java.io.BufferedReader;
import java.io.FileReader;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class CorpusPreprocessorAllSystemsTest extends TestCase 
{
	String fileName1Path;
	String[] fileNames1;
	String fileName2Path;
	String[] fileNames2;

	protected void setUp() {
		fileName1Path="TestCases/Output/";
		fileNames1=new String[]{
			"Corpus-jEdit4.3-AfterSplit.corpusRawMethodLevelGranularity",
			"Corpus-jEdit4.3-AfterSplitStop.corpusRawMethodLevelGranularity",
			"Corpus-jEdit4.3-AfterSplitStopStem.corpusRawMethodLevelGranularity",
			"Corpus-System1-AfterSplit.corpusRawMethodLevelGranularity",
			"Corpus-System1-AfterSplitStop.corpusRawMethodLevelGranularity",
			"Corpus-System1-AfterSplitStopStem.corpusRawMethodLevelGranularity",
			"Corpus-System2-AfterSplit.corpusRawMethodLevelGranularity",
			"Corpus-System2-AfterSplitStop.corpusRawMethodLevelGranularity",
			"Corpus-System2-AfterSplitStopStem.corpusRawMethodLevelGranularity",
			"Corpus-System3-AfterSplit.corpusRawMethodLevelGranularity",
			"Corpus-System3-AfterSplitStop.corpusRawMethodLevelGranularity",
			"Corpus-System3-AfterSplitStopStem.corpusRawMethodLevelGranularity"
		};
		fileName2Path="TestCases/CorrectResultsOracle/";
		fileNames2=new String[]{
			"Corpus-jEdit4.3-AfterSplit.corpusRawMethodLevelGranularity",
			"Corpus-jEdit4.3-AfterSplitStop.corpusRawMethodLevelGranularity",
			"Corpus-jEdit4.3-AfterSplitStopStem.corpusRawMethodLevelGranularity",
			"Corpus-System1-AfterSplit.corpusRawMethodLevelGranularity",
			"Corpus-System1-AfterSplitStop.corpusRawMethodLevelGranularity",
			"Corpus-System1-AfterSplitStopStem.corpusRawMethodLevelGranularity",
			"Corpus-System2-AfterSplit.corpusRawMethodLevelGranularity",
			"Corpus-System2-AfterSplitStop.corpusRawMethodLevelGranularity",
			"Corpus-System2-AfterSplitStopStem.corpusRawMethodLevelGranularity",
			"Corpus-System3-AfterSplit.corpusRawMethodLevelGranularity",
			"Corpus-System3-AfterSplitStop.corpusRawMethodLevelGranularity",
			"Corpus-System3-AfterSplitStopStem.corpusRawMethodLevelGranularity"
		};
	}

	public void tearDown() {
		fileName1Path=null;
		fileName2Path=null;
	}

	public void testEqualFile1() throws Exception {
		String contentFile1 = getContentFromFile(fileName1Path+fileNames1[0]);
		String contentFile2 = getContentFromFile(fileName2Path+fileNames2[0]);
		assertEquals(contentFile1, contentFile2);
	}

	public void testEqualFile2() throws Exception {
		String contentFile1 = getContentFromFile(fileName1Path+fileNames1[1]);
		String contentFile2 = getContentFromFile(fileName2Path+fileNames2[1]);
		assertEquals(contentFile1, contentFile2);
	}
	
	public void testEqualFile3() throws Exception {
		String contentFile1 = getContentFromFile(fileName1Path+fileNames1[2]);
		String contentFile2 = getContentFromFile(fileName2Path+fileNames2[2]);
		assertEquals(contentFile1, contentFile2);
	}
	
	public void testEqualFile4() throws Exception {
		String contentFile1 = getContentFromFile(fileName1Path+fileNames1[3]);
		String contentFile2 = getContentFromFile(fileName2Path+fileNames2[3]);
		assertEquals(contentFile1, contentFile2);
	}
	
	public void testEqualFile5() throws Exception {
		String contentFile1 = getContentFromFile(fileName1Path+fileNames1[4]);
		String contentFile2 = getContentFromFile(fileName2Path+fileNames2[4]);
		assertEquals(contentFile1, contentFile2);
	}
	
	public void testEqualFile6() throws Exception {
		String contentFile1 = getContentFromFile(fileName1Path+fileNames1[5]);
		String contentFile2 = getContentFromFile(fileName2Path+fileNames2[5]);
		assertEquals(contentFile1, contentFile2);
	}
	
	public void testEqualFile7() throws Exception {
		String contentFile1 = getContentFromFile(fileName1Path+fileNames1[6]);
		String contentFile2 = getContentFromFile(fileName2Path+fileNames2[6]);
		assertEquals(contentFile1, contentFile2);
	}
	
	public void testEqualFile8() throws Exception {
		String contentFile1 = getContentFromFile(fileName1Path+fileNames1[7]);
		String contentFile2 = getContentFromFile(fileName2Path+fileNames2[7]);
		assertEquals(contentFile1, contentFile2);
	}
	
	public void testEqualFile9() throws Exception {
		String contentFile1 = getContentFromFile(fileName1Path+fileNames1[8]);
		String contentFile2 = getContentFromFile(fileName2Path+fileNames2[8]);
		assertEquals(contentFile1, contentFile2);
	}
	public void testEqualFile10() throws Exception {
		String contentFile1 = getContentFromFile(fileName1Path+fileNames1[9]);
		String contentFile2 = getContentFromFile(fileName2Path+fileNames2[9]);
		assertEquals(contentFile1, contentFile2);
	}
	public void testEqualFile11() throws Exception {
		String contentFile1 = getContentFromFile(fileName1Path+fileNames1[10]);
		String contentFile2 = getContentFromFile(fileName2Path+fileNames2[10]);
		assertEquals(contentFile1, contentFile2);
	}
	public void testEqualFile12() throws Exception {
		String contentFile1 = getContentFromFile(fileName1Path+fileNames1[11]);
		String contentFile2 = getContentFromFile(fileName2Path+fileNames2[11]);
		assertEquals(contentFile1, contentFile2);
	}
	
	String getContentFromFile(String fileName)
	{
		System.out.println("Reading file "+fileName);
		StringBuilder buf=new StringBuilder();
		BufferedReader br;
		try
		{
			br=new BufferedReader(new FileReader(fileName));
			String line;
			while ((line=br.readLine())!=null)
			{
				System.out.println(line);
				buf.append(line+"\n");
			}
			br.close();
		}
		catch (Exception e)
		{

			e.printStackTrace();
		}

		return buf.toString();
	}
	
	public static Test suite() 
	{
	    return new TestSuite(CorpusPreprocessorAllSystemsTest.class);
	}
}
