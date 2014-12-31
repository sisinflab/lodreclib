package it.poliba.sisinflab.LODRecommender.itemFeaturesExtractor;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import it.poliba.sisinflab.LODRecommender.fileManager.ItemFileManager;
import it.poliba.sisinflab.LODRecommender.itemManager.ItemTree;
import it.poliba.sisinflab.LODRecommender.utils.PropertyFileReader;
import it.poliba.sisinflab.LODRecommender.utils.SynchronizedCounter;
import it.poliba.sisinflab.LODRecommender.utils.TextFileUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * This class is part of the LOD Recommender
 * 
 * This class extracts paths from item trees 
 * 
 * @author Vito Mastromarino
 */
public class TopItemFeaturesExtractor {
	
	private String itemsFile; // metadata file name
	private TObjectIntHashMap<String> features_index; // features index
	private SynchronizedCounter counter; // synchronized counter for features index
	private TIntIntHashMap features_count;
	private String item_features_file;
	private TIntObjectHashMap<TIntIntHashMap> item_features;
	
	// parametri per selezione top features
	private int numItemTopFeatures;
	private int lowerLimit;
	private int upperLimit;
	
	private static Logger logger = LogManager.getLogger(TopItemFeaturesExtractor.class.getName());
	
	
	/**
	 * Constuctor
	 */
	public TopItemFeaturesExtractor(){
		
		// load config file
		Map<String, String> prop = null;
		
		try {
			prop = PropertyFileReader.loadProperties("config.properties");
			this.itemsFile = prop.get("itemsFile");
			this.item_features_file = prop.get("outputFeaturesFile");
			this.numItemTopFeatures = Integer.parseInt(prop.get("numItemTopFeatures"));
			this.lowerLimit = Integer.parseInt(prop.get("lowerLimit"));
			this.upperLimit = Integer.parseInt(prop.get("upperLimit"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		prop.clear();
	}
	
	
	
	/**
	 * start path extraction
	 */
	public void start(){
		
		
		// get processors number for multi-threading
		int n_threads =  Runtime.getRuntime().availableProcessors();
		n_threads = 4;
		logger.debug("Threads number: " + n_threads);
		
		ExecutorService executor;
		executor = Executors.newFixedThreadPool(n_threads);
		
		counter = new SynchronizedCounter();
		features_index = new TObjectIntHashMap<String>();
		item_features = new TIntObjectHashMap<TIntIntHashMap>();
		
		logger.info("Top features to select: " + lowerLimit + "% - " + upperLimit + "%");
		logger.info("Items to consider: " + numItemTopFeatures);
		
		try {
			
			ItemFileManager itemReader = new ItemFileManager(itemsFile, ItemFileManager.READ);
			ArrayList<String> items_id = new ArrayList<String>(itemReader.getKeysIndex());
			int num_items = items_id.size();
			
			ArrayList<ItemTree> items = new ArrayList<ItemTree>();
			ItemTree tmp = null;
			
			// carico dim_blocks items in verticale
			for(int i = 0; i < numItemTopFeatures && i < num_items; i++){
							
				tmp = itemReader.read(items_id.get(i));
				
				if(tmp!=null)
					items.add(tmp);
							
			}
			
			for(ItemTree item : items){
				// path extraction t-cols
				Runnable worker = new TopItemFeaturesExtractorWorker(counter, features_index, item, 
						item_features);
				// run the worker thread  
				executor.execute(worker);
			}
			
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			
			itemReader.close();
		    
		    features_count = new TIntIntHashMap();
			TIntIntHashMap features = null;
			
			// Calcolo occorrenze features
			for(int item_id : item_features.keys()){
				
				features = item_features.get(item_id);
				
				for(int feature_id : features.keys())
					features_count.adjustOrPutValue(feature_id, 1, 1);
			}
			
			int soglia_inf = (lowerLimit*numItemTopFeatures)/100;
			int soglia_sup = (upperLimit*numItemTopFeatures)/100;
			
			TIntArrayList top_features_id = new TIntArrayList();
		
			for(int f : features_count.keys()){
				
				int occ = features_count.get(f);
				if(occ >= soglia_inf && occ <= soglia_sup){
					top_features_id.add(f);
				}
				
			}
			
			logger.info("Selected " + top_features_id.size() + " of " + features_count.size() + " features");
			
			TObjectIntHashMap<String> top_features_index = new TObjectIntHashMap<String>(top_features_id.size());
			
			int i = 1;
			for(String ss : features_index.keySet()){
				if(top_features_id.contains(features_index.get(ss)))
					top_features_index.put(ss, i++);
			}
			
			TextFileUtils.writeData(item_features_file + "_index", top_features_index);
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
}
