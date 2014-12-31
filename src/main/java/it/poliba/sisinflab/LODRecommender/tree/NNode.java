package it.poliba.sisinflab.LODRecommender.tree;

import java.util.ArrayList;

public class NNode {
	
	private String value;
	private ArrayList<NNode> childs;
	
	public NNode(){
		this.value = null;
		this.childs = new ArrayList<NNode>();
	}
	
	public NNode(String value){
		this.value = value;
		this.childs = new ArrayList<NNode>();
	}
	
	public void setValue(String value){
		this.value = value;
	}
	
	public String getValue(){
		return value;
	}
	
	public boolean hasChilds(){
		if(childs!=null)
			if(childs.size()>0)
				return true;
			else
				return false;
		else
			return false;
	}
	
	public void addChilds(ArrayList<NNode> childs){
		this.childs = childs;
	}
	
	public ArrayList<NNode> getChilds(){
		return childs;
	}
	
	public void equals(){
		
	}

}
