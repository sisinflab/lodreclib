package it.poliba.sisinflab.LODRecommender.itemFeaturesExtractor;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import it.poliba.sisinflab.LODRecommender.itemManager.ItemTree;
import it.poliba.sisinflab.LODRecommender.utils.SynchronizedCounter;

/**
 * This class is part of the LOD Recommender
 * 
 * This class is used by PathExtractor for multi-threading paths extraction 
 * 
 * @author Vito Mastromarino
 */
public class TopItemFeaturesExtractorWorker implements Runnable {
	
	private SynchronizedCounter counter; // synchronized counter for features index
	private ItemTree item; // main item
	private TObjectIntHashMap<String> features_index; // features index
	private TIntObjectHashMap<TIntIntHashMap> item_features;
	private int item_id;
	
	private static Logger logger = LogManager.getLogger(TopItemFeaturesExtractorWorker.class.getName());
	
	/**
	 * Constuctor
	 */
	public TopItemFeaturesExtractorWorker(SynchronizedCounter counter, 
			TObjectIntHashMap<String> features_index, ItemTree item, 
			TIntObjectHashMap<TIntIntHashMap> item_features){
		
		this.counter = counter;
		this.item = item;
		this.features_index = features_index;
		this.item_features = item_features;
	}
	
	
	/**
	 * run path extraction
	 */
	public void run(){
		
		item_id = item.getItemId();
		
		logger.info("item " + item_id + ": start features extraction");
		
		long start = System.currentTimeMillis();
		
		start();
		
		long stop = System.currentTimeMillis();
		
		logger.info("item " + item_id + ": features extraction terminated in [sec]: " 
				+ ((stop - start) / 1000));
		
	}
	
	/**
	 * start path extraction considering all the pairs main_item-items
	 */
	public void start(){
		
		TIntIntHashMap features = computeFeatures(item);
		if(features!=null){
			synchronized(item_features){
				item_features.put(item_id, features);
			}
		}
		
	}
	
	
	
	/**
	 * Extract paths from a pair of item trees
	 * @param     a  first item tree
	 * @param     b  second item tree
	 * @return    paths map (path index:freq)
	 */
	public TIntIntHashMap computeFeatures(ItemTree a) {
		
		TIntIntHashMap item_features = new TIntIntHashMap();
		
		// get a branches
		THashMap<String, TIntIntHashMap> branches = ((ItemTree) a).getBranches();
		
		for(String s : branches.keySet()){
			
			for(int f : branches.get(s).keys())
				item_features.put(extractKey(s + "#" + f), branches.get(s).get(f));
			
		}
		
		return item_features;
	}
	
	/**
	 * Extract key from path index
	 * @param     s  string to index
	 * @return    index of s
	 */
	private int extractKey(String s) {
		
		synchronized(features_index){
			if(features_index.contains(s)){
				return features_index.get(s);
			}
			else{
				int id = counter.value();
				features_index.put(s, id);
				return id;
			}
		}

	}

}
