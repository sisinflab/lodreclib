package it.poliba.sisinflab.LODRec.sprank.itemPathExtractor;

import java.util.ArrayList;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import it.poliba.sisinflab.LODRec.fileManager.StringFileManager;
import it.poliba.sisinflab.LODRec.fileManager.TextFileManager;
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
public class ItemPathExtractorWorker implements Runnable {
	
	private SynchronizedCounter counter; // synchronized counter for path index
	private TObjectIntHashMap<String> path_index; // path index
	private TIntObjectHashMap<String> inverse_path_index; // key-value path index
	private ItemTree main_item; // main item
	private ArrayList<ItemTree> items; // items 
	private TIntObjectHashMap<String> props_index; // property index
	private boolean inverseProps; // directed property
	private TextFileManager textWriter;
	private StringFileManager pathWriter;
	private boolean select_top_path;
	private TIntIntHashMap input_metadata_id;
	private boolean computeInversePaths;
	private int main_item_id;
	private TIntObjectHashMap<TIntHashSet> items_link;
	
	private static Logger logger = LogManager.getLogger(ItemPathExtractorWorker.class.getName());
	
	/**
	 * Constuctor
	 */
	public ItemPathExtractorWorker(SynchronizedCounter counter, 
			TObjectIntHashMap<String> path_index, TIntObjectHashMap<String> inverse_path_index, 
			ItemTree main_item, ArrayList<ItemTree> items, TIntObjectHashMap<String> props_index, boolean inverseProps,
			TextFileManager textWriter, StringFileManager pathWriter, boolean select_top_path,
			TIntIntHashMap input_metadata_id, boolean computeInversePaths,
			TIntObjectHashMap<TIntHashSet> items_link){
		
		this.counter = counter;
		this.path_index = path_index;
		this.main_item = main_item;
		this.items = items;
		this.props_index = props_index;
		this.inverseProps = inverseProps;
		this.textWriter = textWriter;
		this.pathWriter = pathWriter;
		this.select_top_path = select_top_path;
		this.input_metadata_id = input_metadata_id;
		this.computeInversePaths = computeInversePaths;
		this.items_link = items_link;
		this.inverse_path_index = inverse_path_index;
	}
	
	
	/**
	 * run path extraction
	 */
	public void run(){
		
		main_item_id = main_item.getItemId();
		
		logger.debug("item " + main_item_id + ": start paths extraction");
		
		long start = System.currentTimeMillis();
		
		start();
		
		long stop = System.currentTimeMillis();
		
		logger.debug("item " + main_item_id + ": paths extraction terminated in [sec]: " 
					+ ((stop - start) / 1000));
		
	}
	
	/**
	 * start path extraction considering all the pairs main_item-items
	 */
	private void start(){
		
		int count = 0; // number of computed pairs
		
		TIntIntHashMap paths = null;
		String item_pair_paths = "";
		
		// indicates the item from which to start the extraction
		boolean start = false;
		if(!items.contains(main_item))
			start = true;
		
		for(int j = 0; j < items.size(); j++){
			
			ItemTree b = items.get(j);
			int b_id = b.getItemId();
			
			if(start){
				
				count++;
				paths = computePaths(main_item, b);
				
				if(paths.size() > 0){
					
					item_pair_paths = main_item_id + "-" + b_id + "\t";
					TIntIntIterator it = paths.iterator();
					while(it.hasNext()){
						it.advance();
						item_pair_paths += it.key() + "=" + it.value() + ",";
					}
					item_pair_paths = item_pair_paths.substring(0, item_pair_paths.length()-1);
					
					// text file writing
					if(textWriter != null)
						textWriter.write(item_pair_paths);
					
					// binary file writing
					if(pathWriter != null)
						pathWriter.write(item_pair_paths);
					
					if(computeInversePaths){
						
						item_pair_paths = b_id + "-" + main_item_id + "\t";
						it = paths.iterator();
						while(it.hasNext()){
							it.advance();
							item_pair_paths += reverse(it.key()) + "=" + it.value() + ",";
						}
						
						item_pair_paths = item_pair_paths.substring(0, item_pair_paths.length()-1);
						
						// text file writing
						if(textWriter != null)
							textWriter.write(item_pair_paths);
						
						// binary file writing
						if(pathWriter != null){
							pathWriter.write(item_pair_paths);
						}
					}
				
				}
				
			}
			
			if(!start){
				if(main_item_id==b_id)
					start = true;
			}
			
		}
		
		logger.debug("item " + main_item_id + ": paths extraction completed (computed pairs "+count+")");
		 
	}
	
	private int reverse(int k){
		
		String path;
		synchronized(inverse_path_index){
			path = inverse_path_index.get(k);
		}
		
		String[] paths = path.split("#");
		if(inverseProps)
			return extractKey(StringUtils.reverseDirected(paths[1], props_index) 
					+ "#" + StringUtils.reverseDirected(paths[0], props_index));
		else
			return extractKey(StringUtils.reverse(paths[1]) + "#" + StringUtils.reverse(paths[0]));
	}
	
	
	/**
	 * Extract paths from a pair of item trees
	 * @param     a  first item tree
	 * @param     b  second item tree
	 * @return    paths map (path index:freq)
	 */
	private TIntIntHashMap computePaths(ItemTree a, ItemTree b) {
		
		TIntIntHashMap items_path = new TIntIntHashMap();
		
		//int a_id = a.getItemId();
		int b_id = b.getItemId();
		
		// get a branches
		THashMap<String, TIntIntHashMap> branches_a = ((ItemTree) a).getBranches();
		// get b branches
		THashMap<String, TIntIntHashMap> branches_b = ((ItemTree) b).getBranches();
		
		if(select_top_path)
			items_path = computeTopPaths(branches_a, branches_b, b_id);
		else{
		
			for(String s : branches_a.keySet()){
				
				if(input_metadata_id.containsKey(b_id)){
					if(branches_a.get(s).containsKey(input_metadata_id.get(b_id)))
						items_path.put(extractKey(s), branches_a.get(s).get(input_metadata_id.get(b_id)));
				}
				
				for(String ss : branches_b.keySet()){
					
					int path_id = 0;
					String path = "";
					
					if(inverseProps)
						path = s + "#" + StringUtils.reverseDirected(ss, props_index);
					else
						path = s + "#" + StringUtils.reverse(ss);
					
					
					path_id = extractKey(path);
					
					TIntSet items = branches_a.get(s).keySet();
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
							
						items_path.put(path_id, count);
					}
					
				}	
				
			}
			
		}
		
		if(items_path.size() > 0){
			synchronized(items_link){
				items_link.putIfAbsent(main_item_id, new TIntHashSet());
				items_link.get(main_item_id).add(b_id);
				items_link.putIfAbsent(b_id, new TIntHashSet());
				items_link.get(b_id).add(main_item_id);
			}
		}
		
		return items_path;
	}
	
	
	private TIntIntHashMap computeTopPaths(THashMap<String, TIntIntHashMap> branches_a, 
			THashMap<String, TIntIntHashMap> branches_b, int b_id) {
		
		TIntIntHashMap items_path = new TIntIntHashMap();
		
		for(String s : branches_a.keySet()){
			
			if(isPrefix(s)){
				
				for(String ss : branches_b.keySet()){
					
					if(isPostfix(ss)){
						
						int path_id = 0;
						String path = "";
						
						if(inverseProps)
							path = s + "#" + StringUtils.reverseDirected(ss, props_index);
						else
							path = s + "#" + StringUtils.reverse(ss);
						
						if(existKey(path)){
							
							path_id = extractKey(path);
							
							TIntSet items = branches_a.get(s).keySet();
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
								
								items_path.put(path_id, count);
							}
							
						}
						
					}
					
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
			if(path_index.containsKey(s))
				return path_index.get(s);
			else{
				int id = counter.value();
				path_index.put(s, id);
				if(computeInversePaths)
					inverse_path_index.put(id, s);
				return id;
			}
		}

	}
	
	private boolean existKey(String s){
		
		if(path_index.containsKey(s))
			return true;
						
		return false;
	}
	
	private boolean isPostfix(String s){
		
		if(ItemPathExtractor.top_path_postfix.contains(s))
			return true;
		
		return false;
		
	}
	
	private boolean isPrefix(String s){
		
		if(ItemPathExtractor.top_path_prefix.contains(s))
			return true;
		
		return false;
		
	}

}
