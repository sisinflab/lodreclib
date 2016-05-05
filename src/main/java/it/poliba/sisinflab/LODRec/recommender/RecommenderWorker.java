package it.poliba.sisinflab.LODRec.recommender;

import it.poliba.sisinflab.LODRec.utils.BST;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.Ranker;
import ciir.umass.edu.learning.SparseDataPoint;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;

public class RecommenderWorker implements Runnable {
	
	private BufferedWriter bw;
	private Model libLinearModel;
	private Ranker rankLibModel;
	private String testFile;
	private int topN;
	private Map<Integer, List<Integer>> recc;
	private Map<Integer, BST> predictions;
	private boolean libLinear;
	private boolean rankLib;
	
	public RecommenderWorker(String testFile, BufferedWriter bw, Model libLinearModel, 
			Ranker rankLibModel, int topN, boolean libLinear, boolean rankLib) {
		
		this.testFile = testFile;
		this.libLinearModel = libLinearModel;
		this.rankLibModel = rankLibModel;
		this.bw = bw;
		this.topN = topN;
		this.libLinear = libLinear;
		this.rankLib = rankLib;
		
	}
	
	public RecommenderWorker(String testFile, Map<Integer, List<Integer>> recc, 
			Model model, int topN) {
		
		this.testFile = testFile;
		this.libLinearModel = model;
		this.recc = recc;
		this.topN = topN;
		this.libLinear = true;
		this.rankLib = false;
		
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		computePredictions();
		
		if(bw != null) {
			writeOnFile();
		} 
		else {
			makeMap();			
		}
		
	}
	
	private void makeMap() {
		
		for (int user_id : predictions.keySet()) {
			BST bst = predictions.get(user_id);
			bst.visit();
			List<Integer> list_items = bst.getSortedValues();
			recc.put(user_id, list_items);
		}
		
	}
	
	private void writeOnFile() {
		
		try {
		
			List<Integer> users = new ArrayList<Integer>();
			users.addAll(predictions.keySet());
			Collections.sort(users);
			
			DecimalFormat form = new DecimalFormat("#.###");
			form.setRoundingMode(RoundingMode.CEILING);
	
			StringBuffer str = null;
			for (int user_id : users) {
				str = new StringBuffer();
				str.append(user_id + "\t");
				BST bst = predictions.get(user_id);
				bst.visit();
				List<Double> list_scores = bst.getSortedKeys();
				List<Integer> list_items = bst.getSortedValues();
				
				
				
				for (int i = 0; i < list_items.size(); i++) {
					str.append(list_items.get(i)
							+ ":"
							+ form.format(list_scores.get(i))
									.replace(",", ".") + " ");
				}
				
				synchronized(bw){
					bw.append(str);
					bw.newLine();
				}
			}
			
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private void computePredictions() {
		
		predictions = new HashMap<Integer, BST>();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(testFile));
			
			double pred = 0;

			String line = null;
			while ((line = br.readLine()) != null) {

				String[] vals = line.split(" ");
				
				int user_id = Integer.parseInt(vals[1].split(":")[1]);
				int item_id = Integer.parseInt(vals[2].split(":")[1]);
				
				if(libLinear) {
				
					FeatureNode[] f = new FeatureNode[vals.length - 3];
					for (int i = 3; i < vals.length; i++) {
						String[] ss = vals[i].split(":");
						int key = Integer.parseInt(ss[0]);
						double value = Double.parseDouble(ss[1]);
						f[i - 3] = new FeatureNode(key, value);
					}
	
					pred = computePred(f);
				
				} else if(rankLib){
				
					DataPoint p = new SparseDataPoint(line);
					pred = rankLibModel.predictScore(p);
					
				}				

				predictions.putIfAbsent(user_id, new BST(topN));
				predictions.get(user_id).insert(pred, item_id);

			}

			br.close();

		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private double computePred(FeatureNode[] f) {
		double pred = 0;
		//double[] prob_estimates = new double[2];

		/*if (libLinearModel.isProbabilityModel()) {

			prob_estimates = new double[2];

			Linear.predictProbability(libLinearModel, f, prob_estimates);
			pred = prob_estimates[0];

		} else
			pred += Linear.predict(libLinearModel, f);*/
		
		pred = Linear.predict(libLinearModel, f);

		return pred;
	}

}
