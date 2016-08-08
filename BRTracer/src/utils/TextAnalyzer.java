package utils;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;

public class TextAnalyzer extends Analyzer{

	@Override
	protected TokenStreamComponents createComponents(String field) {
		// TODO Auto-generated method stub
		StandardTokenizer src=new StandardTokenizer();
		
		TokenStream filter= new StandardFilter(src);
		
		WordDelimiterFilter wdf= new WordDelimiterFilter(filter, WordDelimiterFilter.ALPHA|WordDelimiterFilter.SPLIT_ON_CASE_CHANGE, null);
		filter=new LowerCaseFilter(wdf);
		filter=new PorterStemFilter(filter);
		return new TokenStreamComponents(src, filter);
	}
	
	public static void main(String []args) throws Exception{
		Analyzer analyzer=new TextAnalyzer();
		analyzer.setVersion(Version.LATEST);
		QueryParser parser=new QueryParser("content", analyzer);
		Query query=parser.parse("usePackage [] WI-FI this is a good day today");
		System.out.println(query.toString());
	}

}
