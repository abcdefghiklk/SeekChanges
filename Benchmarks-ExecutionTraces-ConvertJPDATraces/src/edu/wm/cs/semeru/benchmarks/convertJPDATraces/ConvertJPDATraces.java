package edu.wm.cs.semeru.benchmarks.convertJPDATraces;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Stack;

class PairMethodID
{
	String method;
	String id;
	
	public PairMethodID(String method,String id)
	{
		this.method=method;
		this.id=id;
	}

	@Override
	public String toString()
	{
		return "PairMethodID [id="+id+", method="+method+"]";
	}
}

public class ConvertJPDATraces
{
	private InputOutputConvertJPDATraces inputOutput;

	public ConvertJPDATraces(InputOutputConvertJPDATraces inputOutput)
	{
		this.inputOutput=inputOutput;
	}
	
	public void convertJPDATracesToUniqueMethods() throws Exception
	{
		inputOutput.initializeFolderStructure();
		
		ArrayList<String> listOfIssues=inputOutput.loadListOfIssues();

		String[] traceLineSplit;
		BufferedReader brTrace;
		String currentLine=null;
		String methodName;
		String methodPath;
		int lineNumber=0;
		
		boolean wasProgressIndicatorActivated=false;

		BufferedWriter bwUniqueMethods;
		BufferedWriter bwDebug;

		try
		{
			for (String issueID : listOfIssues)
			{
				brTrace=new BufferedReader(new FileReader(inputOutput.getFileNameTrace(issueID)));
				bwDebug=new BufferedWriter(new FileWriter(inputOutput.getFileNameUniqueMethodsTraceDebug(issueID)));
				if (wasProgressIndicatorActivated)
				{
					wasProgressIndicatorActivated=false;
					System.out.println();
				}
				System.out.println("Processing File: "+inputOutput.getFileNameTrace(issueID));
				HashSet<String> uniqueMethods=new HashSet<String>();

				while ((currentLine=brTrace.readLine())!=null)
				{
					if (currentLine.equals("-- VM Started --"))
						continue;
					if (currentLine.equals("m-- VM Started --"))
						continue;
					if (currentLine.equals("-- The application exited --"))
						continue;
					if (currentLine.length()==1)
						continue;

					lineNumber++;
//					System.out.println(currentLine);
					traceLineSplit=currentLine.split("\t");

					if (traceLineSplit.length>=3)
					{
						System.err.println("Error at line "+lineNumber+"\n"+currentLine);
						System.exit(-1);
					}

					if (traceLineSplit[1].charAt(0)=='=')
					{
						// do nothing. This is a report line like ===== main =====
						continue;
					}

					String[] buf=traceLineSplit[1].split("  --  ");
					methodName=buf[0];
					methodPath=buf[1];
					// System.out.println(methodPath+"."+methodName);

					//leave method names like method14, but eliminate everything after a dollar (e.g., method$1)
					int indexOfDollar=methodName.indexOf('$');
					if (indexOfDollar>=0)
					{
//						System.out.print(methodName+"->");
						methodName=methodName.substring(0,indexOfDollar);
//						System.out.println(methodName);
					}

					indexOfDollar=methodPath.indexOf('$');
					if (indexOfDollar>=0)
					{
//						System.out.print(methodPath+"->");
						methodPath=methodPath.replace('$','.');
//						methodPath=methodPath.substring(0,indexOfDollar);
//						System.out.println(methodPath);
					}
					
					if (methodName.equals("<init>"))
					{
//						System.out.print("<init>"+"->");
						methodName=methodPath.substring(methodPath.lastIndexOf(".")+1);
//						System.out.println(methodName);
					}
					
					if (methodName.equals("<clinit>"))
					{
//						System.out.print("<cinit>"+"->");
						methodName=methodPath.substring(methodPath.lastIndexOf(".")+1);
//						System.out.println(methodName);
					}

					String currentMethodWithFullPath=methodPath+"."+methodName;
//					String currentMethodWithFullPath=methodPath.toLowerCase().replaceAll("[^a-z]","")+"#"+methodName.toLowerCase().replaceAll("[^a-z0-9]","");

					uniqueMethods.add(currentMethodWithFullPath);
					bwDebug.write(currentMethodWithFullPath+"\n");

					if (lineNumber%10000==0)
					{
						wasProgressIndicatorActivated=true;
						System.out.print("#");
					}
				}
				brTrace.close();

				bwUniqueMethods=new BufferedWriter(new FileWriter(inputOutput.getFileNameUniqueMethodsTrace(issueID)));
				for (String currentMethodWithFullPath:uniqueMethods)
				{
					bwUniqueMethods.write(currentMethodWithFullPath+"\r\n");
				}
				bwUniqueMethods.flush();
				bwUniqueMethods.close();

				bwDebug.close();
				
				System.out.print(" ("+uniqueMethods.size()+" unique methods)");
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
	
	public void convertJPDATracesToBiGramsMethods() throws Exception
	{
		inputOutput.initializeFolderStructure();
		
		ArrayList<String> listOfIssues=inputOutput.loadListOfIssues();
		
		inputOutput.loadCorpusMethodsMappings();

		String[] traceLineSplit;
		BufferedReader brTrace;
		String currentLine=null;
		int numberOfPipes=0;
		String methodName;
		String methodPath;
		int lineNumber=0;
		
		boolean wasProgressIndicatorActivated=false;

		
		BufferedWriter bw;
		String stringToAdd;
		
		try
		{
			for (String issueID : listOfIssues)
			{
				brTrace=new BufferedReader(new FileReader(inputOutput.getFileNameTrace(issueID)));
				if (wasProgressIndicatorActivated)
				{
					wasProgressIndicatorActivated=false;
					System.out.println();
				}
				System.out.println("Processing File: "+inputOutput.getFileNameTrace(issueID));
				bw=new BufferedWriter(new FileWriter(inputOutput.getFileNameBiGramsMethodsTrace(issueID)));
				int numberOfBiGrams=0;
				int numberOfInconsistencies=0;
				Hashtable<String,ArrayList<PairMethodID>> threadToMethodsStackTranslator=new Hashtable<String,ArrayList<PairMethodID>>();

				while ((currentLine=brTrace.readLine())!=null)
				{
					if (currentLine.equals("-- VM Started --"))
						continue;
					if (currentLine.equals("m-- VM Started --"))
						continue;
					if (currentLine.equals("-- The application exited --"))
						continue;
					if (currentLine.length()==1)
						continue;

					lineNumber++;
//					System.out.println(currentLine);
					traceLineSplit=currentLine.split("\t");

					if (traceLineSplit.length>=3)
					{
						System.err.println("Error at line "+lineNumber+"\n"+currentLine);
						System.exit(-1);
					}

					if (traceLineSplit[1].charAt(0)=='=')
					{
						// do nothing. This is a report line like ===== main =====
						continue;
					}
					
					String threadName=traceLineSplit[0].substring(0,traceLineSplit[0].indexOf(':'));
//					if (threadName.equals("org.eclipse.jdt.internal.ui.text.JavaReconciler"))
//					{
//						continue;
//					}
					
					ArrayList<PairMethodID> currentThreadMethodsStack=threadToMethodsStackTranslator.get(threadName);
					if (currentThreadMethodsStack==null)
					{
						currentThreadMethodsStack=new ArrayList<PairMethodID>();
						threadToMethodsStackTranslator.put(threadName,currentThreadMethodsStack);
					}
//					System.out.println("Thread:"+threadName);
					numberOfPipes=0;
					// count number of pipes "|"
					for (int i=0;i<traceLineSplit[0].length();i++)
					{
						if (traceLineSplit[0].charAt(i)=='|')
						{
							numberOfPipes++;
						}
					}
					
					String[] buf=traceLineSplit[1].split("  --  ");
					methodName=buf[0];
					methodPath=buf[1];
					// System.out.println(methodPath+"."+methodName);

					//leave method names like method14, but eliminate everything after a dollar (e.g., method$1)
					int indexOfDollar=methodName.indexOf('$');
					if (indexOfDollar>=0)
					{
//						System.out.print(methodName+"->");
						methodName=methodName.substring(0,indexOfDollar);
//						System.out.println(methodName);
					}

					indexOfDollar=methodPath.indexOf('$');
					if (indexOfDollar>=0)
					{	
//						System.out.print(methodPath+"->");
						methodPath=methodPath.replace('$','.');
//						methodPath=methodPath.substring(0,indexOfDollar);
//						System.out.println(methodPath);
					}
					
					if (methodName.equals("<init>"))
					{
//						System.out.print("<init>"+"->");
						methodName=methodPath.substring(methodPath.lastIndexOf(".")+1);
//						System.out.println(methodName);
					}
					
					if (methodName.equals("<clinit>"))
					{
//						System.out.print("<cinit>"+"->");
						methodName=methodPath.substring(methodPath.lastIndexOf(".")+1);
//						System.out.println(methodName);
					}
					
					if (numberOfPipes==0)	
					{
						currentThreadMethodsStack.clear();
						String fullMethodName=methodPath+"."+methodName;
						String IDFullMethodName=inputOutput.getPositionOfMethodMappingInCorpus(fullMethodName);
						
						
						currentThreadMethodsStack.add(numberOfPipes,new PairMethodID(fullMethodName,IDFullMethodName));
						//if its 0, it couldn't have been called by anything
						continue;
					}
					
					while (currentThreadMethodsStack.size()>numberOfPipes)
					{
						currentThreadMethodsStack.remove(numberOfPipes);
					}
										
//					if (threadName.equals("org.eclipse.jdt.internal.ui.text.JavaReconciler"))
//					{
//						//code to deal with the Reconciler thread which sometimes has methods that start with 20-30 pipes and the size of the stack traces is less than that
//						//solution: add an unknownMethod
//						
//						if (currentThreadMethodsStack.size()<numberOfPipes)
//						{
//							int numberOfUnknownMethodsToAdd=numberOfPipes-currentThreadMethodsStack.size();
//							for (int i=0;i<numberOfUnknownMethodsToAdd;i++)
//								currentThreadMethodsStack.add("orgeclipsejdtinternaluitextjavareconciler#unknownMethod");
//						}
//					}
					
					if (currentThreadMethodsStack.size()<numberOfPipes)
					{
						numberOfInconsistencies++;
//						System.out.println(currentLine);
						continue;
					}
					String fullMethodName=methodPath+"."+methodName;
					String IDFullMethodName=inputOutput.getPositionOfMethodMappingInCorpus(fullMethodName);
					currentThreadMethodsStack.add(numberOfPipes,new PairMethodID(fullMethodName,IDFullMethodName));

					PairMethodID parentMethod=currentThreadMethodsStack.get(numberOfPipes-1);
					PairMethodID childMethod=currentThreadMethodsStack.get(numberOfPipes);
					if (parentMethod.id.equals("-1")||childMethod.id.equals("-1"))
						continue;
					
					stringToAdd=parentMethod.method+"\t"+childMethod.method+"\t"+parentMethod.id+"\t"+childMethod.id;
					bw.write(stringToAdd+"\r\n");
					stringToAdd=null;
					numberOfBiGrams++;
					
					if (lineNumber%10000==0)
					{
						wasProgressIndicatorActivated=true;
						System.out.print("#");
					}
				}
				brTrace.close();

				bw.flush();
				bw.close();
				
				System.out.println(" ("+numberOfBiGrams+" bigrams, "+numberOfInconsistencies+" inconsistencies)");
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
