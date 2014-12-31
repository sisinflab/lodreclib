package it.poliba.sisinflab.LODRecommender.learning;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.hash.TIntObjectHashMap;
import it.poliba.sisinflab.LODRecommender.utils.ItemUtils;
import it.poliba.sisinflab.LODRecommender.utils.PropertyFileReader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;

public class Predictor {
	
	private String unknown_file;
	private Model model;
	private int num_top_items;
	private String model_file;
	
	private static Logger logger = LogManager.getLogger(Predictor.class.getName());
	
	public Predictor(){
		
		// load config file
		Map<String, String> prop = null;
						
		try {
					
			prop = PropertyFileReader.loadProperties("config.properties");
			this.num_top_items = Integer.parseInt(prop.get("topItems"));
			this.unknown_file = "unknown_set";
			this.model_file = "model";
		
		}
		catch(Exception e){
			e.printStackTrace();
		}
				
		prop.clear();
		
		loadModel();
		
	}
	
	private void loadModel(){
		
		logger.info("Loading prediction model");
		
		try{
			
			File modelFile = new File(model_file);
			model = Model.load(modelFile);
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	public void predict(){
    	
    	logger.info("Start prediction");
    	
    	TIntObjectHashMap<HashMap<Integer, Long>> user_prediction = 
    			new TIntObjectHashMap<HashMap<Integer, Long>>();
    	
    	try{
			BufferedReader br = new BufferedReader(new FileReader(unknown_file));
			BufferedWriter bw = new BufferedWriter(new FileWriter("predictions"));
			
			String line = null;
			while((line=br.readLine()) != null){
				
				String[] vals = line.split(" ");
				FeatureNode[] f = new FeatureNode[vals.length-3];
				for(int i = 3; i < vals.length; i++){
					String[] ss = vals[i].split(":");
					int key = Integer.parseInt(ss[0]);
					double value = Double.parseDouble(ss[1]);
					f[i-3] = new FeatureNode(key, value);
				}
				
				double prediction = Linear.predict(model, f);
				
				int user_id = Integer.parseInt(vals[1].split(":")[1]);
				int item_id = Integer.parseInt(vals[2].split(":")[1]);
				
				user_prediction.putIfAbsent(user_id, new HashMap<Integer, Long>());
				user_prediction.get(user_id).put(item_id, Math.round(prediction));
				
			}
			
			TIntObjectIterator<HashMap<Integer, Long>> it = user_prediction.iterator();
			while(it.hasNext()){
				
				it.advance();
				bw.append(it.key() + "\t");
				
				Map<Integer, Long> sorted_items = ItemUtils.sortByValues(it.value());
				
				int i = 0;
				for(int p : sorted_items.keySet()){
					if(i < num_top_items){
						bw.append(p + ":" + sorted_items.get(p) + " ");
						i++;
					}
					else
						break;
				}
				
				bw.newLine();
				
			}
			
			br.close();
			bw.flush();
			bw.close();
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
    	
    	
    }

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		long start = System.currentTimeMillis();
		
		Learning l = new Learning();
		l.start();
		
		long stop = System.currentTimeMillis();
		logger.info("Learning terminated in [sec]: " 
				+ ((stop - start) / 1000));
		
		start = System.currentTimeMillis();
		
		Predictor p = new Predictor();
		p.predict();
		
		stop = System.currentTimeMillis();
		logger.info("Prediction terminated in [sec]: " 
				+ ((stop - start) / 1000));

	}

}
