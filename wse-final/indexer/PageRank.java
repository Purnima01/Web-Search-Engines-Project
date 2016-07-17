
// A Program to calculate Weighted Page Ranks for a set of Web Pages.

package indexer;

import java.text.*;
import java.util.*;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.net.*;
import java.io.*;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.tidy.Tidy;
import util.Util;

public class PageRank {
    /* Captures information about an anchor on the page, such 
     * as its URL, and whether it is highlighted
     */
    private class AnchorInfo {
	String url;
	boolean isHighlighted;

	public AnchorInfo(String url, boolean isHighlighted) {
	    this.url = url;
	    this.isHighlighted = isHighlighted;
	}
    }

    // The directory to read the documents from.
    String docsDir_;

    // The "F" parameter for the PageRank computation.
    Double f_;

    // The initial qualities of the pages based on lengths.
    HashMap<String, Double> pageQualities_;

    // The PageRank scores of the pages.
    HashMap<String, Double> pageRanks_;

    // The weights of the links, as calculated based on link counts and emphasis.
    HashMap<String, HashMap<String, Double>> incomingLinkWeights_;

    // The list of files.
    Collection<File> files_;

    public PageRank(String docsDir, Double f) {
	this.docsDir_ = docsDir;
	this.f_ = f;
	this.pageQualities_ = new HashMap<String, Double>();
	this.pageRanks_ = new HashMap<String, Double>();
	// this.outgoingLinks_ = new HashMap<String, Set<String>>();
	this.incomingLinkWeights_ = new HashMap<String, HashMap<String, Double>>();
	this.files_ = new Vector<File>();
    }

    // Reads the documents, finds anchors, and calculates the weights for outgoing links
    // from each document.
    public void readPages() throws Exception {
	for (File file : this.files_) {
	    // Normalize the documents by the base name. For instance, www.x.com/y/z.html
	    // will be normalized to z.html
	    String baseName = Util.getBasename(file.toString());
	    System.out.println("Links from page: " + baseName);
	    // Find the anchors on this page
	    Vector<AnchorInfo> anchors = new Vector<AnchorInfo>();
	    searchAnchors(file, anchors);
	    HashMap<String, Double> myLinkWeights = new HashMap<String, Double>();
	    double totalScores = 0.0;
	    for (AnchorInfo ai : anchors) {
		System.out.println(ai.url + ", isHighlighted: " + ai.isHighlighted);
		String outBaseName = Util.getBasename(ai.url);
		if (this.pageQualities_.containsKey(outBaseName)) {
		    // If there was another link found to this page, add to the previous weight.
		    // Otherwise, set the weight to the currently computed weight
		    double existingScore = myLinkWeights.containsKey(outBaseName) ? myLinkWeights.get(outBaseName) : 0.0;
		    double currentScore = getScore(ai);
		    totalScores += currentScore;
		    myLinkWeights.put(outBaseName, existingScore + currentScore);
		}
	    }
	    if (totalScores > 0) {
		// Scale the weights
		for (Map.Entry<String, Double> entry : myLinkWeights.entrySet()) {
		    double scaled = entry.getValue() / totalScores;
		    addIncomingLink(entry.getKey(), baseName, scaled);
		    System.out.println(entry.getKey() + ":" + scaled);
		}
	    } else {
		// If there are no outgoing links from this page, add "weights"
		// of 1/N to all other pages in the collection.
		double oneOverN = 1.0 / (this.pageQualities_.size() + 0.0);
		for (String page : this.pageQualities_.keySet()) {
		    addIncomingLink(page, baseName, oneOverN);
		}
	    }
	}
    }
    
    // Adds information about an incoming link to the incomingLinkWeights_ hash map.
    private void addIncomingLink(String to, String from, Double weight) {
	if (!this.incomingLinkWeights_.containsKey(to)) {
	    this.incomingLinkWeights_.put(to, new HashMap<String, Double>());
	}
	this.incomingLinkWeights_.get(to).put(from, weight);
    }
    
    // Calculates the PageRank scores for all documents.
    public HashMap<String, Double> calculatePageRanks() {
	// initialize
	this.pageRanks_ = this.pageQualities_;
	boolean changed = true;
	double epsilon = 0.01 / (this.pageQualities_.size() + 0.0);
	do {
	    HashMap<String, Double> updatedPageRanks = new HashMap<String, Double>();
	    changed = false;
	    for (String page : this.pageQualities_.keySet()) {
		double base = this.pageQualities_.get(page);
		double sumIncoming = 0.0;
		if (this.incomingLinkWeights_.containsKey(page)) {
		    for (Map.Entry<String, Double> entry : 
			 this.incomingLinkWeights_.get(page).entrySet()) {
			sumIncoming += (entry.getValue() * this.pageRanks_.get(entry.getKey()));
		    }
		}
		double newScore = (1.0 - this.f_) * base + (this.f_ * sumIncoming);
		if (Math.abs(newScore - this.pageRanks_.get(page)) > epsilon) {
		    changed = true;
		}
		updatedPageRanks.put(page, newScore);
	    }
	    if (changed) {
		this.pageRanks_ = updatedPageRanks;
	    }
	} while (changed);
	System.out.println("\n\nPageRank Scores:\n\n");
	// Sort the documents in descending order of their PageRank scores for the final output of the algorithm.
	PriorityQueue<Map.Entry<String, Double>> pq = new PriorityQueue<Map.Entry<String, Double>>(this.pageQualities_.size(),
												   new Util.PageRankComparator());
	pq.addAll(this.pageRanks_.entrySet());
	while (pq.size() > 0) {
	    Map.Entry<String, Double> entry = pq.poll();
	    System.out.printf("%s: %.4f\n", entry.getKey(), entry.getValue());
	}
	return this.pageRanks_;
    }

    // Gets the "score" or weight for an anchor. A link is worth '1', or '2' when it's highlighted/emphasized.
    private double getScore(AnchorInfo ai) {
	return ai.isHighlighted ? 2.0 : 1.0;
    }

    /* Parse the HTML using JTidy and iterate over the DOM tree to find anchors.
     */
    public void searchAnchors(File file, Vector<AnchorInfo> anchors) throws FileNotFoundException {
        Tidy tidy = new Tidy();
        tidy.setQuiet(true);
        tidy.setShowWarnings(false);
        org.w3c.dom.Document root = tidy.parseDOM(new FileInputStream(file), null);
        Element rawDoc = root.getDocumentElement();
	NodeList body = rawDoc.getElementsByTagName("body");
	if (body.getLength() > 0) {
	    getAnchors(body.item(0), anchors, 
		       false /* isHighlighted */);
	}
    }

    /* Recurses over the nodes in the DOM tree. 
     * Finds anchors and record information about their
     * URLs and highlighting into 'anchors'.
     */
    protected void getAnchors(Node node, Vector<AnchorInfo> anchors, boolean isHighlighted) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            switch (child.getNodeType()) {
	    case Node.ELEMENT_NODE:
		if (isAnchor( (Element) child)) {
		    // This node is an anchor. Get its link, and record whether it was highlighted.
		    Element childElement = (Element) child;
		    String href = childElement.getAttribute("href");
		    AnchorInfo anchorInfo = new AnchorInfo(href, isHighlighted);
		    anchors.add(anchorInfo);
		} else if (isHeaderOrBold( (Element) child)) {
		    getAnchors(child, anchors, 
			       true  /* isHighlighted */);
		} else {
		    // This Node is not an anchor. Recurse into its children.
		    getAnchors(child, anchors, isHighlighted);
		}
		break;
	    }
	}
    }

    /* Determines whether the given Element is an anchor (an HTML tag of form <a> </a>).
     */
    protected boolean isAnchor(Element element) {
	return element.getTagName().equalsIgnoreCase("a");
    }

    /* Determines whether the given Element is a header (<h1>, <h2>, <h3>, or <h4> tags), 
     * bold (<b>), or an emphasis (<em>).
     */
    protected boolean isHeaderOrBold(Element element) {
	return element.getTagName().equalsIgnoreCase("b") || 
	    element.getTagName().equalsIgnoreCase("em") || 
	    element.getTagName().equalsIgnoreCase("h1") || 
	    element.getTagName().equalsIgnoreCase("h2") ||
	    element.getTagName().equalsIgnoreCase("h3") ||
	    element.getTagName().equalsIgnoreCase("h4");
    }

    // Calculates the initial quality (P.base) of each page based on the word count 
    // of the page. P.base is initially calculated as the log base 2 of the page's word count.
    // P.base values are then scaled.
    public void calculateQualities(FileFilter filter) throws FileNotFoundException, IOException {
        Collection<File> files = Util.walk(docsDir_); 
        double pageQualitiesSum = 0;
	HashMap<String, Double> unscaledPageQualities = new HashMap<String, Double>();
        for (File f : files) {
            if (!f.isDirectory() &&
                !f.isHidden() &&
                f.exists() &&
                f.canRead() &&
                (filter == null || filter.accept(f))) {
		this.files_.add(f);
		int numWords = numWords(f);
		// P.base = log_2 WordCount(P)
		double pageQuality = Math.log(numWords) / Math.log(2);
		pageQualitiesSum += pageQuality;
		unscaledPageQualities.put(Util.getBasename(f.toString()), 
					  pageQuality);
		System.out.println(f.toString() + ":" + numWords + ":" + pageQuality);
            }
        }
	System.out.println("-");
	// Scale the page qualities.
	for (Map.Entry<String, Double> entry : unscaledPageQualities.entrySet()) {
	    double scaled = entry.getValue() / pageQualitiesSum;
	    this.pageQualities_.put(entry.getKey(), scaled);
	    System.out.println(entry.getKey() + ":" + scaled);
	}	
    }

    // Counts the number of words in a file.
    private int numWords(File f) throws FileNotFoundException, IOException {
	BufferedReader reader = new BufferedReader(new FileReader(f));
	int count = 0;
	String line = reader.readLine();
	while (line != null) {
	    String[] tokens = line.split("\\s+");
	    for (String t : tokens) {
		if (t.length() > 0) {
		    count++;
		}
	    }
	    line = reader.readLine();
	}
	return count;
    }
}