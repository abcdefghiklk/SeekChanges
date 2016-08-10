package edu.wm.cs.semeru.CorpusGenerator;

import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class ParserCorpusMethodLevelGranularity
{
	private String fileContent;
	private String packageName;
	private InputOutputCorpusMethodLevelGranularity inputOutput;
	
	public ParserCorpusMethodLevelGranularity(InputOutputCorpusMethodLevelGranularity inputOutput,String fileContent)
	{
		this.inputOutput=inputOutput;
		this.fileContent=fileContent;
		this.packageName="";
	}

	public CompilationUnit parseSourceCode()
	{
		char[] fileContentAsChar=fileContent.toCharArray();
		ASTParser parser=ASTParser.newParser(AST.JLS4);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(fileContentAsChar);
		return (CompilationUnit)parser.createAST(null);
	}
	
	public void exploreSourceCode(CompilationUnit compilationUnitSourceCode)
	{
		packageName=compilationUnitSourceCode.getPackage().getName().toString();
		
		List<ASTNode> declaredTypes=compilationUnitSourceCode.types();

		for (ASTNode currentDeclaredType:declaredTypes)
		{
			//if node is a class
			if (currentDeclaredType.getNodeType()==ASTNode.TYPE_DECLARATION)
			{
				TypeDeclaration typeDeclaration=(TypeDeclaration)currentDeclaredType;
//				if (typeDeclaration.isInterface())
//				{
//					//record the interfaces if needed 
//				}
				exploreClassContents((TypeDeclaration)currentDeclaredType,"");
			}
		}
	}
	
	private void exploreClassContents(TypeDeclaration classNode, String prefixClass)
	{
		List<ASTNode> bodyDeclarations=classNode.bodyDeclarations();

		SimpleName className=classNode.getName();
		
		String fullClassName=prefixClass+className+".";

		for (ASTNode bodyDeclaration:bodyDeclarations)
		{
			//explore method contents 
			if (bodyDeclaration.getNodeType()==ASTNode.METHOD_DECLARATION)
			{
				exploreMethodContents((MethodDeclaration)bodyDeclaration,fullClassName);
			}

			//recursively explore inner classes
			if (bodyDeclaration.getNodeType()==ASTNode.TYPE_DECLARATION)
			{
				exploreClassContents((TypeDeclaration)bodyDeclaration,fullClassName);
			}
		}
	}
	
	private void exploreMethodContents(MethodDeclaration methodDeclaration, String fullClassName)
	{
		String currentMethodName=methodDeclaration.getName().getFullyQualifiedName();
		String idMethod=fullClassName+currentMethodName;
//		System.out.println("ID="+idMethod);

		//ignore a method declared inside an interface
		//this allows for methods declared inside classes declared inside interfaces to be indexed
		TypeDeclaration parentOfMethod=(TypeDeclaration)methodDeclaration.getParent();
		if (parentOfMethod.isInterface())
			return;
		
		//detect abstract methods and ignore them
		if (Modifier.isAbstract(methodDeclaration.getModifiers()))
		{
			System.out.println("ID="+idMethod);
			System.err.println("Abstract method (ignored) "+"ID="+idMethod);
			return;
		}
		                                          
		List<SingleVariableDeclaration> listOfParameters=methodDeclaration.parameters();
		idMethod+="\t"+listOfParameters.size()+"\t";
		for (SingleVariableDeclaration p:listOfParameters)
		{
			idMethod+=p.getType()+"\t";
		}

		int methodStartPosition=methodDeclaration.getStartPosition();
		int methodLength=methodDeclaration.getLength();
		String methodContents=fileContent.substring(methodStartPosition,methodStartPosition+methodLength);
//		System.out.println(methodContentFromSubstring);

		String idMethodWithPackageSeparator=packageName+"$"+idMethod;
		idMethod=packageName+"."+idMethod;
		
		//save to mapping
		inputOutput.appendToCorpusMapping(convertMethodIDToFinalFormat(idMethod));
		inputOutput.appendToCorpusMappingWithPackageSeparator(convertMethodIDToFinalFormat(idMethodWithPackageSeparator));

		//save method contents
		inputOutput.appendToCorpusRaw(convertMultipleLinesToSingleLines(methodContents));

		//save debug information to CorpusRawAndMappingDebug
		inputOutput.appendToCorpusDebug(idMethod);
		inputOutput.appendToCorpusDebug(convertMultipleLinesToSingleLines(methodContents)+InputOutput.LINE_ENDING);
	}
	
	
	private String convertMultipleLinesToSingleLines(String methodContentsMultipleLines)
	{
		StringBuilder methodContentsSingleLine=new StringBuilder();
		String[] splittedLines=methodContentsMultipleLines.split("\r\n");
		
		for (String buf:splittedLines)
		{
			methodContentsSingleLine.append(buf+"\t");
		}

		return methodContentsSingleLine.toString();
	}

	
	private static String convertMethodIDToFinalFormat(String idMethod)
	{
		String[] splittedBuf=idMethod.split("\t");
		String methodNameFullPath=splittedBuf[0];
		String methodNameFullPathFinal=methodNameFullPath+"(";
		int numberOfParameters=Integer.parseInt(splittedBuf[1]);

		if (numberOfParameters!=0)
		{
			for (int indexParameter=0;indexParameter<numberOfParameters-1;indexParameter++)
			{
				methodNameFullPathFinal+=splittedBuf[indexParameter+2]+",";

			}
			methodNameFullPathFinal+=splittedBuf[numberOfParameters+1];
		}
		methodNameFullPathFinal+=")";

		return methodNameFullPathFinal;
	}
}
