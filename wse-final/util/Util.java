package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Util {

    // Runs the python 'findShingles' module by creating a subprocess and invoking the python code.                                         
    // Reads the duplicate documents detected by the algorithm.                                                                             
    // Subprocess logic is borrowed & adapted from:                                                                                         
    // stackoverflow.com/questions/6575299/fetch-and-store-output-from-a-subprocess-in-java
    public static Set<String> runShinglesAlgorithm(String outputDir) throws IOException {
        Set<String> algoDuplicates = new HashSet<String>();
        String command = "python python/findShingles.py " + outputDir;
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(command);
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String temp = br.readLine();
        System.out.println("Implementation results:");
        while( temp != null ) {
            algoDuplicates.add(temp);
            System.out.println(temp);
            temp = br.readLine();
        }
        br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        temp = br.readLine();
        if (temp != null) {
            System.out.println("Subprocess errors:");
        }
        while( temp != null ) {
            System.out.println(temp);
            temp = br.readLine();
        }
        return algoDuplicates;
    }

    // Gets the basename of a URL. For instance, if the URL is www.x.com/y/z.html ,                                                         
    // the basename will be z.html .                                                                                                        
    public static String getBasename(String urlStr) {
        // Note: I used http://stackoverflow.com/questions/605696/get-file-name-from-url                                                    
        // for the URL filename substring logic below.                                                                                      
        return urlStr.substring(urlStr.lastIndexOf('/') + 1);
    }

    // Reads the contents of a file into a String.                                                                                          
    // Logic is borrowed & adapted from:                                                                                                    
    // stackoverflow.com/questions/326390/how-to-create-a-java-string-from-the-contents-of-a-file                                           
    public static String readFile(Path path) throws IOException
    {
        byte[] encoded = Files.readAllBytes(path);
        return new String(encoded);
    }

    // Recursively lists the files in the given path                                                                                        
    // The implementation for "walk" adapted from:                                                                                          
    // http://stackoverflow.com/questions/2056221/recursively-list-files-in-java                                                            
    public static Collection<File> walk(String path) {

        File root = new File(path);
        ArrayList<File> files = new ArrayList<File>();
        ArrayList<File> outFiles = new ArrayList<File>();
        for (File f : root.listFiles()) {
            files.add(f);
        }

        if (files.size() == 0) return null;

        for ( File f : files ) {
            if ( f.isDirectory() ) {
                outFiles.addAll(walk( f.getPath() ));
            }
            else {
                outFiles.add(f);
            }
        }
        return outFiles;
    }

    // FileFilter that accepts .htm or .html files                                                                                          
    public static class HTMLFilesFilter implements FileFilter {
        public boolean accept(File path) {
            return path.getName().toLowerCase().endsWith(".html") ||
                path.getName().toLowerCase().endsWith(".htm") ;
        }
    }

    // Small helper class to compare the PageRank scores of documents in descending order.
    public static class PageRankComparator implements Comparator<Map.Entry<String, Double>> {
        public int compare(Map.Entry<String, Double> a, Map.Entry<String, Double> b) {
            return -1 * Double.compare(a.getValue(), b.getValue());
        }
    }

}