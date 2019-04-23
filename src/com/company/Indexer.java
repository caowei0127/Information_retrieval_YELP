package com.company;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.index.TieredMergePolicy;
import org.apache.lucene.index.LogDocMergePolicy;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

class Indexer {

    private IndexWriter writer;


    //for recording time used for indexing
    private static final DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    Indexer(String dir) throws IOException {
        //specify the directory to store the Lucene index
        Directory indexDir = FSDirectory.open(Paths.get(dir));

        // Model 1: simple analyzer
        Analyzer analyzer = new SimpleAnalyzer();
        IndexWriterConfig cfg = new IndexWriterConfig(analyzer);
        cfg.setOpenMode(OpenMode.CREATE);

        // Model 2: standard analyzer with configurations
//        Analyzer analyzer = new StandardAnalyzer();
//        IndexWriterConfig cfg = new IndexWriterConfig(analyzer);
//        cfg.setOpenMode(OpenMode.CREATE);
//        cfg.setSimilarity(new BM25Similarity((float) 100.0, (float) 0.5));
//        cfg.setRAMPerThreadHardLimitMB(1024);
//        cfg.setRAMBufferSizeMB(1024);
//        cfg.setMaxBufferedDocs(100);
//        cfg.setMergePolicy(new TieredMergePolicy());
//        LogDocMergePolicy logDocMergePolicy = new LogDocMergePolicy();
//        logDocMergePolicy.setMinMergeDocs(100);
//        logDocMergePolicy.setMergeFactor(100); //the merge factor default value is 10
//        cfg.setMergePolicy(logDocMergePolicy);

        //create the IndexWriter
        writer = new IndexWriter(indexDir, cfg);
    }

    //specify what is a document, and how its fields are indexed
    private Document getDocument(String business_id, String name, String address, String city, String state, String postal_code,
                                 Double longitude, Double latitude, Double stars, int review_count, String[] categories) throws Exception {
        Document doc = new Document();
        doc.add(new TextField("business_id", business_id, Field.Store.YES));
        doc.add(new TextField("name", name, Field.Store.YES));
        doc.add(new TextField("address", address, Field.Store.YES));
        doc.add(new TextField("city", city, Field.Store.YES));
        doc.add(new TextField("state", state, Field.Store.YES));
        doc.add(new TextField("postal_code", postal_code, Field.Store.YES));
        doc.add(new LatLonPoint("location", latitude, longitude));
        doc.add(new TextField("lat", Double.toString(latitude), Field.Store.YES));
        doc.add(new TextField("lng", Double.toString(longitude), Field.Store.YES));
        doc.add(new TextField("stars", Double.toString(stars), Field.Store.YES));
        doc.add(new DoublePoint("stars", stars));
        doc.add(new IntPoint("review_count", review_count));
        doc.add(new TextField("review_count", Integer.toString(review_count), Field.Store.YES));
        for (String category : categories) {
            doc.add(new TextField("categories", category.trim(), Field.Store.YES));
        }
        return doc;
    }


    void indexQAs(String fileName) throws Exception {

        System.out.println("Start indexing " + fileName + " " + sdf.format(new Date()));
        // Get the time and memory before indexing
        long startTime = System.nanoTime();
        long startUsedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        //read a JSON file
        Scanner in = new Scanner(new File(fileName));
        int lineNumber = 1;
        String jLine = "";
        while (in.hasNextLine()) {
            try {
                jLine = in.nextLine().trim();
                //parse the JSON file and extract the values for "question" and "answer"
                JSONObject jObj = new JSONObject(jLine);
                String business_id = jObj.getString("business_id");
                String name = jObj.getString("name");
                String address = jObj.getString("address");
                String city = jObj.getString("city");
                String state = jObj.getString("state");
                String postal_code = jObj.getString("postal_code");
                Double longitude = jObj.getDouble("longitude");
                Double latitude = jObj.getDouble("latitude");
                Double stars = jObj.getDouble("stars");
                int review_count = jObj.getInt("review_count");
                String categories = jObj.getString("categories");
                String[] categoriesList = categories.split(",");

                //create a document for each JSON record
                Document doc = getDocument(business_id, name, address, city, state, postal_code, longitude, latitude, stars, review_count, categoriesList);

                //index the document
                writer.addDocument(doc);

                lineNumber++;
            } catch (Exception e) {
                System.out.println("Error at: " + lineNumber + "\t" + jLine);
                e.printStackTrace();
            }
        }
        //close the file reader
        in.close();
        System.out.println("Index completed at " + sdf.format(new Date()));
        System.out.println("Total number of documents indexed: " + writer.getDocStats().maxDoc);

        // Calculate total time used and total memory used through the indexing
        long endTime = System.nanoTime();
        long endUsedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long indexTotalTime = (endTime - startTime) / 1000000;
        long indexUsedMem = (endUsedMem - startUsedMem) / 1000000;
        System.out.println("Total time used for indexing: " + indexTotalTime + " milliseconds");
        System.out.println("Total memory used for indexing: " + indexUsedMem + " MB");

        //close the index writer.
        writer.close();

    }

}
