package feature;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import utils.FileUtils;
import utils.MatrixUtil;
import utils.TreeNode;
import Jama.Matrix;

public class StructuredIR {
	public static void getElementScoringMat(ArrayList<String> bugFileList, 
											ArrayList<String> codeFileList,
											ArrayList<String> corpusFileList, 
											String simMatFilePath, 
											double lambda) throws Exception{
		int corpusSize=getCorpusSize(corpusFileList);	
		HashMap<String, Double> corpusLM=getCorpusIEF(corpusFileList);
		//dirichlet smoothing \mu=1000
		int mu=1000;
		
		Matrix probMat=new Matrix(bugFileList.size(),codeFileList.size());
		for(String oneCodeFile:codeFileList){
			HashMap<String, Double> docLM=getDocLM(oneCodeFile);
			int docSize=getDocSize(oneCodeFile);
			double priorProb=(docSize+0.0d)/(corpusSize+0.0d);
			lambda=(docSize+0.0d)/(docSize+mu+0.0d);
			for(String oneBugFile:bugFileList){	
				HashMap<String, Integer> docTF=getDocTF(oneBugFile);
				double prob=Math.exp(getLogSmoothedLMProb(docTF, docLM, corpusLM,lambda));
				
				probMat.set(bugFileList.indexOf(oneBugFile), codeFileList.indexOf(oneCodeFile),prob*priorProb);
			}
		}
		MatrixUtil.exportMatrix(bugFileList, codeFileList, probMat, simMatFilePath);
	}
	public static void getPropagationMat2(String bugCorpusDirPath, 
										String codeCorpusDirPath, 
										String simMatFilePath, 
										double alpha, 
										double rho,
										double lambda) throws Exception{
		ArrayList<String> bugFileList=new ArrayList<String>();
		ArrayList<String> codeFileList=new ArrayList<String>();
		ArrayList<String> corpusFileList = new ArrayList<String>();
		getFileList(bugCorpusDirPath, codeCorpusDirPath, corpusFileList, bugFileList, codeFileList);
		HashMap<String, Double> corpusLM=getCorpusIEF(corpusFileList);
		Matrix probMat=new Matrix(bugFileList.size(),codeFileList.size());
		long totalIndexingTime=0;
		ArrayList<TreeNode<ProgramElement>> packageStructure = getPackageStructure(codeFileList);
		for(String oneBugFile:bugFileList){
			computeLeafRelevanceScore(codeCorpusDirPath, packageStructure, oneBugFile, corpusLM,lambda);
//			propagate(packageStructure, rho, lambda);
//			assignScores(packageStructure, probMat);
//			clearRelevanceScore(packageStructure);
		}
//		probMat.set(bugFileList.indexOf(oneBugFile), codeFileList.indexOf(oneCodeFile),propagationScore);
			
		MatrixUtil.exportMatrix(bugFileList, codeFileList, probMat, simMatFilePath);
		return;
	}
	
	public static void computeLeafRelevanceScore(String codeCorpusDirPath, ArrayList<TreeNode<ProgramElement>> packageStructure, String oneBugFile, HashMap<String, Double>corpusLM, double lambda) throws Exception{
		for(TreeNode<ProgramElement> oneElement:packageStructure){
			if(oneElement.isLeaf()){
				String filePath=Paths.get(codeCorpusDirPath, oneElement.getData().getFullElementPath()+".java").toString();
				double score=computeRelevanceScore(filePath, oneBugFile, corpusLM, lambda);
				oneElement.getData().setRelevanceScore(score);
			}
		}
	}
	
	
	public static long getPropagationMat(String bugCorpusDirPath, String codeCorpusDirPath, String simMatFilePath, double alpha, double rho, double lambda) throws Exception{
		ArrayList<String> corpusFileList=new ArrayList<String>();
		ArrayList<String> bugFileList=new ArrayList<String>();
		ArrayList<String> codeFileList=new ArrayList<String>();
		for(File oneFile:new File(bugCorpusDirPath).listFiles()){
			if(oneFile.length()!=0){
				bugFileList.add(oneFile.getAbsolutePath());
			}
			
		}
		for(File oneFile:new File(codeCorpusDirPath).listFiles()){
			BufferedReader reader=new BufferedReader(new FileReader(oneFile));
			String str=reader.readLine();
			if(str!=null){
				if(str.trim().length()!=0){
					codeFileList.add(oneFile.getAbsolutePath());
				}
			}
			reader.close();
		}
		corpusFileList.addAll(bugFileList);
		corpusFileList.addAll(codeFileList);
		HashMap<String, Double> corpusLM=getCorpusIEF(corpusFileList);
		Matrix probMat=new Matrix(bugFileList.size(),codeFileList.size());
		long totalIndexingTime=0;
		for(String oneCodeFile:codeFileList){
//			long mainTic=System.currentTimeMillis();
//			System.out.println("preprocessing "+oneCodeFile);
			if(oneCodeFile.endsWith(".java")){
				for(String oneBugFile:bugFileList){	
					double prob=computeRelevanceScore(oneCodeFile, oneBugFile,corpusLM, lambda);
					probMat.set(bugFileList.indexOf(oneBugFile), codeFileList.indexOf(oneCodeFile),Math.exp(prob));
				}
			}
			else{
				long tic=System.currentTimeMillis();
				HashMap<String,Integer> leafFileDistancePairs=getLeafFiles(oneCodeFile, codeFileList);
				totalIndexingTime+=(System.currentTimeMillis()-tic);
//				System.out.println(System.currentTimeMillis()-tic);
				int leafNum=leafFileDistancePairs.size();
				for(String oneBugFile:bugFileList){
					double sum=0.0d;
					tic=System.currentTimeMillis();
					String rootPackage=getRootPackage(codeFileList);
					double rootPackageScore=Math.exp(computeRelevanceScore(rootPackage, oneBugFile, corpusLM, lambda));
					totalIndexingTime+=(System.currentTimeMillis()-tic);
					
					for(Entry<String, Integer> onePair: leafFileDistancePairs.entrySet()){
						tic=System.currentTimeMillis();
						double prob=Math.exp(computeRelevanceScore(onePair.getKey(),oneBugFile, corpusLM, lambda));
						totalIndexingTime+=(System.currentTimeMillis()-tic);
						sum+=Math.pow(alpha, onePair.getValue()-1)*prob;
					}
					double propagationScore=sum*leafNum*rho+(1-rho)*rootPackageScore;
//					System.out.println(propagationScore);
					probMat.set(bugFileList.indexOf(oneBugFile), codeFileList.indexOf(oneCodeFile),propagationScore);
				}
			}
//			System.out.println(System.currentTimeMillis()-mainTic);
//			System.out.println(totalIndexingTime);
		}
		MatrixUtil.exportMatrix(bugFileList, codeFileList, probMat, simMatFilePath);
		return totalIndexingTime;
	}
	
	public static String getRootPackage(ArrayList<String> codeFileList){
		String rootPackage=null;
		int shortestLength=-1;
		for(String oneCodeFile:codeFileList){
			if(shortestLength==-1 || oneCodeFile.split("\\.").length<shortestLength){
				rootPackage=oneCodeFile;
				shortestLength=oneCodeFile.split("\\.").length;
			}
		}
		return rootPackage;
	}
	
	public static HashMap<String, Integer> getLeafFiles(String oneCodeFile,  ArrayList<String> codeFileList){
		HashMap<String, Integer> leafFileList=new HashMap<String, Integer>();
		for(String codeFile:codeFileList){
			if(codeFile.contains(oneCodeFile)){
				String diffStr=codeFile.substring(codeFile.indexOf(oneCodeFile)+1, codeFile.length());
				int distance=diffStr.split("\\.").length-1;
				leafFileList.put(codeFile, distance);
			}
		}
		return leafFileList;
	}
	public static double computeRelevanceScore(String codeFileName,String bugFileName, HashMap<String, Double> corpusLM, double alpha) throws Exception{
		HashMap<String, Double> docLM=getDocLM(codeFileName);
		int docSize=getDocSize(codeFileName);
		alpha=(docSize+0.0d)/(docSize+0.0d+1000);
		HashMap<String, Integer> docTF=getDocTF(bugFileName);
		double prob=getLogSmoothedLMProb(docTF, docLM, corpusLM,alpha);
		return prob;
	}
	
	
	public static long getAggregationMat(String bugCorpusDirPath, String codeCorpusDirPath, String simMatFilePath,double lambda) throws Exception{
		ArrayList<String> corpusFileList=new ArrayList<String>();
		ArrayList<String> bugFileList=new ArrayList<String>();
		ArrayList<String> codeFileList=new ArrayList<String>();
		for(File oneFile:new File(bugCorpusDirPath).listFiles()){
			if(oneFile.length()!=0){
				bugFileList.add(oneFile.getAbsolutePath());
			}
		}
		for(File oneFile:new File(codeCorpusDirPath).listFiles()){
			BufferedReader reader=new BufferedReader(new FileReader(oneFile));
			String str=reader.readLine();
			if(str!=null){
				if(str.trim().length()!=0){
					codeFileList.add(oneFile.getAbsolutePath());
				}
			}
			reader.close();
		}
		corpusFileList.addAll(bugFileList);
		corpusFileList.addAll(codeFileList);
//		int corpusSize=getCorpusSize(corpusFileList);	
		HashMap<String, Double> corpusLM=getCorpusIEF(corpusFileList);
		Matrix probMat=new Matrix(bugFileList.size(),codeFileList.size());
		ArrayList<Long> indexingTime=new ArrayList<Long> ();
		indexingTime.add((long) 0);
		for(String oneBugFile:bugFileList){
			HashMap<String, Double> aggregationScoreMap=getAggregationScoreMap(codeFileList, oneBugFile, corpusLM, lambda,indexingTime);
			for(String oneCodeFile:codeFileList){
				probMat.set(bugFileList.indexOf(oneBugFile), codeFileList.indexOf(oneCodeFile),aggregationScoreMap.get(oneCodeFile));
			}
			
		}
		MatrixUtil.exportMatrix(bugFileList, codeFileList, probMat, simMatFilePath);
		return indexingTime.get(0);
	}
	public static HashMap<String, Double> getAggregationScoreMap(ArrayList<String> codeFileList, String oneBugFile, HashMap<String, Double> corpusLM, double alpha, ArrayList<Long> indexingTime ) throws Exception{
		HashMap<String, Double> aggregationScoreMap=new HashMap<String, Double>();
		HashMap<String, HashMap<String, Double>>probDistributionMap=new HashMap<String, HashMap<String, Double>>();
//		ArrayList<Long> indexingTime=new ArrayList<Long>();
//		indexingTime.add((long) 0);
		for(String oneFile:codeFileList){
			updateAggregationScoreMap(oneFile, codeFileList, oneBugFile,aggregationScoreMap,probDistributionMap, corpusLM, alpha,indexingTime);
		}
//		System.out.println(indexingTime.get(0));
		return aggregationScoreMap;
	}
	
	
	public static HashMap<String, Double> updateAggregationScoreMap(String oneFile, ArrayList<String> codeFileList, String oneBugFile, HashMap<String, Double> map,HashMap<String, HashMap<String, Double>> fileLMPairs, HashMap<String, Double> corpusLM, double alpha, ArrayList<Long> indexingTime) throws Exception{
		if(map.containsKey(oneFile)){
			return fileLMPairs.get(oneFile);
		}
		double score=1.0d;	
		int mu=1000;
		
		if(oneFile.endsWith(".java")){
			HashMap<String,Double> docLM=getDocLM(oneFile);
			int docSize=getDocSize(oneFile);
			double lambda=(docSize+0.0d)/(docSize+0.0d+mu);
			HashMap<String, Double> smoothedLM=getSmoothedLM(docLM,corpusLM,lambda);
			fileLMPairs.put(oneFile, smoothedLM);
			score=Math.exp(computeRelevanceScore(oneFile, oneBugFile, corpusLM, alpha));
			map.put(oneFile, score);
			return smoothedLM;
		}		
		else{
			long tic=System.currentTimeMillis();
			ArrayList<String> childFiles=getChildFiles(oneFile, codeFileList);
			long toc=System.currentTimeMillis();
			indexingTime.set(0, indexingTime.get(0)+toc-tic);
			int childFileNum = childFiles.size();
			HashMap<String, Double> docLM=new HashMap<String, Double>();
			for(String oneChildFile:childFiles){
				HashMap<String, Double> childLM=updateAggregationScoreMap(oneChildFile,codeFileList, oneBugFile, map, fileLMPairs, corpusLM, alpha,indexingTime);
				for(Entry<String, Double> pair:childLM.entrySet()){
					if(docLM.containsKey(pair.getKey())){
						docLM.replace(pair.getKey(), docLM.get(pair.getKey())+pair.getValue()/(childFileNum+0.0d));
					}
					else{
						docLM.put(pair.getKey(),pair.getValue()/(childFileNum+0.0d));
					}
				}
			}
			fileLMPairs.put(oneFile, docLM);
			
			HashMap<String, Integer>docTF=getDocTF(oneBugFile);
			for(Entry<String, Integer> pair:docTF.entrySet()){
				if(docLM.containsKey(pair.getKey())){
					double corpusProb=corpusLM.get(pair.getKey());
					for(int i=0;i<pair.getValue();i++){
						score=score*corpusProb;
					}
				}
			}
			map.put(oneFile, score);
			return docLM;
		}
		
		
	}	
	
	public static ArrayList<String> getChildFiles(String oneCodeFile, ArrayList<String> codeFileList){
		ArrayList<String> childFileList=new ArrayList<String>();
		for(String oneFile: codeFileList){
			String processedFile=null;
			if(oneFile.endsWith(".java")){
				processedFile=oneFile.substring(0, oneFile.length()-".java".length());
			}
			else{
				processedFile=oneFile;
			}
			if(processedFile.contains(".") && processedFile.substring(0, processedFile.lastIndexOf(".")).equals(oneCodeFile)){
				childFileList.add(oneFile);
			}
		}
		return childFileList;
	}
	public static void getElementScoringMat(String bugCorpusDirPath, String codeCorpusDirPath, String simMatFilePath, double lambda) throws Exception{
		ArrayList<String> corpusFileList=new ArrayList<String>();
		ArrayList<String> bugFileList=new ArrayList<String>();
		ArrayList<String> codeFileList=new ArrayList<String>();
		for(File oneFile:new File(bugCorpusDirPath).listFiles()){
			if(oneFile.length()!=0){
				bugFileList.add(oneFile.getAbsolutePath());
			}
		}
		for(File oneFile:new File(codeCorpusDirPath).listFiles()){
			BufferedReader reader=new BufferedReader(new FileReader(oneFile));
			String str=reader.readLine();
			if(str!=null){
				if(str.trim().length()!=0){
					codeFileList.add(oneFile.getAbsolutePath());
				}
			}
			reader.close();
		}
		corpusFileList.addAll(bugFileList);
		corpusFileList.addAll(codeFileList);
		int corpusSize=getCorpusSize(corpusFileList);	
		HashMap<String, Double> corpusLM=getCorpusIEF(corpusFileList);
		//dirichlet smoothing \mu=1000
		int mu=1000;
		
		Matrix probMat=new Matrix(bugFileList.size(),codeFileList.size());
		for(String oneCodeFile:codeFileList){
			HashMap<String, Double> docLM=getDocLM(oneCodeFile);
			int docSize=getDocSize(oneCodeFile);
			double priorProb=(docSize+0.0d)/(corpusSize+0.0d);
			lambda=(docSize+0.0d)/(docSize+mu+0.0d);
			for(String oneBugFile:bugFileList){	
				HashMap<String, Integer> docTF=getDocTF(oneBugFile);
				double prob=Math.exp(getLogSmoothedLMProb(docTF, docLM, corpusLM,lambda));
				
				probMat.set(bugFileList.indexOf(oneBugFile), codeFileList.indexOf(oneCodeFile),prob*priorProb);
			}
		}
		MatrixUtil.exportMatrix(bugFileList, codeFileList, probMat, simMatFilePath);
	}
	
	public static void getLMProbMat(String bugCorpusDirPath, String codeCorpusDirPath, String simMatFilePath, double lambda) throws Exception{
		ArrayList<String> corpusFileList=new ArrayList<String>();
		ArrayList<String> bugFileList=new ArrayList<String>();
		ArrayList<String> codeFileList=new ArrayList<String>();
		for(File oneFile:new File(bugCorpusDirPath).listFiles()){
			if(oneFile.length()!=0){
				bugFileList.add(oneFile.getAbsolutePath());
			}
		}
		for(File oneFile:new File(codeCorpusDirPath).listFiles()){
//			System.out.println(oneFile.getAbsolutePath());
			BufferedReader reader=new BufferedReader(new FileReader(oneFile));
			String str=reader.readLine();
			if(str!=null){
				if(str.trim().length()!=0){
					codeFileList.add(oneFile.getAbsolutePath());
				}
			}
			reader.close();
		}
		corpusFileList.addAll(bugFileList);
		corpusFileList.addAll(codeFileList);
		HashMap<String, Double> corpusLM=getCorpusIEF(corpusFileList);
		Matrix probMat=new Matrix(bugFileList.size(),codeFileList.size());
		int mu=1000;
		for(String oneCodeFile:codeFileList){
			int docSize=getDocSize(oneCodeFile);
			lambda=(docSize+0.0d)/(docSize+mu+0.0d);
			HashMap<String, Double> docLM=getDocLM(oneCodeFile);
			for(String oneBugFile:bugFileList){		
				HashMap<String, Integer> docTF=getDocTF(oneBugFile);
				double prob=getLogSmoothedLMProb(docTF, docLM, corpusLM,lambda);
				probMat.set(bugFileList.indexOf(oneBugFile), codeFileList.indexOf(oneCodeFile), prob);
			}
		}
		MatrixUtil.exportMatrix(bugFileList, codeFileList, probMat, simMatFilePath);
	}
	public static HashMap<String, Double> getSmoothedLM(HashMap<String, Double> docLM, HashMap<String, Double> corpusLM, double lambda){
		HashMap<String,Double> smoothedLM=new HashMap<String, Double> ();
		for(Entry<String, Double> pair:corpusLM.entrySet()){
			String term=pair.getKey();
			double prob=(1-lambda)*pair.getValue();
			if(docLM.containsKey(term)){
				prob+=lambda*docLM.get(term);
			}
			smoothedLM.put(term,prob);
		}
		return smoothedLM;
	}
	public static double getLogSmoothedLMProb(HashMap<String, Integer> docTF, HashMap<String, Double> docLM, HashMap<String, Double> corpusLM, double lambda){
		double logSmoothedLMProb=0.0d;
		for(Entry<String, Integer> pair:docTF.entrySet()){
			if(corpusLM.containsKey(pair.getKey())){
				double corpusProb=corpusLM.get(pair.getKey());
				double docProb=0.0d;
				if(docLM.containsKey(pair.getKey())){
					docProb=docLM.get(pair.getKey());
				}
				double smoothedProb=docProb*lambda+corpusProb*(1-lambda);
				for(int i=0;i<pair.getValue();i++){
					logSmoothedLMProb=logSmoothedLMProb+Math.log(smoothedProb);
//					smoothedLMProb=smoothedLMProb*smoothedProb;
				}
				
				
			}
		}

		return logSmoothedLMProb;
	}
	public static int getDocSize(String oneDocFile) throws Exception{
		BufferedReader reader=new BufferedReader(new FileReader(oneDocFile));
		String contentStr=reader.readLine();
		reader.close();
		return contentStr.split(" ").length;
	}
	public static HashMap<String, Integer> getDocTF(String oneDocFile) throws Exception{
		HashMap<String, Integer> docTF=new HashMap<String, Integer>();
		BufferedReader reader=new BufferedReader(new FileReader(oneDocFile));
		String contentStr=reader.readLine();
		for(String term:contentStr.split(" ")){
			if(docTF.containsKey(term)){
				int tfValue=docTF.get(term);
				tfValue++;
				docTF.replace(term, tfValue);
			}
			else{
				docTF.put(term, 1);
			}
		}
		reader.close();
		return docTF;
	}
	public static HashMap<String, Double> getCorpusIEF(ArrayList<String> corpusFileList) throws Exception {
		int corpusElementNum=corpusFileList.size();
		HashMap<String, Double> corpusIEF=new HashMap<String, Double>();
		HashMap<String, Integer> corpusDF=new HashMap<String, Integer>();
		for(String oneFile:corpusFileList){
			BufferedReader reader=new BufferedReader(new FileReader(oneFile));
			String contentStr=reader.readLine();
			HashSet<String> docVocabulary=new HashSet<String>();
			for(String term:contentStr.split(" ")){
				docVocabulary.add(term);
			}
			for(String term:docVocabulary){
				if(corpusDF.containsKey(term)){
					int dfValue=corpusDF.get(term);
					dfValue++;
					corpusDF.replace(term, dfValue);
				}
				else{
					corpusDF.put(term, 1);
				}
			}
			reader.close();
		}
		double sum=0.0d;
		for(Entry<String, Integer> pair:corpusDF.entrySet()){
			double idfVal=Math.log((corpusElementNum+0.0d)/(pair.getValue()+0.0d));
			corpusIEF.put(pair.getKey(), idfVal);
			sum+=idfVal;
		}
		for(Entry<String, Double> pair:corpusIEF.entrySet()){
			
			corpusIEF.put(pair.getKey(), (pair.getValue()+0.0d)/(sum+0.0d));
		}
		return corpusIEF;
	}
	public static HashMap<String, Double> getDocLM(String oneDocFile) throws Exception{
		ArrayList<String> corpusFileList=new ArrayList<String>();
		corpusFileList.add(oneDocFile);
		return(getCorpusLM(corpusFileList));
	}
	public static int getCorpusSize(ArrayList<String> corpusFileList) throws Exception{
		int collectionTermCount=0;
		for(String oneFile:corpusFileList){
			BufferedReader reader=new BufferedReader(new FileReader(oneFile));
			String contentStr=reader.readLine();
			collectionTermCount+=contentStr.split(" ").length;
			reader.close();
		}
		return collectionTermCount;
	}
	public static HashMap<String, Double> getCorpusLM(ArrayList<String> corpusFileList) throws Exception{
		HashMap<String, Integer> termTFPairs=new HashMap<String, Integer>();
		HashMap<String, Double> corpusLM=new HashMap<String, Double>();
		int collectionTermCount=0;
		for(String oneFile:corpusFileList){
			BufferedReader reader=new BufferedReader(new FileReader(oneFile));
			String contentStr=reader.readLine();
			for(String term:contentStr.split(" ")){
				if(termTFPairs.containsKey(term)){
					int tfValue=termTFPairs.get(term);
					tfValue++;
					termTFPairs.replace(term, tfValue);
				}
				else{
					termTFPairs.put(term, 1);
				}
				collectionTermCount++;
			}
			reader.close();
		}
		for(Entry<String, Integer> onePair: termTFPairs.entrySet()){
			corpusLM.put(onePair.getKey(), (onePair.getValue()+0.0d)/(collectionTermCount+0.0d));
		}
		return corpusLM;
	}
	
	public static void getFileList(String bugCorpusDirPath,String codeCorpusDirPath, 
									ArrayList<String> corpusFileList, 
									ArrayList<String> bugFileList,
									ArrayList<String> codeFileList) throws IOException{
		for(File oneFile:new File(bugCorpusDirPath).listFiles()){
			if(oneFile.length()!=0){
				bugFileList.add(oneFile.getAbsolutePath());
			}
			
		}
		for(File oneFile:new File(codeCorpusDirPath).listFiles()){
			BufferedReader reader=new BufferedReader(new FileReader(oneFile));
			String str=reader.readLine();
			if(str!=null){
				if(str.trim().length()!=0){
					codeFileList.add(oneFile.getAbsolutePath());
				}
			}
			reader.close();
		}
		corpusFileList.addAll(bugFileList);
		corpusFileList.addAll(codeFileList);
		
	}
	
	
	public static ArrayList<TreeNode<ProgramElement>> getPackageStructure(ArrayList<String> codeFileList){
		ArrayList<TreeNode<ProgramElement>> treeList=new ArrayList<TreeNode<ProgramElement>>();
		TreeNode<ProgramElement> rootNode=new TreeNode<ProgramElement>(null);
		treeList.add(rootNode);
		
		for(String oneCodeFile:codeFileList){
			String packagePath=oneCodeFile.substring(oneCodeFile.lastIndexOf("\\")+1, oneCodeFile.length());
			if(packagePath.endsWith(".java")){
				packagePath=packagePath.substring(0,packagePath.length()-".java".length());
			}
//			System.out.println(packagePath);
			String []packages=packagePath.split("\\.");
			if(rootNode.isLeaf()){
				ProgramElement rootElement=new ProgramElement();
				rootElement.setElementName(packages[0]);
				rootElement.setFullElementPath(packages[0]);
				rootNode.setData(rootElement);
				TreeNode<ProgramElement> fatherNode=rootNode;
				for(int index=1;index<packages.length;index++){
					ProgramElement childElement=new ProgramElement();
					childElement.setElementName(packages[index]);
					childElement.setFullElementPath(fatherNode.getData().getFullElementPath()+"."+packages[index]);
					TreeNode<ProgramElement> childNode=new TreeNode<ProgramElement>(childElement,fatherNode);
					treeList.add(childNode);
					fatherNode=childNode;
				}
			}
			else{
				TreeNode<ProgramElement> fatherNode=rootNode;
				for(int i=1;i<packagePath.length();i++){
					ProgramElement oneElement=new ProgramElement();
					oneElement.setElementName(packages[i]);
					oneElement.setFullElementPath(fatherNode.getData().getFullElementPath()+"."+packages[i]);
					if(fatherNode.hasChild(oneElement)){
						fatherNode=fatherNode.getChild(oneElement);
					}
					else{
						for(int index=i;index<packages.length;index++){
							oneElement.setElementName(packages[index]);
							oneElement.setFullElementPath(fatherNode.getData().getFullElementPath()+"."+packages[index]);
							TreeNode<ProgramElement> childNode=new TreeNode<ProgramElement>(oneElement,fatherNode);
							treeList.add(childNode);
							fatherNode=childNode;
						}
						break;
					}
				}
			}
		}
		return treeList;
	}
	
	
	public static void main(String[] args) throws Exception {
//		 TODO Auto-generated method stub
		String bugDirPath="C:/Users/dell/Documents/EClipse/experimentResult/Corpus/Myfaces-2.0.1/bug/summary";
		String codeDirPath="C:/Users/dell/Documents/EClipse/experimentResult/Corpus/Myfaces-2.0.1/code/codeContentCorpus";
		ArrayList<String> corpusFileList=new ArrayList<String>();
		ArrayList<String> bugFileList=new ArrayList<String>();
		ArrayList<String> codeFileList=new ArrayList<String>();
		getFileList(bugDirPath, codeDirPath, corpusFileList, bugFileList, codeFileList);
		int num=0;
		for(String oneCodeFile:codeFileList){
			if(!oneCodeFile.endsWith(".java")){
				num++;
			}
		}
		System.out.println(num);
		
//		for(String oneCodeFile: codeFileList){
//			String packagePath=oneCodeFile.substring(oneCodeFile.lastIndexOf("\\")+1, oneCodeFile.length());
//			String []packages=packagePath.split("\\.");
//			System.out.println(packages[0]);
//		}
//		String simMatFilePath="C:/Users/dell/Documents/EClipse/experimentResult/probMat";
//		long tic=System.currentTimeMillis();
//		long indexingTime=getAggregationMat(bugDirPath, codeDirPath,simMatFilePath,0.1);
//		long indexingTime=getPropagationMat(bugDirPath, codeDirPath,simMatFilePath,0.9,0.9,0.1);
//		long toc=System.currentTimeMillis();
//		System.out.println(toc-tic);
//		System.out.println(indexingTime);

//		ArrayList<String> fileList=new ArrayList<String>();
//		String filePath1="C:/Users/dell/Documents/EClipse/experimentResult/c.txt";
//		String filePath2="C:/Users/dell/Documents/EClipse/experimentResult/b.txt";
//		String filePath3="C:/Users/dell/Documents/EClipse/experimentResult/a.txt";
//		fileList.add(filePath1);
//		fileList.add(filePath2);
//		HashMap<String, Integer> docTF = getDocTF(filePath3);
//		HashMap<String, Double> docLM = getDocLM(filePath2);
//		HashMap<String, Double> corpusLM = getCorpusIEF(fileList);
//		for(Entry<String, Double> pair:corpusLM.entrySet()){
//			System.out.println(pair.getKey()+" "+pair.getValue());
//		}
//		System.out.println(getLogSmoothedLMProb(docTF, docLM, corpusLM, 0.1));
		
	}

}
