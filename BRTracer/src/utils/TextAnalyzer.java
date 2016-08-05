package utils;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseTokenizer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.util.Version;

public class TextAnalyzer extends Analyzer{
	@Override
	protected TokenStreamComponents createComponents(String field) {
		
		// TODO Auto-generated method stub
		Tokenizer source = new LowerCaseTokenizer();
		return new TokenStreamComponents(source, new PorterStemFilter(source));
	}
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		EnglishAnalyzer en_an = new EnglishAnalyzer();
		QueryParser parser = new QueryParser("yas", en_an);
		String str = "[ZOOKEEPER-1271] testEarlyLeaderAbandonment failing on solaris";
		str=str.replace("[", "");
		str=str.replace("]", "");
		System.out.println("result: " + parser.parse(str)); //amenit
	}
}
