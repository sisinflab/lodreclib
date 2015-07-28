package it.poliba.sisinflab.LODRec.sprank.itemPathExtractor;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import it.poliba.sisinflab.LODRec.itemManager.ItemTree;
import it.poliba.sisinflab.LODRec.utils.StringUtils;
import it.poliba.sisinflab.LODRec.utils.SynchronizedCounter;

/**
 * This class is part of the LOD Recommender
 * 
 * This class is used by PathExtractor for multi-threading paths extraction 
 * 
 * @author Vito Mastromarino
 */
public class TopItemPathExtractorWorker implements Runnable {
	
	private SynchronizedCounter counter; // synchronized counter for path index
	private TObjectIntHashMap<String> path_index; // path index
	private ItemTree main_item; // main item
	private ArrayList<ItemTree> items; // items 
	private TIntObjectHashMap<String> props_index; // property index
	private boolean inverseProps; // directed property
	private HashMap<Integer, Integer> path;
	
	private static Logger logger = LogManager.getLogger(TopItemPathExtractorWorker.class.getName());
	
	/**
	 * Constuctor
	 */
	public TopItemPathExtractorWorker(SynchronizedCounter counter, 
			TObjectIntHashMap<String> path_index, ItemTree main_item,
			ArrayList<ItemTree> items, TIntObjectHashMap<String> props_index, boolean inverseProps,
			HashMap<Integer, Integer> path){
		
		this.counter = counter;
		this.path_index = path_index;
		this.main_item = main_item;
		this.items = items;
		this.props_index = props_index;
		this.inverseProps = inverseProps;
		this.path = path;
	}
	
	
	/**
	 * run path extraction
	 */
	public void run(){
//		
		start();
		
	}
	
	/**
	 * start path extraction considering all the pairs main_item-items
	 */
	public void start(){
		
		int count = 0; // number of computed pairs
		int main_item_id = main_item.getItemId();
		
		TIntIntHashMap paths = null;
		
		// indicates the item from which to start the extraction
		boolean start = false;
		
		for(int j = 0; j < items.size(); j++){
			
			ItemTree b = items.get(j);
			int b_id = b.getItemId();
			
			if(start){
			
				paths = computePaths(main_item, b);
				
				if(paths!=null){
					
					count++;
					
					TIntIntIterator it = paths.iterator();
					while(it.hasNext()){
						
						it.advance();
						int key = it.key();
						int value = it.value();
						
						synchronized(path){
							
							if(path.containsKey(key))
								value += path.get(key);		

							path.put(key, value);
						}
					}
				}
				
			}
			
			if(!start){
				if(main_item_id==b_id)
					start = true;
			}
			
		}
		
		logger.debug(main_item_id + ": extraction completed (computed pairs " + count + ")");
		
	}
	

	
	/**
	 * Reverse directed paths
	 * @param     in  path to reverse
	 * @return    reversed path
	 */
	public String reverse(String in){
		
		String p = StringUtils.reverse(in);
		
		String[] vals = p.split("-");
		
		String out = "";
		String prop = "";
		String to_search = "";
		
		for(String s : vals){
			
			
			prop = props_index.get(Integer.parseInt(s));
			to_search = "";
			if(prop.startsWith("inv_"))
				to_search = prop.substring(4);
			else
				to_search = "inv_" + prop;
			
			for(int i : props_index.keys()){
				if(props_index.get(i).equals(to_search))
					out += i + "-";
			}
			
		}
		
		return out.substring(0, out.length()-1);
		
	}
	
	/**
	 * Get string path from index
	 * @param     index  index of the string path
	 * @return    string path
	 */
	public String getPathFromIndex(int index){
		
		synchronized(path_index){
			
			TObjectIntIterator<String> it = path_index.iterator();
			while(it.hasNext()){
				it.advance();
				if(it.value()==index)
					return (String) it.key();
			}
			
			return null;
		}
		
	}
	
	/**
	 * Extract paths from a pair of item trees
	 * @param     a  first item tree
	 * @param     b  second item tree
	 * @return    paths map (path index:freq)
	 */
	public TIntIntHashMap computePaths(ItemTree a, ItemTree b) {
		
		TIntIntHashMap items_path = new TIntIntHashMap();
		
		// get a branches
		THashMap<String, TIntIntHashMap> branches_a = ((ItemTree) a).getBranches();
		// get b branches
		THashMap<String, TIntIntHashMap> branches_b = ((ItemTree) b).getBranches();
		//System.out.println(branches_a.size() + "-" + branches_b.size());
		for(String s : branches_a.keySet()){
			
			TIntSet items = branches_a.get(s).keySet();
			
			for(String ss : branches_b.keySet()){
				
				TIntSet items1 = branches_b.get(ss).keySet();
				
				TIntSet tmp;
				// items intersection
				if(items.size() < items1.size()){
					tmp = new TIntHashSet(items);
					tmp.retainAll(items1);
					
				}
				else{
					tmp = new TIntHashSet(items1);
					tmp.retainAll(items);
				}
				

				if(tmp.size()>0){
					
					TIntIterator it = tmp.iterator();
					int count = 0;
					while(it.hasNext()){
						int val = it.next();
						count += (branches_a.get(s).get(val) * branches_b.get(ss).get(val));
					}
					
					if(inverseProps)
						items_path.put(extractKey(s + "#" + reverse(ss)), count);
					else
						items_path.put(extractKey(s + "#" + StringUtils.reverse(ss)), count);
				}
			}	
		}
		
		return items_path;
	}
	
	/**
	 * Extract key from path index
	 * @param     s  string to index
	 * @return    index of s
	 */
	private int extractKey(String s) {
		
		synchronized(path_index){
			if(path_index.contains(s)){
				return path_index.get(s);
			}
			else{
				int id = counter.value();
				path_index.put(s, id);
				return id;
			}
		}

	}

}
