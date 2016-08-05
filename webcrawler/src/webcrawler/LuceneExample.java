package webcrawler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.AnalyzerWrapper;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.core.LowerCaseTokenizer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CodecReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexReader.ReaderClosedListener;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.DFISimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.QueryBuilder;
import org.apache.lucene.util.Version;

public class LuceneExample {

	public static void index(String corpusDirPath, String indexStorePath) throws Exception{
		File corpusDir=new File(corpusDirPath);
		if(!corpusDir.isDirectory()){
			System.out.println("The input directory path is invalid!");
			return;
		}
		
		
		Version matchVersion=Version.LATEST;
		Directory dir = FSDirectory.open(Paths.get(indexStorePath));
		Analyzer analyzer = new StandardAnalyzer();
		analyzer.setVersion(matchVersion);
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
//		iwc.setSimilarity(new ClassicSimilarity());
		iwc.setOpenMode(OpenMode.CREATE);
		IndexWriter writer = new IndexWriter(dir, iwc);
		
		
		for(File oneFile: corpusDir.listFiles()){
			
			Document doc=new Document();
			BufferedReader reader=new BufferedReader(new FileReader(oneFile));
			String line=reader.readLine();
			reader.close();
			TextField contentField = new TextField("fileContents", line, Store.YES);
			StringField fullPathField = new StringField("fullFilePath", oneFile.getAbsolutePath(), Store.YES);
			StringField fileNameField = new StringField("fileName", oneFile.getName(), Store.YES);
			doc.add(contentField);
			doc.add(fullPathField);
			doc.add(fileNameField);
			writer.addDocument(doc);
		}
		
		writer.close();
	}
	
	public static void search(String indexStorePath, String queryString) throws Exception{
		Version matchVersion=Version.LATEST;
		Analyzer queryAnalyzer=new EnglishAnalyzer();
		queryAnalyzer.setVersion(matchVersion);
		Directory dir = FSDirectory.open(Paths.get(indexStorePath));
		IndexReader reader=DirectoryReader.open(dir);

		IndexSearcher searcher=new IndexSearcher(reader);
		searcher.setSimilarity(new BM25Similarity());
		QueryParser parser=new QueryParser("fileContents", queryAnalyzer);
		Query query=parser.parse(queryString);
		System.out.println(query.toString());
		for (ScoreDoc oneScore:searcher.search(query, 3).scoreDocs){
			System.out.println(reader.document(oneScore.doc).get("fileContents")+"\t"+oneScore.score);
		}
		reader.close();
	}
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		String inputPath="C:/Users/ql29/Documents/EClipse/testData";
		String outputPath="C:/Users/ql29/Documents/EClipse/index";
		index(inputPath, outputPath);
		
//		Directory outputDir = FSDirectory.open(Paths.get(outputPath));
//		IndexReader reader = DirectoryReader.open(outputDir);
//		for(int i=0;i<reader.numDocs();i++){
//			Document doc=reader.document(i);
//			System.out.println(doc.get("fileContents"));
//		}
		search(outputPath,"This is a fine day today");
//		long startTime=System.currentTimeMillis();
//		String indexStorePath="C:/Users/ql29/Documents/EClipse/index";
//		Version matchVersion=Version.LATEST;
//		Directory dir = FSDirectory.open(Paths.get(indexStorePath));
//		Analyzer analyzer = new StandardAnalyzer();
//		analyzer.setVersion(matchVersion);
//		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
//		iwc.setSimilarity(new ClassicSimilarity());
//		iwc.setOpenMode(OpenMode.CREATE);
//		IndexWriter writer = new IndexWriter(dir, iwc);
//		
//		for(int i=1;i<=3;i++){
//			String filePath="C:/Users/ql29/Documents/EClipse/"+String.valueOf(i)+".txt";
//			File f=new File(filePath);
//			Document doc=new Document();
//			BufferedReader reader=new BufferedReader(new FileReader(f));
//			String line=reader.readLine();
//			reader.close();
//			TextField contentField = new TextField("fileContents",line, Store.YES);
//
//			doc.add(contentField);
//			writer.addDocument(doc);
//		}
//		
//		writer.close();
//		Analyzer queryAnalyzer=new StandardAnalyzer();
//		queryAnalyzer.setVersion(matchVersion);
//		IndexReader reader=DirectoryReader.open(dir);
//
//		IndexSearcher searcher=new IndexSearcher(reader);
//		searcher.setSimilarity(new BM25Similarity());
//		QueryParser parser=new QueryParser("fileContents", queryAnalyzer);
//		String queryString="This is a fine day today";
//		Query query=parser.parse(queryString);
//		for (ScoreDoc oneScore:searcher.search(query, 3).scoreDocs){
//			System.out.println(oneScore.doc+"\t"+oneScore.score);
//		}
//		long endTime=System.currentTimeMillis();
//		System.out.println(endTime-startTime);
	}

}
