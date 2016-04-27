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
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.QueryBuilder;
import org.apache.lucene.util.Version;

public class LuceneExample {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
//		Version matchVersion = Version.LATEST; // Substitute desired Lucene version for XY
//	    Analyzer analyzer = new StandardAnalyzer(); // or any other analyzer
//	    analyzer.setVersion(matchVersion);
//	    TokenStream ts = analyzer.tokenStream("asa", new StringReader("some text goes here"));
//	    // The Analyzer class will construct the Tokenizer, TokenFilter(s), and CharFilter(s),
//	    //   and pass the resulting Reader to the Tokenizer.
//	    OffsetAttribute offsetAtt = ts.addAttribute(OffsetAttribute.class);
//	     
//	    try {
//	    	ts.reset(); // Resets this stream to the beginning. (Required)
//	    	while (ts.incrementToken()) {
//	    		// Use AttributeSource.reflectAsString(boolean)
//	    		// for token stream debugging.
//	    		System.out.println("token: " + ts.reflectAsString(true));
//	 
//	    		System.out.println("token start offset: " + offsetAtt.startOffset());
//	    		System.out.println("  token end offset: " + offsetAtt.endOffset());
//	    	}
//	    	ts.end();   // Perform end-of-stream operations, e.g. set the final offset.
//	    } 	
//	    finally {
//	    	ts.close(); // Release resources associated with this stream.
//	    }
//	    analyzer.close();
		
		
		long startTime=System.currentTimeMillis();
		String indexStorePath="C:/Users/dell/Documents/EClipse/index";
		Version matchVersion=Version.LATEST;
		Directory dir = FSDirectory.open(Paths.get(indexStorePath));
		Analyzer analyzer = new StandardAnalyzer();
		analyzer.setVersion(matchVersion);
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
		iwc.setSimilarity(new ClassicSimilarity());
		iwc.setOpenMode(OpenMode.CREATE);
		IndexWriter writer = new IndexWriter(dir, iwc);
//
		for(int i=1;i<=3;i++){
			String filePath="C:/Users/dell/Documents/EClipse/"+String.valueOf(i)+".txt";
			File f=new File(filePath);
			Document doc=new Document();
			BufferedReader reader=new BufferedReader(new FileReader(f));
			String line=reader.readLine();
			reader.close();
			TextField contentField = new TextField("fileContents",line, Store.YES);

			doc.add(contentField);
			writer.addDocument(doc);
		}
		
		writer.close();
		Analyzer queryAnalyzer=new StandardAnalyzer();
		queryAnalyzer.setVersion(matchVersion);
		IndexReader reader=DirectoryReader.open(dir);

		IndexSearcher searcher=new IndexSearcher(reader);
		searcher.setSimilarity(new LMDirichletSimilarity());
		QueryParser parser=new QueryParser("fileContents", queryAnalyzer);
		String queryString="This is a fine day today";
		Query query=parser.parse(queryString);
//		System.out.println(query.toString());
		for (ScoreDoc oneScore:searcher.search(query, 3).scoreDocs){
			System.out.println(oneScore.doc+"\t"+oneScore.score);
		}
		long endTime=System.currentTimeMillis();
		System.out.println(endTime-startTime);
	}

}
