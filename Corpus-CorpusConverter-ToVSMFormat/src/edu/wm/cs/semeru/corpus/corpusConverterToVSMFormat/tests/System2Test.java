package edu.wm.cs.semeru.corpus.corpusConverterToVSMFormat.tests;

import java.io.BufferedReader;
import java.io.FileReader;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class System2Test extends TestCase 
{
	String fileName1Path;
	String[] fileNames1;
	String fileName2Path;
	String[] fileNames2;

	protected void setUp() {
		fileName1Path="TestCases/Output/";
		fileNames1=new String[]{
			"Corpus-System2-AfterSplitStopStem.corpusRawMethodLevelGranularity.CoOccurenceMatrixVSMFormat",
		};
		fileName2Path="TestCases/CorrectResultsOracle/";
		fileNames2=new String[]{
			"Corpus-System2-AfterSplitStopStem.corpusRawMethodLevelGranularity.CoOccurenceMatrixVSMFormat",
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
	    return new TestSuite(System2Test.class);
	}
}
