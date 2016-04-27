package webcrawler;
import java.text.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map.Entry;
import java.net.*;
import java.io.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.util.Version;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import webdata.ApacheJira;
import webdata.SourceForge;



public class WebCrawler {
	
	/**
	 * Extracting the Jira Record for a project
	 * @param projectName
	 * @param issueTypes
	 * @param statuses
	 * @param resolutions
	 * @param fixVersions
	 * @param database
	 * @return
	 * @throws Exception
	 */
	public static ArrayList<BugTrackingSystemData> ExtractJiraRecord(String projectName, String[] issueTypes, String[] statuses, String[] resolutions, String[] fixVersions, HashMap<String, HashMap<String, ArrayList<String>>> database) throws Exception{
		ArrayList<BugTrackingSystemData> systemDataList = new ArrayList<BugTrackingSystemData>();
		
		//get the issueID list
//		String []issueTypes={"Bug"};
//		String []statuses={"Resolved","Closed"};
//		String []resolutions={"Fixed"};
//		String []fixVersions={"3.4.0"};
		ArrayList<String> issueIDList = GetJiraIssueListforQuery(projectName,issueTypes,statuses,resolutions,fixVersions);
		for(String oneIssueID: issueIDList){
			//Extract one IssueID
			BugTrackingSystemData systemData= ExtractOneJiraIssue(projectName, oneIssueID,database);			
			systemDataList.add(systemData);
		}	
		return systemDataList;
	}
	
	/**
	 * Extracting one issue for a Jira project
	 * @param projectName
	 * @param issueID
	 * @param database
	 * @return
	 * @throws Exception
	 */
	public static BugTrackingSystemData ExtractOneJiraIssue(String projectName,String issueID,HashMap<String, HashMap<String, ArrayList<String>>> database) throws Exception{
		//get the full url of the issue
		String url=ApacheJira.getInstance().getRootUrl()+projectName+"-"+issueID;
		System.out.println(url);
		//load the date format for Jira project
		DateFormat format=ApacheJira.getInstance().getLogDateFormat();
		
		BugTrackingSystemData systemData=new BugTrackingSystemData();
		
		//set the issueID
		systemData.setID(issueID);
		
		//get the whole web content
		Connection c=Jsoup.connect(url);
		c.timeout(0);
		Document doc=c.get();
		
		//set the issue summary
		systemData.setSummary(doc.title().trim());
		
		//set the issue description
		systemData.setDescription(doc.select("div#description-val").text());
		
		//set the issue details
		Elements viewHolderList=doc.select("ul#issuedetails").first().select("div.wrap");
		for(Element e:viewHolderList){
			if(e.children().size()==0) continue;
			String firstChildrenText=e.child(0).text();
			
			//set status
			if(firstChildrenText.equals("Status:")){
				systemData.setStatus(e.child(1).text());
			}
			
			//set priority
			else if(firstChildrenText.equals("Priority:")){
				if(e.child(1).text().equals("Minor")){
					systemData.setPriority(2);
				}
				else{
					//Major
					systemData.setPriority(4);
				}
			}
		}
		//set owner
		systemData.setOwner("nobody");
		
		//set private or not
		systemData.setPrivateOrNot("no");

		//set the created and resolved time
		Element datesModuleElement=doc.select("div#datesmodule").first();
		for(int itemId=0;itemId<datesModuleElement.select("dd").size();itemId++){
			//set the created time
			if(datesModuleElement.select("dd").get(itemId).child(0).attr("data-name").equals("Created")){
				systemData.setCreateDate(format.parse(datesModuleElement.select("dd").get(itemId).child(0).child(0).attr("datetime")));
			}
			//set the resolved time
			else if(datesModuleElement.select("dd").get(itemId).child(0).attr("data-name").equals("Resolved")){
				systemData.setUpdateDate(format.parse(datesModuleElement.select("dd").get(itemId).child(0).child(0).attr("datetime")));
			}
		}
		
		//set the creator
		Element creatorElement=doc.select("li.people-details").first();
		systemData.setCreator(creatorElement.select("dd").last().text());
		
		//set the comments data
		Element issuePanelElement=doc.select("div.issuePanelWrapper").first();
		Elements actionDetails=issuePanelElement.select("div.action-details");
		Elements actionBody=issuePanelElement.select("div.action-body");
		
		//traverse each comment
		for(int i=0;i<actionBody.size();i++){
			BugComment comment=new BugComment();
			//set the commentor
			comment.setCommentor(actionDetails.get(2*i).child(0).text());
			
			//set the comment date
			comment.setCommentDate(format.parse(actionDetails.get(2*i).child(1).child(0).child(0).attr("datetime")));
			
			//set the comment content
			comment.setCommentContent(actionBody.get(i).text());
			systemData.addOneComment(comment);
		}
		
		//set the fixed files 
		HashSet<String> fixedFileList=new HashSet<String>();
		
		//get all patches, and for each patch get all files changed by the patch
		HashSet<String> relevantPatchList=GetJiraRelevantPatches(projectName,issueID);
		for(String onePatch:relevantPatchList){
			fixedFileList.addAll(GetJiraFixedFilesFromPatch(onePatch));
		}
		//get all fixed files if there are relevant commits for the issue
		if(GetJiraCommitsForIssue(projectName, issueID, database)!=null){
			for(String str:GetJiraCommitsForIssue(projectName, issueID, database)){
				fixedFileList.addAll(getJiraFixedFilesFromCommit(projectName, str));
			}
		}
		systemData.setFixedFileList(fixedFileList);
		
		return systemData;
	}
	
	/**
	 * Get relevant patches of a Jira issue
	 * @param issueID
	 * @return
	 * @throws Exception
	 */
	public static HashSet<String> GetJiraRelevantPatches(String projectName,String issueID) throws Exception{
		HashSet<String> patchesSet=new HashSet<String>();
		String url=ApacheJira.getInstance().getRootUrl()+projectName+"-"+issueID;	
		Connection c=Jsoup.connect(url);
		c.timeout(0);
		Document doc = c.get();
		for(Element e:doc.select("li.attachment-content")){
			Element ee=e.select("dt.attachment-title").first();
			patchesSet.add("https://issues.apache.org"+ee.child(0).attr("href"));
		}
		return patchesSet;
	}
	
	/**
	 * Generate url query string for issue lists
	 * @param projectNames
	 * @param issueTypes
	 * @param statuses
	 * @param resolutions
	 * @return
	 * @throws Exception
	 */
	public static String FormUrlQueryString(String []projectNames, String []issueTypes, 
			String []statuses, String []resolutions, String[]fixVersions) throws Exception{
		String urlQueryStr="?jql=";
		
		//project
		String projectQueryStr="project ";
		for(int i=0;i<projectNames.length;i++){
			if(projectNames[i].split(" ").length>1){
				projectNames[i]="\""+projectNames[i]+"\"";
			}
		}
		if(projectNames.length>1){
			projectQueryStr+="in ";
			projectQueryStr+="("+String.join(", ", projectNames)+")";
		}
		else{
			projectQueryStr+="= ";
			projectQueryStr+=projectNames[0];
		}
		
		//issuetypes
		String issueTypeQueryStr="issuetype ";
		for(int i=0;i<issueTypes.length;i++){
			if(issueTypes[i].split(" ").length>1){
				issueTypes[i]="\""+issueTypes[i]+"\"";
			}
		}
		if(issueTypes.length>1){
			issueTypeQueryStr+="in ";
			issueTypeQueryStr+="("+String.join(", ", issueTypes)+")";
		}
		else{
			issueTypeQueryStr+="= ";
			issueTypeQueryStr+=issueTypes[0];
		}
		
		//status
		String statusQueryStr="status ";
		for(int i=0;i<statuses.length;i++){
			if(statuses[i].split(" ").length>1){
				statuses[i]="\""+statuses[i]+"\"";
			}
		}
		if(statuses.length>1){
			statusQueryStr+="in ";
			statusQueryStr+="("+String.join(", ", statuses)+")";
		}
		else{
			statusQueryStr+="= ";
			statusQueryStr+=statuses[0];
		}
		
		//resolutions
		String resolutionQueryStr="resolution ";
		for(int i=0;i<resolutions.length;i++){
			if(resolutions[i].split(" ").length>1){
				resolutions[i]="\""+resolutions[i]+"\"";
			}
		}
		if(resolutions.length>1){
			resolutionQueryStr+="in ";
			resolutionQueryStr+="("+String.join(", ", resolutions)+")";
		}
		else{
			resolutionQueryStr+="= ";
			resolutionQueryStr+=resolutions[0];
		}
		
		//resolutions
		String fixVersionQueryStr="fixVersion ";
		for(int i=0;i<fixVersions.length;i++){
			if(fixVersions[i].split(" ").length>1){
				fixVersions[i]="\""+fixVersions[i]+"\"";
			}
		}
		if(fixVersions.length>1){
			fixVersionQueryStr+="in ";
			fixVersionQueryStr+="("+String.join(", ", fixVersions)+")";
		}
		else{
			fixVersionQueryStr+="= ";
			fixVersionQueryStr+=fixVersions[0];
		}
		
		
//		System.out.println(String.join(" AND ",projectQueryStr,issueTypeQueryStr,statusQueryStr,resolutionQueryStr,fixVersionQueryStr));
		return urlQueryStr+URLEncoder.encode(String.join(" AND ",projectQueryStr,issueTypeQueryStr,statusQueryStr,resolutionQueryStr,fixVersionQueryStr), "utf-8");
	}
	/**
	 * Get the issue list for a Jira project controlled by query
	 * @param projectName
	 * @return
	 * @throws Exception
	 */
	public static ArrayList<String> GetJiraIssueListforQuery(String projectName, String []issueTypes, String []statuses, String []resolutions, String[]fixVersions) throws Exception{
		ArrayList<String> issuesList=new ArrayList<String>();
		
		String []projectNames={projectName};
		//form the url for the query page
		String url=ApacheJira.getInstance().getRootUrl()+projectName+"-0000"+FormUrlQueryString(projectNames, issueTypes, statuses, resolutions, fixVersions);
		//get the web content for the url
		Connection c=Jsoup.connect(url);
		c.timeout(0);
		Document doc = c.get();
		
		//extract the part of the program with issueID
		for(Element e:doc.select("li[data-key]")){
			String fullIssueID=e.child(0).attr("data-issue-key");//projectName-issueID
			String []strs=fullIssueID.split("-");
			String issueID=strs[1];
			issuesList.add(issueID);
		}
		return issuesList;
	}
	
	/**
	 * Extract the fixed files from a given commitID for a Jira project
	 * @param projectName
	 * @param commitID
	 * @return
	 * @throws Exception
	 */
	public static ArrayList<String> getJiraFixedFilesFromCommit(String projectName, String commitID) throws Exception{
		ArrayList<String> fixedFilesSet=new ArrayList<String>();
		String commitUrl="https://fisheye6.atlassian.com/changelog/"+projectName+"?cs="+commitID;
		Connection c=Jsoup.connect(commitUrl);
		c.timeout(0);
		Document doc;
		try{
			doc=c.ignoreContentType(true).get();
		}
		catch(Exception e){
			commitUrl=commitUrl.replace("?", "-git?");
			c=Jsoup.connect(commitUrl);
			c.timeout(0);
			doc=c.ignoreContentType(true).get();
		}
		for(Element e:doc.select("span.file")){
			String rawFilePath=e.child(1).attr("href");
			String fileName=rawFilePath.substring(rawFilePath.indexOf("trunk/")+"trunk/".length(),rawFilePath.length());
			fixedFilesSet.add(fileName);
		}
		return fixedFilesSet;
	}
	
	/**
	 * Extract the fixed files from a Jira patch
	 * @param patchUrl
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<String> GetJiraFixedFilesFromPatch(String patchUrl) throws IOException{
//		System.out.println(patchUrl);
		ArrayList<String> fixedFilesSet=new ArrayList<String>();
		if(!patchUrl.endsWith(".patch")){
			return fixedFilesSet;
		}
		
		//get the patch content
		Connection c=Jsoup.connect(patchUrl);
		c.timeout(0);
		Document doc = c.ignoreContentType(true).get();
		String documentString=doc.text();
		String []str=documentString.split(" ");
		
		//find the changed files
		for(int i=0;i<str.length;i++){
			if(str[i].equals("---") || str[i].equals("+++")){
				fixedFilesSet.add(str[i+1].trim());
				i=i+2;
			}
			
		}
		return fixedFilesSet;
	}
	/**
	 * Get Jira Commits for issue
	 * @param projectName
	 * @param issueID
	 * @param database
	 * @return
	 */
	public static ArrayList<String> GetJiraCommitsForIssue(String projectName, String issueID, HashMap<String, HashMap<String, ArrayList<String>>> database){
		if(database.containsKey(projectName)){
			if(database.get(projectName).containsKey(issueID)){
				return database.get(projectName).get(issueID);
			}
		}
		return null;
	}
	
	/**
	 * Import Issue Commit Database from file where the <projectName, issueID, commitID> is recorded
	 * @param inputFilePath
	 * @return
	 * @throws Exception
	 */
	public static HashMap<String, HashMap<String, ArrayList<String>>> ImportJiraIssueCommitDatabase(String inputFilePath) throws Exception{
		HashMap<String, HashMap<String, ArrayList<String>>> database=new HashMap<String, HashMap<String, ArrayList<String>>>();
		BufferedReader reader=new BufferedReader(new FileReader(inputFilePath));
		String line="";
		while((line=reader.readLine())!=null){
			String []strs=line.split("\t");
			if(strs.length!=3){
				continue;
			}
			else{
				if(database.containsKey(strs[0])){
					if(database.get(strs[0]).containsKey(strs[1])){
						if(!database.get(strs[0]).get(strs[1]).contains(strs[2])){
							database.get(strs[0]).get(strs[1]).add(strs[2]);
						}
					}
					else{
						ArrayList<String> list=new ArrayList<String> ();
						list.add(strs[2]);
						database.get(strs[0]).put(strs[1], list);
					}
				}
				else{
					ArrayList<String> list=new ArrayList<String> ();
					list.add(strs[2]);
					HashMap<String, ArrayList<String>> map=new HashMap<String, ArrayList<String>>();
					map.put(strs[1], list);
					database.put(strs[0],map);
				}
			}
		}
		reader.close();
		return database;
	}
	
	/**
	 * Export Issue Commit Database to file in the <projectName, issueID, commitID> format
	 * @param database
	 * @param outputFilePath
	 * @throws Exception
	 */
	public static void ExportJiraIssueCommitDatabase(HashMap<String,HashMap<String, ArrayList<String>>> database, String outputFilePath) throws Exception{
		FileWriter writer=new FileWriter(outputFilePath);
		StringBuffer buf=new StringBuffer();
		for(Entry<String, HashMap<String, ArrayList<String>>> entry: database.entrySet()){
			String projectName=entry.getKey();
			HashMap<String, ArrayList<String>> map=entry.getValue();
			for(Entry <String, ArrayList<String>> subEntry:map.entrySet()){
				String issueID=subEntry.getKey();
				for(String commitID:subEntry.getValue()){
					String oneLine=String.join("\t",projectName,issueID,commitID)+"\n";
					buf.append(oneLine);
				}
			}
		}
		writer.append(buf.toString());
		writer.close();
	}
	
	
	/**
	 * For the projects with commits instead of patches, e.g.OpenJPA
	 * Get the fixed files from the commits session
	 * @param issueID
	 * @param projectName
	 * @return
	 * @throws Exception
	 */
	public static ArrayList<String> GetFixedFiles(String issueID, String projectName) throws Exception{
		ArrayList<String> fixedFileList=new ArrayList<String>();
		//get the url with the commits information for the issueID
		String url="https://issues.apache.org/jira/activity?maxResults=20&streams=issue-key+IS+OPENJPA-"+issueID+"&streams=key+IS+OPENJPA&title=undefined";
		
		//get the web content
		Connection c=Jsoup.connect(url);
		c.timeout(0);
		Document doc = c.get();

		//get the comment list
		Elements commentList=doc.select("content");
		for(Element oneComment:commentList){
			//get the comment content, and find changed files if any
			String oneCommentText=oneComment.text();
			Document d=Jsoup.parse(oneCommentText);
			if(d.select("span.changeset-file").size()>0){
				Elements elements=d.select("ul.commit-list").first().select("a");;
				for(Element oneElement:elements){
					String fullTextName=oneElement.html().trim();
					if(fullTextName.startsWith("trunk/")){
						fixedFileList.add(fullTextName.substring("trunk/".length()));
					}
					
				}
			}
			
		}
		return fixedFileList;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////
	
	public static BugTrackingSystemData ExtractSourceForgeRecord(String url) throws Exception{
		DateFormat format=SourceForge.getInstance().getLogDateFormat();
		BugTrackingSystemData systemData=new BugTrackingSystemData();
		String []str=url.split("/");
		systemData.setID(str[str.length-1]);
		Connection c=Jsoup.connect(url);
		c.timeout(0);
		Document doc=c.get();
//		DateFormat f = new SimpleDateFormat("EEE MMM dd, yyyy hh:mm a z");
		
		//bug summary
		systemData.setSummary(doc.title().trim());
		
		//bug view Holder information
		Element viewHolderElement=doc.select("div.view_holder").first();
		for(Element e:viewHolderElement.children()){
			if(e.children().size()==0) continue;
			String firstChildrenText=e.children().first().text();
			if(firstChildrenText.equals("Status:")){
				systemData.setStatus(e.child(1).text());
			}
			else if(firstChildrenText.equals("Owner:")){
				systemData.setOwner(e.ownText());
			}
			else if(firstChildrenText.equals("Priority:")){
				systemData.setPriority(Integer.parseInt(e.ownText()));
			}
			else if(firstChildrenText.equals("Creator:")){
				systemData.setCreator(e.child(1).text());
			}
			else if(firstChildrenText.equals("Private:")){
				systemData.setPrivateOrNot(e.ownText());
			}
			else if(firstChildrenText.equals("Created:")){
				systemData.setCreateDate(format.parse(e.child(1).attr("title")));
			}
			else if(firstChildrenText.equals("Updated:")){
				systemData.setUpdateDate(format.parse(e.child(1).child(0).attr("title")));
			}
		}
		//forum
		Elements discussionHolderElements=doc.select("p.gravatar");
		Elements discussionContentElements=doc.select("div.display_post");
		
		//bug description
		Element ticketContentElement=doc.select("#ticket_content").first();
		Element wq=ticketContentElement.children().first();
		String content="";
		for(Element ee:wq.children()){
			content=content+ee.text();
		}
		systemData.setDescription(content);
		
		
		for(int i=0;i<discussionHolderElements.size();i++){
			BugComment comment=new BugComment();
			Element oneHolderElement=discussionHolderElements.get(i);
			Element oneContentElement=discussionContentElements.get(i);
			Elements es=oneHolderElement.children();
			
			comment.setCommentor(es.get(2).text());
			comment.setCommentDate(format.parse(es.get(4).child(0).attr("title")));
			if(oneContentElement.select("div.markdown_content").size()>0){
				Element element=oneContentElement.select("div.markdown_content").first();
				comment.setCommentContent(element.text());
			}
			else{
				comment.setCommentContent(new String());
			}
			systemData.addOneComment(comment);
		}
		
		return systemData;
	}
	public static ArrayList<String> GetTicketTypeList(String rootUrl) throws Exception{
		Document rootDoc = Jsoup.connect(rootUrl).get();
		Element contentWrapperElement=rootDoc.select("article.content-wrapper").first();
		Elements elements=contentWrapperElement.child(1).child(0).child(0).child(0).children();
		ArrayList<String> itemList=new ArrayList<String>();
		for(Element oneElement:elements){
			if(oneElement.text().contains("Tickets")){
				for(org.jsoup.nodes.Element oneTicketTypeElement:oneElement.child(1).children()){
					itemList.add(oneTicketTypeElement.text().replace(" ", "-").toLowerCase());
				}
				break;
			}
		}
		return itemList;
	}
	public static String GetUrlForBugID(String rootUrl, String inputQueryID, ArrayList<String> ticketTypeList) throws Exception{
		String url="";
		for(String oneItem:ticketTypeList){
			String searchPageUrl=rootUrl+oneItem+"/search/?q="+inputQueryID;
			System.out.println(searchPageUrl);
			Connection c=Jsoup.connect(searchPageUrl);
			c.timeout(0);
			Document searchDoc = c.get();
			if(searchDoc.select("tbody").size()!=0){
				Element e=searchDoc.select("tbody").first();
				String pageID=e.children().first().children().first().text();
				url=rootUrl+oneItem+'/'+pageID+"/";
				break;
			}
		}	
		return url;
	}
	
	
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		String url="https://fisheye6.atlassian.com/changelog/WICKET?cs=72c2b22";
		Connection c=Jsoup.connect(url);
		c.timeout(0);
		try{
			Document doc = c.ignoreContentType(true).get();
			System.out.println(doc);
		}
		catch(Exception e){
//			System.out.println(e.getMessage());
			url.replace("?", "-git?");
			c=Jsoup.connect(url);
			Document doc = c.ignoreContentType(true).get();
			System.out.println(doc);
		}
		
		Analyzer analyzer=null;
		analyzer.setVersion(Version.LATEST);

//		analyzer
		//		for(Element e:doc.select("span.file")){
//			String rawFilePath=e.child(1).attr("href");
//			String fileName=rawFilePath.substring(rawFilePath.indexOf("trunk/")+"trunk/".length(),rawFilePath.length());
//			System.out.println(fileName);
//		}
		

//		HashMap<String, HashMap<String, ArrayList<String>>> map=new HashMap<String, HashMap<String, ArrayList<String>>>();
//		HashMap<String, ArrayList<String>> subMap=new HashMap<String, ArrayList<String>>();
//		ArrayList<String> aStrList=new ArrayList<String> ();
//		aStrList.add("e");
//		aStrList.add("d");
//		subMap.put("a", aStrList);
//		ArrayList<String> bStrList=new ArrayList<String> ();
//		bStrList.add("f");
//		bStrList.add("g");
//		subMap.put("b", bStrList);
//		map.put("firstMap", subMap);
//		map.get("firstMap").get("a").add("k");
//		System.out.println(map.get("firstMap").get("a").size());
//		String targetFilePath="D:/qiuchi/ApacheDatasets/example.txt";
//		HashMap<String, HashMap<String, ArrayList<String>>> database=ImportJiraIssueCommitDatabase(targetFilePath);
//		if(GetJiraCommitsForIssue("OPENJPA", "1232", database)!=null){
//			for(String str:GetJiraCommitsForIssue("OPENJPA", "1232", database)){
//				System.out.println(str);
//				for(String oneFile:getJiraFixedFilesFromCommit("OPENJPA", str)){
//					System.out.println(oneFile);
//				}
//			}
//		}
	}

}
