package edu.wm.cs.semeru.benchmarks.goldSetsGeneratorFromSVNCommits;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.CompilationUnit;

public class GoldSetGeneratorFromSVNCommits
{
	private InputOutputGoldSetsGeneratorFromSVNCommits inputOutput;
	public static final String DUMMY_JAVA_FILE_FOR_COMPARISON_WITH_ADDED_CLASSES="DummyJavaFileForComparisonForAddedClasses.java";

	GoldSetGeneratorFromSVNCommits(String fileNameListOfSVNCommits,String folderNameListOfFiles,String folderNameListOfFilesSideBySide,String outputFolder) throws Exception
	{
		this.inputOutput=new InputOutputGoldSetsGeneratorFromSVNCommits(fileNameListOfSVNCommits,folderNameListOfFiles,folderNameListOfFilesSideBySide,outputFolder);
	}
	
	void parseAndSaveMultipleSVNCommits() throws Exception
	{
		ArrayList<String> listOfSVNCommits=inputOutput.getListOfSVNCommits();
		System.out.println("Number of svn commits loaded: "+listOfSVNCommits.size());
		
		for (String svnCommit:listOfSVNCommits)
		{
			inputOutput.initializeFolderStructure();
			inputOutput.initializeGoldSetFilesStream(svnCommit);
			parseAndSaveSVNCommit(svnCommit,inputOutput);
			inputOutput.closeGoldSetFilesStream();
		}
	}

	private void parseAndSaveSVNCommit(String svnCommit,InputOutputGoldSetsGeneratorFromSVNCommits inputOutput) throws Exception
	{
		inputOutput.appendToGoldSetFileDebug("SVN Commit: "+svnCommit);
		
		ArrayList<String> listOfFiles=inputOutput.getListOfFiles(svnCommit);
		
		for (String fileNameAndType:listOfFiles)
		{
			inputOutput.appendToGoldSetFileDebug("File Name and Type: "+fileNameAndType);
			String[] fileNameAndTypeSplitted=fileNameAndType.split("\t");
			if (fileNameAndTypeSplitted[0].length()!=1)
				throw new Exception();
			
			char fileType=fileNameAndTypeSplitted[0].charAt(0);
			String fileName=fileNameAndTypeSplitted[1];
			
			//ignore non-java files
			if (fileName.endsWith(".java")==false)
				continue;
			
			String filePathCurrentVersion=inputOutput.getCurrentVersionForFileNameForCommit(svnCommit,fileName);
			String filePathPreviousVersion=inputOutput.getPreviousVersionFileNameForCommit(svnCommit,fileName);

			switch (fileType)
			{
				case 'M':
					genenerateGoldSet(filePathPreviousVersion,filePathCurrentVersion);
					break;
				case 'A':
					genenerateGoldSet(DUMMY_JAVA_FILE_FOR_COMPARISON_WITH_ADDED_CLASSES,filePathCurrentVersion);
					break;
				case 'R':
					genenerateGoldSet(filePathPreviousVersion,filePathCurrentVersion);
					break;
				default:
					throw new Exception("");
			}
		}
	}
	
	private void genenerateGoldSet(String fileNamePreviousVersion,String fileNameCurrentVersion) throws Exception
	{
		ArrayList<CorpusMethod> listOfCorpusMethodsPreviousVersion=getMethodsFromFile(fileNamePreviousVersion);
		ArrayList<CorpusMethod> listOfCorpusMethodsCurrentVersion=getMethodsFromFile(fileNameCurrentVersion);

		inputOutput.appendToGoldSetFileDebug("ListOfCorpusMethodsPreviousVersion size: "+listOfCorpusMethodsPreviousVersion.size());
		inputOutput.appendToGoldSetFileDebug("ListOfCorpusMethodsCurrentVersion size: "+listOfCorpusMethodsCurrentVersion.size());
		inputOutput.appendToGoldSetFileDebug("Gold set methods: ");
		
		for (CorpusMethod currentCorpusMethod:listOfCorpusMethodsCurrentVersion)
		{
			CorpusMethod correspondingMethodInPreviousVersion=findInList(listOfCorpusMethodsPreviousVersion,currentCorpusMethod);
			if (correspondingMethodInPreviousVersion!=null)
			{
				if (correspondingMethodInPreviousVersion.methodContent.equals(currentCorpusMethod.methodContent)==false)
				{
					inputOutput.appendToGoldSetFile(currentCorpusMethod.methodID);
					inputOutput.appendToGoldSetFileDebug(currentCorpusMethod.methodID);
				}
			}
			else
			{
				inputOutput.appendToGoldSetFile(currentCorpusMethod.methodID);
				inputOutput.appendToGoldSetFileDebug(currentCorpusMethod.methodID);
			}
		}
		
		inputOutput.appendToGoldSetFileDebug("\n##########################################################\n");
	}
	
	private ArrayList<CorpusMethod> getMethodsFromFile(String fileName) throws Exception
	{
		inputOutput.appendToGoldSetFileDebug("Reading file: "+fileName);
		if (fileName.equals(DUMMY_JAVA_FILE_FOR_COMPARISON_WITH_ADDED_CLASSES))
			return new ArrayList<CorpusMethod>();
		
		String fileContent=InputOutputGoldSetsGeneratorFromSVNCommits.readFile(fileName);

		ParserGoldSets parser=new ParserGoldSets(inputOutput,fileContent);
		CompilationUnit compilationUnitSourceCode=parser.parseSourceCode();
		ArrayList<CorpusMethod> listOfCorpusMethods=parser.exploreSourceCodeAndIgnoreComments(compilationUnitSourceCode);

		inputOutput.appendToGoldSetFileDebug("List of methods:");
		for (CorpusMethod corpusMethod : listOfCorpusMethods)
		{
			inputOutput.appendToGoldSetFileDebug(corpusMethod.toString());
		}

		inputOutput.appendToGoldSetFileDebug("-------------------------------------");
		return listOfCorpusMethods;
	}

	private CorpusMethod findInList(ArrayList<CorpusMethod> listOfCorpusMethods,CorpusMethod currentCorpusMethod)
	{
		for (CorpusMethod corpusMethod:listOfCorpusMethods)
		{
			if (corpusMethod.equals(currentCorpusMethod))
				return corpusMethod;
		}
		return null;
	}
}
