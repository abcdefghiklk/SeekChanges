package edu.wm.cs.semeru.benchmarks.convertTPTPTraces;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Stack;

class PairMethodIDTraceMethodIDCorpus
{
	String methodIDTrace;
	String methodName;
	String methodIDCorpus;
	
	public PairMethodIDTraceMethodIDCorpus(String methodIDTrace,String methodName,String methodIDCorpus)
	{
		this.methodIDTrace=methodIDTrace;
		this.methodName=methodName;
		this.methodIDCorpus=methodIDCorpus;
	}

	@Override
	public String toString()
	{
		return "PairMethodIDTraceMethodIDCorpus [methodIDCorpus="+methodIDCorpus+", methodName="+methodName+", methodIDTrace="+methodIDTrace+"]";
	}
}

public class ConvertTPTPTraces
{
	private InputOutputConvertTPTPTraces inputOutput;

	public ConvertTPTPTraces(InputOutputConvertTPTPTraces inputOutput)
	{
		this.inputOutput=inputOutput;
	}
	
	public void convertTPTPTracesToUniqueMethods() throws Exception
	{
		inputOutput.initializeFolderStructure();
		
		ArrayList<String> listOfIssues=inputOutput.loadListOfIssues();

		String[] traceLineSplit;
		BufferedReader brTrace;
		String currentLine=null;
		int lineNumber=0;
		
		try
		{
			for (String issueID : listOfIssues)
			{
				brTrace=new BufferedReader(new FileReader(inputOutput.getFileNameTrace(issueID)));
				System.out.println("Processing File: "+inputOutput.getFileNameTrace(issueID));
				
				HashSet<String> setOfUniqueMethodsTrace=new HashSet<String>();
				lineNumber=0;
				Hashtable<String,String> idToClass=new Hashtable<String,String>();
				Hashtable<String,String> idToMethod=new Hashtable<String,String>();

				while ((currentLine=brTrace.readLine())!=null)
				{
					lineNumber++;
					if (currentLine.startsWith("<classDef "))
					{
						traceLineSplit=currentLine.split(" ");
						if (traceLineSplit[1].startsWith("name=")==false)
							throw new Exception();
						if (traceLineSplit[2].startsWith("sourceName=")==false)
							throw new Exception();
						if (traceLineSplit[3].startsWith("classId=")==false)
							throw new Exception();

						String name=traceLineSplit[1].substring(traceLineSplit[1].indexOf("\"")+1,traceLineSplit[1].length()-1);
						String classID=traceLineSplit[3].substring(traceLineSplit[3].indexOf("\"")+1,traceLineSplit[3].length()-1);
						System.out.println(classID+"\t"+name);

						idToClass.put(classID,name);
						continue;
					}
					
					if (currentLine.startsWith("<methodDef "))
					{
						traceLineSplit=currentLine.split(" ");
						if (traceLineSplit[1].startsWith("name=")==false)
							throw new Exception();
						if (traceLineSplit[2].startsWith("signature=")==false)
							throw new Exception();
						if (traceLineSplit[3].startsWith("startLineNumber=")==false)
							throw new Exception();
						if (traceLineSplit[4].startsWith("endLineNumber=")==false)
							throw new Exception();
						if (traceLineSplit[5].startsWith("methodId=")==false)
							throw new Exception();
						if (traceLineSplit[6].startsWith("classIdRef=")==false)
							throw new Exception();

						String name=traceLineSplit[1].substring(traceLineSplit[1].indexOf("\"")+1,traceLineSplit[1].length()-1);
						String signature=traceLineSplit[2].substring(traceLineSplit[2].indexOf("\"")+1,traceLineSplit[2].length()-1);
						String methodID=traceLineSplit[5].substring(traceLineSplit[5].indexOf("\"")+1,traceLineSplit[5].length()-1);
						String classID=traceLineSplit[6].substring(traceLineSplit[6].indexOf("\"")+1,traceLineSplit[6].lastIndexOf("\""));
						System.out.println("#"+name+"\t"+signature+"\t"+methodID+"\t"+classID);
						
						//eliminate names such as "access$123"
						if (name.indexOf("$")>0)
							name=name.substring(0,name.indexOf("$"));
						
						// clinit are for static blocks
						if (name.equals("-clinit-"))
						{
							continue;
						}
						
						if (traceLineSplit[1].equals("name=\"class$\""))
						{
							continue;
						}

						if (name.equals("-init-"))
						{
							String fullClassName=idToClass.get(classID);
							System.out.println("\tfullClassName="+fullClassName);
							String className=fullClassName.substring(fullClassName.lastIndexOf("/")+1);

							// remove "outer" classes from inner classes
							if (className.indexOf("$")>0)
							{
								System.out.println(currentLine);
								className=className.substring(className.lastIndexOf("$")+1);
							}

							name=className;

							System.out.println("\tclassName="+className);
							System.out.println("\tname="+name);
						}
						String fullMethodName=idToClass.get(classID)+"."+name;
						fullMethodName=fullMethodName.replace('/','.');
						fullMethodName=fullMethodName.replace('#','.');
						fullMethodName=fullMethodName.replace('$','.');
						System.out.println("MethodName="+fullMethodName);
						idToMethod.put(methodID,fullMethodName);

						setOfUniqueMethodsTrace.add(fullMethodName);
						continue;
					}
				}
				brTrace.close();
				System.out.println(inputOutput.getFileNameUniqueMethodsTrace(issueID));
				BufferedWriter bwUniqueMethods=new BufferedWriter(new FileWriter(inputOutput.getFileNameUniqueMethodsTrace(issueID)));
				for (String currentMethodWithFullPath:setOfUniqueMethodsTrace)
				{
					bwUniqueMethods.write(currentMethodWithFullPath+"\r\n");
				}
				bwUniqueMethods.flush();
				bwUniqueMethods.close();
				
				System.out.print(" ("+setOfUniqueMethodsTrace.size()+" unique methods)");
			}
		}
		catch (Exception e)
		{
			System.out.println("Line number="+lineNumber);
			System.out.println("Line content="+currentLine);
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public void convertTPTPTracesToBiGramsMethods() throws Exception
	{
		inputOutput.initializeFolderStructure();
		
		ArrayList<String> listOfIssues=inputOutput.loadListOfIssues();
		
		inputOutput.loadCorpusMethodsMappings();
		
		String[] traceLineSplit;
		BufferedReader brTrace;
		String currentLine=null;
		String stringToAdd;
		int lineNumber=0;
		
		try
		{
			for (String issueID : listOfIssues)
			{
				brTrace=new BufferedReader(new FileReader(inputOutput.getFileNameTrace(issueID)));
				System.out.println("Processing File: "+inputOutput.getFileNameTrace(issueID));
				BufferedWriter bw=new BufferedWriter(new FileWriter(inputOutput.getFileNameBiGramsMethodsTrace(issueID)));
				
				int numberOfBiGrams=0;
				lineNumber=0;
				Hashtable<String,String> idToClass=new Hashtable<String,String>();
				Hashtable<String,String> idToMethod=new Hashtable<String,String>();

				Hashtable<String,Stack<PairMethodIDTraceMethodIDCorpus>> threadToMethodsStackTranslator=new Hashtable<String,Stack<PairMethodIDTraceMethodIDCorpus>>();

				while ((currentLine=brTrace.readLine())!=null)
				{
					lineNumber++;
					if (currentLine.startsWith("<classDef "))
					{
						traceLineSplit=currentLine.split(" ");
						if (traceLineSplit[1].startsWith("name=")==false)
							throw new Exception();
						if (traceLineSplit[2].startsWith("sourceName=")==false)
							throw new Exception();
						if (traceLineSplit[3].startsWith("classId=")==false)
							throw new Exception();

						String name=traceLineSplit[1].substring(traceLineSplit[1].indexOf("\"")+1,traceLineSplit[1].length()-1);
						String classID=traceLineSplit[3].substring(traceLineSplit[3].indexOf("\"")+1,traceLineSplit[3].length()-1);
						System.out.println(classID+"\t"+name);

						idToClass.put(classID,name);
						continue;
					}
					
					if (currentLine.startsWith("<methodDef "))
					{
						traceLineSplit=currentLine.split(" ");
						if (traceLineSplit[1].startsWith("name=")==false)
							throw new Exception();
						if (traceLineSplit[2].startsWith("signature=")==false)
							throw new Exception();
						if (traceLineSplit[3].startsWith("startLineNumber=")==false)
							throw new Exception();
						if (traceLineSplit[4].startsWith("endLineNumber=")==false)
							throw new Exception();
						if (traceLineSplit[5].startsWith("methodId=")==false)
							throw new Exception();
						if (traceLineSplit[6].startsWith("classIdRef=")==false)
							throw new Exception();

						String name=traceLineSplit[1].substring(traceLineSplit[1].indexOf("\"")+1,traceLineSplit[1].length()-1);
						String signature=traceLineSplit[2].substring(traceLineSplit[2].indexOf("\"")+1,traceLineSplit[2].length()-1);
						String methodID=traceLineSplit[5].substring(traceLineSplit[5].indexOf("\"")+1,traceLineSplit[5].length()-1);
						String classID=traceLineSplit[6].substring(traceLineSplit[6].indexOf("\"")+1,traceLineSplit[6].lastIndexOf("\""));
						System.out.println("#"+name+"\t"+signature+"\t"+methodID+"\t"+classID);
						
						//leave all the names in; the methods that are not in the corpus will be eliminated because they will not have an ID  

						if (name.equals("-init-"))
						{
							String fullClassName=idToClass.get(classID);
							System.out.println("\tfullClassName="+fullClassName);
							String className=fullClassName.substring(fullClassName.lastIndexOf("/")+1);

							// remove "outer" classes from inner classes
							if (className.indexOf("$")>0)
							{
								System.out.println(currentLine);
								className=className.substring(className.lastIndexOf("$")+1);
							}

							name=className;

							System.out.println("\tclassName="+className);
							System.out.println("\tname="+name);
						}
						String fullMethodName=idToClass.get(classID)+"#"+name;
						fullMethodName=fullMethodName.replace('/','.');
						fullMethodName=fullMethodName.replace('#','.');
						fullMethodName=fullMethodName.replace('$','.');
						System.out.println("MethodName="+fullMethodName);
						idToMethod.put(methodID,fullMethodName);

						continue;
					}
					
					if (currentLine.startsWith("<methodEntry "))
					{
						traceLineSplit=currentLine.split(" ");
						if (traceLineSplit[1].startsWith("threadIdRef=")==false)
							throw new Exception();
						if (traceLineSplit[2].startsWith("time=")==false)
							throw new Exception();
						if (traceLineSplit[3].startsWith("methodIdRef=")==false)
							throw new Exception();
						if (traceLineSplit[4].startsWith("classIdRef=")==false)
							throw new Exception();
						if (traceLineSplit[5].startsWith("ticket=")==false)
							throw new Exception();
						if (traceLineSplit[6].startsWith("stackDepth=")==false)
							throw new Exception();
						
						String threadID=traceLineSplit[1].substring(traceLineSplit[1].indexOf("\"")+1,traceLineSplit[1].length()-1);
						String methodIDTrace=traceLineSplit[3].substring(traceLineSplit[3].indexOf("\"")+1,traceLineSplit[3].length()-1);

						Stack<PairMethodIDTraceMethodIDCorpus> currentThreadMethodsStack=threadToMethodsStackTranslator.get(threadID);
						if (currentThreadMethodsStack==null)
						{
							currentThreadMethodsStack=new Stack<PairMethodIDTraceMethodIDCorpus>();
							threadToMethodsStackTranslator.put(threadID,currentThreadMethodsStack);
						}
						
						String fullMethodName=idToMethod.get(methodIDTrace);
						String IDCorpusForFullMethodName=inputOutput.getPositionOfMethodMappingInCorpus(fullMethodName);
						
						PairMethodIDTraceMethodIDCorpus parentMethod=null;
						try
						{
							parentMethod=currentThreadMethodsStack.peek();
						}
						catch (EmptyStackException e) 
						{
						}
						
						PairMethodIDTraceMethodIDCorpus childMethod=new PairMethodIDTraceMethodIDCorpus(methodIDTrace,fullMethodName,IDCorpusForFullMethodName);
						currentThreadMethodsStack.push(childMethod);
						
						if (currentThreadMethodsStack.size()==1)
						{
							//if its only 1 element, it couldn't have been called by anything
							continue;
						}

						if (parentMethod.methodIDCorpus.equals("-1")||childMethod.methodIDCorpus.equals("-1"))
							continue;
						
						//do not add "recursive calls". This is due to the fact that these are calls between overwritten methods 
						if (parentMethod.methodIDCorpus.equals(childMethod.methodIDCorpus))
							continue;
						
						stringToAdd=parentMethod.methodName+"\t"+childMethod.methodName+"\t"+parentMethod.methodIDCorpus+"\t"+childMethod.methodIDCorpus;
						bw.write(stringToAdd+"\r\n");
						stringToAdd=null;
						numberOfBiGrams++;
						
						continue;
					}
					
					if (currentLine.startsWith("<methodExit "))
					{
						traceLineSplit=currentLine.split(" ");
						if (traceLineSplit[1].startsWith("threadIdRef=")==false)
							throw new Exception();
						if (traceLineSplit[2].startsWith("methodIdRef=")==false)
							throw new Exception();
						if (traceLineSplit[3].startsWith("classIdRef=")==false)
							throw new Exception();
						if (traceLineSplit[4].startsWith("ticket=")==false)
							throw new Exception();
						if (traceLineSplit[5].startsWith("time=")==false)
							throw new Exception();
						
						String threadID=traceLineSplit[1].substring(traceLineSplit[1].indexOf("\"")+1,traceLineSplit[1].length()-1);
						String methodIDTrace=traceLineSplit[2].substring(traceLineSplit[2].indexOf("\"")+1,traceLineSplit[2].length()-1);
					
						Stack<PairMethodIDTraceMethodIDCorpus> currentThreadMethodsStack=threadToMethodsStackTranslator.get(threadID);
						PairMethodIDTraceMethodIDCorpus topOfStack=currentThreadMethodsStack.peek();
						if (topOfStack.methodIDTrace.equals(methodIDTrace)==false)
							throw new Exception(currentLine+"\r\n"+currentThreadMethodsStack);
						currentThreadMethodsStack.pop();

						continue;
					}
				}
				brTrace.close();
				System.out.println(inputOutput.getFileNameBiGramsMethodsTrace(issueID));
				bw.flush();
				bw.close();
				
				System.out.println(" ("+numberOfBiGrams+" bigrams)");
			}
		}
		catch (Exception e)
		{
			System.out.println("Line number="+lineNumber);
			System.out.println("Line content="+currentLine);
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
