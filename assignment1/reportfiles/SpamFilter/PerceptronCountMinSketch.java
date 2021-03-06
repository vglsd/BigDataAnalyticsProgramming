import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;


/**
 * @author Jessa Bekker
 *
 * This class is a stub for a perceptron with count-min sketch
 *
 * (c) 2017
 */
public class PerceptronCountMinSketch extends OnlineTextClassifier{

    private int nbOfHashes;
    private int logNbOfBuckets;
    private double learningRate;
    private double bias;
    private double[][] weights; // weights[h][i]: The h'th weight estimate for n-grams that hash to value i for the h'th hash function


    /* FILL IN HERE */
    private int[] seeds; // Different seeds for different hash functions
    private double queue[]; //The predictions queue to avoid making the same prediction twice, 
    private int qstart; // and also not having our prediction change after we saw the first
    private int qend; // true labels of the predicted period.

    /**
     * Initialize the perceptron classifier
     *
     * THIS CONSTRUCTOR IS REQUIRED, DO NOT CHANGE THE HEADER
     * You can write additional constructors if you wish, but make sure this one works
     *
     * This classifier uses the count-min sketch to estimate the weights of the n-grams
     *
     * @param nbOfHashes The number of hash functions in the count-min sketch
     * @param logNbOfBuckets The hash functions hash to the range [0,2^NbOfBuckets-1]
     * @param learningRate The size of the updates of the weights
     */
    public PerceptronCountMinSketch(int nbOfHashes, int logNbOfBuckets, double learningRate, int reportingPeriod){
        this.nbOfHashes = nbOfHashes;
        this.logNbOfBuckets=logNbOfBuckets;
        this.learningRate = learningRate;
        this.threshold = 0;

        /* FILL IN HERE */
        this.seeds = new int[nbOfHashes];
        weights = new double[nbOfHashes][1 + (1 << logNbOfBuckets)] ;


        Random rand = new Random();
        for (int i = 0; i<nbOfHashes; i++) {
          this.seeds[i] = rand.nextInt();
        }

        // Initialize with random weights

       for (int h = 0; h < nbOfHashes; h++) {
        	for (int i = 0; i <= 1 << logNbOfBuckets; i++) {
        		weights[h][i] = rand.nextGaussian()/1000;
        	}
        }

    	this.queue = new double[reportingPeriod];
    	this.qstart = 0;
    	this.qend = 0;
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

        v = 1 + Math.floorMod(MurmurHash.hash32(str, seeds[h]) >>> h, 1 << logNbOfBuckets);

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
        double step = 2 * labeledText.label - 1 - pop();

       	for (int h =0; h < nbOfHashes; h++) {

       		weights[h][0] +=  learningRate * step; 

       		for (String ng: labeledText.text.ngrams) {
        			weights[h][hash(ng,h)] +=  learningRate * step; 
       		}
       	}
 
    }

    /**
     * 
     * @param pr Pushes the last prediction made to the queue for later retrieval
     */
    private void push(double pr) {
        queue[qend] = pr;
        qend = (qend + 1) % queue.length;
    }

    /**
     * Pops the latest prediction made out of the queue 
     * @return 
     */
    private double pop() {
    	double result = queue[qstart];
        qstart = Math.floorMod(qstart - 1, queue.length);
        return result;
    }
    /**
     * Uses the current model to make a prediction about the incoming e-mail belonging to class "1" (spam)
     * If the prediction is positive, then the e-mail is classified as spam.
     *
     * This method gives the output of the perceptron, before it is passed through the threshold function.
     *
     * THIS METHOD IS REQUIRED
     *
     * @param text is an parsed incoming e-mail
     * @return the prediction
     */
    @Override
    public double makePrediction(ParsedText text) {
        double pr = 0;

        /* FILL IN HERE */
        pr += weights[0][0];

        // Hash Elements, To improve
        double hels[] = new double[nbOfHashes];

        for (String ng: text.ngrams) {
        	
        	for(int h = 0; h< nbOfHashes; h++) {
        		hels[h] = weights[h][hash(ng,h)];
        	}
        	

        	pr += (nbOfHashes % 2 == 1)?hels[nbOfHashes/2]:(hels[nbOfHashes/2 + 1]+hels[nbOfHashes/2])/2;
        }
        
        push(pr);

        return pr;
    }



    /**
     * This runs your code.
     */
    public static void main(String[] args) throws IOException {

        if (args.length < 8) {
            System.err.println("Usage: java PerceptronCountMinSketch <indexPath> <stopWordsPath> <logNbOfBuckets> <nbOfHashes> <learningRate> <outPath> <reportingPeriod> <maxN> [-writeOutAllPredictions]");
            throw new Error("Expected 8 or 9 arguments, got " + args.length + ".");
        }
        try {
            // parse input
            String indexPath = args[0];
            String stopWordsPath = args[1];
            int logNbOfBuckets = Integer.parseInt(args[2]);
            int nbOfHashes = Integer.parseInt(args[3]);
            double learningRate = Double.parseDouble(args[4]);
            String out = args[5];
            int reportingPeriod = Integer.parseInt(args[6]);
            int n = Integer.parseInt(args[7]);
            boolean writeOutAllPredictions = args.length>8 && args[8].equals("-writeOutAllPredictions");

            // initialize e-mail stream
            MailStream stream = new MailStream(indexPath, new EmlParser(stopWordsPath,n));

            // initialize learner
            PerceptronCountMinSketch perceptron = new PerceptronCountMinSketch(nbOfHashes ,logNbOfBuckets, learningRate, reportingPeriod);

            // generate output for the learning curve
            EvaluationMetric[] evaluationMetrics = new EvaluationMetric[]{new Accuracy(),new Precision(),new TruePositiveRate(), new TrueNegativeRate()}; //ADD AT LEAST TWO MORE EVALUATION METRICS
            perceptron.makeLearningCurve(stream, evaluationMetrics, out+".pcms", reportingPeriod, writeOutAllPredictions);

        } catch (FileNotFoundException e) {
            System.err.println(e.toString());
        }
    }


}
