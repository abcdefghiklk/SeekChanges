package feature;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.SmallFloat;

public class OriginalTFIDFSimilarity extends Similarity{

	private static final float[] NORM_TABLE = new float[256];

	static {
		for (int i = 0; i < 256; i++) {
			NORM_TABLE[i] = SmallFloat.byte315ToFloat((byte)i);
	    }
	}

	public OriginalTFIDFSimilarity() {
		// TODO Auto-generated constructor stub
	}
	

	public Explanation idfExplain(CollectionStatistics collectionStats, TermStatistics termStats) {
		final long df = termStats.docFreq();
	    final long docCount = collectionStats.docCount() == -1 ? collectionStats.maxDoc() : collectionStats.docCount();
	    final float idf = idf(df, docCount);
	    return Explanation.match(idf, "idf(docFreq=" + df + ", docCount=" + docCount + ")");
	}
	
	public Explanation idfExplain(CollectionStatistics collectionStats, TermStatistics termStats[]) {
		final long docCount = collectionStats.docCount() == -1 ? collectionStats.maxDoc() : collectionStats.docCount();
		float idf = 0.0f;
		List<Explanation> subs = new ArrayList<>();
		for (final TermStatistics stat : termStats ) {
			final long df = stat.docFreq();
			final float termIdf = idf(df, docCount);
			subs.add(Explanation.match(termIdf, "idf(docFreq=" + df + ", docCount=" + docCount + ")"));
			idf += termIdf;
	    }
	    return Explanation.match(idf, "idf(), sum of:", subs);
	}
	
	public static float tf(float freq){
		return (float)Math.sqrt(freq);
	}
	
	public float idf(long docFreq, long docCount){
		return (float)(Math.log((docCount+1)/(double)(docFreq+1)) + 1.0);
	}
	
	public static float decodeNormValue(long norm){

	    return NORM_TABLE[(int) (norm & 0xFF)];  
	}
	
	public float sloppyFreq(int distance){
		return 1.0f / (distance + 1);
	}
	
	public float scorePayload(int doc, int start, int end, BytesRef payload){
		return 1.0f;
	}
	
	@Override
	public final long computeNorm(FieldInvertState state) {
		// TODO Auto-generated method stub
		return 1;
	}
	
	@Override
	public final SimWeight computeWeight(CollectionStatistics collectionStats,
			TermStatistics... termStats) {
		// TODO Auto-generated method stub
		final Explanation idf = termStats.length == 1
			    ? idfExplain(collectionStats, termStats[0])
			    : idfExplain(collectionStats, termStats);
		return new IDFStats(collectionStats.field(), idf);
	}

	@Override
	public SimScorer simScorer(SimWeight stats, LeafReaderContext context)
			throws IOException {
		// TODO Auto-generated method stub
		IDFStats idfstats = (IDFStats) stats;
	    return new TFIDFSimScorer(idfstats, context.reader().getNormValues(idfstats.field));
	}
	
	
	private final class TFIDFSimScorer extends SimScorer {
		private final IDFStats stats;
		private final float weightValue;
		private final NumericDocValues norms;
		    
		TFIDFSimScorer(IDFStats stats, NumericDocValues norms) throws IOException {
		    this.stats = stats;
		    this.weightValue = stats.value;
		    this.norms = norms;
		}
		    
		@Override
		public float score(int doc, float freq) {
			final float raw = tf(freq) * weightValue; // compute tf(f)*weight
		      
		    return norms == null ? raw : raw * decodeNormValue(norms.get(doc));  // normalize for field
		}
		    
		@Override
		public float computeSlopFactor(int distance) {
		    return sloppyFreq(distance);
		}

		@Override
		public float computePayloadFactor(int doc, int start, int end, BytesRef payload) {
			return scorePayload(doc, start, end, payload);
		}

		@Override
		public Explanation explain(int doc, Explanation freq) {
		    return explainScore(doc, freq, stats, norms);
		}
	}	  
	
	private static class IDFStats extends SimWeight {
		private final String field;
		/** The idf and its explanation */
		private final Explanation idf;
		private float queryNorm;
		private float boost;
		private float queryWeight;
		private float value;
		    
		public IDFStats(String field, Explanation idf) {
		      // TODO: Validate?
			this.field = field;
			this.idf = idf;
		    normalize(1f, 1f);
		}

		@Override
		public float getValueForNormalization() {
		   // TODO: (sorta LUCENE-1907) make non-static class and expose this squaring via a nice method to subclasses?
		    return queryWeight * queryWeight;  // sum of squared weights
		}

		@Override
		public void normalize(float queryNorm, float boost) {
			this.boost = boost;
		    this.queryNorm = queryNorm;
		    queryWeight = queryNorm * boost * idf.getValue();
		    value = queryWeight * idf.getValue();         // idf for document
		}
	}  
	private Explanation explainQuery(IDFStats stats) {
		  List<Explanation> subs = new ArrayList<>();

		    Explanation boostExpl = Explanation.match(stats.boost, "boost");
		    if (stats.boost != 1.0f)
		      subs.add(boostExpl);
		    subs.add(stats.idf);

		    Explanation queryNormExpl = Explanation.match(stats.queryNorm,"queryNorm");
		    subs.add(queryNormExpl);

		    return Explanation.match(
		        boostExpl.getValue() * stats.idf.getValue() * queryNormExpl.getValue(),
		        "queryWeight, product of:", subs);
		  }

	private Explanation explainField(int doc, Explanation freq, IDFStats stats, NumericDocValues norms) {
		Explanation tfExplanation = Explanation.match(tf(freq.getValue()), "tf(freq="+freq.getValue()+"), with freq of:", freq);
		Explanation fieldNormExpl = Explanation.match(
		norms != null ? decodeNormValue(norms.get(doc)) : 1.0f, "fieldNorm(doc=" + doc + ")");

		return Explanation.match(
			tfExplanation.getValue() * stats.idf.getValue() * fieldNormExpl.getValue(), 
			"fieldWeight in " + doc + ", product of:",
			tfExplanation, stats.idf, fieldNormExpl);
	}

	private Explanation explainScore(int doc, Explanation freq, IDFStats stats, NumericDocValues norms) {
		Explanation queryExpl = explainQuery(stats);
		Explanation fieldExpl = explainField(doc, freq, stats, norms);
	    if (queryExpl.getValue() == 1f) {
	    	return fieldExpl;
	    }
	    return Explanation.match(
  		queryExpl.getValue() * fieldExpl.getValue(),
	        "score(doc="+doc+",freq="+freq.getValue()+"), product of:",
	        queryExpl, fieldExpl);
	}
}
