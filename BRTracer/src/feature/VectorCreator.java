package feature;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Paths;
import java.util.HashMap;

import config.Config;
import utils.MatrixUtil;
import utils.WVToolWrapper;
import Jama.Matrix;
import edu.udo.cs.wvtool.config.WVTConfiguration;
import edu.udo.cs.wvtool.config.WVTConfigurationFact;
import edu.udo.cs.wvtool.generic.charmapper.DummyCharConverter;
import edu.udo.cs.wvtool.generic.charmapper.WVTCharConverter;
import edu.udo.cs.wvtool.generic.inputfilter.TextInputFilter;
import edu.udo.cs.wvtool.generic.inputfilter.WVTInputFilter;
import edu.udo.cs.wvtool.generic.loader.SourceAsTextLoader;
import edu.udo.cs.wvtool.generic.loader.UniversalLoader;
import edu.udo.cs.wvtool.generic.stemmer.DummyStemmer;
import edu.udo.cs.wvtool.generic.stemmer.WVTStemmer;
import edu.udo.cs.wvtool.generic.tokenizer.NGramTokenizer;
import edu.udo.cs.wvtool.generic.tokenizer.SimpleTokenizer;
import edu.udo.cs.wvtool.generic.tokenizer.WVTTokenizer;
import edu.udo.cs.wvtool.generic.wordfilter.DummyWordFilter;
import edu.udo.cs.wvtool.generic.wordfilter.WVTWordFilter;
import edu.udo.cs.wvtool.main.WVTDocumentInfo;
import edu.udo.cs.wvtool.main.WVTFileInputList;
import edu.udo.cs.wvtool.main.WVTool;
import edu.udo.cs.wvtool.util.TokenEnumeration;
import edu.udo.cs.wvtool.util.WVToolException;
import edu.udo.cs.wvtool.wordlist.WVTWordList;

public class VectorCreator {
	/**
	 * Create the code and bug vectors and save them in files
	 * @param bugCorpusDirPath
	 * @param codeCorpusDirPath
	 * @param bugVecFilePath
	 * @param codeVecFilePath
	 * @throws Exception
	 */
	public static void create(String bugCorpusDirPath, String codeCorpusDirPath, String bugVecFilePath, String codeVecFilePath) throws Exception{
		
		//Extract dictionaries for bug and code corpus
		String []bugAndCodeDirPaths={bugCorpusDirPath,codeCorpusDirPath};
		WVTFileInputList bugAndCodeList=WVToolWrapper.extractCorpusFileList(bugAndCodeDirPaths);
		WVTWordList dic=WVToolWrapper.extractCorpusDic(bugAndCodeList);
		
		//Extract the bug list and code list
		WVTFileInputList bugList=WVToolWrapper.extractCorpusFileList(bugCorpusDirPath);
		WVTFileInputList codeList=WVToolWrapper.extractCorpusFileList(codeCorpusDirPath);
		
		//Generate Vectors and save them to files
		WVToolWrapper.generateVectors(bugVecFilePath, bugList, dic);
		WVToolWrapper.generateVectors(codeVecFilePath, codeList, dic);
		
		//set the dictionary size
		Config.getInstance().setDicSize(dic.getNumWords());
	}
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		String targetDirPath="C:/Users/ql29/Documents/EClipse/testData";
		String filePath=Paths.get(targetDirPath, "1.txt").toString();
//		WVTool wvt= new WVTool(false);
//		WVTFileInputList list = new WVTFileInputList(1);
//		list.addEntry(new WVTDocumentInfo(targetDirPath,
//					"txt", "", "english", 0));
//		WVTConfiguration config=new WVTConfiguration();
//		config.setConfigurationRule(WVTConfiguration.STEP_STEMMER, new WVTConfigurationFact(new DummyStemmer()));
//		config.setConfigurationRule(WVTConfiguration.STEP_WORDFILTER, new WVTConfigurationFact(new DummyWordFilter()));
//		config.setConfigurationRule(WVTConfiguration.STEP_CHAR_MAPPER, new WVTConfigurationFact(new DummyCharConverter()));
//		WVTWordList dictionary = wvt.createWordList(list, config);
//		dictionary.storePlain(new FileWriter("C:/Users/ql29/Documents/EClipse/testDic.txt"));
		WVTDocumentInfo info = new WVTDocumentInfo(filePath, "txt", "", "english",0);
		UniversalLoader loader=new UniversalLoader();
		
		WVTInputFilter infilter=new TextInputFilter();
		
		WVTCharConverter converter= new DummyCharConverter();
		
		WVTTokenizer tokenizer = new SimpleTokenizer();
		
		WVTWordFilter filter = new DummyWordFilter();
		
		WVTStemmer stemmer = new DummyStemmer();
		
		InputStream stream=loader.loadDocument(info);
		
		Reader reader = infilter.convertToPlainText(stream, info);
		
		Reader charReader = converter.convertChars(reader, info);
		
		TokenEnumeration enumeration = tokenizer.tokenize(charReader, info);
		
		while(enumeration.hasMoreTokens()){
			System.out.println(enumeration.nextToken());
		}
		
//		StringBuffer buf = new StringBuffer();
//		char[] arr = new char[8*1024];
//		int numChars;
//		while ((numChars = charReader.read(arr, 0, arr.length)) > 0) {
//		      buf.append(arr, 0, numChars);
//		}
//
//		System.out.println(buf.toString());
		
	}

}
