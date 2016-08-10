package edu.wm.cs.semeru.CorpusGenerator.tests;

import java.io.BufferedReader;
import java.io.FileReader;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class CorpusGeneratorMethodLevelGranularityTest extends TestCase {

	String fileName1Path;
	String[] fileNames1;
	String fileName2Path;
	String[] fileNames2;

	 protected void setUp() {
		fileName1Path="TestCases/Output/MethodLevelGranularity/";
		fileNames1=new String[]{
			"Corpus-System1.corpusMappingMethodLevelGranularity",
			"Corpus-System1.corpusMappingWithPackageSeparatorMethodLevelGranularity",
			"Corpus-System1.corpusRawMethodLevelGranularity",
			"Corpus-System1.corpusRawAndMappingDebugMethodLevelGranularity",
			"Corpus-System2.corpusMappingMethodLevelGranularity",
			"Corpus-System2.corpusMappingWithPackageSeparatorMethodLevelGranularity",
			"Corpus-System2.corpusRawMethodLevelGranularity",
			"Corpus-System2.corpusRawAndMappingDebugMethodLevelGranularity",
			"Corpus-System3.corpusMappingMethodLevelGranularity",
			"Corpus-System3.corpusMappingWithPackageSeparatorMethodLevelGranularity",
			"Corpus-System3.corpusRawMethodLevelGranularity",
			"Corpus-System3.corpusRawAndMappingDebugMethodLevelGranularity",
			"Corpus-jEdit4.3.corpusMappingMethodLevelGranularity",
			"Corpus-jEdit4.3.corpusMappingWithPackageSeparatorMethodLevelGranularity",
			"Corpus-jEdit4.3.corpusRawMethodLevelGranularity",
			"Corpus-jEdit4.3.corpusRawAndMappingDebugMethodLevelGranularity"
		};
		fileName2Path="TestCases/CorrectResultsOracle/MethodLevelGranularity/";
		fileNames2=new String[]{
			"Corpus-System1.corpusMappingMethodLevelGranularity",
			"Corpus-System1.corpusMappingWithPackageSeparatorMethodLevelGranularity",
			"Corpus-System1.corpusRawMethodLevelGranularity",
			"Corpus-System1.corpusRawAndMappingDebugMethodLevelGranularity",
			"Corpus-System2.corpusMappingMethodLevelGranularity",
			"Corpus-System2.corpusMappingWithPackageSeparatorMethodLevelGranularity",
			"Corpus-System2.corpusRawMethodLevelGranularity",
			"Corpus-System2.corpusRawAndMappingDebugMethodLevelGranularity",
			"Corpus-System3.corpusMappingMethodLevelGranularity",
			"Corpus-System3.corpusMappingWithPackageSeparatorMethodLevelGranularity",
			"Corpus-System3.corpusRawMethodLevelGranularity",
			"Corpus-System3.corpusRawAndMappingDebugMethodLevelGranularity",
			"Corpus-jEdit4.3.corpusMappingMethodLevelGranularity",
			"Corpus-jEdit4.3.corpusMappingWithPackageSeparatorMethodLevelGranularity",
			"Corpus-jEdit4.3.corpusRawMethodLevelGranularity",
			"Corpus-jEdit4.3.corpusRawAndMappingDebugMethodLevelGranularity"
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

	public void testEqualFile13() throws Exception {
		String contentFile1 = getContentFromFile(fileName1Path+fileNames1[12]);
		String contentFile2 = getContentFromFile(fileName2Path+fileNames2[12]);
		assertEquals(contentFile1, contentFile2);
	}
	
	public void testEqualFile14() throws Exception {
		String contentFile1 = getContentFromFile(fileName1Path+fileNames1[13]);
		String contentFile2 = getContentFromFile(fileName2Path+fileNames2[13]);
		assertEquals(contentFile1, contentFile2);
	}
	
	public void testEqualFile15() throws Exception {
		String contentFile1 = getContentFromFile(fileName1Path+fileNames1[14]);
		String contentFile2 = getContentFromFile(fileName2Path+fileNames2[14]);
		assertEquals(contentFile1, contentFile2);
	}
	
	public void testEqualFile16() throws Exception {
		String contentFile1 = getContentFromFile(fileName1Path+fileNames1[15]);
		String contentFile2 = getContentFromFile(fileName2Path+fileNames2[15]);
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
	    return new TestSuite(CorpusGeneratorMethodLevelGranularityTest.class);
	}
}
