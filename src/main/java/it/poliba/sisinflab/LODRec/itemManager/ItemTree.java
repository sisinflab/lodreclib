package it.poliba.sisinflab.LODRec.itemManager;

import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntIntHashMap;

public interface ItemTree{
	
	public void setItemId(int value);
	
	public int getItemId();
	
	public int size();
	
	public boolean isEmpty();

	public void addBranches(String key, int value);
	
	public THashMap<String, TIntIntHashMap> getBranches();
	
	public String serialize();
	
	public boolean equals(Object object);
}
