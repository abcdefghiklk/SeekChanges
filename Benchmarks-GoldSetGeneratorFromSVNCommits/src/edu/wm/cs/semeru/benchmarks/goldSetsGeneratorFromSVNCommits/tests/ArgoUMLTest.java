package edu.wm.cs.semeru.benchmarks.goldSetsGeneratorFromSVNCommits.tests;

import java.io.BufferedReader;
import java.io.FileReader;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ArgoUMLTest extends TestCase 
{
	String fileName1Path;
	String[] fileNames1;
	String fileName2Path;
	String[] fileNames2;

	protected void setUp() {
		fileName1Path="TestCases/Output/ArgoUML/";
		
		fileNames1=new String[]{
			"GoldSetsFromSVNCommits/15245.goldSetSVNCommit",
			"GoldSetsFromSVNCommits/15248.goldSetSVNCommit",
			"GoldSetsFromSVNCommitsDebug/15245.goldSetSVNCommitDebug",
			"GoldSetsFromSVNCommitsDebug/15248.goldSetSVNCommitDebug"
		};
		fileName2Path="TestCases/CorrectResultsOracle/ArgoUML/";
		fileNames2=new String[]{
			"GoldSetsFromSVNCommits/15245.goldSetSVNCommit",
			"GoldSetsFromSVNCommits/15248.goldSetSVNCommit",
			"GoldSetsFromSVNCommitsDebug/15245.goldSetSVNCommitDebug",
			"GoldSetsFromSVNCommitsDebug/15248.goldSetSVNCommitDebug"
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
	    return new TestSuite(ArgoUMLTest.class);
	}
}
