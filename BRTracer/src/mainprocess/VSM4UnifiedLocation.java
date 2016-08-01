package mainprocess;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.Map.Entry;
import java.util.TreeSet;

import org.eclipse.core.internal.utils.FileUtil;

import sourcecode.CodeDataProcessor;
import sourcecode.CodeFeatureExtractor;
import sourcecode.SourceCodeCorpus;
import utils.FileUtils;
import utils.MatrixUtil;
import Jama.Matrix;
import bug.BugDataProcessor;
import bug.BugFeatureExtractor;
import bug.BugRecord;
import config.Config;
import eval.FileEvaluate;
import feature.StructuredIR;
import feature.VSMScore;
import feature.VectorCreator;

//source code modification:
//1.LM-->no smoothing(done)
//2.elementScoring-->check if there are an bugs(done)
//3.all smoothing parameter: ~Dirichlet 1000(done)
//4.new metric: S-MAP?
//5.a larger K for topK:k=10
public class VSM4UnifiedLocation {
	public static long run(String outputFilePath, String modelType) throws Exception{
		String bugCorpusDirPath=Paths.get(Config.getInstance().getIntermediateDir(), "bug").toString();
		Config.getInstance().setBugCorpusDir(bugCorpusDirPath);
		if(!new File(bugCorpusDirPath).isDirectory()){
			BugDataProcessor.createBugCorpus(BugDataProcessor.importFromXML());
		}
//		FileUtils.deleteDir(new File(bugCorpusDirPath));
//						
		String codeCorpusDirPath=Paths.get(Config.getInstance().getIntermediateDir(), "code").toString();
		Config.getInstance().setCodeCorpusDir(codeCorpusDirPath);
//		FileUtils.deleteDir(new File(codeCorpusDirPath));
		if(!new File(codeCorpusDirPath).isDirectory()){
			SourceCodeCorpus corpus=CodeDataProcessor.extractCodeData();
			CodeDataProcessor.exportCodeData(corpus);
			String corpusDir= Paths.get(codeCorpusDirPath,"codeContentCorpus").toString();
			CodeFeatureExtractor.AddPackagesToCorpus(corpusDir, corpus);
		}
		String simMatFilePath=Paths.get(Config.getInstance().getIntermediateDir(), "score4ranking").toString();
		long indexingTime=0;
		long tic=System.currentTimeMillis();
		if(modelType=="VSM"){
			String bugVecFilePath=Paths.get(Config.getInstance().getIntermediateDir(), "bugVec").toString();
			String codeVecFilePath=Paths.get(Config.getInstance().getIntermediateDir(), "codeVec").toString();

			VectorCreator.create(Paths.get(bugCorpusDirPath,"information").toString(), Paths.get(codeCorpusDirPath,"codeContentCorpus").toString(), bugVecFilePath, codeVecFilePath);
			VSMScore.generate(bugVecFilePath, codeVecFilePath, simMatFilePath);
		}
		else if(modelType=="LM"){
			StructuredIR.getLMProbMat(Paths.get(bugCorpusDirPath,"information").toString(), Paths.get(codeCorpusDirPath,"codeContentCorpus").toString(), simMatFilePath,1);
		}
		else if(modelType=="Propagation"){
			indexingTime=StructuredIR.getPropagationMat(Paths.get(bugCorpusDirPath,"information").toString(), Paths.get(codeCorpusDirPath,"codeContentCorpus").toString(), simMatFilePath, 0.8, 0.9, 0.1);
		}
		else if(modelType=="Aggregation"){
			indexingTime=StructuredIR.getAggregationMat(Paths.get(bugCorpusDirPath,"information").toString(), Paths.get(codeCorpusDirPath,"codeContentCorpus").toString(), simMatFilePath, 0.1);
		}
		else if(modelType=="ElementScoring"){
			StructuredIR.getElementScoringMat(Paths.get(bugCorpusDirPath,"information").toString(), Paths.get(codeCorpusDirPath,"codeContentCorpus").toString(), simMatFilePath, 0.1);
		}
		
		String fixedFilePath=Paths.get(bugCorpusDirPath, "fixedFiles").toString();
		HashMap<String, TreeSet<String>> fixedFilesMap=BugFeatureExtractor.extractFixedFiles(fixedFilePath);
		
		if(new File(outputFilePath).isFile()){
			new File(outputFilePath).delete();
		}
		for(Entry<String, TreeSet<String>> oneBugFixedListPair: fixedFilesMap.entrySet()){
			String bugID=oneBugFixedListPair.getKey();
			ArrayList<String> bugIdList=new ArrayList<String>();
			ArrayList<String> codeClassList=new ArrayList<String>();
			Matrix scoreMat= MatrixUtil.importSimilarityMatrix_PackageOnly(bugIdList, codeClassList, simMatFilePath);
			String []bugIdArray=bugIdList.toArray(new String[0]);
			String []codeClassArray=codeClassList.toArray(new String[0]);
			int rowIndex=MatrixUtil.getIndex(bugID, bugIdArray);
			ArrayList<String> matchedClassList=new ArrayList<String>();
			for(String oneFixedFile: oneBugFixedListPair.getValue()){
				String matchedClass=new String();
				for(String oneCodeClass:codeClassList){
					if(oneFixedFile.contains(oneCodeClass)){
//						System.out.println(oneCodeClass);
						if(oneCodeClass.length()>matchedClass.length()){
							matchedClass=oneCodeClass;
						}
					}			
				}	
//				System.out.println(matchedClass);
				if(matchedClass.length()>0 && !matchedClass.endsWith(".java") && !matchedClassList.contains(matchedClass) ){
					matchedClassList.add(matchedClass);
					int colIndex=MatrixUtil.getIndex(matchedClass, codeClassArray);
					FileUtils.write_append2file(String.join(",", bugID, matchedClass,String.valueOf(MatrixUtil.getRank(rowIndex, colIndex, scoreMat)),"0.0")+"\n",outputFilePath);
				}
			}
		}
		long toc=System.currentTimeMillis();
		System.out.println("running time: "+(toc-tic));
		System.out.println("indexing time: "+indexingTime);
		return (toc-tic-indexingTime);
	}
	public static void mergeBugReport(String bugReportsDir) throws Exception{
		ArrayList<BugRecord> allRecords= new ArrayList<BugRecord> ();
		for(File oneFile: new File(bugReportsDir).listFiles()){
			if(oneFile.isFile()){
				Config.getInstance().setBugLogFile(oneFile.getAbsolutePath());
				ArrayList<BugRecord> recordList=BugDataProcessor.importFromXML();
				allRecords.addAll(recordList);
			}
		}
		String outputXMLFilePath=Paths.get(bugReportsDir,"All_Repository.xml").toString();
		BugDataProcessor.exportToXML(allRecords, outputXMLFilePath);
	}
//	public static String getPackage(ArrayList<String> file)
	public static void main(String []args) throws Exception{
//		String rootDirPath=args[0];
//		String datasetsDirPath=args[1];
		String rootDirPath="C:/Users/dell/Documents/EClipse";
		String datasetsDirPath=Paths.get(rootDirPath,"SeekChanges", "ApacheDatasets").toString();
		String timestatsFilePath=Paths.get(rootDirPath,"experimentResult","timeStats_package").toString();
		String configFilePath=Paths.get(rootDirPath, "experimentResult","property").toString();
		String intermediateDirPath=Paths.get(rootDirPath, "experimentResult","Corpus").toString();
		if(!new File(intermediateDirPath).isDirectory()){
			new File(intermediateDirPath).mkdir();
		}
		String outputFilePath=Paths.get(rootDirPath, "VSM", "output").toString();
		if(new File(outputFilePath).isFile()){
			new File(outputFilePath).delete();
		}
		String evaluationDirPath=Paths.get(rootDirPath, "experimentResult", "rankingResult_package").toString();
		if(!new File(evaluationDirPath).isDirectory()){
			new File(evaluationDirPath).mkdir();
		}
		HashMap<String,String> datasets=new HashMap<String, String>();
		
		//datasets to be experimented
//		datasets.put("FELIX-framework-4.0.0", "felix-framework-3.2.2");
		datasets.put("MYFACES-2.0.1", "myfaces-2.0.0");
		datasets.put("PIG-0.9.0", "pig-0.8.1");
		datasets.put("WICKET-6.1.0", "wicket-6.0.0-beta3");
		datasets.put("ZOOKEEPER-3.4.0", "zookeeper-3.3.6");
		
		//corpus too large to build
//		datasets.put("CAMEL-2.15.0", "camel-2.14.4");
//		datasets.put("HADOOP-2.7.0", "hadoop-2.6.4");
		
		//only .jar files in corpus, no java files
//		datasets.put("LUCENE-5.0", "lucene-4.9.1");
		
		//The solr corpus cannot match the files mentioned in the report
//		datasets.put("SOLR-5.0", "solr-4.10.4");
		
		String []logNames={/*"Bug","Improvement","New Feature",*/"All"};
		for(Entry<String, String> pair:datasets.entrySet()){
			String oneProjectEvalDir=Paths.get(evaluationDirPath, pair.getKey()).toString();
			if(!new File(oneProjectEvalDir).isDirectory()){
				new File(oneProjectEvalDir).mkdir();
			}
//			String datasetDirPath=Paths.get(datasetsDirPath,"ZOOKEEPER-3.4.0","zookeeper-3.3.6").toString();
//			String bugLogFilePath=Paths.get(datasetsDirPath,"ZOOKEEPER-3.4.0","New Feature_Repository.xml").toString();
			System.out.println("processing "+pair.getKey()+":");
			String datasetDirPath=Paths.get(datasetsDirPath,pair.getKey(),pair.getValue()).toString();
			String oneProjectCorpusPath=Paths.get(intermediateDirPath, pair.getKey()).toString();
			if(!new File(oneProjectCorpusPath).isDirectory()){
				new File(oneProjectCorpusPath).mkdir();
			}
//			FileUtils.deleteDir(new File(intermediateDirPath));
//			new File(intermediateDirPath).mkdir();
			for(String oneLogName:logNames){
				String oneLogEvalDir=Paths.get(oneProjectEvalDir,oneLogName).toString();
				if(!new File(oneLogEvalDir).isDirectory()){
					new File(oneLogEvalDir).mkdir();
				}
				System.out.println("processing "+oneLogName+" Repository.");
				String bugLogFilePath=Paths.get(datasetsDirPath,pair.getKey(),oneLogName+"_Repository.xml").toString();

				Config.getInstance().setPaths(datasetDirPath, bugLogFilePath, oneProjectCorpusPath, outputFilePath);
				Config.getInstance().setEvaluations(Paths.get(rootDirPath, "VSM", "eval").toString(), true, true, 5, true);
				Config.getInstance().exportConfig(configFilePath);
				long runningTime=0;
				String resultFilePath=Paths.get(oneLogEvalDir,"ElementScoring").toString();
				runningTime=run(resultFilePath, "ElementScoring");
				FileUtils.write_append2file(pair.getKey()+" "+oneLogName+" "+"ElementScoring"+" "+runningTime+"\n", timestatsFilePath);
				resultFilePath=Paths.get(oneLogEvalDir,"Propagation").toString();
				runningTime=run(resultFilePath, "Propagation");
				FileUtils.write_append2file(pair.getKey()+" "+oneLogName+" "+"Propagation"+" "+runningTime+"\n", timestatsFilePath);

				resultFilePath=Paths.get(oneLogEvalDir,"Aggregation").toString();
				runningTime=run(resultFilePath, "Aggregation");
				FileUtils.write_append2file(pair.getKey()+" "+oneLogName+" "+"Aggregation"+" "+runningTime+"\n", timestatsFilePath);

				resultFilePath=Paths.get(oneLogEvalDir,"LM").toString();
				runningTime=run(resultFilePath, "LM");
				FileUtils.write_append2file(pair.getKey()+" "+oneLogName+" "+"LM"+" "+runningTime+"\n", timestatsFilePath);

				resultFilePath=Paths.get(oneLogEvalDir,"VSM").toString();
				runningTime=run(resultFilePath, "VSM");
				FileUtils.write_append2file(pair.getKey()+" "+oneLogName+" "+"VSM"+" "+runningTime+"\n", timestatsFilePath);
			}
		}
		String resultDirPath=Paths.get(rootDirPath, "experimentResult","evalResult_package").toString();
		FileEvaluate.evaluateDir(evaluationDirPath, 10, resultDirPath);
		String outputExcelPath=Paths.get(rootDirPath, "experimentResult","output_package.xls").toString();
		FileEvaluate.exportEvaluationResult2Excel(resultDirPath, outputExcelPath);
	}
}

