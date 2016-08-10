package edu.wm.cs.semeru.corpus.corpusConverterToVSMFormat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

public class CorpusConverterToVSMFormat
{
	public static final String EXTENSION_VSM_FORMAT=".CoOccurenceMatrixVSMFormat";
	public static final String EMPTY_DOCUMENT_REPLACEMENT="nbsp";
	
	private String inputFileNamePreprocessedCorpus;
	private String inputFileNamePreprocessedCorpusWithoutPath;
	private String outputFolder;

	private HashSet<String> uniqueTermsSet=null;
	private ArrayList<String> documents=null;
	private Hashtable<String,Integer> uniqueTerms=null;
	
	public CorpusConverterToVSMFormat(String inputFileNamePreprocessedCorpus,String outputFolder)
	{
		this.inputFileNamePreprocessedCorpus=inputFileNamePreprocessedCorpus;
		this.inputFileNamePreprocessedCorpusWithoutPath=new File(inputFileNamePreprocessedCorpus).getName();
		
		this.outputFolder=outputFolder;

		System.out.println("inputFileNamePreprocessedCorpus="+inputFileNamePreprocessedCorpus);
		System.out.println("inputFileNamePreprocessedCorpusWithoutPath="+inputFileNamePreprocessedCorpusWithoutPath);
		System.out.println("outputFolder="+outputFolder);
	}

	public void convertCorpusToVSMFormat() throws Exception
	{
		BufferedReader brPreprocessedCorpus=new BufferedReader(new FileReader(inputFileNamePreprocessedCorpus));
		String document;
		uniqueTermsSet=new HashSet<String>();
		documents=new ArrayList<String>();
		int indexDocument=0;
		while ((document=brPreprocessedCorpus.readLine())!=null)
		{
			indexDocument++;
			if (document.length()==0)
			{
				System.err.println("Buf lenght=0 at line "+indexDocument);
				document=EMPTY_DOCUMENT_REPLACEMENT;
			}

			addUniqueTerms(document);
			documents.add(document);
		}
		brPreprocessedCorpus.close();

		System.out.println("Number of documents: "+documents.size());
		System.out.println("Size of unique terms set: "+uniqueTermsSet.size());

		generateCoOccurenceMatrix(outputFolder+inputFileNamePreprocessedCorpusWithoutPath+EXTENSION_VSM_FORMAT);
	}
	
	public void addUniqueTerms(String document)
	{
		String terms[]=document.split(" ");
		for (String term : terms)
			uniqueTermsSet.add(term);
	}
	
	public void generateCoOccurenceMatrix(String fileNameCoOccurenceMatrix) throws Exception
	{
		uniqueTerms=new Hashtable<String,Integer>();
//		System.out.print("Loading unique terms ");
		int lineNumber=0;
		for (Iterator<String> iteratorTerms=uniqueTermsSet.iterator();iteratorTerms.hasNext();)
		{
			String term=iteratorTerms.next();
			uniqueTerms.put(term,new Integer(lineNumber));
//			System.out.println(term+"\t"+lineNumber);
			lineNumber++;
		}
//		System.out.println("done!");

		int frequencies[]=new int[uniqueTerms.size()];
		BufferedWriter bwCoOccurenceMatrix=new BufferedWriter(new FileWriter(fileNameCoOccurenceMatrix));

		for (String document : documents)
		{
			for (int i=0;i<frequencies.length;i++)
				frequencies[i]=0;
			
			String terms[]=document.split(" ");
			for (String term : terms)
				frequencies[uniqueTerms.get(term)]++;
			
			for (int i=0;i<frequencies.length;i++)
			{
				bwCoOccurenceMatrix.write(frequencies[i]+" ");
			}
			bwCoOccurenceMatrix.write("\r\n");
		}
		bwCoOccurenceMatrix.close();
	}
}
