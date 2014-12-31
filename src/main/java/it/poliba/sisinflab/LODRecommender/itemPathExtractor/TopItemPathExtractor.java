package it.poliba.sisinflab.LODRecommender.itemPathExtractor;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TIntHashSet;
import it.poliba.sisinflab.LODRecommender.fileManager.ItemFileManager;
import it.poliba.sisinflab.LODRecommender.itemManager.ItemTree;
import it.poliba.sisinflab.LODRecommender.utils.ItemUtils;
import it.poliba.sisinflab.LODRecommender.utils.PropertyFileReader;
import it.poliba.sisinflab.LODRecommender.utils.SynchronizedCounter;
import it.poliba.sisinflab.LODRecommender.utils.TextFileUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
public class TopItemPathExtractor {
	
	private String itemsFile; // metadata file name
	private boolean inverseProps; // directed property
	private TObjectIntHashMap<String> path_index; // path index
	private TIntObjectHashMap<String> props_index; // property index
	private SynchronizedCounter counter; // synchronized counter for path index
	private String path_file;
	private HashMap<Integer, Integer> path;
	private int numTopPaths;
	private int numItemTopPaths;
	
	private static Logger logger = LogManager.getLogger(TopItemPathExtractor.class.getName());
	
	
	/**
	 * Constuctor
	 */
	public TopItemPathExtractor(){
		
		// load config file
		Map<String, String> prop = null;
		
		try {
			prop = PropertyFileReader.loadProperties("config.properties");
			this.inverseProps =  Boolean.valueOf(prop.get("directed"));
			this.itemsFile = prop.get("itemsFile");
			this.path_file = prop.get("outputPathFile");
			this.numItemTopPaths = Integer.parseInt(prop.get("numItemTopPaths"));
			this.numTopPaths = Integer.parseInt(prop.get("numTopPaths"));
			loadPropsIndex();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		prop.clear();
	}
	
	/**
	  * load property index
	  */
	private void loadPropsIndex(){
		props_index = new TIntObjectHashMap<String>();
		TextFileUtils.loadIndex("props_index", props_index);
		logger.debug("Properties index loading");
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
		path_index = new TObjectIntHashMap<String>();
		//path = new THashMap<String, TIntIntHashMap>();
		path = new HashMap<Integer, Integer>();
		
		logger.info("Top paths to select: " + numTopPaths);
		logger.info("Items to consider: " + numItemTopPaths);
		
		try {
			
			ItemFileManager itemReader = new ItemFileManager(itemsFile, ItemFileManager.READ);
			ArrayList<String> items_id = new ArrayList<String>(itemReader.getKeysIndex());
			int num_items = items_id.size();
			
			ArrayList<ItemTree> items = new ArrayList<ItemTree>();
			ItemTree tmp = null;
			
			// carico dim_blocks items in verticale
			for(int i = 0; i < numItemTopPaths && i < num_items; i++){
				
				tmp = itemReader.read(items_id.get(i));
				
				if(tmp!=null)
					items.add(tmp);
				
			}
			
			for(ItemTree item : items){
				// path extraction t-cols
				Runnable worker = new TopItemPathExtractorWorker(counter, path_index, item, 
						items, props_index, inverseProps, path);
				// run the worker thread  
				executor.execute(worker);
			}
			
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			
			itemReader.close();
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		Map<Integer, Integer> sorted_paths = ItemUtils.sortByValues(path);
		
		TIntHashSet top_path_id = new TIntHashSet();
		
		int i = 0;
		for(int p : sorted_paths.keySet()){
			if(i < numTopPaths){
				top_path_id.add(p);
				i++;
			}
			else
				break;
		}
		
		logger.info("Selected " + top_path_id.size() + " of " + path.size() + " paths");
		
		TObjectIntHashMap<String> top_path_index = new TObjectIntHashMap<String>(top_path_id.size());
		
		i = 1;
		for(String ss : path_index.keySet()){
			if(top_path_id.contains(path_index.get(ss)))
				top_path_index.put(ss, i++);
		}
		
		// write path index
		TextFileUtils.writeData(path_file + "_index", top_path_index);
		
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		TopItemPathExtractor pe = new TopItemPathExtractor();
		
		long start = System.currentTimeMillis();
		
		pe.start();
		
		long stop = System.currentTimeMillis();
		logger.info("Top paths extraction terminated in [sec]: " 
				+ ((stop - start) / 1000));
		
		//MemoryMonitor.stats();
		
	}
}
