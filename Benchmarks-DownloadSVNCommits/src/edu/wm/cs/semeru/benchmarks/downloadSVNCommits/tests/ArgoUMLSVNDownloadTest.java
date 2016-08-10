package edu.wm.cs.semeru.benchmarks.downloadSVNCommits.tests;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ArgoUMLSVNDownloadTest extends TestCase 
{
	ArrayList<String> listOfFiles;
	String inputPathOutput;
	String inputPathOracle;
	
	protected void setUp() {
		inputPathOracle="TestCases/CorrectResultsOracle/ArgoUML/";
		ArrayList<String> listOfFilesOracleFullPath=Utils.getListOfFilesAndFolders(inputPathOracle);
		
		listOfFiles=new ArrayList<String>();
		for (String fileNameFullPathTemp : listOfFilesOracleFullPath)
		{
			String fileNameFullPath=fileNameFullPathTemp.replace("\\","/");
			String fileName=fileNameFullPath.substring(fileNameFullPath.indexOf(inputPathOracle)+inputPathOracle.length());
			listOfFiles.add(fileName);
		}
		
		inputPathOutput="TestCases/Output/ArgoUML/";

		System.out.println("Number of files loaded : "+listOfFiles.size());
	}

	public void tearDown() {
		listOfFiles=null;
	}

	public void testEqualFileNumbers() throws Exception {
		assertEquals(91,listOfFiles.size());
	}

	public void testEqualFiles() throws Exception {
		for (String fileName : listOfFiles)
		{
			System.out.println(inputPathOutput+fileName);
			String contentFile1 = getContentFromFile(inputPathOutput+fileName);
			String contentFile2 = getContentFromFile(inputPathOracle+fileName);
			assertEquals(contentFile1, contentFile2);
		}
		
		assertEquals(true, true);
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
	    return new TestSuite(ArgoUMLSVNDownloadTest.class);
	}
}
