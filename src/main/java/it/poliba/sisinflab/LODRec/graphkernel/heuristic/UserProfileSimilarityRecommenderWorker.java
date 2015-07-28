package it.poliba.sisinflab.LODRec.graphkernel.heuristic;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class UserProfileSimilarityRecommenderWorker implements Runnable {

	private TIntObjectHashMap<TIntFloatHashMap> map_item_intFeatures;

	private String outFile;
	private boolean implicit;
	private BufferedWriter bw;
	private Map<Integer, Float> userTrainRatings;
	private int u;
	private int topN;
	private HashSet<Integer> items;

	private static final float MIN_SIM = 0f;

	private Float evalRatingThresh;

	private static Logger logger = LogManager
			.getLogger(UserProfileSimilarityRecommenderWorker.class.getName());

	public UserProfileSimilarityRecommenderWorker(int u,
			HashSet<Integer> items, BufferedWriter bw,
			TIntObjectHashMap<TIntFloatHashMap> map_item_intFeatures, int topN,
			Map<Integer, Float> userTrainRatings, boolean implicit,
			Float evalRatingThresh) {

		this.u = u;
		this.items = items;
		this.bw = bw;
		this.map_item_intFeatures = map_item_intFeatures;
		this.topN = topN;
		this.userTrainRatings = userTrainRatings;
		this.implicit = implicit;
		this.evalRatingThresh = evalRatingThresh;
	}

	private float cmpCosineSim(TIntFloatHashMap v1, TIntFloatHashMap v2) {

		TIntHashSet inters = new TIntHashSet();
		inters.addAll(v1.keySet());
		inters.retainAll(v2.keySet());

		if (inters.size() == 0)
			return 0;
		else {
			int i = 0;
			TIntIterator it = inters.iterator();
			float num = 0;
			float norm_v1 = 0;
			float norm_v2 = 0;
			while (it.hasNext()) {
				i = it.next();
				num += v1.get(i) * v2.get(i);
			}
			for (int k1 : v1.keys())
				norm_v1 += (v1.get(k1) * v1.get(k1));
			for (int k2 : v2.keys())
				norm_v2 += (v2.get(k2) * v2.get(k2));
			return num / (float) (Math.sqrt(norm_v1) * Math.sqrt(norm_v2));

		}

	}

	private void computeRecc(int u, Set<Integer> trainItems,
			TIntFloatHashMap user_prof) {
		Map<Double, Set<Integer>> map = new HashMap<Double, Set<Integer>>();

		double pred;
		double[] prob_estimates;
		for (int id : items) {
			pred = 0;
			if (!trainItems.contains(id)) {

				if (map_item_intFeatures.containsKey(id)) {

					pred = this.cmpCosineSim(map_item_intFeatures.get(id),
							user_prof);
					if (pred > MIN_SIM) {
						if (!map.containsKey(pred))
							map.put(pred, new HashSet());

						map.get(pred).add(id);
					}
				}
			}
		}

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

	@Override
	public void run() {

		TIntFloatHashMap user_prof = new TIntFloatHashMap();
		int c = 0;
		for (int i : userTrainRatings.keySet()) {
			if (implicit || userTrainRatings.get(i) >= evalRatingThresh) {
				c++;
				for (int j : this.map_item_intFeatures.get(i).keys()) {
					float val = this.map_item_intFeatures.get(i).get(j);
					user_prof.adjustOrPutValue(j, val, val);
				}
			}
		}
		for (int i : user_prof.keys())
			user_prof.adjustValue(i, 1 / (float) (c));

		computeRecc(u, userTrainRatings.keySet(), user_prof);
	}

}
