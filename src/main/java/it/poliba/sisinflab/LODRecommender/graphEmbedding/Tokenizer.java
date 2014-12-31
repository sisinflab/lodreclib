package it.poliba.sisinflab.LODRecommender.graphEmbedding;

import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
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

public class Tokenizer {
	
	private String items_file;
	private TIntObjectHashMap<String> props_index; // property index
	private TIntObjectHashMap<String> metadata_index; // metadata index
	
	private static Logger logger = LogManager.getLogger(Tokenizer.class.getName());
	
	public Tokenizer(){
		
		// load config file
		Map<String, String> prop = null;
		
		try {
			prop = PropertyFileReader.loadProperties("config.properties");
			this.items_file = prop.get("itemsFile");
			loadPropsIndex();
			loadMetadataIndex();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		prop.clear();
		
	}
	
	private void loadPropsIndex(){
		props_index = new TIntObjectHashMap<String>();
		TextFileUtils.loadIndex("props_index", props_index);
	}
	
	private void loadMetadataIndex(){
		metadata_index = new TIntObjectHashMap<String>();
		TextFileUtils.loadIndex("metadata_index", metadata_index);
	}
	
	private void tokenize(){
		
		try{
			
			BufferedWriter br = new BufferedWriter(new FileWriter("metadata_string_1"));
			
			ItemFileManager itemReader = new ItemFileManager(items_file, ItemFileManager.READ);
			ArrayList<String> items_id = new ArrayList<String>(itemReader.getKeysIndex());
			
			THashMap<String, TIntIntHashMap> branches = null;
			ItemTree item = null;
			StringBuffer str = null;
			
			for(String item_id : items_id){
				
				item = itemReader.read(item_id);
				
				branches = item.getBranches();
				
				logger.info("Convert " + item_id);
				str = new StringBuffer();
				str.append(item_id + "\t");
				
				for(String s : branches.keySet()){
					
					String prop = "";
					
					String[] prop_vals = s.split("-");
					
					if(prop_vals.length==1){
					
						for(String ss: prop_vals){
							String[] p = props_index.get(Integer.parseInt(ss)).split("/");
							prop += p[p.length-1] + "--";
						}
							
						
						for(int f : branches.get(s).keys()){
							String[] lbl = metadata_index.get(f).split("/");
							str.append(prop + lbl[lbl.length-1].replaceAll("[{'.,;:/}]", "_") + ":" + branches.get(s).get(f) + " ");
						}
					}
				}
				
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
		
		Tokenizer ss = new Tokenizer();
		
		long start = System.currentTimeMillis();
		
		ss.tokenize();
		
		long stop = System.currentTimeMillis();
		logger.info("Conversion terminated in [sec]: " 
				+ ((stop - start) / 1000));
		
		MemoryMonitor.stats();
		
	}

}
