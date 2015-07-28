package it.poliba.sisinflab.LODRec.graphkernel;


import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ItemPreProcessing {

	private TIntObjectHashMap<TIntFloatHashMap> map_item_intFeatures;
	private TIntObjectHashMap<String> featureStringIndex;
	private TObjectIntHashMap<String> featureStringInverseIndex;
	private HashSet<Integer> items;

	private int embeddingOption;
	private String entityMapFile;
	private String branchMapFile;
	private String itemMetadataFile;
	private int max_f;
	private int min_f;
	private boolean minmax_norm;
	private boolean idf;
	private boolean length_normaliz; // Euclidean length normalization ->
										// http://nlp.stanford.edu/IR-book/html/htmledition/dot-products-1.html#sec:inner

	public ItemPreProcessing(String itemMetadataFile, int embeddingOption,
			String entityMapFile, String branchMapFile, boolean minmax_norm,
			boolean idf, int max_f, int min_f, boolean length_normaliz) {

		this.itemMetadataFile = itemMetadataFile;
		this.embeddingOption = embeddingOption;
		this.entityMapFile = entityMapFile;
		this.branchMapFile = branchMapFile;
		this.min_f = min_f;
		this.max_f = max_f;
		this.minmax_norm = minmax_norm;
		this.idf = idf;
		this.length_normaliz = length_normaliz;

		if (embeddingOption == 1)
			itemMetadataFile = itemMetadataFile + entityMapFile;
		else if (embeddingOption == 2)
			itemMetadataFile = itemMetadataFile + branchMapFile;

		this.loadItemData(itemMetadataFile);
	}

	public ItemPreProcessing(String itemMetadataFile, boolean minmax_norm,
			boolean idf, int max_f, int min_f, boolean length_normaliz) {

		this.itemMetadataFile = itemMetadataFile;

		this.min_f = min_f;
		this.max_f = max_f;
		this.minmax_norm = minmax_norm;
		this.idf = idf;
		this.length_normaliz = length_normaliz;

		this.loadItemData(itemMetadataFile);
	}

	public void exec() {


		if (idf)
			this.computeIDF();

		if (minmax_norm)
			this.min_max_normalize();

		this.filterByFrequency();

		if (length_normaliz)
			this.length_normaliz();

		this.writeData(itemMetadataFile);
	}

	private void writeData(String filename) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
			TIntFloatHashMap m;
			StringBuffer buf;
			for (int id : items) {

				buf = new StringBuffer();
				buf.append(id + "\t");
				m = map_item_intFeatures.get(id);
				int[] fIDs = m.keys();
				Arrays.sort(fIDs);
				for (int i = 0; i < fIDs.length; i++) {
					buf.append(fIDs[i] + ":" + m.get(fIDs[i]) + " ");
				}
				writer.append(buf);
				writer.newLine();

			}

			writer.flush();
			writer.close();

		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		}
	}

	private void loadItemData(String file_name) {

		this.items = new HashSet<Integer>();
		map_item_intFeatures = new TIntObjectHashMap<TIntFloatHashMap>();
		featureStringIndex = new TIntObjectHashMap<String>();
		featureStringInverseIndex = new TObjectIntHashMap<String>();
		BufferedReader br;
		float avg = 0;
		try {
			br = new BufferedReader(new FileReader(file_name));
			String line = null;
			while ((line = br.readLine()) != null) {
				try {
					String[] vals = line.split("\t");
					int id = Integer.parseInt(vals[0]);

					if (!items.contains(id))
						items.add(id);

					// System.out.println("count "+ items.size());
					map_item_intFeatures.put(id, new TIntFloatHashMap());

					String[] values = vals[1].trim().split(" ");
					for (int i = 0; i < values.length; i++) {
						String[] pair = values[i].split(":");
						String fStr = pair[0];
						int fId;
						if (featureStringInverseIndex.containsKey(fStr))
							fId = featureStringInverseIndex.get(fStr);
						else {
							fId = featureStringInverseIndex.size() + 1;
							featureStringIndex.put(fId, fStr);
							featureStringInverseIndex.put(fStr, fId);
						}
						float fVal = Float.parseFloat(pair[1]);
						map_item_intFeatures.get(id).put(fId, fVal);
					}
					avg += map_item_intFeatures.get(id).size();

				} catch (Exception ex) {
					System.out.println(ex.getMessage());
					System.out.println(line);
				}
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// num_features = featureStringIndex.size();

		avg = avg / (float) map_item_intFeatures.keySet().size();
		System.out
				.println("item data loading terminated. avg features (considering also collaborative features) per item: "
						+ avg
						+ ". n. features in the index: "
						+ featureStringIndex.size());

	}

	private void min_max_normalize() {
		System.out.println("computing MIN MAX normalization");
		Map<Integer, Float> attribute_min_vals = new HashMap<Integer, Float>();
		Map<Integer, Float> attribute_max_vals = new HashMap<Integer, Float>();

		for (int i : map_item_intFeatures.keys()) {
			for (int j : map_item_intFeatures.get(i).keys()) {
				float v = map_item_intFeatures.get(i).get(j);

				if (!attribute_min_vals.containsKey(j))
					attribute_min_vals.put(j, v);
				if (attribute_min_vals.get(j) > v)
					attribute_min_vals.put(j, v);

				if (!attribute_max_vals.containsKey(j))
					attribute_max_vals.put(j, v);

				if (attribute_max_vals.get(j) < v)
					attribute_max_vals.put(j, v);
			}
		}

		for (int i : map_item_intFeatures.keys()) {
			for (int j : map_item_intFeatures.get(i).keys()) {
				float v = map_item_intFeatures.get(i).get(j);
				float max = attribute_max_vals.get(j);
				// float min = attribute_min_vals.get(j);
				float norm_val = (v) / (max);
				if (!Float.isNaN(norm_val))
					map_item_intFeatures.get(i).put(j, norm_val);
			}
		}

	}

	private void length_normaliz() {
		System.out.println("computing euclidean length normalization");
		float sum = 0, val = 0;
		for (int i : map_item_intFeatures.keys()) {
			sum = 0;
			for (int j : map_item_intFeatures.get(i).keys()) {

				val = map_item_intFeatures.get(i).get(j);
				sum += (val * val);

			}
			sum = (float) Math.sqrt(sum);
			for (int j : map_item_intFeatures.get(i).keys()) {

				val = map_item_intFeatures.get(i).get(j);
				map_item_intFeatures.get(i).put(j, val / sum);
			}
		}

	}

	private void computeIDF() {
		System.out.println("computing IDF");
		Map<Integer, Integer> attribute_nnz_vals = new HashMap<Integer, Integer>();

		for (int i : map_item_intFeatures.keys()) {
			for (int j : map_item_intFeatures.get(i).keys()) {

				if (!attribute_nnz_vals.containsKey(j))
					attribute_nnz_vals.put(j, 1);
				else
					attribute_nnz_vals.put(j, attribute_nnz_vals.get(j) + 1);
			}
		}

		int n_items = map_item_intFeatures.keys().length;

		for (int i : map_item_intFeatures.keys()) {
			for (int j : map_item_intFeatures.get(i).keys()) {
				float v = map_item_intFeatures.get(i).get(j);
				// System.out.println(v);
				v = (float) (v * Math.log(n_items
						/ (1 + attribute_nnz_vals.get(j))));
				map_item_intFeatures.get(i).put(j, v);
				// System.out.println(v);
				// System.out.println(" ------ ");
			}
		}
	}

	private void filterByFrequency() {
		System.out.println("computing min max filtering");
		float avg_nfeatures = 0;
		Map<Integer, Integer> attribute_nnz_vals = new HashMap<Integer, Integer>();

		for (int i : map_item_intFeatures.keys()) {
			for (int j : map_item_intFeatures.get(i).keys()) {

				if (!attribute_nnz_vals.containsKey(j))
					attribute_nnz_vals.put(j, 1);
				else
					attribute_nnz_vals.put(j, attribute_nnz_vals.get(j) + 1);
			}
		}

		HashSet<Integer> toRemove = new HashSet<Integer>();

		for (int j : attribute_nnz_vals.keySet()) {

			int occ = attribute_nnz_vals.get(j);

			if (occ >= max_f || occ <= min_f)
				toRemove.add(j);
		}

		for (int i : map_item_intFeatures.keys()) {

			for (int j : toRemove) {
				if (map_item_intFeatures.get(i).containsKey(j))
					map_item_intFeatures.get(i).remove(j);
			}

			avg_nfeatures += map_item_intFeatures.get(i).keys().length;

		}

		System.out.println("removed " + toRemove.size() + " features");

		avg_nfeatures = avg_nfeatures / (float) map_item_intFeatures.size();
		System.out
				.println("avg n. features (considering also collaborative features) per item after minmax filtering "
						+ avg_nfeatures);

	}

	private void cmpItemFeatureStats() {

		Map<Integer, Float> attribute_min_vals = new HashMap<Integer, Float>();
		Map<Integer, Float> attribute_max_vals = new HashMap<Integer, Float>();
		Map<Integer, Integer> attribute_nnz_vals = new HashMap<Integer, Integer>();
		Map<Integer, Set<Integer>> attribute_inverse_nnz_vals = new HashMap<Integer, Set<Integer>>();

		for (int i : map_item_intFeatures.keys()) {
			for (int j : map_item_intFeatures.get(i).keys()) {
				float v = map_item_intFeatures.get(i).get(j);

				if (!attribute_nnz_vals.containsKey(j))
					attribute_nnz_vals.put(j, 1);
				else
					attribute_nnz_vals.put(j, attribute_nnz_vals.get(j) + 1);

				// System.out.println(i+" "+" " + j + " " +v +" curr min: " +
				// attribute_min_vals.get(j));

				if (!attribute_min_vals.containsKey(j))
					attribute_min_vals.put(j, v);
				if (attribute_min_vals.get(j) > v)
					attribute_min_vals.put(j, v);

				if (!attribute_max_vals.containsKey(j))
					attribute_max_vals.put(j, v);

				if (attribute_max_vals.get(j) < v)
					attribute_max_vals.put(j, v);
			}
		}

		for (int j : attribute_nnz_vals.keySet()) {

			int occ = attribute_nnz_vals.get(j);
			if (!attribute_inverse_nnz_vals.containsKey(occ))
				attribute_inverse_nnz_vals.put(occ, new HashSet());

			attribute_inverse_nnz_vals.get(occ).add(j);

		}

		List<Integer> listOcc = new ArrayList<Integer>();
		listOcc.addAll(attribute_inverse_nnz_vals.keySet());
		Collections.sort(listOcc, Collections.reverseOrder());
		int topN = 250;
		int count = 0;
		for (int occ : listOcc) {
			for (int j : attribute_inverse_nnz_vals.get(occ)) {
				if (count >= topN)
					break;
				count++;
				System.out.print(j + " " + this.featureStringIndex.get(j)
						+ ". nnz:" + " " + occ);
				if (attribute_min_vals.containsKey(j))
					System.out.print(" min:" + attribute_min_vals.get(j));
				if (attribute_max_vals.containsKey(j))
					System.out.print(" max:" + attribute_max_vals.get(j));
				System.out.println();
			}
		}

	}
}
