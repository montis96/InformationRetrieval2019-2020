package com.fatality;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.io.PrintWriter;

import org.apache.commons.digester3.Digester;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

/**
 * Simple command-line based search demo.
 * 
 * @author Simone Monti and Gianluca Puleri
 *
 */
public class SearchFilesRocchio {

	private SearchFilesRocchio() {
	}

	/**
	 * Simple command-line based search demo.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		String usage = "Usage:\tjava org.apache.lucene.demo.SearchFilesModule [-index dir] [-field f] [-repeat n] [-queries file] [-query string] [-raw] [-paging hitsPerPage]\n\nSee http://lucene.apache.org/core/4_1_0/demo/ for details.";
		if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
			System.out.println(usage);
			System.exit(0);
		}

		String index = "indexTFIDF";
		List<String> fields = new ArrayList<String>();
//		fields.add("title_question");
		fields.add("body_question");
		fields.add("tags");
		fields.add("answers");
		int repeat = 0;
		boolean raw = false;
		int hitsPerPage = 10;

		for (int i = 0; i < args.length; i++) {
			if ("-index".equals(args[i])) {
				index = args[i + 1];
				i++;
			} else if ("-field".equals(args[i])) {
				fields = new ArrayList<String>();
				fields.add(args[i + 1]);
				i++;
			} else if ("-repeat".equals(args[i])) {
				repeat = Integer.parseInt(args[i + 1]);
				i++;
			} else if ("-raw".equals(args[i])) {
				raw = true;
			} else if ("-paging".equals(args[i])) {
				hitsPerPage = Integer.parseInt(args[i + 1]);
				if (hitsPerPage <= 0) {
					System.err.println("There must be at least 1 hit per page.");
					System.exit(1);
				}
				i++;
			}
		}

//		Managing the fields
		String[] finalFields = fields.toArray(new String[0]);

		IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
		IndexSearcher searcher = new IndexSearcher(reader);
		searcher.setSimilarity(new ClassicSimilarity());
		Analyzer analyzer = new StandardAnalyzer();

//		Use the boosts to prefer some parts against others
//		HashMap<String,Float> boosts = new HashMap<String,Float>();
//		boosts.put("title", 2.0f);
//		boosts.put("body_question", 1.5f);
//		boosts.put("tags", 1.0f);
//		boosts.put("answers", 1.0f);
		MultiFieldQueryParser queryParser = new MultiFieldQueryParser(finalFields, analyzer/* , boosts */);

//		Digester to read XML of queries 
		Digester digester = new Digester();
		digester.setValidating(false);
		digester.addObjectCreate("queries/", QueryXML.class);
		digester.addCallMethod("queries/text", "addQueries", 0);
		digester.addCallMethod("queries/solution", "addSolutions", 0);
		QueryXML question = (QueryXML) digester.parse("file:///D:\\Sgmon\\Git\\InformationRetrieval2019-2020\\IRproject\\queries_test_BM25.xml");

		PrintWriter printerResults = new PrintWriter("finalResultsRocchio.csv");

		for (int n = 0; n < question.getQueries().length; n++) {

			String line = question.getQueries()[n];
			if (line == null || line.length() == -1) {
				break;
			}

			line = line.trim();

			if (line == null || line.length() == 0) {
				break;
			}

//			line = MultiFieldQueryParser.escape(line);

			Query query = queryParser.parse(MultiFieldQueryParser.escape(line));

			if (repeat > 0) { // repeat & time as benchmark
				Date start = new Date();
				for (int i = 0; i < repeat; i++) {
					searcher.search(query, 100); // Finds the top 100 hits for query
				}
				Date end = new Date();
				System.out.println("Time: " + (end.getTime() - start.getTime()) + "ms");
			}
			doPagingSearch(searcher, reader, query, line, hitsPerPage, raw, line == null, false, finalFields,
					question.getSolutions()[n], printerResults);
		}
		printerResults.close();
		reader.close();
	}

	/**
	 * This demonstrates a typical paging search scenario, where the search engine
	 * presents pages of size n to the user. The user can then go to the next page
	 * if interested in the next hits.
	 *
	 * When the query is executed for the first time, then only enough results are
	 * collected to fill 5 result pages. If the user wants to page beyond this
	 * limit, then the query is executed another time and all hits are collected.
	 * 
	 * @param searcher
	 * @param indexReader
	 * @param query
	 * @param originalQueryText
	 * @param hitsPerPage
	 * @param raw
	 * @param interactive
	 * @param newTermQuery
	 * @param finalFields
	 * @param solution
	 * @param printerResults
	 * @throws IOException
	 * @throws ParseException
	 */
	private static void doPagingSearch(IndexSearcher searcher, IndexReader indexReader, Query query,
			String originalQueryText, int hitsPerPage, boolean raw, boolean interactive,
			boolean newTermQuery /* False first time */, String[] finalFields, String solution,
			PrintWriter printerResults) throws IOException, ParseException {
		double beta = 0.75;
		double y = 0.25;
		int kRelevant = 7;
		int kNotRelevant = 0;

		if (!newTermQuery) {
			String[] queryTerms = originalQueryText.split(" ");

//			Escaping all terms
//			for (int n = 0; n < queryTerms.length; n++) {
//				queryTerms[n] = MultiFieldQueryParser.escape(queryTerms[n]);
//			}
//          This function map the word with the number of hits in the query text;
			Map<String, Long> queryVocabulary = Arrays.stream(queryTerms)
					.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
//             Collect enough docs to show 5 pages
			TopDocs results = searcher.search(query, 5 * hitsPerPage);

			ScoreDoc[] hits = results.scoreDocs;
//			Total number of the retrieved article
			int hitsLength = hits.length;

			if (hitsLength < kRelevant)
				kRelevant = hitsLength;
			else
				kNotRelevant = hitsLength - kRelevant;

			beta = beta / kRelevant;
			if (kNotRelevant > 0)
				y = y / kNotRelevant;

			List<List<String>> docs = new ArrayList<>();
			for (int i = 0; i < hitsLength; i++) {
				for (String field : finalFields) {
					Fields fields = indexReader.getTermVectors(results.scoreDocs[i].doc);
					if (fields.terms(field) != null) {

						List<String> l = new ArrayList<>();
						TermsEnum terms = fields.terms(field).iterator();
						BytesRef term = terms.next();
						while (term != null) {
							String termString = term.utf8ToString();
							l.add(termString);
							term = terms.next();
						}

						docs.add(l);
					}
				}
			}

			HashMap<String, Double> relevantVocabulary = new HashMap<>();
			HashMap<String, Double> notRelevantVocabulary = new HashMap<>();
			for (String field : finalFields) {
				for (int i = 0; i < hitsLength; i++) {
					Fields fields = indexReader.getTermVectors(results.scoreDocs[i].doc);
					if (fields.terms(field) != null) {
						TermsEnum terms = fields.terms(field).iterator();
						BytesRef term = terms.next();
						while (term != null) {
							String termString = term.utf8ToString();
							Double prevScore = relevantVocabulary.get(termString);
							if (i > kRelevant)
								prevScore = notRelevantVocabulary.get(termString);
							if (prevScore == null)
								prevScore = 0.0;
							TFIDFCalculator calculator = new TFIDFCalculator();
							double tfidf = calculator.tfIdf(docs.get(i), docs, termString);
							Double score = prevScore + tfidf;
							if (i > kRelevant)
								notRelevantVocabulary.put(termString, score);
							else
								relevantVocabulary.put(termString, score);
							term = terms.next();
						}
					}
				}
			}

			Map<String, Double> fin = new HashMap<>();
			for (Object o : queryVocabulary.entrySet()) {
				Map.Entry pair = (Map.Entry) o;
				String key = pair.getKey().toString();
				Long value = (Long) pair.getValue();
				Double notRelevantValue = notRelevantVocabulary.get(key);
				Double relevantValue = relevantVocabulary.get(key);
				if (notRelevantValue == null)
					notRelevantValue = 0.0;
				if (relevantValue == null)
					relevantValue = 0.0;
				Double v = value + beta * relevantValue - y * notRelevantValue;
				fin.put(key, v);
			}
			for (Object o : relevantVocabulary.entrySet()) {
				Map.Entry pair = (Map.Entry) o;
				String key = pair.getKey().toString();
				if (fin.get(key) == null) {
					Double value = (Double) pair.getValue();

					Double notRelevantValue = notRelevantVocabulary.get(key);
					Long queryValue = queryVocabulary.get(key);

					if (notRelevantValue == null)
						notRelevantValue = 0.0;
					if (queryValue == null)
						queryValue = 0L;

					Double v = queryValue + beta * value - y * notRelevantValue;

					fin.put(key, v);
				}
			}

			for (Object o : notRelevantVocabulary.entrySet()) {
				Map.Entry pair = (Map.Entry) o;
				String key = pair.getKey().toString();
				if (fin.get(key) == null) {
					Double value = (Double) pair.getValue();

					Double relevantValue = relevantVocabulary.get(key);
					Long queryValue = queryVocabulary.get(key);

					if (relevantValue == null)
						relevantValue = 0.0;
					if (queryValue == null)
						queryValue = 0L;

					Double v = queryValue + beta * relevantValue - y * value;

					fin.put(key, v);
				}
			}

			int kExpansionTerms = 10;

			List<String> newQuery = new ArrayList<>();
			int j = 0;
			int finLength = fin.size();

			while (newQuery.size() < kExpansionTerms && j <= finLength) {
				String max = fin.entrySet().stream()
						.max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
				if (newQuery.indexOf(max) == -1)
					newQuery.add(max);
				fin.remove(max);

				j++;
			}

			String joined = String.join(" ", newQuery);
			System.out.println("New query: " + joined);

			Analyzer analyzer = new StandardAnalyzer();
			MultiFieldQueryParser queryParser = new MultiFieldQueryParser(finalFields, analyzer/* , boosts */);
			Query newQueryString = queryParser.parse(MultiFieldQueryParser.escape(joined));
			doPagingSearch(searcher, indexReader, newQueryString, originalQueryText, hitsPerPage, raw, interactive,
					true, finalFields, solution, printerResults);
		} else {
			TopDocs results = searcher.search(query, 5 * hitsPerPage);
			ScoreDoc[] hits = results.scoreDocs;
			int numTotalHits = Math.toIntExact(results.totalHits.value);
			System.out.println(numTotalHits + " total matching documents");
			PrintWriter printer = new PrintWriter("results\\Rocchio\\" + originalQueryText.replaceAll("[^a-zA-Z0-9]", "") + ".csv");
			int start = 0;
			int end = Math.min(numTotalHits, hitsPerPage);

			if (end > hits.length) {
				hits = searcher.search(query, numTotalHits).scoreDocs;
			}

			end = Math.min(hits.length, start + hitsPerPage);

			boolean find = false;
			for (int i = start; i < end; i++) {
				if (raw) { // output raw format
					System.out.println("doc=" + hits[i].doc + " score=" + hits[i].score);
					continue;
				}
			
				Document doc = searcher.doc(hits[i].doc);
				String path = doc.get("path");
				if (path != null) {
					System.out.println((i + 1) + ". " + path);
					printer.print(path + ",");
					String title = doc.get("title");
					if (title != null) {
						System.out.println("   Title: " + doc.get("title"));
						printer.println("   Title: " + doc.get("title"));
					}
					if (path.contains(solution)) {
						printerResults.println(solution + "," + (i + 1) + ",");
						find = true;
					}
				} else {
					System.out.println((i + 1) + ". " + "No path for this document");
					printer.println((i + 1) + ". " + "No path for this document");
				}

			}
			if (!find)
				printerResults.println(solution + "," + ",");
			end = Math.min(numTotalHits, start + hitsPerPage);
			printer.close();
		}
	}
}
