package edu.wm.cs.semeru.CorpusGenerator;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class InputOutputCorpusFileLevelGranularity extends InputOutput
{
	public static final String EXTENSION_CORPUS_RAW=".corpusRawFileLevelGranularity"; 
	public static final String EXTENSION_CORPUS_MAPPING=".corpusMappingFileLevelGranularity"; 
	public static final String EXTENSION_CORPUS_DEBUG=".corpusRawAndMappingDebugFileLevelGranularity";
	
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
	
	public InputOutputCorpusFileLevelGranularity(String inputFileNameWithListOfInputFileNames,String outputFolderName,String outputFileNameWithoutExtension)
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
		System.out.println("CorpusFileLevelGranularity: Corpus was saved to file: "+outputFileNameCorpusRaw);
		System.out.println("CorpusFileLevelGranularity: Mapping was saved to file: "+outputFileNameCorpusMapping);
		System.out.println("CorpusFileLevelGranularity: Corpus with debug information was saved to file: "+outputFileNameCorpusRawAndMappingDebug);
	}
}
