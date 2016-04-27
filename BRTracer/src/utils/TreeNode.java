package utils;

import java.util.ArrayList;
import java.util.List;

public class TreeNode<T> {
	private List<TreeNode<T>> children = new ArrayList<TreeNode<T>>();
	private TreeNode<T> parent = null;
	private T data = null;

	public TreeNode(T data) {
	    this.data = data;
	}
	public TreeNode(T data, TreeNode<T> parent) {
		this.data = data;
		this.parent = parent;
		parent.addChild(this);
	}
	public List<TreeNode<T>> getChildren() {
		return children;
	}
	public void setParent(TreeNode<T> parent) {
//		parent.addChild(this);
		this.parent = parent;
	}
	public void addChild(T data) {
	    TreeNode<T> child = new TreeNode<T>(data);
//	    child.setParent(this);
	    this.children.add(child);
	}
	public void addChild(TreeNode<T> child) {
//		child.setParent(this);
		this.children.add(child);
	}
	public boolean hasChild(TreeNode<T> child){
		return (this.children.contains(child));
	}
	public boolean hasChild(T childName){
		boolean isIncluded=false;
		for(TreeNode<T> oneChildNode:this.children){
			if(oneChildNode.data.equals(childName)){
				isIncluded=true;
				break;
			}
		}
		return isIncluded;
	}
	public TreeNode<T> getChild(T childName){
		for(TreeNode<T> oneChildNode:this.children){
			if(oneChildNode.data.equals(childName)){
				return oneChildNode;
			}
		}
		System.out.println("the child node with the input child name does not exist!");
		return null;
	}
	public T getData() {
		return this.data;
	}
	public void setData(T data) {
		this.data = data;
	}
	public boolean isRoot() {
		return (this.parent == null);
	}
	public boolean isLeaf() {
		if(this.children.size() == 0) 
			return true;
	    else 
	    	return false;
	}
	public void removeParent() {
	    this.parent = null;
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		TreeNode<String> parentNode = new TreeNode<String>("Parent"); 
		TreeNode<String> childNode1 = new TreeNode<String>("Child 1", parentNode);
		TreeNode<String> childNode2 = childNode1;

		TreeNode<String> grandchildNode = new TreeNode<String>("Grandchild of parentNode. Child of childNode1", childNode2); 
		List<TreeNode<String>> childrenNodes = childNode1.getChildren();
//		System.out.println(grandchildNode.getData());
		for(TreeNode<String> n:childrenNodes){
			System.out.println(n.getData());
		}
	}

}
