package it.poliba.sisinflab.LODRec.itemManager;

import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.io.Serializable;

public class PropertyIndexedItemTree implements ItemTree, Serializable {
	
	private static final long serialVersionUID = 1L;
	private int item_id;
	private THashMap<String, TIntIntHashMap> branches;
	
	public PropertyIndexedItemTree(int item_id){
		this.item_id = item_id;
		branches = new THashMap<String, TIntIntHashMap>();
	}

	@Override
	public void setItemId(int item_id) {
		this.item_id = item_id;
		
	}

	@Override
	public int getItemId() {
		return item_id;
	}

	@Override
	public int size() {
		return branches.size();
	}

	@Override
	public boolean isEmpty() {
		if(size()>0)
			return true;
		else
			return false;
	}

	@Override
	public void addBranches(String key, int value){
		
		if (!this.branches.containsKey(key))
			this.branches.put(key, new TIntIntHashMap());
		
		if(this.branches.get(key)!=null){
			if(this.branches.get(key).containsKey(value)){
				if(this.branches.get(key).get(value) < Integer.MAX_VALUE)
					this.branches.get(key).adjustValue(value, 1);
			}
			else
				this.branches.get(key).put(value, 1);
		}
		else{
			TIntIntHashMap tmp = new TIntIntHashMap();
			tmp.put(value, 1);
			this.branches.put(key, tmp);
		}
	}
	

	
	@Override
	public	THashMap<String, TIntIntHashMap> getBranches() {
		return branches;
	}
	
	@Override
	public String serialize(){
		
		StringBuffer res = new StringBuffer();
		
		for(String s : branches.keySet())
			res.append(this.item_id + "\t" + s + "\t" + branches.get(s) + "\n");
			
		// togliamo lo \n finale
		return res.substring(0, res.length()-2);
		
	}
	
	@Override
	public boolean equals(Object object)
	{
	    boolean isEqual= false;

	    if (object != null && object instanceof PropertyIndexedItemTree)
	    	isEqual = (this.item_id == ((PropertyIndexedItemTree) object).item_id);

	    return isEqual;
	}
}
