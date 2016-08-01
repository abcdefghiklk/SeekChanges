package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import Jama.Matrix;
import bug.BugRecord;

public class FileUtils {
	/**
	 *  Appends a given textual contents to an existing text file.
	 *  Creates the file if it does not exist before adding textual contents
	 * @param str Given textual contents
	 * @param outputFilepath Full path of text file to add textual contents
	 */
	public static void write_append2file(String str, String outputFilepath) {
		try {
			File f = new File(outputFilepath);
			
			if (!f.exists()) {
				BufferedWriter bw = new BufferedWriter(new FileWriter(f));
				bw.write("");
				bw.close();
			}
			FileWriter writer = new FileWriter(f,true);
//			System.out.println(writer.getEncoding());;
			BufferedWriter bw = new BufferedWriter(writer);
			bw.append(str);
			bw.close();

		} catch (Exception e) {
			System.out.println("Writing to file error- " + e);
		}
	}
	public static void fromText2XML(String inputFilePath, String outputFilePath) throws Exception{
		BufferedReader reader=new BufferedReader(new FileReader(inputFilePath));
		String bugID=null;
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder domBuilder= domFactory.newDocumentBuilder();
		Document doc=domBuilder.newDocument();
		Element rootNode=doc.createElement("bugrepository");
		doc.appendChild(rootNode);
		while((bugID=reader.readLine())!=null){
			Element _bugNode=doc.createElement("bug");
			_bugNode.setAttribute("id", bugID);
//			_bugNode.setAttribute("opendate", DateFormat.getFormat().format(_bug.getOpenDate()));
//			_bugNode.setAttribute("fixdate", DateFormat.getFormat().format(_bug.getFixDate()));
			System.out.println(bugID);
			String []strs=reader.readLine().split("  ");
//			System.out.println(strs[0]);
			String bugSummary=strs[0];
			String bugDescription=strs[1];
			Element _bugInformationNode=doc.createElement("buginformation");
			
			Element _summaryNode=doc.createElement("summary");
			_summaryNode.appendChild(doc.createTextNode(bugSummary));
			_bugInformationNode.appendChild(_summaryNode);
				
			Element _descriptionNode=doc.createElement("description");
			_descriptionNode.appendChild(doc.createTextNode(bugDescription));
			_bugInformationNode.appendChild(_descriptionNode);
			
			_bugNode.appendChild(_bugInformationNode);
			int fixedDocsNum=Integer.parseInt(reader.readLine().trim());
			
			Element _fixedFilesNode=doc.createElement("fixedFiles");
				
			rootNode.appendChild(_bugNode);
			for(int i=0;i<fixedDocsNum;i++){
				Element _oneFixedFileNode=doc.createElement("file");
				_oneFixedFileNode.appendChild(doc.createTextNode(reader.readLine().trim()));
				_fixedFilesNode.appendChild(_oneFixedFileNode);
			}
			_bugNode.appendChild(_fixedFilesNode);
			_bugNode.setAttribute("opendate", reader.readLine().trim());
			_bugNode.setAttribute("fixdate", reader.readLine().trim());
			reader.readLine();
			rootNode.appendChild(_bugNode);
		}
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer;
		transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File(outputFilePath));
		transformer.transform(source, result);
		reader.close();
	}
	
	public static void deleteDir(File file) {
	    File[] contents = file.listFiles();
	    if (contents != null) {
	        for (File f : contents) {
	            deleteDir(f);
	        }
	    }
	    file.delete();
	}
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
//		String []projectNames={"ArgoUML-0.22","JabRef-2.6"/*,"jEdit-4.3","muCommander-0.8.5","openjpa-2.2.0","zookeeper-3.4.5"*/};
//		for(String projectName:projectNames){
//			String inputFile="C:/Users/dell/git/mct_txl_nicad/mct_txl_nicad/datasets/"+projectName+"/"+projectName+"_Original_Queries.txt";
//			String outputFile="C:/Users/dell/git/mct_txl_nicad/mct_txl_nicad/datasets/"+projectName+"/"+projectName+"_Repository.xml";
//			System.out.println("processing the project:"+projectName);
//			fromText2XML(inputFile,outputFile);
//		}
		Matrix mat=Matrix.random(3,3);
		int []A={0,2};
		
		Matrix subMat=mat.getMatrix(A, A);
		System.out.println(subMat.get(1, 1));
		System.out.println(mat.get(2, 2));
	}

}
