package edu.wm.cs.semeru.corpus.corpusAndQueriesConverterToVSMFormat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

public class CorpusAndQueriesConverterToVSMFormat
{
	public static final String EXTENSION_VSM_FORMAT=".CoOccurenceMatrixVSMFormat";
	public static final String EMPTY_DOCUMENT_REPLACEMENT="nbsp";
	public static final String PREFIX_FILE_NAME_CORPUS="Corpus-";
	public static final String PREFIX_FILE_NAME_CORPUS_AND_QUERIES="CorpusAndQueries-";
	
	private String inputFileNamePreprocessedCorpus;
	private String inputFileNamePreprocessedCorpusWithoutPath;
	private String inputFileNamePreprocessedCorpusQueries;
	private String inputFileNamePreprocessedCorpusQueriesWithoutPath;
	private String inputFileNamePreprocessedCorpusAndQueriesWithoutPath;
	private String outputFolder;

	private HashSet<String> uniqueTermsSet=null;
	private ArrayList<String> documents=null;
	private static ArrayList<String> queries=null;
	private Hashtable<String,Integer> uniqueTerms=null;
	
	public CorpusAndQueriesConverterToVSMFormat(String inputFileNamePreprocessedCorpus,String inputFileNamePreprocessedCorpusQueries,String outputFolder) throws Exception
	{
		this.inputFileNamePreprocessedCorpus=inputFileNamePreprocessedCorpus;
		this.inputFileNamePreprocessedCorpusWithoutPath=new File(inputFileNamePreprocessedCorpus).getName();
		this.inputFileNamePreprocessedCorpusQueries=inputFileNamePreprocessedCorpusQueries;
		this.inputFileNamePreprocessedCorpusQueriesWithoutPath=new File(inputFileNamePreprocessedCorpusQueries).getName();
		if (inputFileNamePreprocessedCorpusWithoutPath.startsWith(PREFIX_FILE_NAME_CORPUS)==false)
			throw new Exception("InputFileNamePreprocessedCorpus must start with "+PREFIX_FILE_NAME_CORPUS);
		this.inputFileNamePreprocessedCorpusAndQueriesWithoutPath=PREFIX_FILE_NAME_CORPUS_AND_QUERIES+inputFileNamePreprocessedCorpusWithoutPath.substring(PREFIX_FILE_NAME_CORPUS.length());
		
		this.outputFolder=outputFolder;

		System.out.println("inputFileNamePreprocessedCorpus="+inputFileNamePreprocessedCorpus);
		System.out.println("inputFileNamePreprocessedCorpusWithoutPath="+inputFileNamePreprocessedCorpusWithoutPath);
		System.out.println("inputFileNamePreprocessedCorpusQueries="+inputFileNamePreprocessedCorpusQueries);
		System.out.println("inputFileNamePreprocessedCorpusQueriesWithoutPath="+inputFileNamePreprocessedCorpusQueriesWithoutPath);
		System.out.println("inputFileNamePreprocessedCorpusAndQueriesWithoutPath="+inputFileNamePreprocessedCorpusAndQueriesWithoutPath);
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
		
		BufferedReader brPreprocessedCorpusQueries=new BufferedReader(new FileReader(inputFileNamePreprocessedCorpusQueries));
		queries=new ArrayList<String>();
		String query;
		while ((query=brPreprocessedCorpusQueries.readLine())!=null)
		{
			queries.add(query);
		}
		brPreprocessedCorpusQueries.close();

		System.out.println("Number of documents: "+documents.size());
		System.out.println("Number of queries: "+queries.size());
		System.out.println("Size of unique terms set: "+uniqueTermsSet.size());

		generateCoOccurenceMatrix();
		generateCoOccurenceMatrixQueries();
	}
	
	public void addUniqueTerms(String document)
	{
		String terms[]=document.split(" ");
		for (String term : terms)
			uniqueTermsSet.add(term);
	}
	
	public void generateCoOccurenceMatrix() throws Exception
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
		BufferedWriter bwCoOccurenceMatrixCorpus=new BufferedWriter(new FileWriter(outputFolder+inputFileNamePreprocessedCorpusWithoutPath+EXTENSION_VSM_FORMAT));
		BufferedWriter bwCoOccurenceMatrixCorpusAndQueries=new BufferedWriter(new FileWriter(outputFolder+inputFileNamePreprocessedCorpusAndQueriesWithoutPath+EXTENSION_VSM_FORMAT));

		for (String document : documents)
		{
			for (int i=0;i<frequencies.length;i++)
				frequencies[i]=0;
			
			String terms[]=document.split(" ");
			for (String term : terms)
				frequencies[uniqueTerms.get(term)]++;
			
			for (int i=0;i<frequencies.length;i++)
			{
				bwCoOccurenceMatrixCorpus.write(frequencies[i]+" ");
				bwCoOccurenceMatrixCorpusAndQueries.write(frequencies[i]+" ");
			}
			bwCoOccurenceMatrixCorpus.write("\r\n");
			bwCoOccurenceMatrixCorpusAndQueries.write("\r\n");
		}
		bwCoOccurenceMatrixCorpus.close();
		bwCoOccurenceMatrixCorpusAndQueries.close();
	}
	
	public void generateCoOccurenceMatrixQueries() throws Exception
	{
		int frequencies[]=new int[uniqueTerms.size()];
		BufferedWriter bwCoOccurenceMatrixCorpusQueries=new BufferedWriter(new FileWriter(outputFolder+inputFileNamePreprocessedCorpusQueriesWithoutPath+EXTENSION_VSM_FORMAT));
		BufferedWriter bwCoOccurenceMatrixCorpusAndQueries=new BufferedWriter(new FileWriter(outputFolder+inputFileNamePreprocessedCorpusAndQueriesWithoutPath+EXTENSION_VSM_FORMAT,true));

		for (String query : queries)
		{
			for (int i=0;i<frequencies.length;i++)
				frequencies[i]=0;
			
			String terms[]=query.split(" ");
			int numberOfWordsFromCurrentQueryThatMatchWordsInCorpus=0;
			for (String term : terms)
			{
				if (uniqueTerms.get(term)==null)		//if the word in the query does not match any of the words in the corpus of documents, ignore it
					continue;
				frequencies[uniqueTerms.get(term)]++;
				numberOfWordsFromCurrentQueryThatMatchWordsInCorpus++;
			}
			
			if (numberOfWordsFromCurrentQueryThatMatchWordsInCorpus==0)
				throw new Exception("numberOfWordsFromCurrentQueryThatMatchWordsInCorpus=0");
			
			for (int i=0;i<frequencies.length;i++)
			{
				bwCoOccurenceMatrixCorpusQueries.write(frequencies[i]+" ");
				bwCoOccurenceMatrixCorpusAndQueries.write(frequencies[i]+" ");
			}
			bwCoOccurenceMatrixCorpusQueries.write("\r\n");
			bwCoOccurenceMatrixCorpusAndQueries.write("\r\n");
		}
		bwCoOccurenceMatrixCorpusQueries.close();
		bwCoOccurenceMatrixCorpusAndQueries.close();
	}
}
