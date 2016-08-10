package edu.wm.cs.semeru.benchmarks.convertJPDATraces;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Hashtable;

public class InputOutputConvertJPDATraces
{
	public static final String FOLDER_NAME_UNIQUE_METHODS_TRACES="UniqueMethodsTraces/";
	public static final String FOLDER_NAME_UNIQUE_METHODS_TRACES_DEBUG="UniqueMethodsTraces/Debug/";
	public static final String FOLDER_NAME_BIGRAMS_METHODS_TRACES="BiGramsMethodsTraces/";
	public static final String EXTENSION_FILE_NAME_UNIQUE_METHODS_TRACE=".uniqueMethodsTrace";
	public static final String EXTENSION_FILE_NAME_UNIQUE_METHODS_TRACE_DEBUG=".uniqueMethodsTraceDebug";
	public static final String EXTENSION_FILE_NAME_BIGRAMS_METHODS_TRACE=".BiGramsMethodsTrace";

	private String fileNameListOfIssues;
	private String folderInputTraces;
	private String outputFolder;
	private String fileNameCorpusMapping;
	private boolean computeUniqueMethods;		//false for computing bigrams, true for computing unique methods

	private Hashtable<String,Integer> corpusMethodsMappingToPositionInCorpus;
	
	public InputOutputConvertJPDATraces(String fileNameListOfIssues,String folderInputTraces,String outputFolder)
	{
		this.fileNameListOfIssues=fileNameListOfIssues;
		this.folderInputTraces=folderInputTraces;
		this.outputFolder=outputFolder;
		
		this.computeUniqueMethods=true;
	}

	public InputOutputConvertJPDATraces(String fileNameListOfIssues,String folderInputTraces,String outputFolder,String fileNameCorpusMapping)
	{
		this(fileNameListOfIssues,folderInputTraces,outputFolder);
		this.fileNameCorpusMapping=fileNameCorpusMapping;
		
		this.computeUniqueMethods=false;
	}

	public ArrayList<String> loadListOfIssues() throws Exception
	{
		BufferedReader brIssues=new BufferedReader(new FileReader(fileNameListOfIssues));
		String buf;
		
		ArrayList<String> issues=new ArrayList<String>();
		
		while ((buf=brIssues.readLine())!=null)
		{
			issues.add(buf);
		}
		brIssues.close();
		
		return issues;
	}

	public void initializeFolderStructure() throws Exception
	{
		createFolder(outputFolder);
		if (computeUniqueMethods)
		{
			createFolder(outputFolder+FOLDER_NAME_UNIQUE_METHODS_TRACES);
			createFolder(outputFolder+FOLDER_NAME_UNIQUE_METHODS_TRACES_DEBUG);
		}
		else
			createFolder(outputFolder+FOLDER_NAME_BIGRAMS_METHODS_TRACES);
	}
	
	private void createFolder(String folderName) throws Exception
	{
		File folder=new File(folderName);
		if (folder.exists())
			return;
		
		if (folder.mkdir()==false)
			throw new Exception();
	}
	
	public String getFileNameTrace(String issueID)
	{
		return folderInputTraces+"trace"+issueID+".log";
	}

	public String getFileNameUniqueMethodsTrace(String issueID)
	{
		return outputFolder+FOLDER_NAME_UNIQUE_METHODS_TRACES+issueID+EXTENSION_FILE_NAME_UNIQUE_METHODS_TRACE;
	}

	public String getFileNameUniqueMethodsTraceDebug(String issueID)
	{
		return outputFolder+FOLDER_NAME_UNIQUE_METHODS_TRACES_DEBUG+issueID+EXTENSION_FILE_NAME_UNIQUE_METHODS_TRACE_DEBUG;
	}
	
	public String getFileNameBiGramsMethodsTrace(String issueID)
	{
		return outputFolder+FOLDER_NAME_BIGRAMS_METHODS_TRACES+issueID+EXTENSION_FILE_NAME_BIGRAMS_METHODS_TRACE;
	}

	public void loadCorpusMethodsMappings() throws Exception
	{
		String method;
		BufferedReader brCorpusMethodsMapping=new BufferedReader(new FileReader(fileNameCorpusMapping));
		
		corpusMethodsMappingToPositionInCorpus=new Hashtable<String,Integer>();
		int positionInCorpus=0;
		
		while ((method=brCorpusMethodsMapping.readLine())!=null)
		{
			//ignore parameters
			int indexParenthesis=method.indexOf("(");
			if (indexParenthesis>=0)
				method=method.substring(0,indexParenthesis);
			
			positionInCorpus++;

			//temporary solution to deal with overwritten methods: only the first method from the corpus will be considered 
			if (corpusMethodsMappingToPositionInCorpus.get(method)==null)
				corpusMethodsMappingToPositionInCorpus.put(method,positionInCorpus);
		}
		brCorpusMethodsMapping.close();
	}
	
	
	public String getPositionOfMethodMappingInCorpus(String method)
	{
		Integer documentID=corpusMethodsMappingToPositionInCorpus.get(method);
		if (documentID==null)
			return "-1";
		return ""+documentID;
	}
}
