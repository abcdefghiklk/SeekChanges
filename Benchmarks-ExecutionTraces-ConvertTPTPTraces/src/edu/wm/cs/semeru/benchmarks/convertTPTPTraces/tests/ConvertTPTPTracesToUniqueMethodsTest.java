package edu.wm.cs.semeru.benchmarks.convertTPTPTraces.tests;

import java.io.BufferedReader;
import java.io.FileReader;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ConvertTPTPTracesToUniqueMethodsTest extends TestCase 
{
	String fileName1Path;
	String[] fileNames;
	String fileName2Path;

	protected void setUp() {
		fileName1Path="TestCases/Output/ArgoUML0.22/UniqueMethodsTraces/";
		fileNames=new String[]{
			"4298.uniqueMethodsTrace",
		};
		fileName2Path="TestCases/CorrectResultsOracle/ArgoUML0.22/UniqueMethodsTraces/";
	}

	public void tearDown() {
		fileName1Path=null;
		fileName2Path=null;
	}

	public void testEqualFile1() throws Exception {
		String contentFile1 = getContentFromFile(fileName1Path+fileNames[0]);
		String contentFile2 = getContentFromFile(fileName2Path+fileNames[0]);
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
	    return new TestSuite(ConvertTPTPTracesToUniqueMethodsTest.class);
	}
}
