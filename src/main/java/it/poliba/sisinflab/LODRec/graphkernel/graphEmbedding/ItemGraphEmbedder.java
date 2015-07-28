package it.poliba.sisinflab.LODRec.graphkernel.graphEmbedding;

import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectFloatHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import it.poliba.sisinflab.LODRec.fileManager.ItemFileManager;
import it.poliba.sisinflab.LODRec.graphkernel.ItemPreProcessing;
import it.poliba.sisinflab.LODRec.itemManager.ItemTree;
import it.poliba.sisinflab.LODRec.utils.TextFileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class ItemGraphEmbedder {

	private static final String ENTITY_DELIM = "#E";
	private static final String PROP_DELIM = "#P";
	private String workingDir;
	private String itemsFile;
	private TIntObjectHashMap<String> metadata_index; // metadata index
	private TIntObjectHashMap<String> props_index; // property index
	private String entityMapFile;
	private String branchMapFile;
	private int max_branch_length = 4;
	private boolean onlyEntityBranches = true;
	private Map<Integer, Float> alphaParams;
	private static Logger logger = LogManager.getLogger(ItemGraphEmbedder.class
			.getName());
	private int option = 1;
	private String trainRatingFile;
	private boolean collabFeatures = true;
	float thresh = 1;
	private boolean onlyCollabFeatures = false;

	// item -> user,rate
	private Map<Integer, Map<Integer, Float>> mapItemUserRatings;
	private String datasetFile;
	private int max_f;
	private int min_f;
	private boolean minmax_norm;
	private boolean idf;
	private boolean length_normaliz; // Euclidean length normalization ->
										// http://nlp.stanford.edu/IR-book/html/htmledition/dot-products-1.html#sec:inner

	private Map<Integer, Float> alpha_vals;

	public ItemGraphEmbedder(String workingDir, String itemMetadataFile,
			String entityMapFile, String branchMapFile, int embeddingOption,
			String trainRatingFile, int max_branch_length,
			boolean addCollabFeatures, boolean onlyEntityBranches,
			boolean minmax_norm, boolean idf, int max_f, int min_f,
			boolean length_normaliz, String listAlphaVals) {

		this.workingDir = workingDir;
		this.itemsFile = itemMetadataFile;
		this.entityMapFile = entityMapFile;
		this.branchMapFile = branchMapFile;
		this.option = embeddingOption;
		this.trainRatingFile = trainRatingFile;
		this.max_branch_length = max_branch_length;
		this.collabFeatures = addCollabFeatures;
		this.onlyEntityBranches = onlyEntityBranches;

		this.min_f = min_f;
		this.max_f = max_f;
		this.minmax_norm = minmax_norm;
		this.idf = idf;
		this.length_normaliz = length_normaliz;

		if (listAlphaVals!=null) {

			this.alpha_vals = new HashMap<Integer, Float>();

			String[] parts = listAlphaVals.split(",");
			for (int i = 0; i < parts.length; i++) {
				float val = Float.parseFloat(parts[i]);
				alpha_vals.put((i + 1), val);
			}
		}
		init();

	}

	public ItemGraphEmbedder(String workingDir, String itemMetadataFile,
			String entityMapFile, String branchMapFile, int embeddingOption,
			String trainRatingFile, int max_branch_length,
			boolean addCollabFeatures, boolean onlyEntityBranches,
			boolean minmax_norm, boolean idf, int max_f, int min_f,
			boolean length_normaliz) {

		this.workingDir = workingDir;
		this.itemsFile = itemMetadataFile;
		this.entityMapFile = entityMapFile;
		this.branchMapFile = branchMapFile;
		this.option = embeddingOption;
		this.trainRatingFile = trainRatingFile;
		this.max_branch_length = max_branch_length;
		this.collabFeatures = addCollabFeatures;
		this.onlyEntityBranches = onlyEntityBranches;

		this.min_f = min_f;
		this.max_f = max_f;
		this.minmax_norm = minmax_norm;
		this.idf = idf;
		this.length_normaliz = length_normaliz;

		init();

	}

	private void init() {

		mapItemUserRatings = new HashMap<Integer, Map<Integer, Float>>();
		this.branchMapFile = this.itemsFile + this.branchMapFile;
		this.entityMapFile = this.itemsFile + this.entityMapFile;
		if (this.collabFeatures) {
			mapItemUserRatings = this.loadRatingData(trainRatingFile);
		}

		alphaParams = new HashMap<Integer, Float>();

		if (option == 4)
			loadPropsIndex();
		if (option == 3 || option == 4)
			loadMetadataIndex();

		if (alpha_vals != null) {
			for (int h : alpha_vals.keySet()) {
				alphaParams.put(h, alpha_vals.get(h));
			}
		} else {
			for (int h = 1; h < 10; h++) {
				// if (h >= 8)
				// alphaParams.put(h, 0f);
				// else
				alphaParams.put(h, 1 / (1 + (float) Math.log(h)));
			}
		}
		
		System.out.println("ALPHA WEIGHTS");
		for(int h:alphaParams.keySet()){
			if(h>max_branch_length)
				break;
			System.out.println(h+":"+alphaParams.get(h));
		}
	}

	public void computeMapping() {
		if (option == 1) {
			entity_based_mapping_from_textfile();
		} else if (option == 2) {
			branch_based_mapping_from_textfile();
		}

		// else if (option == 3) {
		// entity_based_mapping();
		//
		// } else if (option == 4) {
		// branch_based_mapping();
		//
		// }

		System.out.println("embedding terminated");
		preprocess();
	}

	private void preprocess() {

		ItemPreProcessing prep = new ItemPreProcessing(datasetFile,
				minmax_norm, idf, max_f, min_f, length_normaliz);

		prep.exec();

	}

	private void loadPropsIndex() {
		props_index = new TIntObjectHashMap<String>();
		TextFileUtils.loadIndex(workingDir + "props_index", props_index);
	}

	private void loadMetadataIndex() {
		metadata_index = new TIntObjectHashMap<String>();
		TextFileUtils.loadIndex(itemsFile + "_index", metadata_index);
	}

	public Map<Integer, Map<Integer, Float>> loadRatingData(String filename) {
		Map<Integer, Map<Integer, Float>> ratings = new HashMap<Integer, Map<Integer, Float>>();

		try {
			BufferedReader reader = new BufferedReader(new FileReader(filename));

			String line = reader.readLine();

			while (line != null) {

				String[] str = line.split("\t");

				int u = Integer.parseInt(str[0].trim());
				int i = Integer.parseInt(str[1].trim());
				float rel = Float.parseFloat(str[2].trim());
				if (rel >= thresh) {
					if (!ratings.containsKey(i))
						ratings.put(i, new HashMap());
					ratings.get(i).put(u, rel);
				}
				line = reader.readLine();
			}

		} catch (IOException e) {
		}
		return ratings;
	}

	private void branch_based_mapping_from_textfile() {

		try {

			BufferedWriter writer = new BufferedWriter(new FileWriter(
					branchMapFile));

			datasetFile = branchMapFile;

			BufferedReader reader = new BufferedReader(new FileReader(
					this.itemsFile));
			StringBuffer str = null;
			TObjectFloatHashMap<String> res = null;
			String line = "";
			int item_id;
			while ((line = reader.readLine()) != null) {

				line = line.replace(":", "");

				res = new TObjectFloatHashMap<String>();
				String[] parts = line.split("\t");

				if (parts.length == 2) {
					item_id = Integer.parseInt(parts[0]);
					String[] branches = parts[1].split(" ");

					str = new StringBuffer();
					str.append(item_id + "\t");
					float val = 0;
					for (int i = 0; i < branches.length; i++) {
						String branch = cut_branch(branches[i]);

						if (!onlyEntityBranches)
							res.adjustOrPutValue(branch, 1, 1);

						int ind = 0;
						int j = 1;
						int ent_start = 0, ent_end = 0;
						while (ind != -1) {
							j++;
							ind = branch.indexOf(PROP_DELIM);
							if (ind != -1) {
								branch = branch.substring(ind
										+ PROP_DELIM.length());
								val = 1 / (float) j;

								if (!onlyEntityBranches)
									res.adjustOrPutValue(
											branch.replace(ENTITY_DELIM, "#")
													.replace(PROP_DELIM, "#"),
											val, val);

								String entityBranch = "";
								ent_start = 0;
								ent_end = 0;
								while (ent_start != -1) {
									ent_start = branch.indexOf(ENTITY_DELIM,
											ent_end + PROP_DELIM.length());
									ent_end = branch.indexOf(PROP_DELIM,
											ent_start + ENTITY_DELIM.length());
									if (ent_end == -1)
										ent_end = branch.length();
									if (ent_start != -1) {
										entityBranch += branch
												.substring(
														ent_start
																+ ENTITY_DELIM
																		.length(),
														ent_end);
										if (ent_end != branch.length())
											entityBranch += "#";
									}
								}

								res.adjustOrPutValue(
										entityBranch.replace(ENTITY_DELIM, "#")
												.replace(PROP_DELIM, "#"), val,
										val);

							}
						}

					}

					if (!onlyCollabFeatures) {
						for (String s : res.keySet()) {
							if(res.get(s)>0)
								str.append(s + ":" + res.get(s) + " ");
						}
					}

					if (collabFeatures
							& mapItemUserRatings.containsKey(item_id)) {
						for (int u : this.mapItemUserRatings.get(item_id)
								.keySet()) {
							str.append(u + ":" + 1 + " ");
						}
					}

					writer.append(str);
					writer.newLine();

				} else
					System.out.println(line + " ---- no data");

			}

			writer.flush();
			writer.close();

			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void entity_based_mapping_from_textfile() {

		try {

			BufferedWriter writer = new BufferedWriter(new FileWriter(
					this.entityMapFile));

			datasetFile = entityMapFile;

			BufferedReader reader = new BufferedReader(new FileReader(
					this.itemsFile));
			StringBuilder str = null;

			Map<String, Map<String, Float>> map = null;
			String line = "";
			int item_id;
			while ((line = reader.readLine()) != null) {

				line = line.replace(":", "");

				String[] parts = line.split("\t");

				if (parts.length == 2) {
					item_id = Integer.parseInt(parts[0]);

					String[] branches = parts[1].split(" ");

					int h = 0;
					str = new StringBuilder();
					str.append(item_id + "\t");
					float val = 0, w = 0;
					map = new HashMap();
					String ent, prefix;

					int start, end;

					for (int i = 0; i < branches.length; i++) {

						String branch = cut_branch(branches[i]);
						start = 0;

						while (start != -1) {

							start = branch.lastIndexOf(ENTITY_DELIM);
							end = branch.indexOf(PROP_DELIM, start
									+ ENTITY_DELIM.length());
							if (end == -1)
								end = branch.length();

							if (start != -1) {
								ent = branch.substring(
										start + ENTITY_DELIM.length(), end);
								if (start != -1)
									prefix = branch.substring(0, start);
								else
									prefix = ent;

								if (!map.containsKey(ent))
									map.put(ent, new HashMap<String, Float>());
								h = branch.split(ENTITY_DELIM).length - 1;
								map.get(ent).put(prefix, alphaParams.get(h));

								branch = prefix;
							}
						}

					}

					if (!onlyCollabFeatures) {
						for (String s : map.keySet()) {
							w = 0;
							for (String ss : map.get(s).keySet()) {

								w += map.get(s).get(ss);
							}
							if(w>0)
								str.append(s + ":" + w + " ");
						}

					}
					if (collabFeatures
							& mapItemUserRatings.containsKey(item_id)) {
						for (int u : this.mapItemUserRatings.get(item_id)
								.keySet()) {
							str.append(u + ":" + 1 + " ");
						}
					}

					writer.append(str);
					writer.newLine();

				} else
					System.out.println(line + " ---- no data");
			}

			writer.flush();
			writer.close();

			reader.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// cut the last part of the branch if it is longer than max_branch_length
	private String cut_branch(String branch) {
		int l = branch.split(PROP_DELIM).length - 1;

		if (l > max_branch_length) {
			String tmp = "";
			int ind = PROP_DELIM.length();

			for (int i = 0; i < max_branch_length; i++) {

				ind = branch.indexOf(PROP_DELIM, ind) + PROP_DELIM.length();
			}
			tmp = branch.substring(0, ind - PROP_DELIM.length());

			return tmp;

		} else
			return branch;
	}

	// da controllare
	private void branch_based_mapping() {

		try {

			BufferedWriter br = new BufferedWriter(
					new FileWriter(branchMapFile));

			ItemFileManager itemReader = new ItemFileManager(itemsFile,
					ItemFileManager.READ);
			ArrayList<String> items_id = new ArrayList<String>(
					itemReader.getKeysIndex());

			THashMap<String, TIntIntHashMap> branches = null;
			ItemTree item = null;
			StringBuilder str = null;
			TObjectIntHashMap<String> res = null;

			for (String item_id : items_id) {

				res = new TObjectIntHashMap<String>();
				item = itemReader.read(item_id);

				branches = item.getBranches();

				str = new StringBuilder();
				str.append(item_id + "\t");

				for (String s : branches.keySet()) {

					if (!isPrefix(s, branches.keySet())) {

						String b[] = s.split("-");

						for (int i = 0; i < b.length; i++) {

							String path = "";
							String features = "";

							for (int j = i; j < b.length; j++) {

								String[] bb = b[j].split("#");
								String[] p = props_index.get(
										Integer.parseInt(bb[0])).split("/");
								String[] f = metadata_index.get(
										Integer.parseInt(bb[1])).split("/");
								String clean_p = p[p.length - 1].replaceAll(
										"[{'.,;:/\\-}]", "_");
								String clean_f = f[f.length - 1].replaceAll(
										"[{'.,;:/\\-}]", "_") + "-";

								path += clean_p + "-" + clean_f;
								features += clean_f;

							}

							res.adjustOrPutValue(
									path.substring(0, path.length() - 1), 1, 1);
							res.adjustOrPutValue(features.substring(0,
									features.length() - 1), 1, 1);

						}

					}

				}

				for (String s : res.keySet())
					str.append(s + ":" + res.get(s) + " ");

				br.append(str);
				br.newLine();

			}

			br.flush();
			br.close();

			itemReader.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// da rivedere
	private void entity_based_mapping() {

		try {

			BufferedWriter br = new BufferedWriter(
					new FileWriter(entityMapFile));

			ItemFileManager itemReader = new ItemFileManager(itemsFile,
					ItemFileManager.READ);
			ArrayList<String> items_id = new ArrayList<String>(
					itemReader.getKeysIndex());

			THashMap<String, TIntIntHashMap> branches = null;
			ItemTree item = null;
			StringBuffer str = null;
			TObjectFloatHashMap<String> res = null;
			TIntIntHashMap resources = null;

			for (String item_id : items_id) {

				item = itemReader.read(item_id);

				branches = item.getBranches();

				str = new StringBuffer();
				str.append(item_id + "\t");
				res = new TObjectFloatHashMap<String>();
				float weight = 0;

				int h = 0;
				for (String prop : branches.keySet()) {

					resources = new TIntIntHashMap();
					resources = branches.get(prop);

					h = prop.split("-").length;

					if (h > max_branch_length) {
						System.out.println(prop + "  branch longer than "
								+ max_branch_length);
						break;
					}

					TIntIntIterator it = resources.iterator();
					int key, value = 0;
					while (it.hasNext()) {

						it.advance();
						key = it.key();
						value = it.value();

						weight = value * alphaParams.get(h);

						String[] lbl = metadata_index.get(key).split("/");
						String clean_lbl = lbl[lbl.length - 1].replaceAll(
								"[{'.,;:/}]", "_");

						res.adjustOrPutValue(clean_lbl, weight, weight);

					}

				}

				for (String s : res.keySet())
					str.append(s + ":" + res.get(s) + " ");

				br.append(str);
				br.newLine();

			}

			br.flush();
			br.close();

			itemReader.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private boolean isPrefix(String s, Set<String> list) {

		// System.out.println(s + "-" + list);

		for (String ss : list) {

			if (!ss.contentEquals(s) && ss.startsWith(s)) {
				// System.out.println(s + " - " + ss);
				return true;
			}
		}

		return false;

	}

}
