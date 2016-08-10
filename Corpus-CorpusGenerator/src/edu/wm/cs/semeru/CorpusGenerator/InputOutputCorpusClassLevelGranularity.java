package edu.wm.cs.semeru.CorpusGenerator;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class InputOutputCorpusClassLevelGranularity extends InputOutput
{
	public static final String EXTENSION_CORPUS_RAW=".corpusRawClassLevelGranularity"; 
	public static final String EXTENSION_CORPUS_MAPPING=".corpusMappingClassLevelGranularity"; 
	public static final String EXTENSION_CORPUS_DEBUG=".corpusRawAndMappingDebugClassLevelGranularity";
	
	private String outputFileNameCorpusRaw;
	private String outputFileNameCorpusMapping;
	private String outputFileNameCorpusRawAndMappingDebug;
	
	private BufferedWriter outputFileCorpusRaw;
	private BufferedWriter outputFileCorpusMapping;
	private BufferedWriter outputFileCorpusRawAndMappingDebug;
	
	public String getOutputFileNameCorpusRaw()
	{
		return outputFileNameCorpusRaw;
	}

	public String getOutputFileNameCorpusMapping()
	{
		return outputFileNameCorpusMapping;
	}

	public String getOutputFileNameCorpusRawAndMappingDebug()
	{
		return outputFileNameCorpusRawAndMappingDebug;
	}
	
	public InputOutputCorpusClassLevelGranularity(String inputFileNameWithListOfInputFileNames,String outputFolderName,String outputFileNameWithoutExtension)
	{
		super(inputFileNameWithListOfInputFileNames,outputFolderName,outputFileNameWithoutExtension);
		this.outputFileNameCorpusRaw=outputFolderName+outputFileNameWithoutExtension+EXTENSION_CORPUS_RAW;
		this.outputFileNameCorpusMapping=outputFolderName+outputFileNameWithoutExtension+EXTENSION_CORPUS_MAPPING;
		this.outputFileNameCorpusRawAndMappingDebug=outputFolderName+outputFileNameWithoutExtension+EXTENSION_CORPUS_DEBUG;
	}

	public void initializeOutputStream() throws Exception
	{
		outputFileCorpusRaw=new BufferedWriter(new FileWriter(outputFileNameCorpusRaw));
		outputFileCorpusMapping=new BufferedWriter(new FileWriter(outputFileNameCorpusMapping));
		outputFileCorpusRawAndMappingDebug=new BufferedWriter(new FileWriter(outputFileNameCorpusRawAndMappingDebug));
	}
	public void appendToCorpusMapping(String idMethod)
	{
		appendToFile(outputFileCorpusMapping,idMethod);
	}
	
	public void appendToCorpusRaw(String methodContent)
	{
		appendToFile(outputFileCorpusRaw,methodContent);
	}
	
	public void appendToCorpusDebug(String buf)
	{
		appendToFile(outputFileCorpusRawAndMappingDebug,buf);
	}
	
	public void closeOutputStreams() throws Exception
	{
		outputFileCorpusRaw.close();
		outputFileCorpusMapping.close();
		outputFileCorpusRawAndMappingDebug.close();
	}
	
	public void printMessageWhereOutputFilesWereSaved()
	{
		System.out.println("CorpusClassLevelGranularity: Corpus was saved to file: "+outputFileNameCorpusRaw);
		System.out.println("CorpusClassLevelGranularity: Mapping was saved to file: "+outputFileNameCorpusMapping);
		System.out.println("CorpusClassLevelGranularity: Corpus with debug information was saved to file: "+outputFileNameCorpusRawAndMappingDebug);
	}
}
