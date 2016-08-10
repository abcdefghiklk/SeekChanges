package edu.wm.cs.semeru.benchmarks.goldSetsGeneratorFromSVNCommits;

public class MainGoldSetsGeneratorFromSVNCommits
{
	static void testArgoUML() throws Exception
	{
		String fileNameListOfSVNCommits="TestCases/Input/ArgoUML/listOfSVNCommits.txt";
		String folderNameListOfFiles="TestCases/Input/ArgoUML/SVNListOfFiles/";
		String folderNameListOfFilesSideBySide="TestCases/Input/ArgoUML/SVNFilesSideBySide/";

		String outputFolder="TestCases/Output/ArgoUML/";

		GoldSetGeneratorFromSVNCommits goldSetGeneratorFromSVNCommits=new GoldSetGeneratorFromSVNCommits(
				fileNameListOfSVNCommits,
				folderNameListOfFiles,
				folderNameListOfFilesSideBySide,
				outputFolder);
		
		goldSetGeneratorFromSVNCommits.parseAndSaveMultipleSVNCommits();
		
		System.out.println("The data has been saved in the folder: "+outputFolder);
	}

	static void testjEdit() throws Exception
	{
		String fileNameListOfSVNCommits="TestCases/Input/jEdit/listOfSVNCommits.txt";
		String folderNameListOfFiles="TestCases/Input/jEdit/SVNListOfFiles/";
		String folderNameListOfFilesSideBySide="TestCases/Input/jEdit/SVNFilesSideBySide/";

		String outputFolder="TestCases/Output/jEdit/";

		GoldSetGeneratorFromSVNCommits goldSetGeneratorFromSVNCommits=new GoldSetGeneratorFromSVNCommits(
				fileNameListOfSVNCommits,
				folderNameListOfFiles,
				folderNameListOfFilesSideBySide,
				outputFolder);
		
		goldSetGeneratorFromSVNCommits.parseAndSaveMultipleSVNCommits();
		
		System.out.println("The data has been saved in the folder: "+outputFolder);
	}

	public static void main(String[] args) throws Exception
	{
		testArgoUML();
		testjEdit();
		if (1==1)
			return;
		
//		args=new String[4];
//		args[0]="TestCases/Input/ArgoUML/listOfSVNCommits.txt";
//		args[1]="TestCases/Input/ArgoUML/SVNListOfFiles/";
//		args[2]="TestCases/Input/ArgoUML/SVNFilesSideBySide/";
//		args[3]="TestCases/Output/ArgoUML/";
		
//		args=new String[4];
//		args[0]="TestCases/Input/jEdit/listOfSVNCommits.txt";
//		args[1]="TestCases/Input/jEdit/SVNListOfFiles/";
//		args[2]="TestCases/Input/jEdit/SVNFilesSideBySide/";
//		args[3]="TestCases/Output/jEdit/";
		
		if (args.length!=4)
		{
			System.err.println("Generate gold sets from SVN commits");
			System.err.println("Usage:");
			System.err.println("  java -jar GoldSetGeneratorFromSVNCommits.jar fileListOfSVNCommits folderListOfFiles folderFilesSideBySide outputFolder");
			System.err.println();
			System.err.println("Where:");
			System.err.println("  fileListOfSVNCommits");
			System.err.println("    is a file that contains the revision numbers (one per line) that have files with valid extension (e.g., *.java) that were added or modified in the commit.");
			System.err.println("  folderListOfFiles");
			System.err.println("    contains a file for each SVN revision (e.g., [N].SVNListOfFiles, where [N] is the revision number). Each line of that file contains the file type (e.g., A, R, M) and the file name");
			System.err.println("  folderFilesSideBySide");
			System.err.println("    contains a folder for each SVN revision (e.g., [N]). Each of those folders contains the previous version of the file (if it exists) and the version of the file at revision [N]");
			System.err.println("  outputFolder");
			System.err.println("    is the folder name where the gold sets (i.e., the diffs) will be saved");
			System.err.println();
			System.err.println("The output produced by this tool will contain the following types of files:");
			System.err.println("  [outputFolder]/GoldSetsFromSVNCommits/[revision]"+InputOutputGoldSetsGeneratorFromSVNCommits.EXTENSION_GOLD_SET_FROM_SVN_COMMITS);
			System.err.println("    contains the list of methods (one per line) that were modified between revision [revision] and the previous revision");
			System.err.println("  [outputFolder]/GoldSetsFromSVNCommitsDebug/[revision]"+InputOutputGoldSetsGeneratorFromSVNCommits.EXTENSION_GOLD_SET_FROM_SVN_COMMITS_DEBUG);
			System.err.println("    contains verbose information about the gold set generation process from SVN commits");
			System.err.println();
			System.err.println("Example:");
			System.err.println("  java -jar GoldSetGeneratorFromSVNCommits.jar TestCases/Input/ArgoUML/listOfSVNCommits.txt TestCases/Input/ArgoUML/SVNListOfFiles/ TestCases/Input/ArgoUML/SVNFilesSideBySide/ TestCases/Output/ArgoUML/");
			System.err.println("  java -jar GoldSetGeneratorFromSVNCommits.jar TestCases/Input/jEdit/listOfSVNCommits.txt TestCases/Input/jEdit/SVNListOfFiles/ TestCases/Input/jEdit/SVNFilesSideBySide/ TestCases/Output/jEdit/");
			System.exit(1);
		}
		
		GoldSetGeneratorFromSVNCommits goldSetGeneratorFromSVNCommits=new GoldSetGeneratorFromSVNCommits(
				args[0],
				args[1],
				args[2],
				args[3]);
		
		goldSetGeneratorFromSVNCommits.parseAndSaveMultipleSVNCommits();
		
		System.out.println("The data has been saved in the folder: "+args[3]);
	}
}
