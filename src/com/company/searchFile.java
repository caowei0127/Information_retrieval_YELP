package com.company;


import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.QueryBuilder;
import org.apache.lucene.search.BoostQuery;
import org.json.JSONObject;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.file.Paths;
import java.util.Arrays;

public class searchFile {

    private IndexSearcher lSearcher;
    private IndexReader lReader;

    searchFile(String dir) {
        try {
            //create an index reader and index searcher
            lReader = DirectoryReader.open(FSDirectory.open(Paths.get(dir)));
            lSearcher = new IndexSearcher(lReader);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //report the number of documents indexed
    public int getCollectionSize() {
        return this.lReader.numDocs();
    }

    ScoreDoc[] searchRankingQuery(String field, String keywords, int numHits) {
        System.out.println("Scenario 1: search for keywords \"club\" in field \"name\", and request for the top 20 results");

        //the query has to be analyzed the same way as the documents being index
        //using the same Analyzer
        QueryBuilder builder = new QueryBuilder(new StandardAnalyzer());
        Query query = builder.createBooleanQuery(field, keywords);
        ScoreDoc[] hits = null;
        try {
            //Create a TopScoreDocCollector
            TopScoreDocCollector collector = TopScoreDocCollector.create(numHits, numHits);

            //search index
            lSearcher.search(query, collector);

            //collect results
            hits = collector.topDocs().scoreDocs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hits;
    }

    ScoreDoc[] searchBooleanQuery(String searchField1, String searchField2, String searchQuery1, String searchQuery2, int numHits) {
        System.out.println("Scenario 2: boolean query search for keywords \"club\" in the field \"name\" or \"Las Vegas\" in the field \"city\" for the top 20 results");

        ScoreDoc[] hits = null;

        Term term1 = new Term(searchField1, searchQuery1);
        Query query1 = new TermQuery(term1);

        Term term2 = new Term(searchField2, searchQuery2);
        Query query2 = new PrefixQuery(term2);

        BooleanQuery.Builder query = new BooleanQuery.Builder();
        query.add(query1, BooleanClause.Occur.SHOULD);
        query.add(query2, BooleanClause.Occur.SHOULD);

        //do the search
        try {
            TopDocs topDocsHits = lSearcher.search(query.build(), numHits);
            hits = topDocsHits.scoreDocs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hits;
    }

    ScoreDoc[] searchBooleanQuery2(String searchField1, String searchField2, String searchField3, String searchQuery1, String searchQuery2, String searchQuery3, int numHits) {
        System.out.println("Scenario 3: boolean query search for keywords \\\"5.0\\\" in the field \\\"stars\\\" and \\\"spas\\\" in the field \\\"categories\\\" and not \\\"glendale\\\" in the field \\\"city\\\" for the top 10 results");

        ScoreDoc[] hits = null;

        Term term1 = new Term(searchField1, searchQuery1);
        Query query1 = new TermQuery(term1);

        Term term2 = new Term(searchField2, searchQuery2);
        Query query2 = new TermQuery(term2);

        Term term3 = new Term(searchField3, searchQuery3);
        Query query3 = new TermQuery(term3);

        BooleanQuery.Builder query = new BooleanQuery.Builder();
        query.add(query1, BooleanClause.Occur.MUST);
        query.add(query2, BooleanClause.Occur.MUST);
        query.add(query3, BooleanClause.Occur.MUST_NOT);

        //do the search
        try {
            TopDocs topDocsHits = lSearcher.search(query.build(), numHits);
            hits = topDocsHits.scoreDocs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hits;
    }


    ScoreDoc[] searchPhraseQuery(String searchField, String searchQuery1, String searchQuery2, int numHits) {
        System.out.println("Scenario 4: phraseQuery search for keywords \"beer bar\"");

        ScoreDoc[] hits = null;

        PhraseQuery.Builder query = new PhraseQuery.Builder();
        query.add(new Term(searchField, searchQuery1), 0);
        query.add(new Term(searchField, searchQuery2), 1);

        //do the search
        try {
            TopDocs topDocsHits = lSearcher.search(query.build(), numHits);
            hits = topDocsHits.scoreDocs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hits;
    }

    ScoreDoc[] searchRangeQuery(String searchField, double lowerBound, double upperBound, int numHits) {
        System.out.println("Scenario 6: point rangeQuery search for stars");
        ScoreDoc[] hits = null;
        Query query = DoublePoint.newRangeQuery(searchField, lowerBound, upperBound);

        //do the search
        try {
            TopDocs topDocsHits = lSearcher.search(query, numHits);
            hits = topDocsHits.scoreDocs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hits;
    }

    ScoreDoc[] searchWildcard(String searchField, String searchQuery, int numHits) {
        System.out.println("Scenario 7: wildcard search for any biz name containing undefined word  \"?bar\"");
        ScoreDoc[] hits = null;
        Term term = new Term(searchField, searchQuery);
        WildcardQuery query = new WildcardQuery(term);

        //do the search
        try {
            TopDocs topDocsHits = lSearcher.search(query, numHits);
            hits = topDocsHits.scoreDocs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hits;
    }

    ScoreDoc[] searchLocation(String searchField, String searchQuery, int numHits) throws Exception {
        System.out.println("Scenario 8: POI search for location \"1025 Morehead Medical Dr, Ste 225, Charlotte\"");
        ScoreDoc[] hits = null;
        // Default location Singapore
        double lat = 1.3521;
        double lng = 103.8198;
        String json = getLatAndLng(searchQuery);
        JSONObject obj = new JSONObject(json);
        try {
            if (obj.get("status").toString().equals("OK")) {
                JSONObject result = (JSONObject) obj.getJSONArray("results").get(0);
                lng = result.getJSONObject("geometry").getJSONObject("location").getDouble("lng");
                lat = result.getJSONObject("geometry").getJSONObject("location").getDouble("lat");
                System.out.println("latitude: " + lat + "\tLongitude: " + lng);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        Query query = LatLonPoint.newDistanceQuery(searchField, lat, lng, 100);

        //do the search
        try {
            TopDocs topDocsHits = lSearcher.search(query, numHits);
            hits = topDocsHits.scoreDocs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hits;
    }

    ScoreDoc[] searchFuzzy(String searchField, String searchQuery, int numHits) {
        System.out.println("Scenario 10: fuzzy search for misspelled word \"barbacue\"");
        ScoreDoc[] hits = null;
        Term term = new Term(searchField, searchQuery);
        FuzzyQuery query = new FuzzyQuery(term, 1); // maxEdits must be between 0 and 2

        //do the search
        try {
            TopDocs topDocsHits = lSearcher.search(query, numHits);
            hits = topDocsHits.scoreDocs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hits;
    }

    ScoreDoc[] searchMultifield(String searchField1, String searchField2, String searchQuery1, String searchQuery2, int numHits) throws ParseException {
        System.out.println("Scenario 11: search for name filed and address which contains \"club\" OR \"Valley\"");
        ScoreDoc[] hits = null;
        String[] fields = new String[]{searchField1, searchField2};
        MultiFieldQueryParser MultiFieldQueryParser = new MultiFieldQueryParser(fields, new StandardAnalyzer());
        Query query = MultiFieldQueryParser.parse(searchQuery1.trim() + " OR " + searchQuery2.trim());

        //do the search
        try {
            TopDocs topDocsHits = lSearcher.search(query, numHits);
            hits = topDocsHits.scoreDocs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hits;
    }

    ScoreDoc[] searchQueryParser(String searchField, String searchQuery, int numHits) throws ParseException {
        System.out.println("Scenario 12: Use queryParse to implement wildcard query. City: Los \\Angel*");
        ScoreDoc[] hits = null;
        QueryParser queryParser = new QueryParser(searchField, new StandardAnalyzer());
        Query query = queryParser.parse(searchQuery);

        //do the search
        try {
            TopDocs topDocsHits = lSearcher.search(query, numHits);
            hits = topDocsHits.scoreDocs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hits;
    }

    ScoreDoc[] searchBoostBooleanQuery(String searchField1, String searchField2, String searchQuery1, String searchQuery2, int numHits) {
        System.out.println("Scenario 20: Use boostQuery with boost and booleanQuery to add priority for the first query");
        ScoreDoc[] hits = null;
        Query q1 = new TermQuery(new Term(searchField1, searchQuery1));
        Query q2 = new TermQuery(new Term(searchField2, searchQuery2));

        BoostQuery boostQuery = new BoostQuery(q1, 100f);
        BooleanQuery.Builder query = new BooleanQuery.Builder();
        query.add(boostQuery, BooleanClause.Occur.SHOULD);
        query.add(q2, BooleanClause.Occur.SHOULD);

        //do the search
        try {
            TopDocs topDocsHits = lSearcher.search(query.build(), numHits);
            hits = topDocsHits.scoreDocs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hits;
    }

    ScoreDoc[] searchQueryParserRBooleanQuery(String searchField, String searchQuery, int numHits) throws ParseException {
        System.out.println("Scenario 19: Use queryParse to replace booleanQuery");
        ScoreDoc[] hits = null;
        QueryParser queryParser = new QueryParser(searchField, new StandardAnalyzer());
        Query query = queryParser.parse(searchQuery);

        //do the search
        try {
            TopDocs topDocsHits = lSearcher.search(query, numHits);
            hits = topDocsHits.scoreDocs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hits;
    }

    ScoreDoc[] searchByLatAndLng(double lat, double lng, int numHits) {
        System.out.println("Scenario 9: Search by latitude and longitude");
        ScoreDoc[] hits = null;
        Query query = LatLonPoint.newDistanceQuery("location", lat, lng, 1000);

        //do the search
        try {
            TopDocs topDocsHits = lSearcher.search(query, numHits);
            hits = topDocsHits.scoreDocs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hits;
    }

    ScoreDoc[] searchCharWithParser(String searchField, String searchQuery, int numHits) throws ParseException {
        System.out.println("Scenario 13: use boosted character search with query parser: Yoga^4 club^3 fitness (^ means a boost)");
        ScoreDoc[] hits = null;
        QueryParser queryParser = new QueryParser(searchField, new StandardAnalyzer());
        Query query = queryParser.parse(searchQuery);

        //do the search
        try {
            TopDocs topDocsHits = lSearcher.search(query, numHits);
            hits = topDocsHits.scoreDocs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hits;
    }

    ScoreDoc[] searchFuzzyWithParser(String searchField, String searchQuery, int numHits) throws ParseException {
        System.out.println("Scenario 14: fuzzy search with query parser: saerch for wildcard cinem~");
        ScoreDoc[] hits = null;
        QueryParser queryParser = new QueryParser(searchField, new StandardAnalyzer());
        Query query = queryParser.parse(searchQuery);

        //do the search
        try {
            TopDocs topDocsHits = lSearcher.search(query, numHits);
            hits = topDocsHits.scoreDocs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hits;
    }

    ScoreDoc[] searchRangeWithParser(String searchField, String searchQuery, int numHits) throws ParseException {
        System.out.println("Scenario 15: character range search with query parser: [club TO spa} (including \"club\" but excluding \"spa\")");
        ScoreDoc[] hits = null;
        QueryParser queryParser = new QueryParser(searchField, new StandardAnalyzer());
        Query query = queryParser.parse(searchQuery);

        //do the search
        try {
            TopDocs topDocsHits = lSearcher.search(query, numHits);
            hits = topDocsHits.scoreDocs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hits;
    }

    ScoreDoc[] searchGroupingWithParser(String searchField, String searchQuery, int numHits) throws ParseException {
        System.out.println("Scenario 16: character range search with query parser: (club OR bar) AND wine");
        ScoreDoc[] hits = null;
        QueryParser queryParser = new QueryParser(searchField, new StandardAnalyzer());
        Query query = queryParser.parse(searchQuery);

        //do the search
        try {
            TopDocs topDocsHits = lSearcher.search(query, numHits);
            hits = topDocsHits.scoreDocs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hits;
    }

    ScoreDoc[] searchPhraseWithSlop(String searchField, String searchQuery1, String searchQuery2, int slop, int numHits) {
        System.out.println("Scenario 5: phrase search for \"name\" with slop 2 words");
        ScoreDoc[] hits = null;
        PhraseQuery.Builder builder = new PhraseQuery.Builder();
        builder.add(new Term(searchField, searchQuery1), 0);
        builder.add(new Term(searchField, searchQuery2), 1);
        builder.setSlop(slop);
        PhraseQuery query = builder.build();

        //do the search
        try {
            TopDocs topDocsHits = lSearcher.search(query, numHits);
            hits = topDocsHits.scoreDocs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hits;
    }

    ScoreDoc[] searchProximityWithParser(String searchField, String searchQuery, int numHits) throws ParseException {
        System.out.println("Scenario 17: proximity search for \"name\" ");
        ScoreDoc[] hits = null;
        QueryParser queryParser = new QueryParser(searchField, new StandardAnalyzer());
        Query query = queryParser.parse(searchQuery);

        //do the search
        try {
            TopDocs topDocsHits = lSearcher.search(query, numHits);
            hits = topDocsHits.scoreDocs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hits;
    }

    ScoreDoc[] searchBooleanWithParser(String searchField, String searchQuery, int numHits) throws ParseException {
        System.out.println("Scenario 18: boolean parser search for \"name\"");
        ScoreDoc[] hits = null;
        QueryParser queryParser = new QueryParser(searchField, new StandardAnalyzer());
        Query query = queryParser.parse(searchQuery);

        //do the search
        try {
            TopDocs topDocsHits = lSearcher.search(query, numHits);
            hits = topDocsHits.scoreDocs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hits;
    }


    ScoreDoc[] searchLocationAndField(String searchField, String searchQuery, String address, int numHits) throws Exception {
        System.out.println("Scenario 6: POI search for location \"1025 Morehead Medical Dr, Ste 225, Charlotte\"");
        ScoreDoc[] hits = null;
        // Default location Singapore
        double lat = 1.3521;
        double lng = 103.8198;
        String json = getLatAndLng(address);
        JSONObject obj = new JSONObject(json);
        try {
            if (obj.get("status").toString().equals("OK")) {
                JSONObject result = (JSONObject) obj.getJSONArray("results").get(0);
                lng = result.getJSONObject("geometry").getJSONObject("location").getDouble("lng");
                lat = result.getJSONObject("geometry").getJSONObject("location").getDouble("lat");
                System.out.println("latitude: " + lat + "\tLongitude: " + lng);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        Query locationQuery = LatLonPoint.newDistanceQuery("location", lat, lng, 100);
        BoostQuery boostQuery1 = new BoostQuery(locationQuery, 100f);

        QueryBuilder builder = new QueryBuilder(new StandardAnalyzer());
        Query fieldQuery = builder.createBooleanQuery(searchField, searchQuery);
        BoostQuery boostQuery2 = new BoostQuery(fieldQuery, 100f);

        BooleanQuery.Builder query = new BooleanQuery.Builder();
        query.add(boostQuery1, BooleanClause.Occur.SHOULD);
        query.add(boostQuery2, BooleanClause.Occur.SHOULD);

        //do the search
        try {
            TopDocs topDocsHits = lSearcher.search(query.build(), numHits);
            hits = topDocsHits.scoreDocs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hits;
    }


    // Get latitude and Longitude using address from Google Map service
    private static String getLatAndLng(String address) {
        StringBuilder json = new StringBuilder();
        try {
            URL oracle = new URL(LuceneConstants.URL + URLEncoder.encode(address, "UTF-8") + "&key=" + LuceneConstants.key);
            URLConnection yc = oracle.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    yc.getInputStream()));
            String inputLine = null;
            while ((inputLine = in.readLine()) != null) {
                json.append(inputLine);
            }
            in.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return json.toString();

    }


    // present the search results
    void printResult(ScoreDoc[] hits) {
        System.out.println("Total hits " + hits.length);
        int i = 1;
        for (ScoreDoc hit : hits) {
            System.out.println("\nResult " + i + "\tDocID: " + hit.doc + "\t Score: " + hit.score);
            try {
                System.out.println("Business_id: " + lReader.document(hit.doc).get("business_id"));
                System.out.println(
                        "Name: " + lReader.document(hit.doc).get("name") +
                                "\t\tNumber of Reviews: " + lReader.document(hit.doc).get("review_count") +
                                "\t\tStars: " + lReader.document(hit.doc).get("stars"));
                System.out.println(
                        "City: " + lReader.document(hit.doc).get("city") +
                                "\t\tState: " + lReader.document(hit.doc).get("state") +
                                "\t\tPostal Code: " + lReader.document(hit.doc).get("postal_code"));
                System.out.println("Address: " + lReader.document(hit.doc).get("address"));
                System.out.println("Location: lat " + lReader.document(hit.doc).get("lat") + ",lng " + lReader.document(hit.doc).get("lat"));
                // Print out the categories
                String cat = Arrays.toString(lReader.document(hit.doc).getValues("categories"));
                System.out.println("Categories: " + cat);

            } catch (Exception e) {
                e.printStackTrace();
            }
            i++;
        }
        System.out.println(
                "\n------------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println(
                "------------------------------------------------------------------------------------------------------------------------------------------\n");
    }


    private void close() {
        try {
            if (lReader != null) {
                lReader.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
