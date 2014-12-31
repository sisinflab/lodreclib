package it.poliba.sisinflab.LODRecommender.itemFeaturesExtractor;

import gnu.trove.map.hash.TObjectIntHashMap;
import it.poliba.sisinflab.LODRecommender.fileManager.ItemFileManager;
import it.poliba.sisinflab.LODRecommender.fileManager.StringFileManager;
import it.poliba.sisinflab.LODRecommender.fileManager.TextFileManager;
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
public class ItemFeaturesExtractor {
	
	private String itemsFile; // metadata file name
	private int itemsInMemory; // number of items to load in memory
	private TObjectIntHashMap<String> features_index; // features index
	private SynchronizedCounter counter; // synchronized counter for features index
	private boolean outputFeaturesTextFormat;
	private boolean outputFeaturesBinaryFormat;
	private String item_features_file;
	private boolean selectTopFeatures;
	
	
	private static Logger logger = LogManager.getLogger(ItemFeaturesExtractor.class.getName());
	
	
	/**
	 * Constuctor
	 */
	public ItemFeaturesExtractor(){
		
		// load config file
		Map<String, String> prop = null;
		
		try {
			prop = PropertyFileReader.loadProperties("config.properties");
			this.itemsInMemory = Integer.parseInt(prop.get("itemsInMemory"))/2;
			this.itemsFile = prop.get("itemsFile");
			this.item_features_file = prop.get("outputFeaturesFile");
			this.outputFeaturesTextFormat =  Boolean.valueOf(prop.get("outputExtractionTextFormat"));
			this.outputFeaturesBinaryFormat =  Boolean.valueOf(prop.get("outputExtractionBinaryFormat"));
			this.selectTopFeatures =  Boolean.valueOf(prop.get("computeTopFeatures"));
			
			if(selectTopFeatures){
				logger.debug("Selection of top features abilited");
				computeTopFeatures();
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		prop.clear();
	}
	
	
		
	private void computeTopFeatures(){
		
		TopItemFeaturesExtractor top = new TopItemFeaturesExtractor();
		top.start();
			
		loadFeaturesIndex();
		
	}
	
	/**
	  * load features index
	  */
	private void loadFeaturesIndex(){
		features_index = new TObjectIntHashMap<String>();
		TextFileUtils.loadIndex("features_index", features_index);
		logger.debug("Features index loading");
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
		if(!selectTopFeatures)
			features_index = new TObjectIntHashMap<String>();
		
		try {
			
			TextFileManager textWriter = null;
			if(outputFeaturesTextFormat)
				textWriter = new TextFileManager(item_features_file + ".txt");
			
			StringFileManager itemFeaturesWriter = null;
			if(outputFeaturesBinaryFormat)
				itemFeaturesWriter = new StringFileManager(item_features_file, StringFileManager.WRITE);
			
			ItemFileManager itemReader = new ItemFileManager(itemsFile, ItemFileManager.READ);
			ArrayList<String> items_id = new ArrayList<String>(itemReader.getKeysIndex());
			int num_items = items_id.size();
			
			int index_v = 0;
			
			ItemTree item = null;
			
			while(index_v < num_items){
				
				if(executor.isTerminated())
		    		executor = Executors.newFixedThreadPool(n_threads);
				
				// carico dim_blocks items
				for(int i = index_v; i < (index_v + itemsInMemory) && i < num_items; i++){
					
					item = itemReader.read(items_id.get(i));
					
					if(item!=null){
						Runnable worker = new ItemFeaturesExtractorWorker(counter, item, features_index, 
								selectTopFeatures, textWriter, itemFeaturesWriter);
						// run the worker thread  
						executor.execute(worker);
					}
					
				}
				
				executor.shutdown();
				executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
				
				index_v += itemsInMemory;
			}
			
			itemReader.close();
			
			if(textWriter!=null)
				textWriter.close();
		    
		    if(itemFeaturesWriter!=null)
		    	itemFeaturesWriter.close();
		    
		    TextFileUtils.writeData(item_features_file + "_index", features_index);
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		ItemFeaturesExtractor pe = new ItemFeaturesExtractor();
		
		long start = System.currentTimeMillis();
		
		pe.start();
		
		long stop = System.currentTimeMillis();
		logger.info("Item features extraction terminated in [sec]: " + ((stop - start) / 1000));
		
		//MemoryMonitor.stats();
		
	}
	
	
}
