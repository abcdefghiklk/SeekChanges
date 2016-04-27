package webcrawler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import webdata.ApacheJira;
import webdata.SourceForge;

public class BugTrackingSystemData {
	private String _id;
	private String _summary;
	private String _description;
	private String _status;
	private String _owner;
	private String _creator;
	private int _priority;
	private String _privateOrNot;
	private Date _createDate;
	private Date _updateDate;
	private ArrayList<BugComment> _commentList;
	private HashSet<String> _fixedFileList;
	BugTrackingSystemData(){
		this._summary=null;
		this._description=null;
		this._status=null;
		this._owner=null;
		this._creator=null;
		this._priority=0;
		this._privateOrNot=null;
		this._createDate=null;
		this._updateDate=null;
		this._commentList=new ArrayList<BugComment> ();
		this._fixedFileList=new HashSet<String> ();
	}
	
	public void setID(String id){
		this._id=id;
	}
	public String getID(){
		return this._id;
	}
	public void setSummary(String summary){
		this._summary=summary;
	}
	
	public String getSummary(){
		return this._summary;
	}
	
	public void setDescription(String description){
		this._description=description;
	}
	
	public String getDescription(){
		return this._description;
	}
	
	public void setStatus(String status){
		this._status=status;
	}
	
	public String getStatus(){
		return this._status;
	}
	
	public void setOwner(String owner){
		this._owner=owner;
	}
	
	public String getOwner(){
		return this._owner;
	}
	
	public void setCreator(String creator){
		this._creator=creator;
	}
	
	public String getCreator(){
		return this._creator;
	}
	
	public void setPriority(int priority){
		this._priority=priority;
	}
	
	public int getPriority(){
		return this._priority;
	}
	
	public void setPrivateOrNot(String privateOrNot){
		this._privateOrNot=privateOrNot;
	}
	
	public String getPrivateOrNot(){
		return this._privateOrNot;
	}
	
	public void setCreateDate(Date createDate){
		this._createDate=createDate;
	}
	
	public Date getCreateDate(){
		return this._createDate;
	}
	
	public void setUpdateDate(Date updateDate){
		this._updateDate=updateDate;
	}
	
	public Date getUpdateDate(){
		return this._updateDate;
	}
	
	public void setCommentList(ArrayList<BugComment> commentList){
		this._commentList=commentList;
	}
	
	public ArrayList<BugComment> getCommentList(){
		return this._commentList;
	}
	
	public void addOneComment(BugComment comment){
		this._commentList.add(comment);
	}
	
	public void setFixedFileList(HashSet<String> fixedFileList){
		this._fixedFileList=fixedFileList;
	}
	
	public HashSet<String> getFixedFileList(){
		return this._fixedFileList;
	}
	
	public void addOneFixedFile(String oneFixedFile){
		this._fixedFileList.add(oneFixedFile);
	}
	public void print(DateFormat format){
//		DateFormat f = new SimpleDateFormat("EEE MMM dd, YYYY hh:mm a Z");
		System.out.println("id:"+this._id+"\nsummary:"+this._summary+ "\ndescription:"+this._description);
		System.out.println("Status="+this._status+"\n"+"Owner="
				+this._owner+"\n"+"Creator="+this._creator+"\n"+"Priority="
				+this._priority+"\n"+"Private="+this._privateOrNot+"\n"
				+"createdDate="+format.format(this._createDate)+"\n"+"updatedDate="+format.format(this._updateDate));
		int i=1;
		for(BugComment comment:this._commentList){
			System.out.println("for commentor "+i+" :"+"commentorName:"+comment.getCommentor()+
						"\ncomment Date is:"+ format.format(comment.getCommentDate())+"\ncontent:"+ comment.getCommentContent());
			i++;
		}
		System.out.println("fixedFileList:");
		for(String file:this._fixedFileList){
			System.out.println(file);
		}
	}
	
	public static void Export2BugLocatorInput(String XMLFilePath, ArrayList<BugTrackingSystemData> bugRecordList) throws Exception{
		DateFormat format=new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder domBuilder= domFactory.newDocumentBuilder();
		Document doc=domBuilder.newDocument();
		Element rootNode=doc.createElement("bugrepository");
		doc.appendChild(rootNode);
		for(BugTrackingSystemData systemData: bugRecordList){
			
			Element _bugNode=doc.createElement("bug");
			_bugNode.setAttribute("id", systemData.getID());
//			_bugNode.setAttribute("opendate", DateFormat.getFormat().format(_bug.getOpenDate()));
//			_bugNode.setAttribute("fixdate", DateFormat.getFormat().format(_bug.getFixDate()));
			Element _bugInformationNode=doc.createElement("buginformation");
			
			Element _summaryNode=doc.createElement("summary");
			_summaryNode.appendChild(doc.createTextNode(systemData.getSummary()));
			_bugInformationNode.appendChild(_summaryNode);
				
			Element _descriptionNode=doc.createElement("description");
			_descriptionNode.appendChild(doc.createTextNode(systemData.getDescription()));
			_bugInformationNode.appendChild(_descriptionNode);
			
			_bugNode.appendChild(_bugInformationNode);			
			_bugNode.setAttribute("opendate", format.format(systemData.getCreateDate()));
			_bugNode.setAttribute("fixdate", format.format(systemData.getUpdateDate()));
			Element _fixedFilesNode=doc.createElement("fixedFiles");
			int validFixedFileCount=0;
			for(String oneFixedFile: systemData.getFixedFileList()){
				if(oneFixedFile.endsWith(".java") && !oneFixedFile.contains("InvalidAuthProvider") && !oneFixedFile.contains("ClientCnxnSocketNIOTest") && !oneFixedFile.contains("ludwig")){
					Element _oneFixedFileNode=doc.createElement("file");
					String fullFilePath=oneFixedFile.replace("/", ".");
//					boolean started=false;
//					String [] packagePaths=fullFilePath.split("\\.");
//					String validFileName="org";
//					for(int k=0;k<packagePaths.length;k++){
//						if(started){
//							validFileName+="."+packagePaths[k];
//						}
//						if(packagePaths[k].equals("org")){
//							started=true;
//						}		
//					}
					String validFileName=fullFilePath;
					_oneFixedFileNode.appendChild(doc.createTextNode(validFileName));
					_fixedFilesNode.appendChild(_oneFixedFileNode);
					validFixedFileCount++;
				}
			}
			_bugNode.appendChild(_fixedFilesNode);
			if(validFixedFileCount>0){
				rootNode.appendChild(_bugNode);
			}
		}
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer;
		transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File(XMLFilePath));
		transformer.transform(source, result);
	}
	
	public static void AddTimeStamp2BugLog(String dataFolderPath, String projectType, HashMap<String, HashMap<String, ArrayList<String>>> database) throws Exception{
		String folderName=new File(dataFolderPath).getName();
		String []strs=folderName.split("-");
		String projectName=strs[0].toLowerCase();
		String logFileName=folderName+"_Original_Queries.txt";
		String originalLogFile=Paths.get(dataFolderPath, logFileName).toString();
		System.out.println(originalLogFile);
		String XMLFileName=folderName+"_Repository.xml";
		String outputXMLFile=Paths.get(dataFolderPath, XMLFileName).toString();
		System.out.println(outputXMLFile);
		DateFormat f_bugLocator = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		String rootUrl="";
		ArrayList<String> itemList=new ArrayList<String> ();
		if(projectType.equals("SourceForge")){
			rootUrl=SourceForge.getInstance().getRootUrl()+projectName+"/";
			itemList=WebCrawler.GetTicketTypeList(rootUrl);
		}
		else if(projectType.equals("ApacheJira")){
			rootUrl=ApacheJira.getInstance().getRootUrl();
		}
		
		BufferedReader reader=new BufferedReader(new FileReader(originalLogFile));
		String bugID=null;
		String bugLine=null;
		
		
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder domBuilder= domFactory.newDocumentBuilder();
		Document doc=domBuilder.newDocument();
		Element rootNode=doc.createElement("bugrepository");
		doc.appendChild(rootNode);
		
		while((bugLine=reader.readLine())!=null){
			String []strings=bugLine.split(" ");
			bugID=strings[strings.length-1];
			System.out.println(bugID);
			
			String url=new String();
			if(projectType.equals("SourceForge")){
				url=WebCrawler.GetUrlForBugID(rootUrl, bugID, itemList);
			}
			else if(projectType.equals("ApacheJira")){
				url=rootUrl+bugID;
			}
			String []s=bugID.split("-");
			Element _bugNode=doc.createElement("bug");
			_bugNode.setAttribute("id", s[s.length-1]);
			
			System.out.println(url);
			BugTrackingSystemData systemData=null;
			if(projectType.equals("SourceForge")){
				systemData=WebCrawler.ExtractSourceForgeRecord(url);
			}
			else if(projectType.equals("ApacheJira")){
				systemData=WebCrawler.ExtractOneJiraIssue(projectName, bugID,database);
			}
			
			String summaryAndDescription=reader.readLine();
			Element _bugInformationNode=doc.createElement("buginformation");
			
			Element _summaryNode=doc.createElement("summary");
			_summaryNode.appendChild(doc.createTextNode(systemData.getSummary()));
			_bugInformationNode.appendChild(_summaryNode);
				
			Element _descriptionNode=doc.createElement("description");
			_descriptionNode.appendChild(doc.createTextNode(systemData.getDescription()));
			_bugInformationNode.appendChild(_descriptionNode);
			
			_bugNode.appendChild(_bugInformationNode);
			int fixedDocsNum=Integer.parseInt(reader.readLine().trim());
			
			Element _fixedFilesNode=doc.createElement("fixedFiles");
			ArrayList<String> fixedDocsSet=new ArrayList<String>();
			for(int i=0;i<fixedDocsNum;i++){
				Element _oneFixedFileNode=doc.createElement("file");
				String oneFixedDoc=reader.readLine().trim();
				System.out.println("target path:"+oneFixedDoc);
				if(fixedDocsSet.size()==0){
					boolean started=false;
					String [] packagePaths=oneFixedDoc.split("\\.");
					String validFileName="org";
					for(int k=0;k<packagePaths.length;k++){
						if(started){
							validFileName+="."+packagePaths[k];
							if(Character.isUpperCase(packagePaths[k].charAt(0))){
								break;
							}
						}
						if(packagePaths[k].equals("org")){
							started=true;
						}		
					}
					fixedDocsSet.add(validFileName);
					_oneFixedFileNode.appendChild(doc.createTextNode(validFileName+".java"));
					_fixedFilesNode.appendChild(_oneFixedFileNode);
				}
				else{
					boolean isRepeatedDoc=false;
					for(String oneDoc:fixedDocsSet){
						if(oneFixedDoc.contains(oneDoc)){
							isRepeatedDoc=true;
							break;
						}
					}
					if(!isRepeatedDoc){
						boolean started=false;
						String [] packagePaths=oneFixedDoc.split("\\.");
						String validFileName="org";
						for(int k=0;k<packagePaths.length;k++){
							if(started){
								validFileName+="."+packagePaths[k];
								if(Character.isUpperCase(packagePaths[k].charAt(0))){
									break;
								}
							}
							if(packagePaths[k].equals("org")){
								started=true;
							}		
						}
						fixedDocsSet.add(validFileName);
						_oneFixedFileNode.appendChild(doc.createTextNode(validFileName+".java"));
						_fixedFilesNode.appendChild(_oneFixedFileNode);
					}
				}

			}
			_bugNode.appendChild(_fixedFilesNode);
			_bugNode.setAttribute("opendate", f_bugLocator.format(systemData.getCreateDate()));
			_bugNode.setAttribute("fixdate", f_bugLocator.format(systemData.getUpdateDate()));
			reader.readLine();
			if(fixedDocsSet.size()!=0){
				rootNode.appendChild(_bugNode);	
			}
		}
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer;
		transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File(outputXMLFile));
		transformer.transform(source, result);
		reader.close();
	}
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		String targetFilePath="D:/qiuchi/ApacheDatasets/IssueCommitDatas.txt";
		HashMap<String, HashMap<String, ArrayList<String>>> database=WebCrawler.ImportJiraIssueCommitDatabase(targetFilePath);
		HashMap<String, String> projectVersionPairs=new HashMap<String, String>();
//		projectVersionPairs.put("ZOOKEEPER", "3.4.0");
//		projectVersionPairs.put("MYFACES", "2.0.1");
//		projectVersionPairs.put("HADOOP", "2.7.0");
//		projectVersionPairs.put("LUCENE", "5.0");
//		projectVersionPairs.put("MTOMCAT", "2.0");
//		projectVersionPairs.put("SOLR", "5.0");
//		projectVersionPairs.put("PIG", "0.9.0");
//		projectVersionPairs.put("FELIX", "framework-4.0.0");
		projectVersionPairs.put("WICKET", "6.1.0");
		projectVersionPairs.put("CAMEL", "2.15.0");
		String generatedDatasetsDir="D:/qiuchi/ApacheDatasets";
		if(!new File(generatedDatasetsDir).isDirectory()){
			new File(generatedDatasetsDir).mkdir();
		}
		String []statuses={"Resolved","Closed"};
		String []resolutions={"Fixed"};
		String []issueTypes=new String[1];
		for(Entry<String, String> onePair:projectVersionPairs.entrySet()){
			String projectName=onePair.getKey();
			String version=onePair.getValue();
			String []fixVersions={version};
			String projectDir=Paths.get(generatedDatasetsDir,projectName+"-"+fixVersions[0]).toString();
			if(!new File(projectDir).isDirectory()){
				new File(projectDir).mkdir();
			}
			issueTypes[0]="Bug";
			Export2BugLocatorInput(Paths.get(projectDir, issueTypes[0]+"_Repository.xml").toString(), WebCrawler.ExtractJiraRecord(projectName,issueTypes,statuses,resolutions,fixVersions,database));
			issueTypes[0]="Improvement";
			Export2BugLocatorInput(Paths.get(projectDir, issueTypes[0]+"_Repository.xml").toString(), WebCrawler.ExtractJiraRecord(projectName,issueTypes,statuses,resolutions,fixVersions,database));
			issueTypes[0]="New Feature";
			Export2BugLocatorInput(Paths.get(projectDir, issueTypes[0]+"_Repository.xml").toString(), WebCrawler.ExtractJiraRecord(projectName,issueTypes,statuses,resolutions,fixVersions,database));
		}		
		
//		AddTimeStamp2BugLog("D:/mct_txl_nicad/datasets/jEdit-4.3/", "SourceForge");
//		AddTimeStamp2BugLog("D:/mct_txl_nicad/datasets/openjpa-2.2.0/", "ApacheJira");
//		AddTimeStamp2BugLog("D:/mct_txl_nicad/datasets/pig-0.8.0/", "ApacheJira");
//		AddTimeStamp2BugLog("D:/mct_txl_nicad/datasets/zookeeper-3.4.5/", "ApacheJira");
//		System.out.println(new File(originalLogFile).getName());
		
		
	}

}
