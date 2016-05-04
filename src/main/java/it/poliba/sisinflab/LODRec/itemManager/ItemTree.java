package it.poliba.sisinflab.LODRec.itemManager;

import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntIntHashMap;

public interface ItemTree{
	
	void setItemId(int value);
	
	int getItemId();
	
	int size();
	
	boolean isEmpty();

	void addBranches(String key, int value);
	
	THashMap<String, TIntIntHashMap> getBranches();
	
	String serialize();
	
	boolean equals(Object object);
}
