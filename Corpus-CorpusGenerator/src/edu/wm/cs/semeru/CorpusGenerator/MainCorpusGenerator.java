package edu.wm.cs.semeru.CorpusGenerator;

import org.eclipse.jdt.core.dom.CompilationUnit;

public class MainCorpusGenerator
{
	static void testMainMethodLevelGranularity() throws Exception
	{
		String workFolder="TestCases/";

		String[] listOfSystems=new String[]{
				"System1",
				"System2",
				"System3",
				"jEdit4.3"
		};
		
		int i=0;
		for (String systemName : listOfSystems)
		{
			System.out.println("Current system="+systemName);
			InputOutputCorpusMethodLevelGranularity inputOutput=new InputOutputCorpusMethodLevelGranularity(workFolder+"Input/inputFileNames"+listOfSystems[i]+".txt",workFolder+"Output/MethodLevelGranularity/","Corpus-"+listOfSystems[i]);
			i++;

			parseAndSaveMultipleFiles(inputOutput);
		}
	}
	
	static void testMainClassLevelGranularity() throws Exception
	{
		String workFolder="TestCases/";

		String[] listOfSystems=new String[]{
				"System1",
				"System2",
				"System3",
				"jEdit4.3"
		};
		
		int i=0;
		for (String systemName : listOfSystems)
		{
			System.out.println("Current system="+systemName);
			InputOutputCorpusClassLevelGranularity inputOutput=new InputOutputCorpusClassLevelGranularity(workFolder+"Input/inputFileNames"+listOfSystems[i]+".txt",workFolder+"Output/ClassLevelGranularity/","Corpus-"+listOfSystems[i]);
			i++;

			parseAndSaveMultipleFilesClassLevelGranularity(inputOutput);
		}
	}
	
	static void testMainFileLevelGranularity() throws Exception
	{
		String workFolder="TestCases/";

		String[] listOfSystems=new String[]{
				"System1",
				"System2",
				"System3",
				"jEdit4.3"
		};
		
		int i=0;
		for (String systemName : listOfSystems)
		{
			System.out.println("Current system="+systemName);
			InputOutputCorpusFileLevelGranularity inputOutput=new InputOutputCorpusFileLevelGranularity(workFolder+"Input/inputFileNames"+listOfSystems[i]+".txt",workFolder+"Output/FileLevelGranularity/","Corpus-"+listOfSystems[i]);
			i++;

			parseAndSaveMultipleFilesFileLevelGranularity(inputOutput);
		}
	}
	
	public static void main(String[] args) throws Exception
	{
		testMainMethodLevelGranularity();
		testMainClassLevelGranularity();
		testMainFileLevelGranularity();
		if (1==1)
			return;
	
//		args=new String[4];
//		args[0]="-methodLevelGranularity";
//		args[1]="TestCases/Input/inputFileNamesjEdit4.3.txt";
//		args[2]="TestCases/Output/";
//		args[3]="Corpus-jEdit4.3";
		
//		args=new String[4];
//		args[0]="-classLevelGranularity";
//		args[1]="TestCases/Input/inputFileNamesjEdit4.3.txt";
//		args[2]="TestCases/Output/ClassLevelGranularity/";
//		args[3]="Corpus-jEdit4.3";
		
//		args=new String[4];
//		args[0]="-fileLevelGranularity";
//		args[1]="TestCases/Input/inputFileNamesjEdit4.3.txt";
//		args[2]="TestCases/Output/FileLevelGranularity/";
//		args[3]="Corpus-jEdit4.3";

		
		if (args.length!=4)
		{
			System.err.println("Generates a corpus from the source code at different levels of granularity.");
			System.err.println("Usage:");
			System.err.println("  java -jar CorpusGenerator.jar -methodLevelGranularity inputFileNameWithListOfInputFileNames outputFolder outputFileNameWithoutExtension");
			System.err.println("  java -jar CorpusGenerator.jar -classLevelGranularity inputFileNameWithListOfInputFileNames outputFolder outputFileNameWithoutExtension");
			System.err.println("  java -jar CorpusGenerator.jar -fileLevelGranularity inputFileNameWithListOfInputFileNames outputFolder outputFileNameWithoutExtension");
			System.err.println();
			System.err.println("Where:");
			System.err.println("  inputFileNameWithListOfInputFileNames");
			System.err.println("    is a file name containing n lines. Each line is a full path of a java file to be analyzed.");
			System.err.println("  outputFolder");
			System.err.println("    is the folder name where the corpus will be saved");
			System.err.println("  outputFileNameWithoutExtension");
			System.err.println("    the prefix of the output files (e.g., the name of the software system)");
			System.err.println();
			System.err.println("The output produced by this tool using the -methodLevelGranularity option will contain 4 files:");
			System.err.println("  outputFolder/outputFileNameWithoutExtension"+InputOutputCorpusMethodLevelGranularity.EXTENSION_CORPUS_RAW);
			System.err.println("    contains the corpus where each method extracted from the java files is on its own line");
			System.err.println("  outputFolder/outputFileNameWithoutExtension"+InputOutputCorpusMethodLevelGranularity.EXTENSION_CORPUS_MAPPING);
			System.err.println("    contains the id of the method from the corpus on its own line (e.g., packageName.className.methodName)");
			System.err.println("  outputFolder/outputFileNameWithoutExtension"+InputOutputCorpusMethodLevelGranularity.EXTENSION_CORPUS_MAPPING_WITH_PACKAGE_SEPARATOR);
			System.err.println("    contains the id of the method from the corpus on its own line, with a separator character ('$') between package and class name (e.g., packageName$className.methodName)");
			System.err.println("  outputFolder/outputFileNameWithoutExtension"+InputOutputCorpusMethodLevelGranularity.EXTENSION_CORPUS_DEBUG);
			System.err.println("    contains some verbose information about the corpus extraction (for verification purposes only)");
			System.err.println();
			System.err.println("The output produced by this tool using the -classLevelGranularity option will contain 3 files:");
			System.err.println("  outputFolder/outputFileNameWithoutExtension"+InputOutputCorpusClassLevelGranularity.EXTENSION_CORPUS_RAW);
			System.err.println("    contains the corpus where each class extracted from the java files is on its own line (i.e., multiple classes in the same file are considered as separate documents)");
			System.err.println("  outputFolder/outputFileNameWithoutExtension"+InputOutputCorpusClassLevelGranularity.EXTENSION_CORPUS_MAPPING);
			System.err.println("    contains the id of the class from the corpus on its own line");
			System.err.println("  outputFolder/outputFileNameWithoutExtension"+InputOutputCorpusClassLevelGranularity.EXTENSION_CORPUS_DEBUG);
			System.err.println("    contains some verbose information about the corpus extraction (for verification purposes only)");
			System.err.println();
			System.err.println("The output produced by this tool using the -fileLevelGranularity option will contain 3 files:");
			System.err.println("  outputFolder/outputFileNameWithoutExtension"+InputOutputCorpusFileLevelGranularity.EXTENSION_CORPUS_RAW);
			System.err.println("    contains the corpus where all the classes extracted from the java files is on its own line (i.e., multiple classes in the same file are considered as one document)");
			System.err.println("  outputFolder/outputFileNameWithoutExtension"+InputOutputCorpusFileLevelGranularity.EXTENSION_CORPUS_MAPPING);
			System.err.println("    contains the id of the file from the corpus on its own line");
			System.err.println("  outputFolder/outputFileNameWithoutExtension"+InputOutputCorpusFileLevelGranularity.EXTENSION_CORPUS_DEBUG);
			System.err.println("    contains some verbose information about the corpus extraction (for verification purposes only)");
			System.err.println();
			System.err.println("Example:");
			System.err.println("  java -jar CorpusGenerator.jar -methodLevelGranularity TestCases/Input/inputFileNamesjEdit4.3.txt TestCases/Output/MethodLevelGranularity/ Corpus-jEdit4.3");
			System.err.println("  java -jar CorpusGenerator.jar -classLevelGranularity TestCases/Input/inputFileNamesjEdit4.3.txt TestCases/Output/ClassLevelGranularity/ Corpus-jEdit4.3");
			System.err.println("  java -jar CorpusGenerator.jar -fileLevelGranularity TestCases/Input/inputFileNamesjEdit4.3.txt TestCases/Output/FileLevelGranularity/ Corpus-jEdit4.3");
			System.exit(1);
		}
		
		if (args[0].equals("-methodLevelGranularity"))
		{
			InputOutputCorpusMethodLevelGranularity inputOutput=new InputOutputCorpusMethodLevelGranularity(args[1],args[2],args[3]);
			parseAndSaveMultipleFiles(inputOutput);
			System.exit(0);
		}

		if (args[0].equals("-classLevelGranularity"))
		{
			InputOutputCorpusClassLevelGranularity inputOutputClassLevelGranularity=new InputOutputCorpusClassLevelGranularity(args[1],args[2],args[3]);
			parseAndSaveMultipleFilesClassLevelGranularity(inputOutputClassLevelGranularity);
			System.exit(0);
		}
		
		if (args[0].equals("-fileLevelGranularity"))
		{
			InputOutputCorpusFileLevelGranularity inputOutputFileLevelGranularity=new InputOutputCorpusFileLevelGranularity(args[1],args[2],args[3]);
			parseAndSaveMultipleFilesFileLevelGranularity(inputOutputFileLevelGranularity);
			System.exit(0);
		}

		//-interfaces: list all the interfaces
		//-classlevelgranularity: list all class level granularities
		//maybe use the INputOutputClassLevel, interface, clss level granularity, method level granularity
	}

	private static void parseAndSaveMultipleFiles(InputOutputCorpusMethodLevelGranularity inputOutput) throws Exception
	{
		//delete output files if they already exist
		inputOutput.initializeOutputStream();
		
		String[] inputFileNames=InputOutput.readFile(inputOutput.getInputFileNameWithListOfInputFileNames()).split(InputOutput.LINE_ENDING);

		for (String inputFileName : inputFileNames)
		{
			inputOutput.appendToCorpusDebug("Preprocessing file:\t"+inputFileName);
			parseAndSaveOneFile(inputOutput,inputFileName);
		}
		
		inputOutput.closeOutputStreams();
	}

	private static void parseAndSaveOneFile(InputOutputCorpusMethodLevelGranularity inputOutput,String inputFileName)
	{
		String fileContent="";
		fileContent=InputOutput.readFile(inputFileName);

		ParserCorpusMethodLevelGranularity parser=new ParserCorpusMethodLevelGranularity(inputOutput,fileContent);
		CompilationUnit compilationUnitSourceCode=parser.parseSourceCode();
		parser.exploreSourceCode(compilationUnitSourceCode);
	}
	
	private static void parseAndSaveMultipleFilesClassLevelGranularity(InputOutputCorpusClassLevelGranularity inputOutput) throws Exception
	{
		//delete output files if they already exist
		inputOutput.initializeOutputStream();
		
		String[] inputFileNames=InputOutput.readFile(inputOutput.getInputFileNameWithListOfInputFileNames()).split(InputOutput.LINE_ENDING);

		for (String inputFileName : inputFileNames)
		{
			inputOutput.appendToCorpusDebug("Preprocessing file:\t"+inputFileName);
			parseAndSaveOneFileClassLevelGranularity(inputOutput,inputFileName);
		}
		
		inputOutput.closeOutputStreams();
	}

	private static void parseAndSaveOneFileClassLevelGranularity(InputOutputCorpusClassLevelGranularity inputOutput,String inputFileName)
	{
		String fileContent="";
		fileContent=InputOutput.readFile(inputFileName);

		ParserCorpusClassLevelGranularity parser=new ParserCorpusClassLevelGranularity(inputOutput,fileContent);
		CompilationUnit compilationUnitSourceCode=parser.parseSourceCode();
		parser.exploreSourceCodeClassLevelGranularity(compilationUnitSourceCode);
	}
	
	private static void parseAndSaveMultipleFilesFileLevelGranularity(InputOutputCorpusFileLevelGranularity inputOutput) throws Exception
	{
		//delete output files if they already exist
		inputOutput.initializeOutputStream();
		
		String[] inputFileNames=InputOutput.readFile(inputOutput.getInputFileNameWithListOfInputFileNames()).split(InputOutput.LINE_ENDING);

		for (String inputFileName : inputFileNames)
		{
			inputOutput.appendToCorpusDebug("Preprocessing file:\t"+inputFileName);
			parseAndSaveOneFileFileLevelGranularity(inputOutput,inputFileName);
		}
		
		inputOutput.closeOutputStreams();
	}

	private static void parseAndSaveOneFileFileLevelGranularity(InputOutputCorpusFileLevelGranularity inputOutput,String inputFileName)
	{
		String fileContent="";
		fileContent=InputOutput.readFile(inputFileName);

		ParserCorpusFileLevelGranularity parser=new ParserCorpusFileLevelGranularity(inputOutput,fileContent,inputFileName);
		CompilationUnit compilationUnitSourceCode=parser.parseSourceCode();
		parser.exploreSourceCodeFileLevelGranularity(compilationUnitSourceCode);
	}
	
}
