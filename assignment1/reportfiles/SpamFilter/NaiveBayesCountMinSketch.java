import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;

/**
 * @author Jessa Bekker
 *
 * This class is a stub for naive Bayes with count-min sketch
 *
 * (c) 2017
 */
public class NaiveBayesCountMinSketch extends OnlineTextClassifier{

    private int nbOfHashes;
    private int logNbOfBuckets;
    private int[][][] counts; // counts[c][h][i]: The count of n-grams in e-mails of class c (spam: c=1)
                              // that hash to value i for the h'th hash function.
    private int[] classCounts; //classCounts[c] the count of e-mails of class c (spam: c=1)
    

    /* FILL IN HERE */

    private int[] seeds; // Different seeds for different hash functions

    /**
     * Initialize the naive Bayes classifier
     *
     * THIS CONSTRUCTOR IS REQUIRED, DO NOT CHANGE THE HEADER
     * You can write additional constructors if you wish, but make sure this one works
     *
     * This classifier uses the count-min sketch to estimate the conditional counts of the n-grams
     *
     * @param nbOfHashes The number of hash functions in the count-min sketch
     * @param logNbOfBuckets The hash functions hash to the range [0,2^NbOfBuckets-1]
     * @param threshold The threshold for classifying something as positive (spam). Classify as spam if Pr(Spam|n-grams)>threshold)
     */
    public NaiveBayesCountMinSketch(int nbOfHashes, int logNbOfBuckets, double threshold){
        this.nbOfHashes = nbOfHashes;
        this.logNbOfBuckets=logNbOfBuckets;
        this.threshold = threshold;

        /* FILL IN HERE */

        
        this.seeds = new int[nbOfHashes];
        Random rand = new Random();
        for (int i = 0; i<nbOfHashes; i++) {
          this.seeds[i] = rand.nextInt();
        }
       
        counts = new int[2][nbOfHashes][1 << logNbOfBuckets];
        classCounts =new int[2];
        
        
        for (int i = 0; i<2; i++) {
          for (int h = 0; h<nbOfHashes; h++) {
        	Arrays.fill(counts[i][h], 1);
  
          }
      	classCounts[i] = 1;
        } 
        
    }

    /**
     * Calculate the hash value of the h'th hash function for string str
     *
     * THIS METHOD IS REQUIRED
     *
     * The hash function hashes to the range [0,2^NbOfBuckets-1]
     * This method should work for h in the range [0, nbOfHashes-1]
     *
     * @param str The string to calculate the hash function for
     * @param h The number of the hash function to use.
     * @return the hash value of the h'th hash function for string str
     */
    private int hash(String str, int h){
        int v=0;

        /* FILL IN HERE */
        v = Math.floorMod(MurmurHash.hash32(str, seeds[h]) >>> h, 1 << logNbOfBuckets);

        return v;
    }

    /**
     * This method will update the parameters of your model using the incoming mail.
     *
     * THIS METHOD IS REQUIRED
     *
     * @param labeledText is an incoming e-mail with a spam/ham label
     */
    @Override
    public void update(LabeledText labeledText){
        super.update(labeledText);

        /* FILL IN HERE */
        classCounts[labeledText.label]++;
        for (String ng: labeledText.text.ngrams) { 
        	for (int h=0;h< nbOfHashes;h++) {
        		counts[labeledText.label][h][hash(ng,h)]++; 
        	}
        	//System.out.println("Hash: " + hash(ng) + " Label : " + labeledText.label + " Update: " + counts[labeledText.label][hash(ng)]);
        }
 
    }


    /**
     * Uses the current model to make a prediction about the incoming e-mail belonging to class "1" (spam)
     * The prediction is the probability for the e-mail to be spam.
     * If the probability is larger than the threshold, then the e-mail is classified as spam.
     *
     * THIS METHOD IS REQUIRED
     *
     * @param text is an parsed incoming e-mail
     * @return the prediction
     */
    @Override
    public double makePrediction(ParsedText text) {
        double pr = 0.0;

        /* FILL IN HERE */
        double productSpam = 0;
        double productNoSpam = 0;
        
        int i, tempSpam,tempNoSpam;
        int minSpam, minNoSpam ;
        
        
        for (String ng: text.ngrams) {
        	minSpam = Integer.MAX_VALUE;
        	minNoSpam = Integer.MAX_VALUE;
        

        	for (int h = 0;h < nbOfHashes; h++) {
        		i = hash(ng,h);

        		tempSpam = counts[1][h][i];
        		tempNoSpam = counts[0][h][i];
        		
        		//System.out.print(tempSpam + " ");

        		minSpam = minSpam<tempSpam?minSpam:tempSpam; 
        		minNoSpam = minNoSpam<tempNoSpam?minNoSpam:tempNoSpam; 
        		
        	}

        	//System.out.println(minSpam + "\n");
        	productSpam += Math.log(minSpam);
        	productNoSpam += Math.log(minNoSpam);
        }
       
        // size of set minus 1
        int lm1 = text.ngrams.size() - 1;
       
        //System.out.println((productNoSpam - productSpam ));
        //System.out.println((lm1*Math.log(this.classCounts[1]) - lm1*Math.log(this.classCounts[0])));

        //
        pr = 1 + Math.exp(productNoSpam - productSpam + lm1*(Math.log(classCounts[1]) - Math.log(classCounts[0])));
     //   System.out.print(1.0/pr + "\n");
        
        return 1.0 / pr;

    }


    /**
     * This runs your code.
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 8) {
            System.err.println("Usage: java NaiveBayesCountMinSketch <indexPath> <stopWordsPath> <logNbOfBuckets> <nbOfHashes> <threshold> <outPath> <reportingPeriod> <maxN> [-writeOutAllPredictions]");
            throw new Error("Expected 8 or 9 arguments, got " + args.length + ".");
        }
        try {
            // parse input
            String indexPath = args[0];
            String stopWordsPath = args[1];
            int logNbOfBuckets = Integer.parseInt(args[2]);
            int nbOfHashes = Integer.parseInt(args[3]);
            double threshold = Double.parseDouble(args[4]);
            String out = args[5];
            int reportingPeriod = Integer.parseInt(args[6]);
            int n = Integer.parseInt(args[7]);
            boolean writeOutAllPredictions = args.length>8 && args[8].equals("-writeOutAllPredictions");

            // initialize e-mail stream
            MailStream stream = new MailStream(indexPath, new EmlParser(stopWordsPath,n));

            // initialize learner
            NaiveBayesCountMinSketch nb = new NaiveBayesCountMinSketch(nbOfHashes ,logNbOfBuckets, threshold);

            // generate output for the learning curve
            EvaluationMetric[] evaluationMetrics = new EvaluationMetric[]{new Accuracy(),new Precision(),new TruePositiveRate(), new TrueNegativeRate()}; //ADD AT LEAST TWO MORE EVALUATION METRICS
            nb.makeLearningCurve(stream, evaluationMetrics, out+".nbcms", reportingPeriod, writeOutAllPredictions);

        } catch (FileNotFoundException e) {
            System.err.println(e.toString());
        }
    }
}
