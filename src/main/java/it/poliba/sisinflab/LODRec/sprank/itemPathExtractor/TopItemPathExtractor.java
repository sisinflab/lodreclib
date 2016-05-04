package it.poliba.sisinflab.LODRec.sprank.itemPathExtractor;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TIntHashSet;
import it.poliba.sisinflab.LODRec.fileManager.ItemFileManager;
import it.poliba.sisinflab.LODRec.itemManager.ItemTree;
import it.poliba.sisinflab.LODRec.utils.ItemUtils;
import it.poliba.sisinflab.LODRec.utils.SynchronizedCounter;
import it.poliba.sisinflab.LODRec.utils.TextFileUtils;

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
	
	private String workingDir;
	private int nThreads;
	
	private String itemsFile; // metadata file name
	private boolean inverseProps; // directed property
	private TObjectIntHashMap<String> path_index; // path index
	private TIntObjectHashMap<String> props_index; // property index
	private SynchronizedCounter counter; // synchronized counter for path index
	private HashMap<Integer, Integer> path;
	private int numTopPaths;
	private int numItemTopPaths;
	private String propsIndexFile;
	private String pathIndexFile;
	
	private static Logger logger = LogManager
			.getLogger(TopItemPathExtractor.class.getName());
	
	
	/**
	 * Constuctor
	 */
	public TopItemPathExtractor(String workingDir, boolean inverseProps, 
			String itemContentFile, int nThreads, int numTopPaths, int numItemTopPaths) {
		
		this.workingDir = workingDir;
		this.numItemTopPaths = numItemTopPaths;
		this.numTopPaths = numTopPaths;
		this.pathIndexFile = workingDir + "path_index";
		this.propsIndexFile = this.workingDir + "props_index";
		this.inverseProps = inverseProps;
		this.itemsFile = itemContentFile;
		this.nThreads = nThreads;
		
		loadPropsIndex();
	}
	
	/**
	  * load property index
	  */
	private void loadPropsIndex(){
		props_index = new TIntObjectHashMap<String>();
		TextFileUtils.loadIndex(propsIndexFile, props_index);
		logger.debug("Properties index loaded");
	}
	
	/**
	 * start path extraction
	 */
	public void start(){
		
		//nThreads = 4;
		logger.debug("Threads number: " + nThreads);
		
		ExecutorService executor;
		executor = Executors.newFixedThreadPool(nThreads);
		
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
				Runnable worker = new TopItemPathExtractorWorker(counter, 
						path_index, item, items, props_index, inverseProps, 
						path);
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
		
		logger.info(top_path_id.size() + " of " + path.size() + " paths selected");
		
		TObjectIntHashMap<String> top_path_index = new TObjectIntHashMap<String>(top_path_id.size());
		
		i = 1;
		for(String ss : path_index.keySet()){
			if(top_path_id.contains(path_index.get(ss)))
				top_path_index.put(ss, i++);
		}
		
		// write path index
		TextFileUtils.writeData(pathIndexFile, top_path_index);
		
	}
}
