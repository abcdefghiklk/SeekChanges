package webdata;

import java.text.DateFormat;
import java.text.SimpleDateFormat;


public class SourceForge {
	private String _rootUrl;
	private DateFormat _logDateFormat;
	
	SourceForge(){
		this._rootUrl="https://sourceforge.net/p/";
		this._logDateFormat=new SimpleDateFormat("EEE MMM dd, yyyy hh:mm a z");
	}
	public String getRootUrl(){
		return _rootUrl;
	}
	public DateFormat getLogDateFormat(){
		return _logDateFormat;
	}
	
	private static SourceForge sourceForge=new SourceForge();
	
	public static SourceForge getInstance(){
		return sourceForge;
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
