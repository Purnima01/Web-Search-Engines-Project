package indexer;

/**
 * Created by purnima on 4/27/16.
 */
public class DocInfo {
    private String docName;
    private double pageRank;

    public String getDocName() {
        return docName;
    }

    public void setDocName(String docName) {
        this.docName = docName;
    }

    public double getPageRank() {
        return pageRank;
    }

    public void setPageRank(double pageRank) {
        this.pageRank = pageRank;
    }

    public DocInfo(String name, double pr) {
        docName = name;
        pageRank = pr;
    }

}
