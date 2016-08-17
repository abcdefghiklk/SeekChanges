package mainprocess;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.Map.Entry;

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
import eval.MAP;
import eval.MRR;
import eval.TopK;
import feature.CodeLength;
import feature.LuceneSimiScore;
import feature.LuceneVectorCreator;
import feature.RevisedVSMScore;
import feature.SimiScore;
import feature.StructuredIR;
import feature.VSMScore;
import feature.VectorCreator;

public class LuceneSearch {
	public static void run() throws Exception{
		String bugCorpusDirPath=Paths.get(Config.getInstance().getIntermediateDir(), "bug").toString();
		Config.getInstance().setBugCorpusDir(bugCorpusDirPath);
		BugDataProcessor.indexBugCorpus(BugDataProcessor.importFromXML());
		
		String codeCorpusDirPath=Paths.get(Config.getInstance().getIntermediateDir(), "code").toString();
		Config.getInstance().setCodeCorpusDir(codeCorpusDirPath);
		SourceCodeCorpus corpus=CodeDataProcessor.extractCodeData();
		CodeDataProcessor.indexCodeData(corpus);
		
		String codeLengthFilePath=Paths.get(Config.getInstance().getIntermediateDir(), "codelength").toString();
		CodeLength.generate(corpus, codeLengthFilePath);
//		CodeDataProcessor.exportCodeData(CodeDataProcessor.extractCodeData());
	
		
		String simMatFilePath=Paths.get(Config.getInstance().getIntermediateDir(), "VSMScore").toString();
		LuceneVectorCreator.generate(bugCorpusDirPath, codeCorpusDirPath, simMatFilePath);
		
		String revisedSimMatFilePath=Paths.get(Config.getInstance().getIntermediateDir(), "RevisedVSMScore").toString();
		RevisedVSMScore.generate(simMatFilePath, codeLengthFilePath,revisedSimMatFilePath);
		
//		String simiScoreMatFilePath = Paths.get(Config.getInstance().getIntermediateDir(), "SimiScore").toString();
//		SimiScore.generate(codeVecFilePath, bugVecFilePath, bugList, scoreMatFilePath);
		String fixedFilePath=Paths.get(bugCorpusDirPath, "fixedFiles").toString();
		if(Config.getInstance().getMRRUsed()){
			MRR mrr=new MRR();
			mrr.set(BugFeatureExtractor.extractFixedFiles(fixedFilePath));
			FileUtils.write_append2file("MRR"+"\t"+mrr.evaluate(revisedSimMatFilePath)+"\n", Config.getInstance().getOutputFile());
		}
				
		if(Config.getInstance().getMAPUsed()){
			MAP map=new MAP();
			map.set(BugFeatureExtractor.extractFixedFiles(fixedFilePath));
			FileUtils.write_append2file("MAP"+"\t"+map.evaluate(revisedSimMatFilePath)+"\n", Config.getInstance().getOutputFile());
		}
				
		if(Config.getInstance().getTopKUsed()){
			TopK topK=new TopK(Config.getInstance().getK());
			topK.set(BugFeatureExtractor.extractFixedFiles(fixedFilePath));
			FileUtils.write_append2file("TopK"+"\t"+topK.evaluate(revisedSimMatFilePath)+"\n", Config.getInstance().getOutputFile());
		}			

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
		String rootDirPath="C:/Users/ql29/Documents/EClipse";
		String configFilePath=Paths.get(rootDirPath, "property").toString();
		String datasetsDirPath=Paths.get(rootDirPath,"Dataset").toString();
		String intermediateDirPath=Paths.get(rootDirPath, "Corpus").toString();
		if(!new File(intermediateDirPath).isDirectory()){
			new File(intermediateDirPath).mkdir();
		}
		String outputFilePath=Paths.get(rootDirPath, "BugLocator", "output").toString();
		if(new File(outputFilePath).isFile()){
			new File(outputFilePath).delete();
		}
		String evaluationDirPath=Paths.get(rootDirPath, "BugLocator", "eval").toString();
		String projectName="swt";
		String datasetDirPath;
		String bugLogFilePath;
		if(projectName=="swt"){
			datasetDirPath=Paths.get(datasetsDirPath,"swt-3.1").toString();
			bugLogFilePath=Paths.get(datasetsDirPath, "SWTBugRepository.xml").toString();
		}
		else if(projectName=="aspectj"){
			datasetDirPath=Paths.get(datasetsDirPath, "aspectj").toString();
			bugLogFilePath=Paths.get(datasetsDirPath, "AspectJBugRepository.xml").toString();
		}
		else if(projectName=="eclipse"){
			datasetDirPath=Paths.get(datasetsDirPath, "Eclipse-3.1").toString();
			bugLogFilePath=Paths.get(datasetsDirPath, "EclipseBugRepository.xml").toString();
		}
		else{
			System.out.println("The project name is invalid");
			return;
		}
		
		if(!new File(evaluationDirPath).isDirectory()){
			new File(evaluationDirPath).mkdir();
		}
		Config.getInstance().setPaths(datasetDirPath, bugLogFilePath, intermediateDirPath, outputFilePath);
		Config.getInstance().setEvaluations(evaluationDirPath, true, true, 5, true);
		Config.getInstance().exportConfig(configFilePath);
		run();
	}

}
