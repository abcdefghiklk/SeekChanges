package mainprocess;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeSet;

import bug.BugDataProcessor;
import bug.BugRecord;
import sourcecode.CodeDataProcessor;
import sourcecode.CodeFeatureExtractor;
import sourcecode.SourceCodeCorpus;
import utils.FileUtils;
import utils.MatrixUtil;
import config.Config;
import eval.FileEvaluate;

public class RemoveNonExistFiles {
	public static void run(String outputFilePath) throws Exception{
		String bugCorpusDirPath=Paths.get(Config.getInstance().getIntermediateDir(), "bug").toString();
		Config.getInstance().setBugCorpusDir(bugCorpusDirPath);
		FileUtils.deleteDir(new File(bugCorpusDirPath));
		ArrayList<BugRecord> bugData=BugDataProcessor.importFromXML();
				
		String codeCorpusDirPath=Paths.get(Config.getInstance().getIntermediateDir(), "code").toString();
		Config.getInstance().setCodeCorpusDir(codeCorpusDirPath);
		FileUtils.deleteDir(new File(codeCorpusDirPath));
		SourceCodeCorpus corpus=CodeDataProcessor.extractCodeData();
		ArrayList<String> codeFileList=CodeFeatureExtractor.GetCorpusFileList(corpus);
		System.out.println("total number of issues:"+bugData.size());
		System.out.println("total number of files:"+codeFileList.size());
		ArrayList<String> matchedClassList=new ArrayList<String>();
		int addedFilesNum=0;
		int totalfixedFilesNum=0;
		for(BugRecord oneBug:bugData){
			TreeSet<String> fixedFilesSet=oneBug.getFixedFileSet();
			for(String oneFixedFile: fixedFilesSet){
				String matchedClass=new String();
				for(String oneCodeClass:codeFileList){
					if(oneFixedFile.contains(oneCodeClass)){
						matchedClass=oneCodeClass;
						break;
					}			
				}	
				if(matchedClass.length()>0){
					matchedClassList.add(matchedClass);
				}
				else{
					addedFilesNum++;
				}
				totalfixedFilesNum++;
			}
			oneBug.cleanFixedFile();
			for(String oneClass:matchedClassList){
				oneBug.addFixedFile(oneClass);
			}
			
		}
		System.out.println("total number of added files:"+addedFilesNum);
		System.out.println("total number of files:"+totalfixedFilesNum);
		BugDataProcessor.exportToXML(bugData, outputFilePath);
		
		
	}
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		String rootDirPath="C:/Users/dell/Documents/EClipse";
		String configFilePath=Paths.get(rootDirPath, "experimentResult","property").toString();
		String datasetsDirPath=Paths.get(rootDirPath,"ApacheDatasets").toString();
		String intermediateDirPath=Paths.get(rootDirPath, "experimentResult","Corpus").toString();
		if(!new File(intermediateDirPath).isDirectory()){
			new File(intermediateDirPath).mkdir();
		}
		String outputFilePath=Paths.get(rootDirPath, "VSM", "output").toString();
		if(new File(outputFilePath).isFile()){
			new File(outputFilePath).delete();
		}
		String outputDirPath=Paths.get(rootDirPath, "experimentResult", "processedDataset").toString();
		if(!new File(outputDirPath).isDirectory()){
			new File(outputDirPath).mkdir();
		}
		HashMap<String,String> datasets=new HashMap<String, String>();
		
		//datasets to be experimented
		datasets.put("FELIX-framework-4.0.0", "felix-framework-3.2.2");
		datasets.put("MYFACES-2.0.1", "myfaces-2.0.0");
		datasets.put("PIG-0.9.0", "pig-0.8.1");
		datasets.put("WICKET-6.1.0", "wicket-6.0.0-beta3");
		datasets.put("ZOOKEEPER-3.4.0", "zookeeper-3.3.6");
		
		String []logNames={"Bug","Improvement","New Feature","All"};
		for(Entry<String, String> pair:datasets.entrySet()){
			String oneProjectEvalDir=Paths.get(outputDirPath, pair.getKey()).toString();
			if(!new File(oneProjectEvalDir).isDirectory()){
				new File(oneProjectEvalDir).mkdir();
			}
//			String datasetDirPath=Paths.get(datasetsDirPath,"ZOOKEEPER-3.4.0","zookeeper-3.3.6").toString();
//			String bugLogFilePath=Paths.get(datasetsDirPath,"ZOOKEEPER-3.4.0","New Feature_Repository.xml").toString();
			System.out.println("processing "+pair.getKey()+":");
			String datasetDirPath=Paths.get(datasetsDirPath,pair.getKey()).toString();
//			FileUtils.deleteDir(new File(intermediateDirPath));
//			new File(intermediateDirPath).mkdir();
			for(String oneLogName:logNames){
				String oneLogPath=Paths.get(oneProjectEvalDir,oneLogName+"_Repository.xml").toString();
				System.out.println("processing "+oneLogName+" Repository.");
				String bugLogFilePath=Paths.get(datasetDirPath,oneLogName+"_Repository.xml").toString();

				Config.getInstance().setPaths(datasetDirPath, bugLogFilePath, intermediateDirPath, outputFilePath);
				Config.getInstance().setEvaluations(Paths.get(rootDirPath, "VSM", "eval").toString(), true, true, 5, true);
				Config.getInstance().exportConfig(configFilePath);
//				run(resultFilePath, "ElementScoring");
//				resultFilePath=Paths.get(oneLogEvalDir,"Propagation").toString();
//				run(resultFilePath, "Propagation");
				run(oneLogPath);
//				resultFilePath=Paths.get(oneLogEvalDir,"LM").toString();
//				run(resultFilePath, "LM");
//				resultFilePath=Paths.get(oneLogEvalDir,"VSM").toString();
//				run(resultFilePath,"VSM");
			}
		}
	}

}
