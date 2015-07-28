/*===============================================================================
 * Copyright (c) 2010-2012 University of Massachusetts.  All Rights Reserved.
 *
 * Use of the RankLib package is subject to the terms of the software license set 
 * forth in the LICENSE file included with this software, and also available at
 * http://people.cs.umass.edu/~vdang/ranklib_license.html
 *===============================================================================
 */

package ciir.umass.edu.learning;

import java.util.Arrays;
import java.util.Set;

/**
 * @author vdang
 * 
 *         This class implements objects to be ranked. In the context of
 *         Information retrieval, each instance is a query-url pair represented
 *         by a n-dimentional feature vector. It should be general enough for
 *         other ranking applications as well (not limited to just IR I hope).
 */
public abstract class DataPoint {

	int MAX_FEATURE = 100;
	public static int FEATURE_INCREASE = 20;
	protected static int featureCount = 0;

	protected static float UNKNOWN = Float.NaN;

	// attributes
	protected float label = 0.0f;// [ground truth] the real label of the data
									// point (e.g. its degree of relevance
									// according to the relevance judgment)
	protected String user_id = "";// id of this data point (e.g. query-id)
	protected String item_id = "";// id of this data point (e.g. query-id)

	protected String description = "";
	protected float[] fVals = null; // fVals[0] is un-used. Feature id MUST
									// start from 1

	// helper attributes
	protected int knownFeatures; // number of known feature values

	// internal to learning procedures
	protected double cached = -1.0;// the latest evaluation score of the learned
									// model on this data point

	protected static boolean isUnknown(float fVal) {
		return Float.isNaN(fVal);
	}

	protected static String getKey(String pair) {
		return pair.substring(0, pair.indexOf(":"));
	}

	protected static String getValue(String pair) {
		return pair.substring(pair.lastIndexOf(":") + 1);
	}

	/**
	 * Parse the given line of text to construct a dense array of feature values
	 * and reset metadata.
	 * 
	 * @param text
	 * @return Dense array of feature values
	 */
	protected float[] parse(String text, Set<Integer> listFeatures) {
		this.MAX_FEATURE = listFeatures.size() + 1;
		float[] fVals = new float[MAX_FEATURE];//
		Arrays.fill(fVals, UNKNOWN);
		int lastFeature = -1;
		try {
			int idx = text.indexOf("# ");
			if (idx != -1) {
				// int uid, iid;
				description = text.substring(idx + 1);
				String[] vals = description.split("\t"); // #user_id item_id

				// uid = Integer.parseInt(vals[0].trim()); // extract the
				// item_id
				//
				// if (uid == 307)
				// System.out.println(text);

				this.item_id = vals[1].trim(); // extract the item_id

				// iid = Integer.parseInt(this.item_id);

				text = text.substring(0, idx).trim();// remove the comment part
														// at the end of the
														// line

			} else {

				System.out.println("No text description! No itemId! " + text);
				System.exit(1);
			}
			String[] fs = text.split(" ");
			label = Float.parseFloat(fs[0]);
			if (label < 0) {
				System.out
						.println("Relevance label cannot be negative. System will now exit.");
				System.exit(1);
			}
			user_id = getValue(fs[1]);
			String key = "";
			String val = "";
			for (int i = 2; i < fs.length; i++) {
				key = getKey(fs[i]);
				val = getValue(fs[i]);
				int f = Integer.parseInt(key);

				if (listFeatures.contains(f)) {
					knownFeatures++;

					if (f >= MAX_FEATURE) {
						while (f >= MAX_FEATURE)
							MAX_FEATURE += FEATURE_INCREASE;
						float[] tmp = new float[MAX_FEATURE];
						System.arraycopy(fVals, 0, tmp, 0, fVals.length);
						Arrays.fill(tmp, fVals.length, MAX_FEATURE, UNKNOWN);
						fVals = tmp;
					}
					fVals[f] = Float.parseFloat(val);

					if (f > featureCount)// #feature will be the max_id observed
						featureCount = f;

					if (f > lastFeature)// note that lastFeature is the max_id
										// observed for this current data point,
										// whereas featureCount is the max_id
										// observed on the entire dataset
						lastFeature = f;
				}
			}
			// shrink fVals
			float[] tmp = new float[lastFeature + 1];
			System.arraycopy(fVals, 0, tmp, 0, lastFeature + 1);
			fVals = tmp;
		} catch (Exception ex) {
			System.out.println("Error in DataPoint::parse(): " + ex.toString());
			System.out.println(text);
			System.exit(1);
		}
		return fVals;
	}

	/**
	 * Parse the given line of text to construct a dense array of feature values
	 * and reset metadata.
	 * 
	 * @param text
	 * @return Dense array of feature values
	 */
	protected float[] parse(String text) {
		float[] fVals = new float[MAX_FEATURE];
		Arrays.fill(fVals, UNKNOWN);
		int lastFeature = -1;
		try {

			// int idx = text.indexOf("# ");
			// if (idx != -1) {
			// // int uid, iid;
			// description = text.substring(idx + 1);
			// String[] vals = description.split("\t"); // #user_id item_id
			//
			// // uid = Integer.parseInt(vals[0].trim()); // extract the
			// // item_id
			// //
			// // if (uid == 307)
			// // System.out.println(text);
			//
			// this.item_id = vals[1].trim(); // extract the item_id
			//
			// // iid = Integer.parseInt(this.item_id);
			//
			// text = text.substring(0, idx).trim();// remove the comment part
			// // at the end of the
			// // line
			//
			// } else {
			//
			// System.out.println("No text description! No itemId! " + text);
			// System.exit(1);
			// }
			String[] fs = text.split(" ");

			label = Float.parseFloat(fs[0]);

			if (label < 0) {
				System.out
						.println("Relevance label cannot be negative. System will now exit.");
				System.exit(1);
			}
			user_id = getValue(fs[1]);
			// item_id = fs[2].trim(); // extract the item_id

			String key = "";
			String val = "";

			key = getKey(fs[2]);
			val = getValue(fs[2]);
			if (key.compareTo("1") == 0)
				item_id = val;

			
			for (int i = 3; i < fs.length; i++) {
				knownFeatures++;
				key = getKey(fs[i])  ;
				val = getValue(fs[i]);


				int f = Integer.parseInt(key)   -1 ; //because of the itemID
				
				if (f >= MAX_FEATURE) {
					while (f >= MAX_FEATURE)
						MAX_FEATURE += FEATURE_INCREASE;
					float[] tmp = new float[MAX_FEATURE];
					System.arraycopy(fVals, 0, tmp, 0, fVals.length);
					Arrays.fill(tmp, fVals.length, MAX_FEATURE, UNKNOWN);
					fVals = tmp;
				}
				fVals[f] = Float.parseFloat(val);

				if (f > featureCount)// #feature will be the max_id observed
					featureCount = f;

				if (f > lastFeature)// note that lastFeature is the max_id
									// observed for this current data point,
									// whereas featureCount is the max_id
									// observed on the entire dataset
					lastFeature = f;
			}
			// shrink fVals
			float[] tmp = new float[lastFeature + 1];
			System.arraycopy(fVals, 0, tmp, 0, lastFeature + 1);
			fVals = tmp;
		} catch (Exception ex) {
			System.out.println("Error in DataPoint::parse(): " + ex.toString());
			System.out.println(text);
			System.exit(1);
		}
		return fVals;
	}

	/**
	 * Get the value of the feature with the given feature ID
	 * 
	 * @param fid
	 * @return
	 */
	public abstract float getFeatureValue(int fid);

	/**
	 * Set the value of the feature with the given feature ID return user_id; }
	 * 
	 * @param fid
	 * @param fval
	 */
	public abstract void setFeatureValue(int fid, float fval);

	/**
	 * Sets the value of all features with the provided dense array of feature
	 * values
	 */
	public abstract void setFeatureVector(float[] dfVals);

	/**
	 * Gets the value of all features as a dense array of feature values.
	 */
	public abstract float[] getFeatureVector();

	/**
	 * Default constructor. No-op.
	 */
	protected DataPoint() {
	};

	/**
	 * The input must have the form:
	 * 
	 * @param text
	 */
	protected DataPoint(String text) {
		float[] fVals = parse(text);
		setFeatureVector(fVals);
	}

	protected DataPoint(String text, Set<Integer> listFeatures) {
		float[] fVals = parse(text, listFeatures);
		setFeatureVector(fVals);
	}

	public String getID() {
		return user_id;
	}

	public void setID(String id) {
		this.user_id = id;
	}

	public String getItemID() {
		return item_id;
	}

	// public void setItemID(String id) {
	// this.item_id = id;
	// }

	public float getLabel() {
		return label;
	}

	public void setLabel(float label) {
		this.label = label;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setCached(double c) {
		cached = c;
	}

	public double getCached() {
		return cached;

	}

	public void resetCached() {
		cached = -100000000.0f;
		;
	}

	public String toString() {
		float[] fVals = getFeatureVector();
		String output = ((int) label) + " " + "qid:" + user_id + " ";
		for (int i = 1; i < fVals.length; i++)
			if (!isUnknown(fVals[i]))
				output += i + ":" + fVals[i]
						+ ((i == fVals.length - 1) ? "" : " ");
		output += " " + description;
		return output;
	}

	public static int getFeatureCount() {
		return featureCount;
	}
}