package feature;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

import Jama.Matrix;
import utils.MatrixUtil;
import utils.TextAnalyzer;
import utils.WVToolWrapper;
import config.Config;
import edu.udo.cs.wvtool.main.WVTFileInputList;
import edu.udo.cs.wvtool.wordlist.WVTWordList;

public class LuceneVectorCreator {
	public static final FieldType TYPE_STORED = new FieldType();
	static {
		TYPE_STORED.setStored(true);
		TYPE_STORED.setTokenized(true);
		TYPE_STORED.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
		TYPE_STORED.setStoreTermVectors(true);
		TYPE_STORED.setStoreTermVectorPositions(true);
		TYPE_STORED.freeze();
	}
	
	/**
	 * 
	 * @param bugCorpusDirPath
	 * @param codeCorpusDirPath
	 * @param simMatFilePath
	 * @throws Exception
	 */
	public static void generate(String bugCorpusDirPath, String codeCorpusDirPath, String simMatFilePath) throws Exception{
		
		Directory codeDir = FSDirectory.open(Paths.get(codeCorpusDirPath,"index"));
		IndexReader codeReader = DirectoryReader.open(codeDir);
		
		int codeNum=codeReader.numDocs();
		
		String codeVectorFile = "C:/Users/ql29/Documents/EClipse/BugLocatorTool/tmp/output_2.txt";
		FileWriter writer = new FileWriter(codeVectorFile);
		
		HashMap<String, HashMap<String, Double>> codeVectorList = new HashMap<String, HashMap<String, Double>>();
		int sum=0;
		for(int i=0;i<codeNum;i++){
			if(codeReader.getTermVector(i, "content")!=null){
				sum++;
				TermsEnum iter = codeReader.getTermVector(i, "content").iterator();
				
	//			int totalTermCount = (int) codeReader.getTermVector(i, "content").size();
				BytesRef str=new BytesRef();
				HashMap<String, Double> documentVec = new HashMap<String, Double>();
				StringBuffer buf = new StringBuffer();
				int docLength = 0;
				while((str=iter.next())!=null){
					String term=str.utf8ToString();
					
					int termFreq = (int)iter.totalTermFreq();
					int docFreq = codeReader.docFreq(new Term("content",term));
					buf.append(term+":"+termFreq+" "+docFreq+"\t");
					docLength+=termFreq;
					double tfidfWeight = (Math.log(termFreq)+1) * Math.log((codeNum)/(docFreq));
					documentVec.put(term, tfidfWeight);
				}
				writer.write(codeReader.document(i).get("fullClassName")+"\t"+docLength+";"+buf.toString()+"\n");
				codeVectorList.put(codeReader.document(i).get("fullClassName"), documentVec);
			}
		}
		writer.close();
		System.out.println(sum);
		
		Directory bugDir = FSDirectory.open(Paths.get(bugCorpusDirPath,"index"));
		IndexReader bugReader = DirectoryReader.open(bugDir);
		
		int bugNum=bugReader.numDocs();
		HashMap<String, HashMap<String, Double>> bugVectorList = new HashMap<String, HashMap<String, Double>>();
		
		for(int i=0;i<bugNum;i++){
			if(bugReader.getTermVector(i,"bugInformation")!=null){
				TermsEnum iter = bugReader.getTermVector(i, "bugInformation").iterator();
	//			int totalTermCount = (int) codeReader.getTermVector(i, "content").size();
				BytesRef str=new BytesRef();
				HashMap<String, Double> documentVec = new HashMap<String, Double>();
				while((str=iter.next())!=null){
					String term=str.utf8ToString();
					int termFreq = (int)iter.totalTermFreq();
					int docFreq = codeReader.docFreq(new Term("content",term));
					if(docFreq!=0){
						double tfidfWeight = (Math.log(termFreq)+1) * Math.log((codeNum)/(docFreq));
						documentVec.put(term, tfidfWeight);
					}
				}
				bugVectorList.put(bugReader.document(i).get("bugID"), documentVec);
			}
		}
		
		String [] bugIDList = bugVectorList.keySet().toArray(new String[0]);
		String [] codeIDList = codeVectorList.keySet().toArray(new String[0]);
		
		ArrayList<String> bugIDArray= new ArrayList<String>(Arrays.asList(bugIDList));
		ArrayList<String> codeIDArray= new ArrayList<String>(Arrays.asList(codeIDList));
		Matrix simMat=new Matrix(bugIDList.length, codeIDList.length);
		System.out.println("bugNum="+bugIDList.length+"\tcodeNum="+codeIDList.length);
		for(int i=0; i< bugIDList.length; i++){
			for(int j=0; j<codeIDList.length; j++){
				double similarity = computeSimilarity(bugVectorList.get(bugIDList[i]), codeVectorList.get(codeIDList[j]));
				simMat.set(i, j, similarity);
			}
		}
		MatrixUtil.exportMatrix(bugIDArray, codeIDArray, simMat, simMatFilePath);
		bugReader.close();
		codeReader.close();
	}
	
	public static double computeSimilarity(HashMap<String, Double> map1, HashMap<String, Double> map2){
		double length1=0.0d;
		double length2=0.0d;
		double intersect=0.0d;
		for(Entry<String, Double> entry: map1.entrySet()){
			length1+=entry.getValue()*entry.getValue();
		}
		
		for(Entry<String, Double> entry: map2.entrySet()){
			length2+=entry.getValue()*entry.getValue();
			if(map1.containsKey(entry.getKey())){
				intersect+=entry.getValue()*map1.get(entry.getKey());
			}
		}
		return intersect/Math.sqrt(length1*length2);
	}
	public static void main(String[] args) {
	
	}

}
