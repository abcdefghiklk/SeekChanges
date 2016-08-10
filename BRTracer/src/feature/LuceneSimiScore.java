package feature;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterFilter;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterFilterFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import bug.BugDataProcessor;
import config.Config;
import sourcecode.CodeDataProcessor;
import sourcecode.SourceCodeCorpus;
import utils.MatrixUtil;
import utils.TextAnalyzer;
import Jama.Matrix;

public class LuceneSimiScore {
	public static void generate(String bugCorpusPath, String codeCorpusPath, String scoreMatFilePath) throws Exception{
		Version matchVersion=Version.LATEST;
		Analyzer queryAnalyzer=new TextAnalyzer();
		queryAnalyzer.setVersion(matchVersion);
		Directory codeDir = FSDirectory.open(Paths.get(codeCorpusPath,"index"));
		IndexReader codeReader = DirectoryReader.open(codeDir);

		IndexSearcher searcher = new IndexSearcher(codeReader);
		Similarity s=new OriginalTFIDFSimilarity();
		searcher.setSimilarity(s);
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
			Query query=parser.parse(queryString);
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
		String corpusPath="C:/Users/ql29/Documents/EClipse/testData";
		String indexPath="C:/Users/ql29/Documents/EClipse/index";
		Version matchVersion=Version.LATEST;
		Analyzer analyzer=new TextAnalyzer();
		analyzer.setVersion(matchVersion);
		Directory dir = FSDirectory.open(Paths.get(indexPath));
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
		iwc.setOpenMode(OpenMode.CREATE);
		IndexWriter writer = new IndexWriter(dir, iwc);
		for(int i=1;i<=3;i++){
			String fileName=Paths.get(corpusPath,String.valueOf(i)+".txt").toString();
			BufferedReader reader=new BufferedReader(new FileReader(fileName));
			Document doc=new Document();
			TextField field=new TextField("content",reader.readLine(),Store.YES);
			doc.add(field);
			writer.addDocument(doc);
			reader.close();
		}
		
		writer.close();
		
		IndexReader codeReader = DirectoryReader.open(dir);

		IndexSearcher searcher = new IndexSearcher(codeReader);
		Similarity s=new BM25Similarity();
		searcher.setSimilarity(s);
		QueryParser parser=new QueryParser("content", analyzer);
		Query query=parser.parse("PowerShot this is a good day today");
		for (ScoreDoc oneScore:searcher.search(query,3).scoreDocs){
			System.out.println(searcher.explain(query, oneScore.doc));
		}

//		System.out.println(wdf.toString());
		
		
		
	}

}
