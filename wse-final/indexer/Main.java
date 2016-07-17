package indexer;

import htmlparser.JTidyHTMLHandler;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import util.UF;
import util.Util;

public class Main {

    // The directory containing the input HTML files.
    String inputDir_;

    // The directory where the index should be written to.
    String indexDir_;

    // Gives the unique integer identifier for each doc name.
    HashMap<String, Integer> docNameToId_;

    // Gives the doc name given the unique integer identifier for the document.
    HashMap<Integer, String> idToDocName_;

    // Number of documents in the collection.
    int numDocs_;

    // Lucene IndexWriter
    private IndexWriter writer_;

    // Utility to read text from HTML documents.
    private JTidyHTMLHandler htmlHandler_;

    private UF unionFindAlgo_;

    private HashMap<String, Double> pageRanks_;

    private Map<Integer, List<String>> docsToComponentIds_;

    private Map<Integer, DocInfo> componentToAuthoritativeDocMap_;

    public Main(String inputDir, String indexDir) throws IOException {
	this.inputDir_ = inputDir;
	this.indexDir_ = indexDir;
	Directory dir = FSDirectory.open(FileSystems.getDefault().getPath(indexDir));
	this.writer_ = new IndexWriter(dir, 
				      new IndexWriterConfig(new StandardAnalyzer()));
	this.htmlHandler_ = new JTidyHTMLHandler();
	numDocs_ = 0;
	docNameToId_ = new HashMap<String, Integer>();
        idToDocName_ = new HashMap<Integer, String>();
    }

    
    public void initialize() throws Exception {
	Collection<File> files = Util.walk(inputDir_);
	for (File f : files) {
	    String basename = Util.getBasename(f.toString());
	    docNameToId_.put(basename, numDocs_);
	    idToDocName_.put(numDocs_++, basename);
	}
	Set<String> algoPairs = Util.runShinglesAlgorithm(inputDir_);
	this.unionFindAlgo_ = assignClusters(algoPairs);
	this.docsToComponentIds_ = mapDocsToComponentIds(unionFindAlgo_);
	PageRank pageRank = new PageRank(inputDir_, 0.7);
	pageRank.calculateQualities(new Util.HTMLFilesFilter());
        pageRank.readPages();
        this.pageRanks_ = pageRank.calculatePageRanks();
	this.componentToAuthoritativeDocMap_ = findMaxPageRankForComponents(pageRanks_, docsToComponentIds_);
    }


    private UF assignClusters(Set<String> algoPairs) {
        UF unionFindAlgo = new UF(numDocs_);
        for (String pair : algoPairs) {
            String[] docs = pair.split(",");
            int docId1 = docNameToId_.get(docs[0]);
            int docId2 = docNameToId_.get(docs[1]);
            unionFindAlgo.union(docId1, docId2);
        }
	return unionFindAlgo;
    }

    /**
     * Map each doc to its component/cluster id, obtained using UF.find(int docId)
     * Note: the call, unionFindAlgo.find(docId), also handles individual pages that are not duplicates
     * or do not have any duplicates, by putting those pages in a cluster of just one document - that
     * page itself. So, suppose docId 1 is a doc with name ("abc") and without any duplicates,
     * componentIdToDocs will store a Map.Entry of (1, [abc]) for this doc.
     *
     * So when componentIdToDocs is passed to findMaxPageRankForComponents(..),
     * the PR of cluster#1 will be that of doc "abc". So regardless of whether duplicates exist or not,
     * every page is in a cluster/component. The size of the component can range from 1 to N (where N =
     * max number of docs in folder), in the case that N-1 docs are duplicates of the remaining doc.
     */

    private Map<Integer, List<String>> mapDocsToComponentIds(UF unionFindAlgo) {
        //initialize in constructor
        Map<Integer, List<String>> componentIdToDocs = new HashMap<Integer, List<String>>();

        Set<Integer> docIds = idToDocName_.keySet();

        for (Integer docId : docIds) {
            String docName = idToDocName_.get(docId);
            Integer componentId = unionFindAlgo.find(docId);

            List<String> docsInComponent;
            if (!componentIdToDocs.containsKey(componentId)) {
                docsInComponent = new ArrayList<String>();
            } else {
                docsInComponent = componentIdToDocs.get(componentId);
            }
            docsInComponent.add(docName);
            componentIdToDocs.put(componentId, docsInComponent);
        }
        return componentIdToDocs;
    }

    /**
     * Returns a mapping of the component/cluster id and
     * the most authoritative doc for that component
     */
    //pass your this.pageRanks_ as 1st param
    public Map<Integer, DocInfo> findMaxPageRankForComponents(Map<String, Double> pageRanks_, 
							      Map<Integer, List<String>> componentIdToDocs) {

        Map<Integer, DocInfo> componentToAuthoritativeDocMap = new HashMap<Integer, DocInfo>();

        for (Integer componentId : componentIdToDocs.keySet()) {
            List<String> docsInComponent = componentIdToDocs.get(componentId);
            double maxPageRankSoFarInComponent = Double.MIN_VALUE;
            String docWithMaxPageRankInComponent = null;

            for (String doc : docsInComponent) {
		System.out.println(doc);
                double pageRankForDoc = pageRanks_.get(doc);
                if (pageRankForDoc > maxPageRankSoFarInComponent) {
                    maxPageRankSoFarInComponent = pageRankForDoc;
                    docWithMaxPageRankInComponent = doc;
                }
            }
            DocInfo authoritativeDocForComponent =
                new DocInfo(docWithMaxPageRankInComponent, maxPageRankSoFarInComponent);
            componentToAuthoritativeDocMap.put(componentId, authoritativeDocForComponent);
        }

        return componentToAuthoritativeDocMap;
    }

    public int index(FileFilter filter) throws FileNotFoundException, IOException {
        Collection<File> files = Util.walk(inputDir_); 
        
        for (File f : files) {
            if (!f.isDirectory() &&
                !f.isHidden() &&
                f.exists() &&
                f.canRead() &&
                (filter == null || filter.accept(f))) {
                indexFile(f);
            }
        }
        return writer_.numDocs();
    }

    private List<String> getRankedDuplicates(String basename) {
        int docId = docNameToId_.get(basename);
        int clusterId = unionFindAlgo_.find(docId);
	List<String> docsInCluster = docsToComponentIds_.get(clusterId);
	PriorityQueue<Map.Entry<String, Double>> pq = new PriorityQueue<Map.Entry<String, Double>>(docsInCluster.size(),
                                                                                                   new Util.PageRankComparator());
	HashMap<String, Double> subsetPageRanks = new HashMap<String, Double>();
	for (String page : docsInCluster) {
	    if (!page.equals(basename)) {
		subsetPageRanks.put(page, this.pageRanks_.get(page));
	    }
	}
        pq.addAll(subsetPageRanks.entrySet());
	List<String> topDuplicates = new ArrayList<String>();
        while (pq.size() > 0 && topDuplicates.size() < 5) {
            Map.Entry<String, Double> entry = pq.poll();
            topDuplicates.add(entry.getKey());
        }
	return topDuplicates;
    }

    private void indexFile(File f) throws FileNotFoundException, IOException {
	String basename = Util.getBasename(f.toString());
	int docId = docNameToId_.get(basename);
	int clusterId = unionFindAlgo_.find(docId);
	String authoritativeDoc = componentToAuthoritativeDocMap_.get(clusterId).getDocName();
	if (basename.equals(authoritativeDoc)) {
	    List<String> topDuplicates = getRankedDuplicates(basename);
	    System.out.println("Indexing: " + basename);
	    Document doc = htmlHandler_.getDocument(new FileInputStream(f));
	    doc.add(new TextField("path", f.getPath(), Field.Store.YES));
	    doc.add(new TextField("dir", inputDir_, Field.Store.YES));
	    StringBuilder duplicates = new StringBuilder();
	    for (int i = 0; i < topDuplicates.size(); ++i) {
		duplicates.append(topDuplicates.get(i));
		if (i < topDuplicates.size() - 1) {
		    duplicates.append(',');
		}
	    }
	    if (duplicates.length() > 0) {
		System.out.println("Duplicates: " + duplicates.toString());
	    }
	    doc.add(new TextField("duplicates", duplicates.toString(), Field.Store.YES));
	    writer_.addDocument(doc);
	}
    }

    public void close() throws IOException {
        writer_.close();
    }

    public static void main(String args[]) throws IOException,ParseException {
        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("docs")
                          .withDescription("Path to the directory containing the data")
                          .hasArg()
                          .isRequired()
                          .create('d'));
        options.addOption(OptionBuilder.withLongOpt("index")
                          .withDescription("Path to the directory where the index should be written")
                          .hasArg()
                          .isRequired()
                          .create('i'));
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        String dataDir = cmd.getOptionValue("d");
        String indexDir = cmd.getOptionValue("i");
	
	Main indexer = new Main(dataDir, indexDir);
        long start = System.currentTimeMillis();
        int numIndexed = 0;
        try {
	    indexer.initialize();
	    FileFilter filter = new Util.HTMLFilesFilter();
            numIndexed = indexer.index(filter);
        } catch (Exception e) {
            System.out.println("Exception caught: " + e); 
	    e.printStackTrace();
        } finally {
            indexer.close();
        }
    }
}