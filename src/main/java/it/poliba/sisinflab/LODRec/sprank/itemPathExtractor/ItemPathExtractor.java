package it.poliba.sisinflab.LODRec.sprank.itemPathExtractor;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.THashSet;
import gnu.trove.set.hash.TIntHashSet;
import it.poliba.sisinflab.LODRec.fileManager.ItemFileManager;
import it.poliba.sisinflab.LODRec.fileManager.StringFileManager;
import it.poliba.sisinflab.LODRec.fileManager.TextFileManager;
import it.poliba.sisinflab.LODRec.itemManager.ItemTree;
import it.poliba.sisinflab.LODRec.utils.StringUtils;
import it.poliba.sisinflab.LODRec.utils.SynchronizedCounter;
import it.poliba.sisinflab.LODRec.utils.TextFileUtils;

import java.util.ArrayList;
import java.util.Iterator;
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
	
	private String workingDir;
	private int nThreads;
	
	private String itemsFile; // metadata file
	private String metadataIndexFile; // metadata index file
	private TIntIntHashMap input_metadata_id;
	private String uriIdIndexFile;
	
	private int itemsInMemory; // number of items to load in memory
	private TObjectIntHashMap<String> path_index; // path index
	private String pathIndexFile; // path index file
	private TIntObjectHashMap<String> inverse_path_index; // key-value path index
	
	private TIntObjectHashMap<String> props_index; // properties index
	private String propsIndexFile; // properties file index
	private boolean inverseProps; // directed properties
	
	private boolean outputPathTextFormat;
	private boolean outputPathBinaryFormat;
	private String path_file;	
	
	private boolean computeInversePaths;
	private boolean selectTopPaths;
	private int numTopPaths;
	private int numItemTopPaths;
	
	private TIntObjectHashMap<TIntHashSet> items_link;
	private String itemLinkFile;
	
	protected static THashSet<String> top_path_prefix;
	protected static THashSet<String> top_path_postfix;
	
	private SynchronizedCounter counter; // synchronized counter for path index
	
	private static Logger logger = LogManager
			.getLogger(ItemPathExtractor.class.getName());
	
	
	/**
	 * Constuctor
	 */
	public ItemPathExtractor(String workingDir, String itemMetadataFile,
							 String pathFile, Boolean computeInversePaths,
							 Boolean selectTopPaths, int numTopPaths, int numItemsTopPaths, Boolean outputPathBinaryFormat,
							 Boolean outputPathTextFormat, Boolean inverseProps,
							 int itemsInMemory, int nThreads) {

		this.workingDir = workingDir;
		this.itemsFile = itemMetadataFile;
		this.path_file = pathFile;
		this.computeInversePaths = computeInversePaths;
		this.selectTopPaths = selectTopPaths;
		this.outputPathBinaryFormat = outputPathBinaryFormat;
		this.outputPathTextFormat = outputPathTextFormat;
		this.inverseProps=inverseProps;
		this.nThreads = nThreads;
		this.itemsInMemory=itemsInMemory/2;
		this.numTopPaths = numTopPaths;
		this.numItemTopPaths = numItemsTopPaths;
		
		init();

	}

	private void init() {

		this.propsIndexFile = this.workingDir + "props_index";
		this.pathIndexFile = this.workingDir + "path_index";
		this.metadataIndexFile = this.workingDir + "metadata_index";
		this.uriIdIndexFile = workingDir + "input_uri_id";
		this.itemLinkFile = workingDir + "items_link";
		loadPropsIndex();
		loadInputMetadataID();

		if (computeInversePaths)
			logger.debug("Compute inverse paths abilited");

		if (selectTopPaths) {
			logger.debug("Top paths selection abilited");
			computeTopPaths(numTopPaths, numItemTopPaths);
		}

	}
	
	public void computeTopPaths(int numTopPaths, int numItemTopPaths){
		
		TopItemPathExtractor top = new TopItemPathExtractor(workingDir, inverseProps, 
				itemsFile, nThreads, numTopPaths, numItemTopPaths);
		top.start();
		
		loadPathIndex();
		computePathPrePostfix();
		
	}
	
	private void computePathPrePostfix(){
		
		top_path_prefix = new THashSet<String>();
		top_path_postfix = new THashSet<String>();
		for(String ss : path_index.keySet()){
			top_path_prefix.add(ss.split("#")[0]);
			if(inverseProps)
				top_path_postfix.add(StringUtils.reverseDirected(ss.split("#")[1], props_index));
			else
				top_path_postfix.add(StringUtils.reverse(ss.split("#")[1]));
		}
		logger.info("Top path prefixes: " + top_path_prefix.size());
		logger.info("Top path postfixes: " + top_path_postfix.size());
	}
	
	/**
	  * load property index
	  */
	private void loadPropsIndex(){
		props_index = new TIntObjectHashMap<String>();
		TextFileUtils.loadIndex(propsIndexFile, props_index);
		logger.debug("Properties index loading");
	}
	
	/**
	  * load path index
	  */
	private void loadPathIndex(){
		path_index = new TObjectIntHashMap<String>();
		inverse_path_index = new TIntObjectHashMap<String>();
		TextFileUtils.loadIndex(pathIndexFile, path_index);
		
		if(computeInversePaths)
			TextFileUtils.loadIndex(pathIndexFile, inverse_path_index);
		
		logger.info("Path index loading: " + path_index.size() + " paths loaded");
	}
	
	
	/**
	  * load metadata input id
	  */
	private void loadInputMetadataID(){
		input_metadata_id = new TIntIntHashMap();
		TextFileUtils.loadInputMetadataID(metadataIndexFile, uriIdIndexFile, 
				input_metadata_id);
		logger.debug("Metadata index loading");
	}
	
	/**
	 * start path extraction
	 */
	public void start(){
		
		//nThreads = 4;
		logger.debug("Threads number: " + nThreads);
		logger.info("Path extraction started");
		
		ExecutorService executor;
		executor = Executors.newFixedThreadPool(nThreads);
		
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
			
			ItemFileManager itemReader = new ItemFileManager(itemsFile, 
					ItemFileManager.READ);
			ArrayList<String> items_id = new ArrayList<String>(itemReader.getKeysIndex());
			int num_items = items_id.size();
			
			int index_v = 0;
			int index_o = 0;
			
			int index_item = 0;
			
			ArrayList<ItemTree> items = null;
			ItemTree tmp = null;
			
			if(num_items < itemsInMemory) {
				itemsInMemory = num_items/2;
			}
			
			while(index_v < num_items) {
				
				// creo lista items verticali
				ArrayList<ItemTree> items_v = new ArrayList<ItemTree>();
				for(int i = index_v; i < (index_v + itemsInMemory) 
						&& i < num_items; i++){
					
					tmp = itemReader.read(items_id.get(i));
					
					if(tmp!=null)
						items_v.add(tmp);
					
				}
				
				index_o = index_v + itemsInMemory;
				
				while(index_o < num_items) {
					
					if(executor.isTerminated())
			    		executor = Executors.newFixedThreadPool(nThreads);
					
					// carico lista items in orizzontali
					ArrayList<ItemTree> items_o = new ArrayList<ItemTree>();
					for(int i = index_o; i < (index_o + itemsInMemory) 
							&& i < num_items; i++){
						
						tmp = itemReader.read(items_id.get(i));
						
						if(tmp!=null)
							items_o.add(tmp);
						
					}
					
					if(items_o.size() > 0) {
					
						for(ItemTree item : items_v){
							
							Runnable worker = new ItemPathExtractorWorker(counter, path_index,
									inverse_path_index, item, items_o, props_index, inverseProps, 
									textWriter, pathWriter, selectTopPaths, input_metadata_id, 
									computeInversePaths, items_link);
							
							executor.execute(worker);
						}
					
					}
					
					// calcolo blocchi sulla diagonale
					// primo blocco diagonale solo su items verticali
					if(index_o == itemsInMemory) {
						
						Iterator<ItemTree> it = items_v.iterator();
						ItemTree item = null;
						index_item = 0;
						while(it.hasNext()) {
							
							item = it.next();
							//index_item = items_v.indexOf(item);
							items = new ArrayList<ItemTree>();
							items.addAll(items_v.subList(index_item + 1, items_v.size()));
							
							index_item++;
							
							if(items.size() > 0) {
								
																
									Runnable worker = new ItemPathExtractorWorker(counter,
											path_index, inverse_path_index, item, items, 
											props_index, inverseProps, textWriter, 
											pathWriter, selectTopPaths, input_metadata_id, 
											computeInversePaths, items_link);
							
									executor.execute(worker);
							}
							
						}
						
					}
					
					// altri blocchi diagonali su items orizzontali
					if(index_v == 0) {
						
						Iterator<ItemTree> it = items_o.iterator();
						ItemTree item = null;
						index_item = 0;
						while(it.hasNext()) {
							
							item = it.next();
							//index_item = items_o.indexOf(item);
							items = new ArrayList<ItemTree>();
							items.addAll(items_o.subList(index_item + 1, items_o.size()));
							index_item++;
							
							if(items.size() > 0) {
							
								Runnable worker = new ItemPathExtractorWorker(counter,
										path_index, inverse_path_index, item, items, 
										props_index, inverseProps, textWriter, 
										pathWriter, selectTopPaths, input_metadata_id, 
										computeInversePaths, items_link);
								
								executor.execute(worker);
							
							}
							
						}
						
					}				
					
					executor.shutdown();
					executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
					
					index_o += itemsInMemory;
					
				}
			
				if(index_v + itemsInMemory < num_items - itemsInMemory)
					index_v += itemsInMemory;
				else 
					index_v = num_items;
				
				logger.info(index_v + " of " + num_items + " items completed");
				
			}
			
			itemReader.close();
			
			if(textWriter!=null)
				textWriter.close();
		    
		    if(pathWriter!=null)
		    	pathWriter.close();
		    
		    TextFileUtils.writeData(pathIndexFile, path_index);
		    TextFileUtils.writeTIntMapTIntHashSet(itemLinkFile, items_link);
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		
	}
}
