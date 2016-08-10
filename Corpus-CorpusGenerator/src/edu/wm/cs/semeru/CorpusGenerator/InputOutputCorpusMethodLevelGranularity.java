package edu.wm.cs.semeru.CorpusGenerator;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class InputOutputCorpusMethodLevelGranularity extends InputOutput
{
	public static final String EXTENSION_CORPUS_RAW=".corpusRawMethodLevelGranularity"; 
	public static final String EXTENSION_CORPUS_MAPPING=".corpusMappingMethodLevelGranularity"; 
	public static final String EXTENSION_CORPUS_MAPPING_WITH_PACKAGE_SEPARATOR=".corpusMappingWithPackageSeparatorMethodLevelGranularity"; 
	public static final String EXTENSION_CORPUS_DEBUG=".corpusRawAndMappingDebugMethodLevelGranularity";
	
	private String outputFileNameCorpusRaw;
	private String outputFileNameCorpusMapping;
	private String outputFileNameCorpusMappingWithPackageSeparator;
	private String outputFileNameCorpusRawAndMappingDebug;
	
	private BufferedWriter outputFileCorpusRaw;
	private BufferedWriter outputFileCorpusMapping;
	private BufferedWriter outputFileCorpusMappingWithPackageSeparator;
	private BufferedWriter outputFileCorpusRawAndMappingDebug;
	
	public String getOutputFileNameCorpusRaw()
	{
		return outputFileNameCorpusRaw;
	}

	public String getOutputFileNameCorpusMapping()
	{
		return outputFileNameCorpusMapping;
	}

	public String getOutputFileNameCorpusMappingWithPackageSeparator()
	{
		return outputFileNameCorpusMappingWithPackageSeparator;
	}

	public String getOutputFileNameCorpusRawAndMappingDebug()
	{
		return outputFileNameCorpusRawAndMappingDebug;
	}

	
	public InputOutputCorpusMethodLevelGranularity(String inputFileNameWithListOfInputFileNames,String outputFolderName,String outputFileNameWithoutExtension)
	{
		super(inputFileNameWithListOfInputFileNames,outputFolderName,outputFileNameWithoutExtension);
		this.outputFileNameCorpusRaw=outputFolderName+outputFileNameWithoutExtension+EXTENSION_CORPUS_RAW;
		this.outputFileNameCorpusMapping=outputFolderName+outputFileNameWithoutExtension+EXTENSION_CORPUS_MAPPING;
		this.outputFileNameCorpusMappingWithPackageSeparator=outputFolderName+outputFileNameWithoutExtension+EXTENSION_CORPUS_MAPPING_WITH_PACKAGE_SEPARATOR;
		this.outputFileNameCorpusRawAndMappingDebug=outputFolderName+outputFileNameWithoutExtension+EXTENSION_CORPUS_DEBUG;
	}

	public void initializeOutputStream() throws Exception
	{
		outputFileCorpusRaw=new BufferedWriter(new FileWriter(outputFileNameCorpusRaw));
		outputFileCorpusMapping=new BufferedWriter(new FileWriter(outputFileNameCorpusMapping));
		outputFileCorpusMappingWithPackageSeparator=new BufferedWriter(new FileWriter(outputFileNameCorpusMappingWithPackageSeparator));
		outputFileCorpusRawAndMappingDebug=new BufferedWriter(new FileWriter(outputFileNameCorpusRawAndMappingDebug));
	}

	public void appendToCorpusMapping(String idMethod)
	{
		appendToFile(outputFileCorpusMapping,idMethod);
	}
	
	public void appendToCorpusMappingWithPackageSeparator(String idMethod)
	{
		appendToFile(outputFileCorpusMappingWithPackageSeparator,idMethod);
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
		outputFileCorpusMappingWithPackageSeparator.close();
		outputFileCorpusRawAndMappingDebug.close();
	}
	
	public void printMessageWhereOutputFilesWereSaved()
	{
		System.out.println("CorpusMethodLevelGranularity: Corpus was saved to file: "+outputFileNameCorpusRaw);
		System.out.println("CorpusMethodLevelGranularity: Mapping was saved to file: "+outputFileNameCorpusMapping);
		System.out.println("CorpusMethodLevelGranularity: Mapping with package separator was saved to file: "+outputFileNameCorpusMappingWithPackageSeparator);
		System.out.println("CorpusMethodLevelGranularity: Corpus with debug information was saved to file: "+outputFileNameCorpusRawAndMappingDebug);
	}
}
