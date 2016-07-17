
// The code in this class is modeled after the example in pages 23-24 of the 
// "Lucene in Action" book, with some snippets taken directly from
// the provided code.

package retriever;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import util.Util;

public class Retriever {

    private static final String DOCUMENT_REPO = "http://cims.nyu.edu/~es2697/docs/";

    public static void main(String args[]) throws Exception {
	Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("query")
                          .withDescription("The search query")
                          .hasArg()
                          .isRequired()
                          .create('q'));
        options.addOption(OptionBuilder.withLongOpt("index")
                          .withDescription("Path to the directory where the index should be read from")
                          .hasArg()
                          .isRequired()
                          .create('i'));
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        String query = cmd.getOptionValue("q");
        String indexDir = cmd.getOptionValue("i");
	search(indexDir, query);
    }
    
    public static void search(String indexDir, String queryStr) throws IOException, ParseException {
	Directory dir = FSDirectory.open(FileSystems.getDefault().getPath(indexDir));

	IndexSearcher is = new IndexSearcher(DirectoryReader.open(dir));

	QueryParser parser = new QueryParser("body", new StandardAnalyzer());

	Query query = parser.parse(queryStr);

	TopDocs hits = is.search(query, 10);

	Document firstDoc = is.doc(0);
	String dataDir = ""; 
	if (firstDoc != null) {
	    dataDir = firstDoc.get("dir");
	}
	
	System.out.println("<html><head><title>Search Results</title></head><body>"); 
	System.out.println("<h1>Results for query <u>" + queryStr + "</u> " +
			   "in directory <u>" + dataDir + "</u></h1>"); 
	
	int result = 1;
	for (ScoreDoc scoreDoc : hits.scoreDocs) {
	    Document doc = is.doc(scoreDoc.doc);
	    System.out.println("<p><h3>" + result++ + ". " + doc.get("title") + "</h3>");
	    String path = doc.get("path");
	    String basename = Util.getBasename(path);
	    String link = DOCUMENT_REPO + basename;
	    System.out.println("<p><a href=\"" + link + "\">" + basename + "</a></p>");
	    String duplicates = doc.get("duplicates");
	    if (duplicates.length() > 0) {
		System.out.println("<p>Duplicates: </p>");
		String[] dups = duplicates.split(",");
		System.out.println("<ul>");
		for (String dup : dups) {
		    String dupLink = DOCUMENT_REPO + dup;
		    System.out.println("<li>" + "<a href=\"" + dupLink + "\">" + dup + "</a>");
		}
		System.out.println("</ul>");
	    }
	    System.out.println("</p>");
	}

	System.out.println("</body></html>");
    }
}