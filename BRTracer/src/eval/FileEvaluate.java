package eval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;

import utils.FileUtils;



public class FileEvaluate {
	public static void evaluate(String outputFilePath, int k, String evaluationFilePath) throws Exception{
		HashMap<String, String> map = new HashMap<String, String>();
		BufferedReader reader=new BufferedReader(new FileReader(outputFilePath));
		String line= new String();
		while((line=reader.readLine())!=null){
			String []strs=line.split(",");
			if(strs.length==4){
				String bugID = strs[0];
				String rankStr= strs[2];
				if(map.containsKey(bugID)){
					String newStr=map.get(bugID)+" "+rankStr;
					map.put(bugID, newStr);
				}
				else{
					map.put(bugID, rankStr);
				}
			}
		}
		int bugNum=map.size();
//		System.out.println(bugNum);
		double sum_MRR=0;
		double sum_MAP=0;
		double sum_topK=0;
		for(Entry<String, String> onePair: map.entrySet()){
//			System.out.println(onePair.getValue());
			String []strs=onePair.getValue().split(" ");
			SortedSet<Integer> rankSet = new TreeSet<Integer>();
			for(String str:strs){
				rankSet.add(Integer.parseInt(str));
			}
			double MAP=0;
			int i=1;
			for(int rank: rankSet){
				MAP+= i/(rank+1.0d);
				if(i==1){
					sum_MRR+=1/(1.0d+rank);
					if((rank+1)<=k){
						sum_topK++;
					}
				}
				i++;
			}
			MAP=MAP/(rankSet.size()+0.0d);
			sum_MAP+=MAP;
		}
		double avg_MRR=sum_MRR/(bugNum+0.0d);
		double avg_MAP=sum_MAP/(bugNum+0.0d);
		double avg_topK=sum_topK/(bugNum+0.0d);
		
//		System.out.println("MRR: "+ avg_MRR);
//		System.out.println("MAP: "+ avg_MAP);
//		System.out.println("topK: "+ avg_topK);
		reader.close();
		FileWriter writer=new FileWriter(evaluationFilePath);
		writer.write("MRR: "+avg_MRR+"\n"+
				"MAP: "+ avg_MAP+"\n"+
				"topK@"+k+": "+ avg_topK);
		writer.close();
	}
	public static void evaluateDir(String outputDirPath, int k, String evaluationDirPath) throws Exception{
		File evaluationDir=new File(evaluationDirPath);
		if(evaluationDir.isDirectory()){
			FileUtils.deleteDir(evaluationDir);
		}
		evaluationDir.mkdir();
		File outputDir=new File(outputDirPath);
		if(!outputDir.isDirectory()){
			System.out.println("the directory containing the buglocator output is invalid!");
			return;
		}
		for(String oneFileName:outputDir.list()){
			String evalFilePath=Paths.get(evaluationDirPath, oneFileName).toString();
			String fullFilePath=Paths.get(outputDirPath,oneFileName).toString();
			if(new File(fullFilePath).isFile()){
				evaluate(fullFilePath,k,evalFilePath);
			}
			else if(new File(fullFilePath).isDirectory()){
				new File(evalFilePath).mkdir();
				evaluateDir(fullFilePath,k,evalFilePath);
			}
		}
	}
	public static void exportEvaluationResult2Excel(String evaluationDirPath, String outputExcelFilePath) throws Exception{
		File evaluationDir=new File(evaluationDirPath);
		FileOutputStream outputStream = new FileOutputStream(new File(outputExcelFilePath));
		HSSFWorkbook workbook=new HSSFWorkbook();
		
		if(!evaluationDir.isDirectory()){
			System.out.println("the directory containing the evaluation result is invalid!");
			outputStream.close();
			return;
		}
		for(String oneProjectName:evaluationDir.list()){
			HSSFSheet sheet= workbook.createSheet(oneProjectName);sheet.setColumnWidth(0, 18000); //Set column width, you'll probably want to tweak the second int
            CellStyle style = workbook.createCellStyle(); //Create new style
            style.setWrapText(true); //Set wordwrap
			
			HSSFRow firstRow=sheet.createRow(0);
			int colCount=1;
			for(String oneLogName:Paths.get(evaluationDirPath,oneProjectName).toFile().list()){
				HSSFCell colNameCell=firstRow.createCell(colCount);
				colNameCell.setCellValue(oneLogName);
				int rowCount=1;
				for(String oneMethod: Paths.get(evaluationDirPath,oneProjectName,oneLogName).toFile().list()){
					HSSFRow row;
					if(sheet.getLastRowNum()>=rowCount){
						row=sheet.getRow(rowCount);
					}
					else{
						row=sheet.createRow(rowCount);
					}
					
					HSSFCell cell=row.createCell(0);
					cell.setCellValue(oneMethod);
//					cell.setCellStyle(style);
					cell=row.createCell(colCount);
					
					BufferedReader reader=new BufferedReader(new FileReader(Paths.get(evaluationDirPath,oneProjectName,oneLogName,oneMethod).toString()));
					StringBuffer buf=new StringBuffer();
					for(int i=0;i<3;i++){
						buf.append(reader.readLine().trim()+"  ");
					}
					
					reader.close();
					cell.setCellValue(buf.toString());
//					cell.setCellStyle(style);
					rowCount++;
				}
				colCount++;
			}

//			HSSFRow row=sheet.getRow(1);
//			row.createCell(1);
//			row.getCell(1).setCellValue("hello!");
			
		}
		
		workbook.write(outputStream);
		outputStream.close();
	}
	private static void showHelp() {
		String usage = "Usage:java -jar Evaluater.jar [-options] \r\n\r\nwhere options must include:\r\n"
				+ "-i	indicates the path of the outputfile of the buglocator (require an absolute path name)\r\n"
				+ "-k	indicates the value of k in topK metric (default=5)\r\n"
				+ "-o	indicates where to store the evaluation file (require an absolute path name).";

		System.out.println(usage);
	}
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		
		int i = 0;
		String outputFilePath = "";
		int k = 5;
        String evalFilePath="";
        boolean isLegal=true;
        if(args.length==0){
        	showHelp();
        }
        else{
			while (i < args.length-1) {
				if (args[i].equals("-i")) {
					i++;
					outputFilePath = args[i];
				} else if (args[i].equals("-k")) {
					i++;
					k = Integer.parseInt(args[i]);
				} else if (args[i].equals("-o")) {
					i++;
					evalFilePath = args[i];
				} else {
					System.out.println("illegal input!");
					showHelp();
					isLegal=false;
					break;
				}
				i++;
			}
        
//		String outputFile="C:/Users/dell/Documents/EClipse/Dataset/output.txt";
//		String evaluationFile="C:/Users/dell/Documents/EClipse/Dataset/eval.txt";
			if(isLegal){
				evaluate(outputFilePath,k,evalFilePath);
			}
        }
	}

}
