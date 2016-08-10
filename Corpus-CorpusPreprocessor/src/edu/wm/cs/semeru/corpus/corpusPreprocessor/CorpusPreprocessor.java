package edu.wm.cs.semeru.corpus.corpusPreprocessor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Hashtable;

public class CorpusPreprocessor
{
	public static final String SUFFIX_AFTER_SPLIT="-AfterSplit";
	public static final String SUFFIX_AFTER_SPLIT_STOP="-AfterSplitStop";
	public static final String SUFFIX_AFTER_SPLIT_STOP_STEM="-AfterSplitStopStem";

	public static String FILE_NAME_LIST_OF_STOP_WORDS="StopWords.txt";
	
	private String inputFileNameCorpus;
	private String inputFileNameCorpusWithoutPath;
	private String inputFileNameCorpusWithoutPathWithoutExtension;
	private String inputFileNameCorpusExtension;

	private String outputFolder;
	
	private Hashtable<String,Integer> listOfStopWords;
	

	public CorpusPreprocessor(String inputFileNameCorpus,String outputFolder)
	{
		this.inputFileNameCorpus=inputFileNameCorpus;
		this.inputFileNameCorpusWithoutPath=new File(inputFileNameCorpus).getName();
		this.inputFileNameCorpusExtension=inputFileNameCorpusWithoutPath.substring(inputFileNameCorpusWithoutPath.lastIndexOf("."));
		this.inputFileNameCorpusWithoutPathWithoutExtension=inputFileNameCorpusWithoutPath.substring(0,inputFileNameCorpusWithoutPath.indexOf(inputFileNameCorpusExtension));
		
		this.outputFolder=outputFolder;

		System.out.println("inputFileNameCorpus="+inputFileNameCorpus);
		System.out.println("inputFileNameCorpusWithoutPath="+inputFileNameCorpusWithoutPath);
		System.out.println("inputFileNameCorpusExtension="+inputFileNameCorpusExtension);
		System.out.println("inputFileNameCorpusWithoutPathWithoutExtension="+inputFileNameCorpusWithoutPathWithoutExtension);
		System.out.println("outputFolder="+outputFolder);
	}

	public void preprocessCorpus() throws Exception
	{
		listOfStopWords=loadListOfStopWords();
		
		BufferedReader brOriginalCorpus=new BufferedReader(new FileReader(inputFileNameCorpus));
		BufferedWriter outCorpusAfterSplit=new BufferedWriter(new FileWriter(outputFolder+inputFileNameCorpusWithoutPathWithoutExtension+SUFFIX_AFTER_SPLIT+inputFileNameCorpusExtension));
		BufferedWriter outCorpusAfterSplitStop=new BufferedWriter(new FileWriter(outputFolder+inputFileNameCorpusWithoutPathWithoutExtension+SUFFIX_AFTER_SPLIT_STOP+inputFileNameCorpusExtension));
		BufferedWriter outCorpusAfterSplitStopStem=new BufferedWriter(new FileWriter(outputFolder+inputFileNameCorpusWithoutPathWithoutExtension+SUFFIX_AFTER_SPLIT_STOP_STEM+inputFileNameCorpusExtension));
		
		String buf;
		while ((buf=brOriginalCorpus.readLine())!=null)
		{
			String bufAfterEliminatingNonLiterals=eliminateNonLiterals(buf);
			String bufAfterSplit=splitIdentifiers(bufAfterEliminatingNonLiterals,false);
			String bufAfterSplitStop=elimiateStopWords(bufAfterSplit,1);
			String bufAfterSplitStopStem=stemBuffer(bufAfterSplitStop);

			outCorpusAfterSplit.write(bufAfterSplit+"\r\n");
			outCorpusAfterSplitStop.write(bufAfterSplitStop+"\r\n");
			outCorpusAfterSplitStopStem.write(bufAfterSplitStopStem+"\r\n");
		}
		
		brOriginalCorpus.close();
		outCorpusAfterSplit.close();
		outCorpusAfterSplitStop.close();
		outCorpusAfterSplitStopStem.close();
	}
	
	public Hashtable<String,Integer> loadListOfStopWords() throws Exception
	{
		BufferedReader br=new BufferedReader(new FileReader(FILE_NAME_LIST_OF_STOP_WORDS));
		String buf;
		Hashtable<String,Integer> listOfStopWords=new Hashtable<String,Integer>();
		while ((buf=br.readLine())!=null)
		{
			listOfStopWords.put(buf,new Integer(1));
		}
		br.close();
		return listOfStopWords;
	}
	
	private String eliminateNonLiterals(String originalBuffer)
	{
//		String newBuffer=originalBuffer.replaceAll("[^a-zA-Z0-9_]"," ");
		String newBuffer=originalBuffer.replaceAll("[^a-zA-Z_]"," ");
//		System.out.println(newBuffer);
		return newBuffer;
	}

	public String splitIdentifiers(String originalBuffer,boolean keepCompoundIdentifier)
	{
		String words[]=originalBuffer.split(" ");
		
		StringBuilder newBuffer=new StringBuilder();
		boolean isCompoundIdentifier;
		
		for (String word : words)
		{
			String originalWord=word;
			if (word.length()==0)
				continue;
		
			isCompoundIdentifier=false;
			if (word.indexOf('_')>=0)
			{
				isCompoundIdentifier=true;
				word=word.replaceAll("_"," ");
			}
			
			StringBuilder newWord=new StringBuilder(word);
			
			for (int i=newWord.length()-1;i>=0;i--)
			{
				if (Character.isUpperCase(newWord.charAt(i)))
				{
					if (i>0)
						if (Character.isLowerCase(newWord.charAt(i-1)))
						{
							newWord.insert(i,' ');
							isCompoundIdentifier=true;
						}
				}
				else
					if (Character.isLowerCase(newWord.charAt(i)))
					{
						if (i>0)
							if (Character.isUpperCase(newWord.charAt(i-1)))
							{
								newWord.insert(i-1,' ');
								isCompoundIdentifier=true;
							}
					}
					
			}
			
			newBuffer.append(newWord.toString().toLowerCase());
			newBuffer.append(' ');
			if (keepCompoundIdentifier)
			{
				if (isCompoundIdentifier)
				{
					newBuffer.append(originalWord.toLowerCase());
					newBuffer.append(' ');
				}
			}
		}
		System.out.println("=====");
		System.out.println(newBuffer.toString());
		return newBuffer.toString();
	}
	
	private static boolean isAllDigits(String word)
	{
		char[] charactersWord=word.toCharArray();
		for (char c : charactersWord)
		{
			if (Character.isDigit(c)==false)
				return false;
		}
		return true;
	}
	
	private String elimiateStopWords(String originalBuffer,int numberOfCharactersForWordToRemove)
	{
		String words[]=originalBuffer.split(" ");
		StringBuilder newBufferAfterEliminatingStopWords=new StringBuilder();

		for (String word:words)
		{
			if (word.length()==0)
				continue;

			if (listOfStopWords.get(word)!=null)
				continue;
			
			if (isAllDigits(word))
				continue;
			
			if (word.length()<=numberOfCharactersForWordToRemove)
				continue;
			
			newBufferAfterEliminatingStopWords.append(word);
			newBufferAfterEliminatingStopWords.append(' ');
		}
//		System.out.println("-----");
//		System.out.println(newBufferAfterEliminatingStopWords.toString());
		return newBufferAfterEliminatingStopWords.toString();
	}	
	
	private String stemBuffer(String originalBuffer)
	{
		String words[]=originalBuffer.split(" ");
		StringBuilder newBufferStemmed=new StringBuilder();
		
		for (String word : words)
		{
			if (word.length()==0)
				continue;
		
			Stemmer stemmer=new Stemmer();
			for (int i=0;i<word.length();i++)
				stemmer.add(word.charAt(i));
			
			stemmer.stem();
			newBufferStemmed.append(stemmer.toString());
			newBufferStemmed.append(' ');
		}
//		System.out.println("-----");
//		System.out.println(newBufferStemmed.toString());
		return newBufferStemmed.toString();
	}
}
