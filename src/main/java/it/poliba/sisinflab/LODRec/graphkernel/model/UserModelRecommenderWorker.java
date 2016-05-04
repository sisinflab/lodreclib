package it.poliba.sisinflab.LODRec.graphkernel.model;

import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectFloatHashMap;
import gnu.trove.set.hash.TIntHashSet;
import it.poliba.sisinflab.LODRec.evaluation.Evaluator;
import it.poliba.sisinflab.LODRec.recommender.Recommender;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;

public class UserModelRecommenderWorker implements Runnable {

	private TIntObjectHashMap<TIntFloatHashMap> map_item_intFeatures;

	private boolean implicit;
	private int num_features;
	private List<Double> listC;
	private List<Double> listEps;
	private List<Integer> listSolverType;
	private TIntHashSet selectedFeatures;
	private float relUnknownItems;
	private BufferedWriter bw;

	private Map<Integer, Float> userTrainRatings;
	private Set<Integer> originalTrainItems;
	private Map<Integer, Float> userValRatings;
	private Evaluator trainEval;
	private Evaluator validEval;

	private boolean silent = true;

	private int topN;

	private HashSet<Integer> items;

	private boolean addNegValidationEx;

	private int timesRealFb; // = 5 -> era 3
	private int nValidNegEx; // = 1000

	private int minTrainEx; // = 100;

	private int u;
	
	private int topK;
	private String metric;

	// private int n_features;

	private static Logger logger = LogManager
			.getLogger(UserModelRecommenderWorker.class.getName());

	public UserModelRecommenderWorker(int u, BufferedWriter bw,
			TIntObjectHashMap<TIntFloatHashMap> map_item_intFeatures,
			Evaluator trainEval, Evaluator validEval, boolean silent, int topN,
			int num_features, List<Double> listC, List<Double> listEps,
			List<Integer> listSolverType, Map<Integer, Float> userTrainRatings,
			Map<Integer, Float> userValRatings, boolean implicit,
			int nValidNegEx, boolean addNegValidationEx, int timesRealFb, int minTrainEx,
			HashSet<Integer> items, float relUnknownItems, int topK, String metric) {

		this.topK = topK;
		this.metric = metric;
		this.u = u;
		this.bw = bw;
		this.map_item_intFeatures = map_item_intFeatures;
		this.trainEval = trainEval;
		this.validEval = validEval;
		this.silent = silent;
		this.topN = topN;
		this.num_features = num_features;
		this.listC = listC;
		this.listEps = listEps;
		this.listSolverType = listSolverType;
		this.userTrainRatings = userTrainRatings;
		this.userValRatings = userValRatings;
		this.implicit = implicit;
		this.relUnknownItems = relUnknownItems;
		this.nValidNegEx = nValidNegEx;
		this.timesRealFb = timesRealFb;
		this.minTrainEx = minTrainEx;
		this.items = items;
		this.addNegValidationEx = addNegValidationEx;
		originalTrainItems = new HashSet<Integer>();

	}

	public void run() {

		// Recommender pred;

		long start = System.currentTimeMillis();

		Model model = train();

		if (model != null)
			computeRecc(model);
		long stop = System.currentTimeMillis();
		// logger.info("user " + u + " terminated in " + (stop - start) / 1000);
	}

	private void computeRecc(Model model) {

		Map<Double, Set<Integer>> map = new HashMap<Double, Set<Integer>>();

		double pred;
		double[] prob_estimates;
		FeatureNode[] f;
		// System.out.println("train ratings for user " + u + " : "
		// + originalTrainItems.size());

		// int count_pred=0;
		for (int id : items) {
			pred = 0;
			if (!originalTrainItems.contains(id)) {

				if (map_item_intFeatures.containsKey(id)) {
					// count_pred++;
					int[] fIDs = map_item_intFeatures.get(id).keys();
					Arrays.sort(fIDs);

					int h = 0;
					for (int i = 0; i < fIDs.length; i++) {
						if (selectedFeatures.contains(fIDs[i])) {
							h++;
						}
					}

					f = new FeatureNode[h];

					h = 0;
					for (int i = 0; i < fIDs.length; i++) {
						if (selectedFeatures.contains(fIDs[i])) {
							f[h] = new FeatureNode(fIDs[i],
									map_item_intFeatures.get(id).get(fIDs[i]));
							h++;
						}
					}

					// for (int i = 0; i < fIDs.length; i++) {
					// f[i] = new FeatureNode(fIDs[i], map_item_intFeatures
					// .get(id).get(fIDs[i]));
					// }

					if (model.isProbabilityModel() & implicit) {
						prob_estimates = new double[2];
						Linear.predictProbability(model, f, prob_estimates);

						pred = prob_estimates[0];
					} else
						pred = Linear.predict(model, f);

					if (!map.containsKey(pred))
						map.put(pred, new HashSet());

					// if (pred != 0)
					map.get(pred).add(id);
				}
			}
		}

		// System.out.println("n.pred :"+ count_pred);

		List<Double> scores = new ArrayList<Double>();
		scores.addAll(map.keySet());

		Collections.sort(scores, Collections.reverseOrder());

		int c = 0;
		Iterator<Double> it = scores.iterator();
		StringBuffer line = new StringBuffer();
		line.append(u + "\t");
		while (it.hasNext() & c < topN) {
			double s = it.next();
			for (int i : map.get(s)) {
				if (c == topN)
					break;
				line.append(i + ":"
						+ String.format("%.3f", s).replace(",", ".") + " ");
				c++;
			}

		}

		try {

			synchronized (bw) {
				bw.append(line);
				bw.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Model train() {

		Set<Integer> trainNegItems = new HashSet<Integer>();
		Set<Integer> valNegItems = new HashSet<Integer>();
		
		TObjectFloatHashMap<String> evalRes = new TObjectFloatHashMap<String>();

		// System.out.println("n.user train ratings at the beginning " +
		// userTrainRatings.size());
		originalTrainItems.addAll(userTrainRatings.keySet());
		if (userValRatings == null)
			userValRatings = new HashMap<Integer, Float>();
		if (implicit) {
			int negEx = timesRealFb * userTrainRatings.keySet().size();
			if ((negEx + userTrainRatings.keySet().size()) < minTrainEx)
				negEx = (minTrainEx - userTrainRatings.keySet().size());

			trainNegItems = selectNegativeItems(userTrainRatings.keySet(),
					items, negEx);
			valNegItems = selectNegativeItems(userValRatings.keySet(), items,
					this.nValidNegEx);

		} else if (userTrainRatings.keySet().size() < minTrainEx) {

			int negEx = (minTrainEx - userTrainRatings.keySet().size());
			trainNegItems = selectNegativeItems(userTrainRatings.keySet(),
					items, negEx);
		}
		if (addNegValidationEx) {
			// System.out.println();
			valNegItems = selectNegativeItems(userValRatings.keySet(), items,
					this.nValidNegEx);

		}

		// if(implicit)
		relUnknownItems = 0;

		for (int i : trainNegItems)
			userTrainRatings.put(i, relUnknownItems);
		for (int i : valNegItems)
			userValRatings.put(i, relUnknownItems);

		Model model = null;
		int nRows = userTrainRatings.keySet().size();
		FeatureNode[][] XTrain = new FeatureNode[nRows][];
		double[] yTrain = new double[nRows];
		int[] trainUserIndex = new int[nRows];
		int[] trainItemIndex = new int[nRows];

		analyzeTrainingFeatures(userTrainRatings);

		buildDataset(u, userTrainRatings, XTrain, yTrain, trainUserIndex,
				trainItemIndex);

		// logger.info("Creating problem");

		Problem problem = new Problem();
		problem.l = XTrain.length;// number of training examples
		problem.n = num_features + 1; // number of features
		problem.x = XTrain; // feature nodes
		problem.y = yTrain; // target values

		// logger.info("Number of training examples: " + problem.l);
		// logger.info("Number of features: " + problem.n);

		nRows = userValRatings.keySet().size();
		FeatureNode[][] XValid = new FeatureNode[nRows][];
		double[] yValid = new double[nRows];
		int[] validUserIndex = new int[nRows];
		int[] validItemIndex = new int[nRows];

		buildDataset(u, userValRatings, XValid, yValid, validUserIndex,
				validItemIndex);

		// logger.info("user " + u + " -- Number of training examples: "
		// + problem.l + ", Number of features: " + problem.n);

		Map<Integer, Float> trainMapPrec = new HashMap<Integer, Float>();
		Map<Integer, Float> trainMapRec = new HashMap<Integer, Float>();
		Map<Integer, Float> trainMapNDCG = new HashMap<Integer, Float>();

		Map<Integer, Float> valMapPrec = new HashMap<Integer, Float>();
		Map<Integer, Float> valMapRec = new HashMap<Integer, Float>();
		Map<Integer, Float> valMapNDCG = new HashMap<Integer, Float>();

		double bestPerf = 0, bestPerfTrain = 0, bestC = 0, bestEps=0;
		Model bestModel = null;
		int bestModelType = 0;

		long start=System.currentTimeMillis();
		try {
			for (int solverType : listSolverType) {
				for (double c : listC) {
					for (double eps : listEps) {

						SolverType solver = SolverType.getById(solverType);

						Parameter parameter = new Parameter(solver, c, eps);

						if (silent)
							Linear.setDebugOutput(null);

						model = Linear.train(problem, parameter);

						Recommender pred = new Recommender(model, topN);

						// Map<Integer, List<Integer>> trainRecc = new
						// HashMap<Integer, List<Integer>>();

						// trainRecc = pred.computeRecc(XTrain, trainUserIndex,
						// trainItemIndex, implicit);
						//
						// trainEval.eval(trainRecc, trainMapNDCG, trainMapPrec,
						// trainMapRec);

						Map<Integer, List<Integer>> validRecc = pred
								.computeRecc(XValid, validUserIndex,
										validItemIndex, implicit);
						
						evalRes = validEval.eval(validRecc);
						
						float perf = evalRes.get(metric);
						if (perf >= bestPerf) {

							bestPerf = perf;
							bestModel = model;
							bestModelType = solverType;
							bestC = c;
							bestEps = eps;
						}

						// System.out.println(" config -- model: "
						// + parameter.getSolverType() + ". C: "
						// + parameter.getC() + ", eps: "
						// + parameter.getEps() + ". validationset prec: "
						// + valMapPrec + ". validationset rec: "
						// + valMapRec);

					}
				}
			}
		} catch (Exception ex) {

			ex.printStackTrace();

			return null;
		}
		if (bestModel == null)
			bestModel = model;
		long stop=System.currentTimeMillis();
		// logger.info("best model - bestPerf_" + k_modeSel + ": " + bestPerf);
		// System.out
		// .println("-----------------------------------------------------------------");
		System.out
				.println("-----------------------------------------------------------------");
		System.out.println("user " + u + " --------- BEST MODEL " + bestModelType + ". C: "
				+ bestC + ", eps: " + bestEps + " . Metric " + metric + "@" + topK + ": " 
				+ bestPerf + " n.train ex "
				+ userTrainRatings.keySet().size() + " n.valid ex "
				+ userValRatings.keySet().size()+" tot time: " +(stop-start)/1000);

		return model;

	}

	private void analyzeTrainingFeatures(Map<Integer, Float> ratings) {

		selectedFeatures = new TIntHashSet();

		for (int id : ratings.keySet()) {
			if (map_item_intFeatures.containsKey(id)) {
				selectedFeatures.addAll(map_item_intFeatures.get(id).keySet());
			}
		}
		// System.out.println(u + " train features " + selectedFeatures.size());
	}

	private void buildDataset(int u, Map<Integer, Float> ratings,
			FeatureNode[][] X, double[] y, int[] userIndex, int[] itemIndex) {

		try {
			int j = 0;
			FeatureNode[] f;
			for (int id : ratings.keySet()) {
				if (map_item_intFeatures.containsKey(id)) {
					int[] fIDs = map_item_intFeatures.get(id).keys();

					Arrays.sort(fIDs);

					int h = 0;
					for (int i = 0; i < fIDs.length; i++) {
						if (selectedFeatures.contains(fIDs[i])) {
							h++;
						}
					}
					f = new FeatureNode[h];
					h = 0;
					for (int i = 0; i < fIDs.length; i++) {
						if (selectedFeatures.contains(fIDs[i])) {
							f[h] = new FeatureNode(fIDs[i],
									map_item_intFeatures.get(id).get(fIDs[i]));
							h++;
						}
					}
					X[j] = f;
					y[j] = ratings.get(id);

					userIndex[j] = u;
					itemIndex[j] = id;

					j++;

				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private Set<Integer> selectNegativeItems(Set<Integer> positiveItems,
			Set<Integer> items, int N) {

		Set<Integer> set_candidate = new HashSet<Integer>();

		set_candidate.addAll(items);

		set_candidate.removeAll(positiveItems);

		List<Integer> candidate = new ArrayList<Integer>();
		candidate.addAll(set_candidate);

		return chooseRndItems(candidate, N);

	}

	private Set<Integer> chooseRndItems(List<Integer> list, int N) {

		Set<Integer> keys = new HashSet<Integer>();
		Set<Integer> ret = new HashSet<Integer>();
		Random r = new Random();
		int cont = 0;

		if (list.size() < N)
			return ret;

		while (cont < N) {
			int rr = r.nextInt(list.size());
			while (keys.contains(rr))
				rr = r.nextInt(list.size());
			keys.add(rr);
			cont++;
		}

		for (int k : keys)
			ret.add(list.get(k));

		return ret;
	}

}
