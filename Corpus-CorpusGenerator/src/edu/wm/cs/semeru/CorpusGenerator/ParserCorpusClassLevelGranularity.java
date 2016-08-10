package edu.wm.cs.semeru.CorpusGenerator;

import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class ParserCorpusClassLevelGranularity
{
	private String fileContent;
	private String packageName;
	private InputOutputCorpusClassLevelGranularity inputOutput;
	
	public ParserCorpusClassLevelGranularity(InputOutputCorpusClassLevelGranularity inputOutput,String fileContent)
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
	
	public void exploreSourceCodeClassLevelGranularity(CompilationUnit compilationUnitSourceCode)
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
				exploreClassContentsClassLevelGranularity((TypeDeclaration)currentDeclaredType,"");
			}
		}
	}
	
	private void exploreClassContentsClassLevelGranularity(TypeDeclaration classNode, String prefixClass)
	{
		String idClass=packageName+"."+classNode.getName();
		System.out.println("ID="+idClass);
		System.out.println(classNode.getStartPosition());
		System.out.println(classNode.getLength());
		String classContent=fileContent.substring(classNode.getStartPosition(),classNode.getStartPosition()+classNode.getLength());
		System.out.println(classContent);

//		handle interfaces
//		if (classNode.isInterface())
//		{
//			System.out.println("is interface");
//			InputOutput.appendToFile(outputFileListOfInterfaces,idClass);
//		}

		//save to mapping
		inputOutput.appendToCorpusMapping(idClass);

		//save method contents
		inputOutput.appendToCorpusRaw(convertMultipleLinesToSingleLinesWithReplace(classContent));

		//save debug information to CorpusRawAndMappingDebug
		inputOutput.appendToCorpusDebug(idClass);
		inputOutput.appendToCorpusDebug(convertMultipleLinesToSingleLinesWithReplace(classContent)+InputOutput.LINE_ENDING);
	}
	
	private String convertMultipleLinesToSingleLinesWithReplace(String methodContentsMultipleLines)
	{
		String methodContentsMultipleLinesOutput=methodContentsMultipleLines;
		methodContentsMultipleLinesOutput=methodContentsMultipleLinesOutput.replace("\r","\t");
		methodContentsMultipleLinesOutput=methodContentsMultipleLinesOutput.replace("\n","\t");

		return methodContentsMultipleLinesOutput;
	}
}
