package feature;

public class ProgramElement {
	private String _elementName;
	private String _fullElementPath;
	private double _relevanceScore;
	
	ProgramElement(){
		this._elementName=null;
		this._fullElementPath=null;
		this._relevanceScore=0.0d;
	}
	ProgramElement(String elementName, String fullElementPath, double relevanceScore){
		this._elementName=elementName;
		this._fullElementPath=fullElementPath;
		this._relevanceScore=relevanceScore;
	}
	public void setElementName(String elementName){
		this._elementName=elementName;
	}
	public void setFullElementPath(String fullElementPath){
		this._fullElementPath=fullElementPath;
	}
	public void setRelevanceScore(double relevanceScore){
		this._relevanceScore=relevanceScore;
	}
	public String getElementName(){
		return this._elementName;
	}
	public String getFullElementPath(){
		return this._fullElementPath;
	}
	public double getRelevanceScore(){
		return this._relevanceScore;
	}
	
	public boolean equals(Object obj){
		if(obj instanceof ProgramElement){
			ProgramElement objElement=(ProgramElement)obj;
			if(objElement._fullElementPath.trim().equals(_fullElementPath.trim())){
				return true;
			}
			else{
				return false;
			}
		}
		else{
			return false;
		}
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
