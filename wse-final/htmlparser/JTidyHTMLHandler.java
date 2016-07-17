
package htmlparser;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.tidy.Tidy;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class JTidyHTMLHandler {

    public org.apache.lucene.document.Document
    getDocument(InputStream is) {

        Tidy tidy = new Tidy();
        tidy.setQuiet(true);
        tidy.setShowWarnings(false);
        org.w3c.dom.Document root = tidy.parseDOM(is, null);
        Element rawDoc = root.getDocumentElement();

        org.apache.lucene.document.Document doc =
                new org.apache.lucene.document.Document();

        String title = getTitle(rawDoc);
        String body = getBody(rawDoc);
        if ((title != null) && (!title.equals(""))) {
            doc.add(new TextField("title", title, Field.Store.YES));
        }
        if ((body != null) && (!body.equals(""))) {
            doc.add(new TextField("body", body, Field.Store.YES));
        }

        return doc;
    }

    /**
     * Gets the title text of the HTML document.
     *
     * @rawDoc the DOM Element to extract title Node from
     * @return the title text
     */
    protected String getTitle(Element rawDoc) {
        if (rawDoc == null) {
            return null;
        }

        String title = "";

        NodeList children = rawDoc.getElementsByTagName("title");
        if (children.getLength() > 0) {
            Element titleElement = ((Element) children.item(0));
            Text text = (Text) titleElement.getFirstChild();
            if (text != null) {
                title = text.getData();
            }
	} 
	if (title.length() == 0) {
	    // If title is not found, try to extract the first
	    // H1, H2, or H3 tag.
	    NodeList body = rawDoc.getElementsByTagName("body");
	    if (body.getLength() > 0) {
		title = getHeader((Element) body.item(0));
	    }
	}
        return title;
    }

    
    // Gets the text contained in the first H1, H2 or H3 tag.
    // Searches the given element recursively.
    protected String getHeader(Element element) {
	NodeList headers = element.getElementsByTagName("h1");
	if (headers.getLength() == 0) {
	    headers = element.getElementsByTagName("h2");
	    if (headers.getLength() == 0) {
		headers = element.getElementsByTagName("h3");
	    }
	}
	if (headers.getLength() > 0) {
	    String text = getText(headers.item(0));
	    System.out.println(text);
	    return text;
	}
	NodeList nodeList = element.getChildNodes();
	if (nodeList.getLength() == 0) {
	    return null;
	} else {
	    for(int i = 0; i < nodeList.getLength(); i++) {
		Node childNode = nodeList.item(i);
		if (childNode.getNodeType() == Node.ELEMENT_NODE) {
		    Element child = ((Element) nodeList.item(i));
		    String childHeader = getHeader(child);
		    if (childHeader != null) {
			return childHeader;
		    }
		}
	    }
	}
	return "";
    }

    /**
     * Gets the body text of the HTML document.
     *
     * @rawDoc the DOM Element to extract body Node from
     * @return the body text
     */
    protected String getBody(Element rawDoc) {
        if (rawDoc == null) {
            return null;
        }

        String body = "";
        NodeList children = rawDoc.getElementsByTagName("body");
        if (children.getLength() > 0) {
            body = getText(children.item(0));
        }
        return body;
    }

    /**
     * Extracts text from the DOM node.
     *
     * @param node a DOM node
     * @return the text value of the node
     */
    protected String getText(Node node) {
        NodeList children = node.getChildNodes();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            switch (child.getNodeType()) {
                case Node.ELEMENT_NODE:
                    sb.append(getText(child));
                    sb.append(" ");
                    break;
                case Node.TEXT_NODE:
                    sb.append(((Text) child).getData());
                    break;
            }
        }
        return sb.toString();
    }

    public static void main(String args[]) throws Exception {
        JTidyHTMLHandler handler = new JTidyHTMLHandler();
        org.apache.lucene.document.Document doc = handler.getDocument(
                new FileInputStream(new File(args[0])));
        System.out.println(doc);
    }
}