package edu.wm.cs.semeru.CorpusGenerator;

import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class ParserCorpusFileLevelGranularity
{
	private String fileContent;
	private String packageName;
	private InputOutputCorpusFileLevelGranularity inputOutput;
	private String inputFileName;	//the input fileName will serve as the ID
	
	public ParserCorpusFileLevelGranularity(InputOutputCorpusFileLevelGranularity inputOutput,String fileContent,String inputFileName)
	{
		this.inputOutput=inputOutput;
		this.fileContent=fileContent;
		this.inputFileName=inputFileName;
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
	
	public void exploreSourceCodeFileLevelGranularity(CompilationUnit compilationUnitSourceCode)
	{
		packageName=compilationUnitSourceCode.getPackage().getName().toString();
		
		List<ASTNode> declaredTypes=compilationUnitSourceCode.types();

		StringBuilder bufMultipleClassesInSameFile=new StringBuilder();
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
				bufMultipleClassesInSameFile.append(exploreClassContentsFileLevelGranularity(typeDeclaration,"")+"\t");
			}
		}
		
		//save to mapping
		inputOutput.appendToCorpusMapping(inputFileName.replace("\\","/"));
		
		//save method contents
		inputOutput.appendToCorpusRaw(bufMultipleClassesInSameFile.toString());
	}
	
	private String exploreClassContentsFileLevelGranularity(TypeDeclaration classNode, String prefixClass)
	{
		String idClass=packageName+"."+classNode.getName();
		System.out.println("ID="+idClass);
//		System.out.println(classNode.getStartPosition());
//		System.out.println(classNode.getLength());
		String classContent=fileContent.substring(classNode.getStartPosition(),classNode.getStartPosition()+classNode.getLength());
//		System.out.println(classContent);

		String classContentSingleLine=convertMultipleLinesToSingleLinesWithReplace(classContent);

		//save debug information to CorpusRawAndMappingDebug
		inputOutput.appendToCorpusDebug(idClass);
		inputOutput.appendToCorpusDebug(classContentSingleLine+InputOutput.LINE_ENDING);
		
		return classContentSingleLine;
	}
	
	private String convertMultipleLinesToSingleLinesWithReplace(String methodContentsMultipleLines)
	{
		String methodContentsMultipleLinesOutput=methodContentsMultipleLines;
		methodContentsMultipleLinesOutput=methodContentsMultipleLinesOutput.replace("\r","\t");
		methodContentsMultipleLinesOutput=methodContentsMultipleLinesOutput.replace("\n","\t");

		return methodContentsMultipleLinesOutput;
	}
}
