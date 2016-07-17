package evaluator;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.POS;
import htmlparser.JTidyHTMLHandler;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.lucene.document.Document;
import util.UF;
import util.Util;

public class DuplicateGenerator {

    /***** CONSTANTS *****/
    // The fraction of pages for which a duplicate should be generated.
    private static final double PAGE_DUP_RATIO = 0.3;

    // If duplicates are being generated for a page, the maximum number
    // of duplicates to generate (picked randomly from [1, MAX_DUPS_PER_PAGE].)
    private static final int MAX_DUPS_PER_PAGE = 3;
    
    // If duplicates are being generated for a page, 
    // the fraction of segments for which a random segment should be inserted.
    private static final double DIF_SEGMENTS_RATIO = 0.05;

    // If duplicates are being generated for a page, the maximum number of 'random words'
    // to insert. 
    private static final int MAX_SEGMENT_LENGTH = 3;


    /***** CLASS MEMBERS *****/
    // The directory to read original files from.
    String inputDir_;

    // The directory to write original files and duplicates to.
    String outputDir_;

    // The directory containing the English dictionary to pull random words from.
    String dictionaryDir_;

    // The mode of duplicate generation (text or html)
    String mode_;

    // The total number of output documents (originals + duplicates) written out.
    int numOutDocs_;

    // Contains pairs of form <docA,docB> identifying the documents that are
    // known to be duplicates. docA is lexicographically < than docB.
    HashSet<String> duplicates_;

    // Gives the unique integer identifier for each doc name.
    HashMap<String, Integer> docNameToId_;

    // Gives the doc name given the unique integer identifier for the document.
    HashMap<Integer, String> idToDocName_;

    public DuplicateGenerator(String inputDir, String outputDir, 
			      String dictionaryDir, String mode) {
	// Initializes the member variables
	inputDir_ = inputDir;
	outputDir_ = outputDir;
	dictionaryDir_ = dictionaryDir;
	mode_ = mode;
	numOutDocs_ = 0;
	duplicates_ = new HashSet<String>();
	docNameToId_ = new HashMap<String, Integer>();
	idToDocName_ = new HashMap<Integer, String>();
    }

    // Records bookkeeping information for a duplicate.
    private void recordDuplicate(String basename, int dupNumber) {
	String dupBasename = basename + "_dup_" + dupNumber;
	if (mode_.equals("html")) {
	    // Strip the ".html" extension
	    String withoutExtension = basename.substring(0, basename.length() - 5);
	    dupBasename = withoutExtension + "_dup_" + dupNumber + ".html";
	}
	duplicates_.add(basename + "," + dupBasename);
	for (int i = 1; i < dupNumber; ++i) {
	    String otherDup = basename + "_dup_" + i;
	    if (mode_.equals("html")) {
		// Strip the ".html" extension
		String withoutExtension = basename.substring(0, basename.length() - 5);
		otherDup = withoutExtension + "_dup_" + i + ".html";
	    }
	    duplicates_.add(otherDup + "," + dupBasename);
	}
    }

    // Records the document ID assigned to a document.
    private void addDocId(String docName, int docId) {
	docNameToId_.put(docName, docId);
	idToDocName_.put(docId, docName);
    }

    // Generates duplicates for a subset of the documents picked randomly
    public void generate() throws FileNotFoundException, IOException {
	String space = " ";
	// We use an English dictionary to randomly insert words.
	Dictionary dict = new Dictionary(new File(dictionaryDir_));
	dict.open();
	// Read nouns from the dictionary.
	Iterator<IIndexWord> nounIterator = dict.getIndexWordIterator(POS.NOUN);
	HashSet<String> randomWords = new HashSet<String>();
	// Add some nounds to the HashSet. 
	for (int i = 0; i < 1000000; i += 10) {
	    randomWords.add(nounIterator.next().getLemma());
	}
	// We use the ordering provided by the HashSet, which should be 
	// somewhat random.
	Iterator<String> randomWordsIterator = randomWords.iterator();
	Random random = new Random();
	Collection<File> files = Util.walk(inputDir_);
	// Iterate over the list of input files.
	for (File f : files) {
	    String basename = Util.getBasename(f.toString());
	    Path outPath = FileSystems.getDefault().getPath(outputDir_, basename);
	    String fileContent = Util.readFile(f.toPath());
	    // Write the original file to the output directory.
	    FileOutputStream outStream = new FileOutputStream(outPath.toFile());
	    outStream.write(fileContent.getBytes());
	    outStream.close();
	    addDocId(basename, numOutDocs_++);
	    // With some probability, generate duplicate version(s) of the original file.
	    if (random.nextDouble() <= PAGE_DUP_RATIO) {
		String[] tokens = fileContent.split("\\s+");
		// Determine the number of duplicate versions of the original.
		int numDups = 1 + random.nextInt(MAX_DUPS_PER_PAGE);
		for (int i = 0; i < numDups; ++i) {
		    // Assign name to the duplicate.
		    String dupBasename = basename + "_dup_" + (i + 1);
		    String title = "";
		    if (mode_.equals("html")) {
			// Strip the ".html" extension
			String withoutExtension = basename.substring(0, basename.length() - 5);
			dupBasename = withoutExtension + "_dup_" + (i + 1) + ".html";
			Document document = new JTidyHTMLHandler().getDocument(new FileInputStream(f));
			title = document.get("title");
			tokens = document.get("body").split("\\s+");
		    }
		    addDocId(dupBasename, numOutDocs_++);
		    recordDuplicate(basename, i + 1);
		    Path dupPath = FileSystems.getDefault().getPath(outputDir_, dupBasename);
		    FileOutputStream dupStream = new FileOutputStream(dupPath.toFile());
		    if (mode_.equals("html")) {
			dupStream.write("<html>\n".getBytes());
			dupStream.write(new String("<title> " + title + "</title>\n").getBytes());
			dupStream.write("<body>\n".getBytes());
		    }
		    // Iterate over the words in the original document.
		    for (String token : tokens) {
			dupStream.write(token.getBytes());
			dupStream.write(space.getBytes());
			// Probabilistically insert a random segment after the original word.
			if (random.nextDouble() <= DIF_SEGMENTS_RATIO) {
			    // Determine the length of the segment.
			    int segmentLength = 1 + random.nextInt(MAX_SEGMENT_LENGTH);
			    for (int j = 0; j < segmentLength; ) {
				// Get a random noun from the dictionary and split it into
				// component words.
				String[] segmentWords = randomWordsIterator.next().split("_");
				for (String word : segmentWords) {
				    if (j < segmentLength) {
					// Write out each random word.
					dupStream.write(word.getBytes());
					dupStream.write(space.getBytes());
					j++;
				    }
				}
			    }
			}
		    }
		    if (mode_.equals("html")) {
			dupStream.write("</body>\n".getBytes());
			dupStream.write("</html>\n".getBytes());
		    }
		    dupStream.close();
		}
	    }	    
	}
    }

    public void printStats(int truePos, int falsePos, int trueNeg, int falseNeg) {
	double precision = (truePos + 0.0) / (truePos + falsePos + 0.0);
	double recall = (truePos + 0.0) / (truePos + falseNeg + 0.0);
	double fScore = (2.0 * precision * recall) / (precision + recall);
	System.out.println("Precision: " + precision);
	System.out.println("Recall: " + recall);
	System.out.println("F score: " + fScore);
    }
    
    // Calculates precision, recall and F for the duplicates detected by our implementation
    public void calculateStats(Set<String> algoPairs) {
	int truePos = 0;
	int falsePos = 0;
	for (String pair : algoPairs) {
	    if (duplicates_.contains(pair)) {
		truePos++;
	    } else {
		falsePos++;
	    }
	}
	int trueNeg = 0;
	int falseNeg = 0;
	System.out.println("Expected results: ");
	for (String pair : duplicates_) {
	    System.out.println(pair);
	    if (!algoPairs.contains(pair)) {
		falseNeg++;
	    }
	}
	// n choose 2 pairs
	int pairs = numOutDocs_ * (numOutDocs_ - 1) / 2;
	trueNeg = pairs - (truePos + falsePos + falseNeg);
	printStats(truePos, falsePos, trueNeg, falseNeg);
    }

    public void calculateUnionFindStats(Set<String> algoPairs) {
	UF unionFindAlgo = new UF(numOutDocs_);
	for (String pair : algoPairs) {
	    String[] docs = pair.split(",");
	    int docId1 = docNameToId_.get(docs[0]);
	    int docId2 = docNameToId_.get(docs[1]);
	    unionFindAlgo.union(docId1, docId2);
	}
	int truePos = 0;
        int falsePos = 0;
	int trueNeg = 0;
        int falseNeg = 0;
	for (int i = 0; i < numOutDocs_; ++i) {
	    for (int j = 0; j < i; ++j) {
		String referenceKey = idToDocName_.get(j) + "," + idToDocName_.get(i);
		boolean inReference = duplicates_.contains(referenceKey);
		boolean inAlgo = unionFindAlgo.connected(j, i);
		if (inAlgo && inReference) truePos++;
		else if (inAlgo && !inReference) falsePos++;
		else if (!inAlgo && inReference) falseNeg++;
		else trueNeg++;
	    }
	}
	printStats(truePos, falsePos, trueNeg, falseNeg);
    }

    public static void main(String[] args) throws Exception {
	Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("input")
                          .withDescription("Path to the directory containing the input data")
                          .hasArg()
                          .isRequired()
                          .create('i'));
	options.addOption(OptionBuilder.withLongOpt("output")
                          .withDescription("Path to the directory containing the output data")
                          .hasArg()
                          .isRequired()
                          .create('o'));
	options.addOption(OptionBuilder.withLongOpt("dict")
                          .withDescription("Path to the directory containing the WordNet dictionary")
                          .hasArg()
                          .isRequired()
                          .create('d'));
	options.addOption(OptionBuilder.withLongOpt("mode")
                          .withDescription("The mode of the duplicate generation. Valid values are \"text\" or \"html\".")
                          .hasArg()
                          .isRequired()
                          .create('m'));
	CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        String inputDir = cmd.getOptionValue("i");
        String outputDir = cmd.getOptionValue("o");
	String dictionaryDir = cmd.getOptionValue("d");
	String mode = cmd.getOptionValue("m");

	DuplicateGenerator generator = new DuplicateGenerator(inputDir, outputDir, dictionaryDir, mode);
	generator.generate();
	Set<String> algoDups = Util.runShinglesAlgorithm(outputDir);
	System.out.println("----");
	generator.calculateStats(algoDups);
	System.out.println("----");
	generator.calculateUnionFindStats(algoDups);
    }
}