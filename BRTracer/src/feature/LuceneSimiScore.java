package feature;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import bug.BugDataProcessor;
import config.Config;
import sourcecode.CodeDataProcessor;
import sourcecode.SourceCodeCorpus;
import utils.MatrixUtil;
import Jama.Matrix;

public class LuceneSimiScore {
	public static void generate(String bugCorpusPath, String codeCorpusPath, String scoreMatFilePath) throws Exception{
		Version matchVersion=Version.LATEST;
		Analyzer queryAnalyzer=new EnglishAnalyzer();
		queryAnalyzer.setVersion(matchVersion);
		Directory codeDir = FSDirectory.open(Paths.get(codeCorpusPath,"index"));
		IndexReader codeReader = DirectoryReader.open(codeDir);

		IndexSearcher searcher = new IndexSearcher(codeReader);
		searcher.setSimilarity(new BM25Similarity());
		QueryParser parser=new QueryParser("content", queryAnalyzer);
		
		
		Directory bugDir = FSDirectory.open(Paths.get(bugCorpusPath,"index"));
		IndexReader bugReader = DirectoryReader.open(bugDir);
		
		int bugNum=bugReader.numDocs();
		ArrayList<String> bugList = new ArrayList<String>();
		for(int i=0;i<bugNum;i++){
			Document doc=bugReader.document(i);
			bugList.add(doc.get("bugID"));
		}
		
		int codeNum=codeReader.numDocs();
		ArrayList<String> codeList = new ArrayList<String>();
		for(int i=0;i<codeNum;i++){
			Document doc=codeReader.document(i);
			codeList.add(doc.get("fullClassName"));
		}
		
		Matrix simMat=new Matrix(bugNum, codeNum);
		String queryString="";
		for(int i=0;i<bugNum;i++){
			Document doc=bugReader.document(i);
			queryString= doc.get("bugInformation");
			System.out.println(queryString);
			Query query=parser.parse(queryString.replace("[", "").replace("]", ""));
			for (ScoreDoc oneScore:searcher.search(query,codeNum).scoreDocs){
				simMat.set(i, oneScore.doc, oneScore.score);
			}
		}
		bugReader.close();
		codeReader.close();
		
		
		MatrixUtil.exportMatrix(bugList, codeList, simMat, scoreMatFilePath);
	}
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		
		
		String bugCorpusDirPath=Paths.get(Config.getInstance().getIntermediateDir(), "bug").toString();
		Config.getInstance().setBugCorpusDir(bugCorpusDirPath);
		BugDataProcessor.indexBugCorpus(BugDataProcessor.importFromXML());
		
		String codeCorpusDirPath=Paths.get(Config.getInstance().getIntermediateDir(), "code").toString();
		Config.getInstance().setCodeCorpusDir(codeCorpusDirPath);
		CodeDataProcessor.indexCodeData(CodeDataProcessor.extractCodeData());
		
		String simScorePath="C:/Users/ql29/Documents/EClipse/simScore";
		generate(bugCorpusDirPath,codeCorpusDirPath, simScorePath);
	}

}
