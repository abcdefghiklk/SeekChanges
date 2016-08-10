package edu.wm.cs.semeru.corpus.corpusAndQueriesConverterToVSMFormat;

public class MainCorpusAndQueriesConverterToVSMFormat
{
	static void testSystem2() throws Exception
	{
		String inputFileNamePreprocessedCorpus="TestCases/Input/Corpus-System2-AfterSplitStopStem.corpusRawMethodLevelGranularity";
		String inputFileNamePreprocessedCorpusQueries="TestCases/Input/CorpusQueries-System2-AfterSplitStopStem.corpusRawMethodLevelGranularity";
		String outputFolder="TestCases/Output/";
		
		CorpusAndQueriesConverterToVSMFormat corpusConverter=new CorpusAndQueriesConverterToVSMFormat(inputFileNamePreprocessedCorpus,inputFileNamePreprocessedCorpusQueries,outputFolder);
		corpusConverter.convertCorpusToVSMFormat();
	}

	public static void main(String[] args) throws Exception
	{
		testSystem2();
		if (1==1)
			return;
		
//		args=new String[3];
//		args[0]="TestCases/Input/Corpus-System2-AfterSplitStopStem.corpusRawMethodLevelGranularity";
//		args[1]="TestCases/Input/CorpusQueries-System2-AfterSplitStopStem.corpusRawMethodLevelGranularity";
//		args[2]="TestCases/Output/";
		
		if (args.length!=3)
		{
			System.err.println("Converts two preprocessed corpora (of source code documents and queries) that were generated using CorpusPreprocessor to a co-occurence matrix (i.e., VSM format). The co-occurence matrix of the queries will contain only the terms that appear in the source code. In other words, if a term from the query does not appear in the source code, it will be discarded.");
			System.err.println("Usage:");
			System.err.println("  java -jar CorpusAndQueriesConverterToVSMFormat.jar inputFileNamePreprocessedCorpus inputFileNamePreprocessedCorpusQueries outputFolder");
			System.err.println();
			System.err.println("Where:");
			System.err.println("  inputFileNamePreprocessedCorpus");
			System.err.println("    is a file name containing the preprocessed corpus of the source code. This file should be the one with the suffix and extension -AfterSplitStopStem.corpusRaw[File/Method/Class]LevelGranularity produced by CorpusPreprocessor");
			System.err.println("  inputFileNamePreprocessedCorpusQueries");
			System.err.println("    is a file name containing the preprocessed corpus of the queries (e.g., issues). This file should be the one with the suffix and extension -AfterSplitStopStem.corpusRaw[File/Method/Class]LevelGranularity produced by CorpusPreprocessor");
			System.err.println("  outputFolder");
			System.err.println("    is the folder name where the converted corpus will be saved");
			System.err.println();
			System.err.println("The output produced by this tool will contain the following files (assuming the input file names are Corpus-[SystemName]-AfterSplitStopStem.[extension] and CorpusQueries-[SystemName]-AfterSplitStopStem.[extension]):");
			System.err.println("  Corpus-[SystemName]-AfterSplitStopStem.[extension]"+CorpusAndQueriesConverterToVSMFormat.EXTENSION_VSM_FORMAT);
			System.err.println("    contains a matrix with [numberOfDocuments] rows and [numberOfUniqueTermsFromTheSourceCode] columns. The elements of the matrix represent the frequency of the terms from the column in the document from the row. The elements are separated by space");
			System.err.println("  CorpusQueries-[SystemName]-AfterSplitStopStem.[extension]"+CorpusAndQueriesConverterToVSMFormat.EXTENSION_VSM_FORMAT);
			System.err.println("    contains a matrix with [numberOfQueries] rows and [numberOfUniqueTermsFromTheSourceCode] columns. The matrix elements have the same format as above");
			System.err.println("  CorpusAndQueries-[SystemName]-AfterSplitStopStem.[extension]"+CorpusAndQueriesConverterToVSMFormat.EXTENSION_VSM_FORMAT);
			System.err.println("    contains a matrix with [numberOfDocuments]+[numberOfQueries] rows and [numberOfUniqueTermsFromTheSourceCode] columns. This matrix is the concatenation of the previous two matrices produced as an output");
			System.err.println();
			System.err.println("Example:");
			System.err.println("  java -jar CorpusPreprocessor.jar TestCases/Input/Corpus-jEdit4.3-AfterSplitStopStem.corpusRawMethodLevelGranularity TestCases/Input/CorpusQueries-jEdit4.3-AfterSplitStopStem.corpusRawMethodLevelGranularity TestCases/Output/");
			System.exit(1);
		}
		
		CorpusAndQueriesConverterToVSMFormat corpusConverter=new CorpusAndQueriesConverterToVSMFormat(args[0],args[1],args[2]);
		corpusConverter.convertCorpusToVSMFormat();
	}
}
