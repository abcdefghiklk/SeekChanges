import java.io.File;
import java.nio.file.Paths;

import property.Property;

public class BRTracer {
	public static void main(String[] args) {
        /* You need to manually specify three program arguments in Eclipse or Intellij before running */
		args=new String[6];
		args[0]="-d";
		args[1]="D:/qiuchi/ApacheDatasets/ZOOKEEPER-3.4.0";
		args[2]="-p";
		args[3]="jEdit-4.3";
		args[4]="-o";
		args[5]="C:/Users/dell/git/mct_txl_nicad/mct_txl_nicad/datasets/jEdit-4.3/result";
		try {
			if (args.length == 0) {
				showHelp();
			} else {
				boolean isLegal = parseArgs(args);
				if (isLegal) {
					Core core = new Core();
					core.process();
					Property.getInstance().print();
				}
				
			}
		} catch (Exception ex) {
			showHelp();
		}	
	}

	private static void showHelp() {
		String usage = "Usage:java -jar BRTracer [-options] \r\n\r\nwhere options must include:\r\n"
				+ "-d	indicates the path of the dataset\r\n"
				+ "-p	indicates which project to evaluate (available option: swt, eclipse, aspectj)\r\n"
				+ "-o	indicates where to store the output file (require an absolute path name).";

		System.out.println(usage);
	}

	private static boolean parseArgs(String[] args) {
		int i = 0;
		String bugFilePath = "";
		String sourceCodeDir = "";
        String datasetPath = "";
		String projectStr = "";
		float alpha = 0.3f;
		String outputFile = "";
		while (i < args.length - 1) {
			if (args[i].equals("-d")) {
				i++;
				datasetPath = args[i];
			} else if (args[i].equals("-p")) {
				i++;
				projectStr = args[i];
			} else if (args[i].equals("-o")) {
				i++;
				outputFile = args[i];
			}
			i++;
		}
        if(projectStr.compareTo("eclipse")==0){
            bugFilePath = Paths.get(datasetPath, "EclipseBugRepository.xml").toString();
            sourceCodeDir = Paths.get(datasetPath,"eclipse-3.1/").toString();
        }
        else if(projectStr.compareTo("swt")==0){
            bugFilePath = Paths.get(datasetPath, "SWTBugRepository.xml").toString();
            sourceCodeDir = Paths.get(datasetPath,"swt-3.1/").toString();
        }
        else if(projectStr.compareTo("AspectJ")==0){
            bugFilePath = Paths.get(datasetPath,"AspectJBugRepository.xml").toString();
            sourceCodeDir = Paths.get(datasetPath,"aspectj/").toString();
        }
        else{
        	bugFilePath = Paths.get(datasetPath, "New Feature_Repository.xml").toString();
        	
        	sourceCodeDir = Paths.get(datasetPath, "zookeeper-3.4.0").toString();
        }
		boolean isLegal = true;
		if (datasetPath.equals("") || datasetPath == null) {
			isLegal = false;
			System.out.println("you must indicate the path of your dataset");
		}
		if (projectStr.equals("") || projectStr == null) {
			isLegal = false;
			System.out.println("you must indicate which project to evaluate");
		}
		if (outputFile.equals("") || outputFile == null) {
			isLegal = false;
			System.out.println("you must indicate where to store the output file");
		} else {
			File file = new File(outputFile);
			if (file.isDirectory()) {
				outputFile += "output.txt";
			}
		}
		if (!isLegal) {
			showHelp();
		} else {
			File file = new File(System.getProperty("user.dir"));
			if (file.getFreeSpace() / 1024 / 1024 / 1024 < 2) {
				System.out
						.println("Not enough free disk space, please ensure your current disk space are bigger than 2G.");
				isLegal = false;
			} else {
				File dir = new File("tmp");
				if (!dir.exists()) {
					dir.mkdir();
				}
				Property.createInstance(bugFilePath, sourceCodeDir, dir
						.getAbsolutePath(), alpha, outputFile, projectStr, sourceCodeDir.length());
			}

		}

		return isLegal;
	}
}