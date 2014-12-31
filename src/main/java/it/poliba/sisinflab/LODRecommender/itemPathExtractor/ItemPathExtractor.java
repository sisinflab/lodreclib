package it.poliba.sisinflab.LODRecommender.itemPathExtractor;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.THashSet;
import gnu.trove.set.hash.TIntHashSet;
import it.poliba.sisinflab.LODRecommender.fileManager.ItemFileManager;
import it.poliba.sisinflab.LODRecommender.fileManager.StringFileManager;
import it.poliba.sisinflab.LODRecommender.fileManager.TextFileManager;
import it.poliba.sisinflab.LODRecommender.itemManager.ItemTree;
import it.poliba.sisinflab.LODRecommender.utils.PropertyFileReader;
import it.poliba.sisinflab.LODRecommender.utils.StringUtils;
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
public class ItemPathExtractor {
	
	private String itemsFile; // metadata file name
	private boolean inverseProps; // directed property
	private int itemsInMemory; // number of items to load in memory
	private TObjectIntHashMap<String> path_index; // path index
	private TIntObjectHashMap<String> inverse_path_index; // key-value path index
	private TIntObjectHashMap<String> props_index; // property index
	private SynchronizedCounter counter; // synchronized counter for path index
	private boolean outputPathTextFormat;
	private boolean outputPathBinaryFormat;
	private boolean computeInversePaths;
	private String path_file;
	private TIntIntHashMap input_metadata_id;
	private boolean selectTopPaths;
	private TIntObjectHashMap<TIntHashSet> items_link;
	protected static THashSet<String> top_path_prefix;
	protected static THashSet<String> top_path_postfix;
	
	private static Logger logger = LogManager.getLogger(ItemPathExtractor.class.getName());
	
	
	/**
	 * Constuctor
	 */
	public ItemPathExtractor(){
		
		// load config file
		Map<String, String> prop = null;
		
		try {
			
			prop = PropertyFileReader.loadProperties("config.properties");
			this.inverseProps =  Boolean.valueOf(prop.get("directed"));
			this.selectTopPaths =  Boolean.valueOf(prop.get("computeTopPaths"));
			this.itemsInMemory = Integer.parseInt(prop.get("itemsInMemory"))/2;
			this.itemsFile = prop.get("itemsFile");
			this.path_file = prop.get("outputPathFile");
			this.outputPathTextFormat =  Boolean.valueOf(prop.get("outputExtractionTextFormat"));
			this.outputPathBinaryFormat =  Boolean.valueOf(prop.get("outputExtractionBinaryFormat"));
			this.computeInversePaths =  Boolean.valueOf(prop.get("computeInversePaths"));
			
			loadPropsIndex();
			loadInputMetadataID();
			
			if(computeInversePaths)
				logger.debug("Compute inverse paths abilited");
			
			if(selectTopPaths){
				logger.debug("Selection of top paths abilited");
				computeTopPaths();
			}
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		prop.clear();
	}
	
	public void computeTopPaths(){
		
		TopItemPathExtractor top = new TopItemPathExtractor();
		top.start();
		
		loadPathIndex();
		computePathPrePostfix();
		
	}
	
	private void computePathPrePostfix(){
		
		top_path_prefix = new THashSet<String>();
		top_path_postfix = new THashSet<String>();
		for(String ss : path_index.keySet()){
			top_path_prefix.add(ss.split("#")[0]);
			top_path_postfix.add(StringUtils.reverseDirected(ss.split("#")[1], props_index));
		}
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
	  * load path index
	  */
	private void loadPathIndex(){
		path_index = new TObjectIntHashMap<String>();
		inverse_path_index = new TIntObjectHashMap<String>();
		TextFileUtils.loadIndex("path_index", path_index);
		
		if(computeInversePaths)
			TextFileUtils.loadIndex("path_index", inverse_path_index);
		
		logger.info("Path index loading: " + path_index.size() + " paths loaded");
	}
	
	
	/**
	  * load metadata input id
	  */
	private void loadInputMetadataID(){
		input_metadata_id = new TIntIntHashMap();
		TextFileUtils.loadInputMetadataID("metadata_index", "input_uri_id", input_metadata_id);
		logger.debug("Input metadata index loading");
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
		items_link = new TIntObjectHashMap<TIntHashSet>();
		
		if(!selectTopPaths){
			path_index = new TObjectIntHashMap<String>();
			if(computeInversePaths)
				inverse_path_index = new TIntObjectHashMap<String>();
		}
		
		try {
			
			TextFileManager textWriter = null;
			if(outputPathTextFormat)
				textWriter = new TextFileManager(path_file + ".txt");
			
			StringFileManager pathWriter = null;
			if(outputPathBinaryFormat)
				pathWriter = new StringFileManager(path_file, StringFileManager.WRITE);
			
			ItemFileManager itemReader = new ItemFileManager(itemsFile, ItemFileManager.READ);
			ArrayList<String> items_id = new ArrayList<String>(itemReader.getKeysIndex());
			int num_items = items_id.size();
			
			ArrayList<ItemTree> items_v = null;
			ArrayList<ItemTree> items_o = null;
			int index_v = 0;
			int index_o = 0;
			
			ItemTree tmp = null;
			
			while(index_v < num_items){
				
				items_v = new ArrayList<ItemTree>();
				
				// carico dim_blocks items in verticale
				for(int i = index_v; i < (index_v + itemsInMemory) && i < num_items; i++){
					
					tmp = itemReader.read(items_id.get(i));
					
					if(tmp!=null)
						items_v.add(tmp);
					
				}
				
				index_o = index_v;
				
				while(index_o < num_items){
					
					if(executor.isTerminated())
			    		executor = Executors.newFixedThreadPool(n_threads);
					
					items_o = new ArrayList<ItemTree>();
					
					// carico dim_blocks items in orizzontale
					for(int i = index_o; i < (index_o + itemsInMemory) && i < num_items; i++){
						
						tmp = itemReader.read(items_id.get(i));
						
						if(tmp!=null)
							items_o.add(tmp);
						
					}
					
					for(ItemTree item : items_v){
						// path extraction t-cols
						Runnable worker = new ItemPathExtractorWorker(counter, path_index, inverse_path_index,
								item, items_o, props_index, inverseProps, textWriter, pathWriter, selectTopPaths,
								input_metadata_id, computeInversePaths, items_link);
						// run the worker thread  
						executor.execute(worker);
					}
					
					executor.shutdown();
					executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
					
					index_o += itemsInMemory;
				}
				
				index_v += itemsInMemory;
			}
			
			itemReader.close();
			
			if(textWriter!=null)
				textWriter.close();
		    
		    if(pathWriter!=null)
		    	pathWriter.close();
		    
		    TextFileUtils.writeData(path_file + "_index", path_index);
		    TextFileUtils.writeTIntMapTIntHashSet("items_link", items_link);
			
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
		
		ItemPathExtractor pe = new ItemPathExtractor();
		
		long start = System.currentTimeMillis();
		
		pe.start();
		
		long stop = System.currentTimeMillis();
		logger.info("Item paths extraction terminated in [sec]: " + ((stop - start) / 1000));
		
		//MemoryMonitor.stats();
		
	}
}
