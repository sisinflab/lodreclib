package it.poliba.sisinflab.LODRecommender.graphEmbedding;

import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectFloatHashMap;
import it.poliba.sisinflab.LODRecommender.fileManager.ItemFileManager;
import it.poliba.sisinflab.LODRecommender.itemManager.ItemTree;
import it.poliba.sisinflab.LODRecommender.utils.MemoryMonitor;
import it.poliba.sisinflab.LODRecommender.utils.PropertyFileReader;
import it.poliba.sisinflab.LODRecommender.utils.TextFileUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class FeaturesMapping {
	
	private String items_file;
	private TIntObjectHashMap<String> metadata_index; // metadata index
	
	private static Logger logger = LogManager.getLogger(FeaturesMapping.class.getName());
	
	public FeaturesMapping(){
		
		// load config file
		Map<String, String> prop = null;
		
		try {
			prop = PropertyFileReader.loadProperties("config.properties");
			this.items_file = prop.get("itemsFile");
			loadMetadataIndex();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		prop.clear();
		
	}
	
	private void loadMetadataIndex(){
		metadata_index = new TIntObjectHashMap<String>();
		TextFileUtils.loadIndex("metadata_index", metadata_index);
	}
	
	private void mapping(){
		
		try{
			
			BufferedWriter br = new BufferedWriter(new FileWriter("metadata_features"));
			
			ItemFileManager itemReader = new ItemFileManager(items_file, ItemFileManager.READ);
			ArrayList<String> items_id = new ArrayList<String>(itemReader.getKeysIndex());
			
			THashMap<String, TIntIntHashMap> branches = null;
			ItemTree item = null;
			StringBuffer str = null;
			TObjectFloatHashMap<String> res = null;
			TIntIntHashMap resources = null;
			
			for(String item_id : items_id){
				
				item = itemReader.read(item_id);
				
				branches = item.getBranches();
				
				logger.info("Mapping item " + item_id);
				str = new StringBuffer();
				str.append(item_id + "\t");
				res = new TObjectFloatHashMap<String>();
				float weight = 0;
				
				for(String s : branches.keySet()){
					
					resources = new TIntIntHashMap();
					resources = branches.get(s);
					int l = s.split("-").length;
					
					TIntIntIterator it = resources.iterator();
					int key, value = 0;
					while(it.hasNext()){
						it.advance();
						key = it.key();
						value = it.value();
						//System.out.println(s + " - " + key + ": " + value + "*" + weight);
						weight = (float) ((1.0/l)*value);
						//System.out.println(weight);
						String[] lbl = metadata_index.get(key).split("/");
						res.adjustOrPutValue(lbl[lbl.length-1].replaceAll("[{'.,;:/}]", "_"), weight, weight);
						
					}
					
				}
				
				for(String s : res.keySet())
					str.append(s + ":" + res.get(s) + " ");
				
				br.append(str);
				br.newLine();
				
			}
			
			br.flush();
			br.close();
			
			itemReader.close();
			
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
		
		FeaturesMapping ss = new FeaturesMapping();
		
		long start = System.currentTimeMillis();
		
		ss.mapping();
		
		long stop = System.currentTimeMillis();
		logger.info("Conversion terminated in [sec]: " 
				+ ((stop - start) / 1000));
		
		MemoryMonitor.stats();
		
	}

}
