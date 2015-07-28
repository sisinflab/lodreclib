package ciir.umass.edu.learning;

public class DenseDataPoint extends DataPoint {

	public DenseDataPoint(String text) {
		super(text);
	}
	
	public DenseDataPoint(DenseDataPoint dp)
	{
		label = dp.label;
		this.user_id = dp.user_id;
		description = dp.description;
		cached = dp.cached;
		fVals = new float[dp.fVals.length];
		System.arraycopy(dp.fVals, 0, fVals, 0, dp.fVals.length);
	}
	
	@Override
	public float getFeatureValue(int fid)
	{
		if(fid <= 0 || fid >= fVals.length)
		{
			System.out.println("Error in DataPoint::getFeatureValue(): requesting unspecified feature, fid=" + fid);
			System.out.println("System will now exit.");
			System.exit(1);
		}
		if(isUnknown(fVals[fid]))//value for unspecified feature is 0
			return 0;
		return fVals[fid];
	}
	
	@Override
	public void setFeatureValue(int fid, float fval)
	{
		if(fid <= 0 || fid >= fVals.length)
		{
			System.out.println("Error in DataPoint::setFeatureValue(): feature (id=" + fid + ") not found.");
			System.exit(1);
		}
		fVals[fid] = fval;
	}

	@Override
	public void setFeatureVector(float[] dfVals) {
		//fVals = new float[dfVals.length];
		//System.arraycopy(dfVals, 0, fVals, 0, dfVals.length);
		fVals = dfVals;
	}

	@Override
	public float[] getFeatureVector() {
		return fVals;
	}
}
