package webcrawler;

import java.util.Date;

public class BugComment {
	private String _commentor;
	private Date _commentDate;
	private String _commentContent;
	BugComment(){
		_commentor=null;
		_commentDate=null;
		_commentContent=null;
	}
	public void setCommentor(String commentor){
		this._commentor=commentor;
	}
	public String getCommentor(){
		return this._commentor;
	}
	public void setCommentDate(Date commentDate){
		this._commentDate=commentDate;
	}
	public Date getCommentDate(){
		return this._commentDate;
	}
	public void setCommentContent(String commentContent){
		this._commentContent=commentContent;
	}
	public String getCommentContent(){
		return this._commentContent;
	}
}
