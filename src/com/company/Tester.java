package com.company;


import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.TFIDFSimilarity;

import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.DFRSimilarity;
import org.apache.lucene.search.similarities.Similarity;

public class Tester {


    public static void main(String[] arg) throws Exception {

        // Get the time and memory before indexing and searching

        long startTime = System.nanoTime();
        long startUsedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // To perform indexing. If there is no change to the data file, index only need to be created once

        Indexer indexer = new Indexer(LuceneConstants.INDEX_PATH);
        indexer.indexQAs(LuceneConstants.DATA_FILE);

        //search index
        searchFile searcher = new searchFile(LuceneConstants.INDEX_PATH);

        // Ranking
        //1. search for keyword "club" in field "name"
        ScoreDoc[] hits = searcher.searchRankingQuery("name", "club", LuceneConstants.MAX_SEARCH);
        searcher.printResult(hits);

        // Boolean Query
        //2. booleanQuery search for keywords "club" in the field "name" and "Las Vegas" in the field "city" for the top 20 results
        ScoreDoc[] booleanQueryHits = searcher.searchBooleanQuery("name", "city", "club", "Las Vegas", LuceneConstants.MAX_SEARCH);
        searcher.printResult(booleanQueryHits);

        //3. boolean query search for keywords \"5.0\" in the field \"stars\" and \"spas\" in the field \"categories\" and not \"glendale\" in the field \"city\" for the top 10 results
        ScoreDoc[] booleanQueryHits2 = searcher.searchBooleanQuery2("stars", "categories", "city", "5.0", "spas", "glendale", LuceneConstants.MAX_SEARCH);
        searcher.printResult(booleanQueryHits2);

        // Phrase Query
        //4. phraseQuery search for keywords "Japanese Sushi"
        ScoreDoc[] phraseQuery = searcher.searchPhraseQuery("name", "beer", "bar", LuceneConstants.MAX_SEARCH);
        searcher.printResult(phraseQuery);

        //5. phrase search for "name" with slop 2 words
        ScoreDoc[] queryPhraseWithSlop = searcher.searchPhraseWithSlop("name", "adeq", "station", 2, LuceneConstants.MAX_SEARCH);
        searcher.printResult(queryPhraseWithSlop);

        // Range
        //6. point rangeQuery search for stars
        ScoreDoc[] rangeQuery = searcher.searchRangeQuery("stars", 4.0, 5.0, LuceneConstants.MAX_SEARCH);
        searcher.printResult(rangeQuery);

        // Wildcard
        //7. wildcard search for any biz name containing undefined word  "bar?"
        ScoreDoc[] wildcardQuery = searcher.searchWildcard("name", "?bar", LuceneConstants.MAX_SEARCH);
        searcher.printResult(wildcardQuery);

        // Location
        //8. POI search for location "1025 Morehead Medical Dr, Ste 225, Charlotte"
        ScoreDoc[] locationQuery = searcher.searchLocation("location", "1025 Morehead Medical Dr, Ste 225, Charlotte", LuceneConstants.MAX_SEARCH);
        searcher.printResult(locationQuery);

        //9. Search by latitude and longitude
        ScoreDoc[] queryLatAndLng = searcher.searchByLatAndLng(36.016723, -115.1171678, LuceneConstants.MAX_SEARCH);
        searcher.printResult(queryLatAndLng);

        //Fuzzy
        //10. fuzzy search for misspelled word "barbacue"
        ScoreDoc[] fuzzyQuery = searcher.searchFuzzy("name", "barbacue", LuceneConstants.MAX_SEARCH);
        searcher.printResult(fuzzyQuery);

        // Multifield
        //11. search for name filed and address which contains "club" OR "Valley"
        ScoreDoc[] multifieldQuery = searcher.searchMultifield("name", "address", "club", "Valley", LuceneConstants.MAX_SEARCH);
        searcher.printResult(multifieldQuery);

        // Query Parser
        //12. Use queryParse to implement wildcard query. City: Los \Angel*
        ScoreDoc[] queryParserWildcard = searcher.searchQueryParser("city", "Los \\Angel?", LuceneConstants.MAX_SEARCH);
        searcher.printResult(queryParserWildcard);

        //13. use boosted character search with query parser: Yoga^4 club^3 fitness (^ means a boost)
        ScoreDoc[] queryNumrangeWithParser = searcher.searchCharWithParser("name", "Yoga^4 club^3 fitness", LuceneConstants.MAX_SEARCH);
        searcher.printResult(queryNumrangeWithParser);

        //14. fuzzy search with query parser: saerch for wildcard cinem~
        ScoreDoc[] queryFuzzyWithParser = searcher.searchFuzzyWithParser("name", "cinem~", LuceneConstants.MAX_SEARCH);
        searcher.printResult(queryFuzzyWithParser);

        //15. character range search with query parser: [club TO spa} (including "club" but excluding "spa")
        ScoreDoc[] queryRangeWithParser = searcher.searchRangeWithParser("name", "[club TO spa}", LuceneConstants.MAX_SEARCH);
        searcher.printResult(queryRangeWithParser);

        //16. character range search with query parser: (club OR bar) AND wine
        ScoreDoc[] queryGroupingWithParser = searcher.searchGroupingWithParser("name", "(club OR bar) AND wine", LuceneConstants.MAX_SEARCH);
        searcher.printResult(queryGroupingWithParser);

        //17. proximity search for "name"
        ScoreDoc[] queryProximityWithParser = searcher.searchProximityWithParser("name", "beer club ~10", LuceneConstants.MAX_SEARCH);
        searcher.printResult(queryProximityWithParser);

        //18. boolean parser search for "name"
        ScoreDoc[] queryBooleanWithParser = searcher.searchBooleanWithParser("name", "+bar club", LuceneConstants.MAX_SEARCH);
        searcher.printResult(queryBooleanWithParser);

        //19. Use queryParse to replace booleanQuer
        ScoreDoc[] queryParserRBooleanQuery = searcher.searchQueryParserRBooleanQuery("categories", "Italian OR food OR Donuts", LuceneConstants.MAX_SEARCH);
        searcher.printResult(queryParserRBooleanQuery);

        // Boost Query
        //20. Use boostQuery with boost and booleanQuery to add priority for the first query
        ScoreDoc[] queryBoostBooleanQuery = searcher.searchBoostBooleanQuery("name", "state", "club", "NV", LuceneConstants.MAX_SEARCH);
        searcher.printResult(queryBoostBooleanQuery);


        // Calculate total time used and total memory used through the indexing and searching
        long endTime = System.nanoTime();
        long endUsedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long indexTotalTime = (endTime - startTime) / 1000000;
        long indexUsedMem = (endUsedMem - startUsedMem) / 1000000;
        System.out.println("Total time used for indexing and searching: " + indexTotalTime + " milliseconds");
        System.out.println("Total memory used for indexing and searching: " + indexUsedMem + " MB");
        System.out.println(
                "\n------------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println(
                "------------------------------------------------------------------------------------------------------------------------------------------\n");


        // Advanced Topic
        // Combined search: location "1025 Morehead Medical Dr, Ste 225, Charlotte" and name "club"
        ScoreDoc[] queryLocationAndField = searcher.searchLocationAndField("name", "club", "1025 Morehead Medical Dr, Ste 225, Charlotte", LuceneConstants.MAX_SEARCH);
        searcher.printResult(queryLocationAndField);


    }

}

