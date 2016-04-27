package webdata;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class ApacheJira {
	private String _rootUrl;
	private DateFormat _logDateFormat;
	
	ApacheJira(){
		this._rootUrl="https://issues.apache.org/jira/browse/";
		this._logDateFormat=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	}
	public String getRootUrl(){
		return _rootUrl;
	}
	public DateFormat getLogDateFormat(){
		return _logDateFormat;
	}
	
	private static ApacheJira apacheJira=new ApacheJira();
	
	public static ApacheJira getInstance(){
		return apacheJira;
	}
}
