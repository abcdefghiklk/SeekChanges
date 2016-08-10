package edu.wm.cs.semeru.benchmarks.convertTPTPTraces;

public class MainConvertTPTPTraces
{
	static void testMainConvertTPTPToUniqueMethods() throws Exception
	{
		String inputFileNamesListOfIssues="TestCases/Input/ArgoUML0.22/listOfIssues.txt";
		String inputFolderTraces="TestCases/Input/ArgoUML0.22/Traces/";
		String outputFolder="TestCases/Output/ArgoUML0.22/";

		InputOutputConvertTPTPTraces inputOutput=new InputOutputConvertTPTPTraces(inputFileNamesListOfIssues,inputFolderTraces,outputFolder);

		ConvertTPTPTraces convertTPTPTraces=new ConvertTPTPTraces(inputOutput);
		
		convertTPTPTraces.convertTPTPTracesToUniqueMethods();
		
		System.out.println("The output was save in the folder "+outputFolder);
	}
	
	static void testMainConvertTPTPToBiGramsMethods() throws Exception
	{
		String inputFileNameListOfIssues="TestCases/Input/ArgoUML0.22/listOfIssues.txt";
		String inputFolderTraces="TestCases/Input/ArgoUML0.22/Traces/";
		String outputFolder="TestCases/Output/ArgoUML0.22/";
		String inputFileNameCorpusMapping="TestCases/Input/ArgoUML0.22/Corpus-ArgoUML0.22.corpusMappingMethodLevelGranularity";

		InputOutputConvertTPTPTraces inputOutput=new InputOutputConvertTPTPTraces(inputFileNameListOfIssues,inputFolderTraces,outputFolder,inputFileNameCorpusMapping);

		ConvertTPTPTraces convertTPTPTraces=new ConvertTPTPTraces(inputOutput);
		
		convertTPTPTraces.convertTPTPTracesToBiGramsMethods();
		
		System.out.println("The output was save in the folder "+outputFolder);
	}

	public static void main(String[] args) throws Exception
	{
		testMainConvertTPTPToUniqueMethods();
		testMainConvertTPTPToBiGramsMethods();
		if (1==1)
			return;
		
//		args=new String[4];
//		args[0]="-toUniqueMethods";
//		args[1]="TestCases/Input/ArgoUML0.22/listOfIssues.txt";
//		args[2]="TestCases/Input/ArgoUML0.22/Traces/";
//		args[3]="TestCases/Output/ArgoUML0.22/";

//		args=new String[5];
//		args[0]="-toBiGramsMethods";
//		args[1]="TestCases/Input/ArgoUML0.22/listOfIssues.txt";
//		args[2]="TestCases/Input/ArgoUML0.22/Traces/";
//		args[3]="TestCases/Output/ArgoUML0.22/";
//		args[4]="TestCases/Input/ArgoUML0.22/Corpus-ArgoUML0.22.corpusMappingMethodLevelGranularity";
		
		if ((args.length!=4)&&(args.length!=5))
		{
			System.err.println("Converts TPTP execution traces to unique methods or bigrams");
			System.err.println("Usage:");
			System.err.println("  java -jar ConvertTPTPTraces.jar -toUniqueMethods inputFileNamesListOfIssues inputFolderTraces outputFolder");
			System.err.println("  java -jar ConvertTPTPTraces.jar -toBiGramsMethods inputFileNamesListOfIssues inputFolderTraces outputFolder inputFileNameCorpusMapping");
			System.err.println();
			System.err.println("Where:");
			System.err.println("  inputFileNamesListOfIssues");
			System.err.println("    is a file name containing n lines. Each line is an [issueID] from the bug tracking system (e.g., 123)");
			System.err.println("  inputFolderTraces");
			System.err.println("    is a folder containg a TPTP execution trace for each [issueID] (e.g., trace[issueID].trcxml)");
			System.err.println("  outputFolder");
			System.err.println("    is the folder name where the unique methods or the bigrams will be saved");
			System.err.println("  inputFileNameCorpusMapping");
			System.err.println("    this parameter is required only for the option -toBiGramsMethods. It represents the corpus mapping at method level granularity produced by CorpusGenerator");
			System.err.println();
			System.err.println("Using the -toUniqueMethods option, the tool will produce the following output:");
			System.err.println("  outputFolder/");
			System.err.println("    will be created if it does not exist");
			System.err.println("  outputFolder/"+InputOutputConvertTPTPTraces.FOLDER_NAME_UNIQUE_METHODS_TRACES);
			System.err.println("    will be created if it does not exist");
			System.err.println("  outputFolder/"+InputOutputConvertTPTPTraces.FOLDER_NAME_UNIQUE_METHODS_TRACES+"[issueID]"+InputOutputConvertTPTPTraces.EXTENSION_FILE_NAME_UNIQUE_METHODS_TRACE);
			System.err.println("    these files will contain the unique methods (one per line) extracted from the execution trace corresponding to [issueID] (e.g., packageName.ClassName.methodName - with no signature)");
			System.err.println();
			System.err.println("Using the -toBiGramsMethods option, the tool will produce the following output:");
			System.err.println("  outputFolder/");
			System.err.println("    will be created if it does not exist");
			System.err.println("  outputFolder/"+InputOutputConvertTPTPTraces.FOLDER_NAME_BIGRAMS_METHODS_TRACES);
			System.err.println("    will be created if it does not exist");
			System.err.println("  outputFolder/"+InputOutputConvertTPTPTraces.FOLDER_NAME_BIGRAMS_METHODS_TRACES+"[issueID]"+InputOutputConvertTPTPTraces.EXTENSION_FILE_NAME_BIGRAMS_METHODS_TRACE);
			System.err.println("    these files will contain the method bigrams (one per line) extracted from the execution trace corresponding to [issueID]. The format of a bigram is callerMethod[tab]calleeMethod[tab]callerMethodPosition[tab]calleeMethodPosition. The caller/calleeMethods are methods (i.e., lines) from the file inputFileNameCorpusMapping. The caller/calleeMethodsPosition are positions of the caller/calleeMethods in the file inputFileNameCorpusMapping");
			System.err.println();
			System.err.println("Example:");
			System.err.println("  java -jar ConvertTPTPTraces.jar -toUniqueMethods TestCases/Input/ArgoUML0.22/listOfIssues.txt TestCases/Input/ArgoUML0.22/Traces/ TestCases/Output/ArgoUML0.22/");
			System.err.println("  java -jar ConvertTPTPTraces.jar -toBiGramsMethods TestCases/Input/ArgoUML0.22/listOfIssues.txt TestCases/Input/ArgoUML0.22/Traces/ TestCases/Output/ArgoUML0.22/ TestCases/Input/ArgoUML0.22/Corpus-ArgoUML0.22.corpusMappingMethodLevelGranularity");
			System.exit(1);
		}
		
		if (args[0].equals("-toUniqueMethods"))
		{
			InputOutputConvertTPTPTraces inputOutput=new InputOutputConvertTPTPTraces(args[1],args[2],args[3]);
			ConvertTPTPTraces convertTPTPTraces=new ConvertTPTPTraces(inputOutput);
			convertTPTPTraces.convertTPTPTracesToUniqueMethods();
			
			System.out.println("The output was save in the folder "+args[3]);
			System.exit(0);
		}
		
		if (args[0].equals("-toBiGramsMethods"))
		{
			InputOutputConvertTPTPTraces inputOutput=new InputOutputConvertTPTPTraces(args[1],args[2],args[3],args[4]);
			ConvertTPTPTraces convertTPTPTraces=new ConvertTPTPTraces(inputOutput);
			convertTPTPTraces.convertTPTPTracesToBiGramsMethods();
			
			System.out.println("The output was save in the folder "+args[3]);
			System.exit(0);
		}
	}
}
