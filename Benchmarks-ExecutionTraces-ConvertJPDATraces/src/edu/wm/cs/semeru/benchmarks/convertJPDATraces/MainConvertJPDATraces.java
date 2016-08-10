package edu.wm.cs.semeru.benchmarks.convertJPDATraces;

public class MainConvertJPDATraces
{
	static void testMainConvertJPDAToUniqueMethods() throws Exception
	{
		String inputFileNamesListOfIssues="TestCases/Input/jEdit4.3/listOfIssues.txt";
		String inputFolderTraces="TestCases/Input/jEdit4.3/Traces/";
		String outputFolder="TestCases/Output/jEdit4.3/";

		InputOutputConvertJPDATraces inputOutput=new InputOutputConvertJPDATraces(inputFileNamesListOfIssues,inputFolderTraces,outputFolder);

		ConvertJPDATraces convertJPDATraces=new ConvertJPDATraces(inputOutput);
		
		convertJPDATraces.convertJPDATracesToUniqueMethods();
		
		System.out.println("\nThe output was save in the folder "+outputFolder);
	}
	
	static void testMainConvertJPDAToBiGramsMethods() throws Exception
	{
		String inputFileNameListOfIssues="TestCases/Input/jEdit4.3/listOfIssues.txt";
		String inputFolderTraces="TestCases/Input/jEdit4.3/Traces/";
		String outputFolder="TestCases/Output/jEdit4.3/";
		String inputFileNameCorpusMapping="TestCases/Input/jEdit4.3/Corpus-jEdit4.3.corpusMappingMethodLevelGranularity";

		InputOutputConvertJPDATraces inputOutput=new InputOutputConvertJPDATraces(inputFileNameListOfIssues,inputFolderTraces,outputFolder,inputFileNameCorpusMapping);

		ConvertJPDATraces convertJPDATraces=new ConvertJPDATraces(inputOutput);
		
		convertJPDATraces.convertJPDATracesToBiGramsMethods();
		
		System.out.println("\nThe output was save in the folder "+outputFolder);
	}

	public static void main(String[] args) throws Exception
	{
//		testMainConvertJPDAToUniqueMethods();
//		testMainConvertJPDAToBiGramsMethods();
//		if (1==1)
//			return;
		
//		args=new String[4];
//		args[0]="-toUniqueMethods";
//		args[1]="TestCases/Input/jEdit4.3/listOfIssues.txt";
//		args[2]="TestCases/Input/jEdit4.3/Traces/";
//		args[3]="TestCases/Output/jEdit4.3/";

		args=new String[5];
		args[0]="-toBiGramsMethods";
		args[1]="TestCases/Input/jEdit4.3/listOfIssues.txt";
		args[2]="TestCases/Input/jEdit4.3/Traces/";
		args[3]="TestCases/Output/jEdit4.3/";
		args[4]="TestCases/Input/jEdit4.3/Corpus-jEdit4.3.corpusMappingMethodLevelGranularity";
		
		if ((args.length!=4)&&(args.length!=5))
		{
			System.err.println("Converts JPDA execution traces to unique methods or bigrams");
			System.err.println("Usage:");
			System.err.println("  java -jar ConvertJPDATraces.jar -toUniqueMethods inputFileNamesListOfIssues inputFolderTraces outputFolder");
			System.err.println("  java -jar ConvertJPDATraces.jar -toBiGramsMethods inputFileNamesListOfIssues inputFolderTraces outputFolder inputFileNameCorpusMapping");
			System.err.println();
			System.err.println("Where:");
			System.err.println("  inputFileNamesListOfIssues");
			System.err.println("    is a file name containing n lines. Each line is an [issueID] from the bug tracking system (e.g., 123)");
			System.err.println("  inputFolderTraces");
			System.err.println("    is a folder containg a JPDA execution trace for each [issueID] (e.g., trace[issueID].log)");
			System.err.println("  outputFolder");
			System.err.println("    is the folder name where the unique methods or the bigrams will be saved");
			System.err.println("  inputFileNameCorpusMapping");
			System.err.println("    this parameter is required only for the option -toBiGramsMethods. It represents the corpus mapping at method level granularity produced by CorpusGenerator");
			System.err.println();
			System.err.println("Using the -toUniqueMethods option, the tool will produce the following output:");
			System.err.println("  outputFolder/");
			System.err.println("    will be created if it does not exist");
			System.err.println("  outputFolder/"+InputOutputConvertJPDATraces.FOLDER_NAME_UNIQUE_METHODS_TRACES);
			System.err.println("    will be created if it does not exist");
			System.err.println("  outputFolder/"+InputOutputConvertJPDATraces.FOLDER_NAME_UNIQUE_METHODS_TRACES+"[issueID]"+InputOutputConvertJPDATraces.EXTENSION_FILE_NAME_UNIQUE_METHODS_TRACE);
			System.err.println("    these files will contain the unique methods (one per line) extracted from the execution trace corresponding to [issueID] (e.g., packageName.ClassName.methodName - with no signature)");
			System.err.println();
			System.err.println("Using the -toBiGramsMethods option, the tool will produce the following output:");
			System.err.println("  outputFolder/");
			System.err.println("    will be created if it does not exist");
			System.err.println("  outputFolder/"+InputOutputConvertJPDATraces.FOLDER_NAME_BIGRAMS_METHODS_TRACES);
			System.err.println("    will be created if it does not exist");
			System.err.println("  outputFolder/"+InputOutputConvertJPDATraces.FOLDER_NAME_BIGRAMS_METHODS_TRACES+"[issueID]"+InputOutputConvertJPDATraces.EXTENSION_FILE_NAME_BIGRAMS_METHODS_TRACE);
			System.err.println("    these files will contain the method bigrams (one per line) extracted from the execution trace corresponding to [issueID]. The format of a bigram is callerMethod[tab]calleeMethod[tab]callerMethodPosition[tab]calleeMethodPosition. The caller/calleeMethods are methods (i.e., lines) from the file inputFileNameCorpusMapping. The caller/calleeMethodsPosition are positions of the caller/calleeMethods in the file inputFileNameCorpusMapping");
			System.err.println();
			System.err.println("Example:");
			System.err.println("  java -jar ConvertJPDATraces.jar -toUniqueMethods TestCases/Input/jEdit4.3/listOfIssues.txt TestCases/Input/jEdit4.3/Traces/ TestCases/Output/jEdit4.3/");
			System.err.println("  java -jar ConvertJPDATraces.jar -toBiGramsMethods TestCases/Input/jEdit4.3/listOfIssues.txt TestCases/Input/jEdit4.3/Traces/ TestCases/Output/jEdit4.3/ TestCases/Input/jEdit4.3/Corpus-jEdit4.3.corpusMappingMethodLevelGranularity");
			System.exit(1);
		}
		
		if (args[0].equals("-toUniqueMethods"))
		{
			InputOutputConvertJPDATraces inputOutput=new InputOutputConvertJPDATraces(args[1],args[2],args[3]);
			ConvertJPDATraces convertJPDATraces=new ConvertJPDATraces(inputOutput);
			convertJPDATraces.convertJPDATracesToUniqueMethods();
			
			System.out.println("\nThe output was save in the folder "+args[3]);
			System.exit(0);
		}
		
		if (args[0].equals("-toBiGramsMethods"))
		{
			InputOutputConvertJPDATraces inputOutput=new InputOutputConvertJPDATraces(args[1],args[2],args[3],args[4]);
			ConvertJPDATraces convertJPDATraces=new ConvertJPDATraces(inputOutput);
			convertJPDATraces.convertJPDATracesToBiGramsMethods();
			
			System.out.println("\nThe output was save in the folder "+args[3]);
			System.exit(0);
		}
	}
}
