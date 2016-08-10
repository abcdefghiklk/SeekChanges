package edu.wm.cs.semeru.CorpusGenerator.tests;

import java.io.BufferedReader;
import java.io.FileReader;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class CorpusGeneratorClassLevelGranularityTest extends TestCase {

	String fileName1Path;
	String[] fileNames1;
	String fileName2Path;
	String[] fileNames2;

	 protected void setUp() {
		fileName1Path="TestCases/Output/ClassLevelGranularity/";
		fileNames1=new String[]{
			"Corpus-System1.corpusMappingClassLevelGranularity",
			"Corpus-System1.corpusRawClassLevelGranularity",
			"Corpus-System1.corpusRawAndMappingDebugClassLevelGranularity",
			"Corpus-System2.corpusMappingClassLevelGranularity",
			"Corpus-System2.corpusRawClassLevelGranularity",
			"Corpus-System2.corpusRawAndMappingDebugClassLevelGranularity",
			"Corpus-System3.corpusMappingClassLevelGranularity",
			"Corpus-System3.corpusRawClassLevelGranularity",
			"Corpus-System3.corpusRawAndMappingDebugClassLevelGranularity",
			"Corpus-jEdit4.3.corpusMappingClassLevelGranularity",
			"Corpus-jEdit4.3.corpusRawClassLevelGranularity",
			"Corpus-jEdit4.3.corpusRawAndMappingDebugClassLevelGranularity"
		};
		fileName2Path="TestCases/CorrectResultsOracle/ClassLevelGranularity/";
		fileNames2=new String[]{
			"Corpus-System1.corpusMappingClassLevelGranularity",
			"Corpus-System1.corpusRawClassLevelGranularity",
			"Corpus-System1.corpusRawAndMappingDebugClassLevelGranularity",
			"Corpus-System2.corpusMappingClassLevelGranularity",
			"Corpus-System2.corpusRawClassLevelGranularity",
			"Corpus-System2.corpusRawAndMappingDebugClassLevelGranularity",
			"Corpus-System3.corpusMappingClassLevelGranularity",
			"Corpus-System3.corpusRawClassLevelGranularity",
			"Corpus-System3.corpusRawAndMappingDebugClassLevelGranularity",
			"Corpus-jEdit4.3.corpusMappingClassLevelGranularity",
			"Corpus-jEdit4.3.corpusRawClassLevelGranularity",
			"Corpus-jEdit4.3.corpusRawAndMappingDebugClassLevelGranularity"
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
	
	String getContentFromFile(String fileName) {
		System.out.println("Reading file "+fileName);
		StringBuilder buf = new StringBuilder();
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(fileName));
			String line;
			while ((line = br.readLine()) != null) {
				System.out.println(line);
				buf.append(line+"\n");
			}
			br.close();
		} catch (Exception e) {
			
			e.printStackTrace();
		}

		return buf.toString();
	}
	
	public static Test suite() {
	    return new TestSuite(CorpusGeneratorClassLevelGranularityTest.class);
	}
}
