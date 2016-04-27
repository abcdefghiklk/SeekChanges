package sourcecode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import utils.WVToolWrapper;
import edu.udo.cs.wvtool.main.WVTFileInputList;
import edu.udo.cs.wvtool.wordlist.WVTWordList;

public class CodeFeatureExtractor {
	/**
	 * Extract the dictionaries for the source code corpus
	 * @param codeCorpusDirPath
	 * @return
	 * @throws Exception
	 */
	public static WVTWordList extractCodeDictionary(String codeCorpusDirPath) throws Exception{
		if(!new File(codeCorpusDirPath).isDirectory()){
			System.out.println("The corpus directory is invalid!");
			return new WVTWordList(1);
		}
		WVTFileInputList list=WVToolWrapper.extractCorpusFileList(codeCorpusDirPath);
		WVTWordList dictionary=WVToolWrapper.extractCorpusDic(list);
		return dictionary;
	}
	
	/**
	 * Save the code dictionary to the target file
	 * @param dictionary
	 * @param dicFilePath
	 * @throws IOException
	 */
	public static void exportCodeDictionary(WVTWordList dictionary, String dicFilePath) throws IOException{
		WVToolWrapper.saveCorpusDic(dictionary, dicFilePath);
	}
	
	/**
	 * Import the code dictionary from the file
	 * @param dicFilePath
	 * @return
	 * @throws Exception
	 */
	public static ArrayList<String> importCodeDictionary(String dicFilePath) throws Exception{
		ArrayList<String> dictionary=new ArrayList<String>();
		if(!new File(dicFilePath).isFile()){
			System.out.println("The dictionary file path is invalid!");
			return dictionary;
		}
		BufferedReader reader=new BufferedReader(new FileReader(dicFilePath));
		String line=new String();
		while((line=reader.readLine())!=null){
			dictionary.add(line.trim());
		}
		reader.close();
		return dictionary;
	}
	
	/**
	 * Extract the <codeFullClass, codeLength> pairs from corpus object
	 * @param corpus
	 * @return
	 */
	public static HashMap<String, Integer> extractCodeLength(SourceCodeCorpus corpus) {
		HashMap<String, Integer> fullClassLengthPairs= new HashMap<String, Integer>();
		for(SourceCode oneCodeFile: corpus.getSourceCodeList()){
			String fullClassName=oneCodeFile.getFullClassName();
			int codeLength=oneCodeFile.getContent().trim().split(" ").length;
			fullClassLengthPairs.put(fullClassName, codeLength);
		}
		return fullClassLengthPairs;
	}
	
	/**
	 * Extract the <codeFullClass, codeLength> pairs from directory
	 * @param corpusDirPath
	 * @return
	 * @throws Exception
	 */
	public static HashMap<String, Integer> extractCodeLength(String corpusDirPath) throws Exception {
		HashMap<String, Integer> fullClassLengthPairs=new HashMap<String, Integer>();
		if(!new File(corpusDirPath).isDirectory()){
			System.out.println("The input directory is invalid!");
			return fullClassLengthPairs;
		}
		for(File file: new File(corpusDirPath).listFiles()){
			if(file.isFile()){
				String fileName=file.getName().trim();
				BufferedReader reader=new BufferedReader(new FileReader(file));
				String line=reader.readLine();
				String []strs=line.split(" ");
				int fileLength=strs.length;
				reader.close();
				fullClassLengthPairs.put(fileName, fileLength);
			}
		}
		return fullClassLengthPairs;
	}
	
	/**
	 * Save the <codeFullClass, codeLength> pairs to the target file
	 * @param dstFilePath
	 * @param fullClassLengthPairs
	 * @throws IOException
	 */
	public static void saveCodeLength(String dstFilePath, HashMap<String, Integer> fullClassLengthPairs) throws IOException{
		FileWriter writer=new FileWriter(dstFilePath);
		StringBuffer buf=new StringBuffer();
		for(Entry<String, Integer> onepair:fullClassLengthPairs.entrySet()){
			buf.append(onepair.getKey()+"\t"+onepair.getValue()+"\r\n");
		}
		writer.write(buf.toString());
		writer.close();
	}
	
	/**
	 * Load the <codeFullClass, codeLength> pairs to the source file
	 * @param srcFilePath
	 * @return
	 * @throws Exception
	 */
	public static HashMap<String, Double> loadCodeLength(String srcFilePath) throws Exception{
		HashMap<String, Double> fileLengthPairs=new HashMap<String, Double>();
		if(!new File(srcFilePath).isFile()){
			System.out.println("The input file path is invalid!");
			return fileLengthPairs;
		}
		BufferedReader reader=new BufferedReader(new FileReader(srcFilePath));
		String line=new String();
		while((line=reader.readLine())!=null){
			String strs[]=line.split("\t");
			String fileName=strs[0].trim();
			double fileSize=Double.parseDouble(strs[1].trim());
			fileLengthPairs.put(fileName, fileSize);
		}
		reader.close();
		return fileLengthPairs;
	}
	
	public static HashSet<String> extractAllPackages(SourceCodeCorpus corpus){
		HashSet<String> allPackages=new HashSet<String>();
		ArrayList<SourceCode> sourceCodeList=corpus.getSourceCodeList();
		for(SourceCode sourceCode: sourceCodeList){
			//fullClassName=packageName.AbsoluteFileName.java
			String fullClassName=sourceCode.getFullClassName();
			
			//className=packageName.AbsoluteFileName
			String className=fullClassName.substring(0, fullClassName.lastIndexOf("."));
			String rootPackageName=fullClassName.substring(0,fullClassName.indexOf("."));
			while(className.lastIndexOf(".")!=-1){
				className=className.substring(0, className.lastIndexOf("."));
				if(!className.equals(rootPackageName)){
					allPackages.add(className);
				}
			}
			
		}
		return allPackages;
	}
	
	public static ArrayList<String> getAllFilesInPackage(String packageName, ArrayList<String> corpusFileList){
		ArrayList<String> fileList=new ArrayList<String> ();
		for(String oneFile:corpusFileList){
			if(oneFile.contains(packageName)){
				fileList.add(oneFile);
			}
		}
		return fileList;
	}
	public static ArrayList<String> GetCorpusFileList(SourceCodeCorpus corpus){
		ArrayList<String> corpusFileList=new ArrayList<String>();
		for(SourceCode oneCodeFile:corpus.getSourceCodeList()){
			corpusFileList.add(oneCodeFile.getFullClassName());
		}
		return corpusFileList;
	}
	
	public static void AddPackagesToCorpus(String corpusDir, SourceCodeCorpus corpus) throws Exception{
		HashSet<String> allPackages=extractAllPackages(corpus);
		ArrayList<String> corpusFileList=GetCorpusFileList(corpus);
		for(String onePackage: allPackages){
			System.out.println("adding package "+onePackage+"...");
			String fileName=Paths.get(corpusDir,onePackage).toString();
			FileWriter writer=new FileWriter(fileName);
			StringBuffer buf=new StringBuffer();
			for(SourceCode oneCodeFile: corpus.getSourceCodeList()){
				if(getAllFilesInPackage(onePackage,corpusFileList).contains(oneCodeFile.getFullClassName())){
					buf.append(oneCodeFile.getContent());
					buf.append(" ");
				}
			}
			writer.write(buf.toString());
			writer.close();
		}
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
