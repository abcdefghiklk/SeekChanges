package edu.wm.cs.semeru.corpus.corpusConverterToVSMFormat;

public class MainCorpusConverterToVSMFormat
{
	static void testSystem2() throws Exception
	{
		String inputFileNamePreprocessedCorpus="TestCases/Input/Corpus-System2-AfterSplitStopStem.corpusRawMethodLevelGranularity";
		String outputFolder="TestCases/Output/";
		
		CorpusConverterToVSMFormat corpusConverter=new CorpusConverterToVSMFormat(inputFileNamePreprocessedCorpus,outputFolder);
		corpusConverter.convertCorpusToVSMFormat();
	}

	public static void main(String[] args) throws Exception
	{
		testSystem2();
		if (1==1)
			return;
		
//		args=new String[2];
//		args[0]="TestCases/Input/Corpus-jEdit4.3-AfterSplitStopStem.corpusRawMethodLevelGranularity";
//		args[1]="TestCases/Output/";
		
		if (args.length!=2)
		{
			System.err.println("Converts a preprocessed corpus that was generated using CorpusPreprocessor to a co-occurence matrix (i.e., VSM format) with numberOfRows documents and numberOfColumns unique terms");
			System.err.println("Usage:");
			System.err.println("  java -jar CorpusConverterToVSMFormat.jar inputFileNamePreprocessedCorpus outputFolder");
			System.err.println();
			System.err.println("Where:");
			System.err.println("  inputFileNamePreprocessedCorpus");
			System.err.println("    is a file name containing the preprocessed corpus. This file should be the one with the suffix and extension -AfterSplitStopStem.corpusRaw[File/Method/Class]LevelGranularity produced by CorpusPreprocessor");
			System.err.println("  outputFolder");
			System.err.println("    is the folder name where the converted corpus will be saved");
			System.err.println();
			System.err.println("The output produced by this tool will contain the following file (assuming the input file name is [fileName].[extension]):");
			System.err.println("  [fileName].[extension]"+CorpusConverterToVSMFormat.EXTENSION_VSM_FORMAT);
			System.err.println("    contains a matrix with numberOfDocuments rows and numberOfUniqueTerms columns. The elements of the matrix represent the frequency of the terms from the column in the document from the row. The elements are separated by space");
			System.err.println();
			System.err.println("Example:");
			System.err.println("  java -jar CorpusPreprocessor.jar TestCases/Input/Corpus-jEdit4.3-AfterSplitStopStem.corpusRawMethodLevelGranularity TestCases/Output/");
			System.exit(1);
		}
		
		CorpusConverterToVSMFormat corpusConverter=new CorpusConverterToVSMFormat(args[0],args[1]);
		corpusConverter.convertCorpusToVSMFormat();
	}
}
