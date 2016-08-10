package edu.wm.cs.semeru.benchmarks.downloadSVNCommits.tests;

import java.io.File;
import java.util.ArrayList;

public class Utils
{
	private static ArrayList<String> listOfFilesAndFolders;

	public static ArrayList<String> getListOfFilesAndFolders(String folderName)
	{
		listOfFilesAndFolders=new ArrayList<String>();
		
		listFilesAndFolders(folderName);
		
		return listOfFilesAndFolders;
	}
	
	private static void listFilesAndFolders(String folderName)
	{
		File[] listOfFiles=new File(folderName).listFiles();
		for (File file : listOfFiles)
		{
			if (file.isDirectory())
			{
				listFilesAndFolders(file.getAbsolutePath());
			}
			if (file.isFile())
			{
				String fileName=file.getAbsolutePath();
//				String fileNameRelativePath=fileName.substring(fileName.indexOf("TestCase"));
//				System.out.println(fileName);
//				System.out.println(fileNameRelativePath);
				listOfFilesAndFolders.add(fileName);
			}
		}
	}

	public static void main(String[] args)
	{
		String folderName="TestCases/Output/";
		
		ArrayList<String> listOfFilesAndFolders;
		listOfFilesAndFolders=getListOfFilesAndFolders(folderName);
		
		System.out.println(listOfFilesAndFolders.size());
	}

}
