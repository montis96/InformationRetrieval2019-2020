package com.fatality;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Date;

import org.apache.commons.digester3.Digester;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.FSDirectory;
/**
 * 
 * @author Simone Montiand Gianluca Puleri
 *
 */
public class SearchFilesTFIDF {

	private SearchFilesTFIDF() {
	}

	/** Simple command-line based search demo.
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

		QueryXML question = (QueryXML) digester
				.parse("file:///D:\\\\Sgmon\\\\Git\\\\InformationRetrieval2019-2020\\\\IRproject\\\\queries_test_TFIDF.xml");
		
		PrintWriter printerResults = new PrintWriter("finalResultsTFIDF.csv");
		System.out.println(question.getQueries()[0]);


		for (int n = 0; n < question.getQueries().length; n++) {
			String line = question.getQueries()[n];
			if (line == null || line.length() == -1) {
				break;
			}

			line = line.trim();
			
			if (line == null || line.length() == 0) {
				break;
			}

			Query query = queryParser.parse(MultiFieldQueryParser.escape(line));

			System.out.println("Searching for: " + query.toString());

			if (repeat > 0) { // repeat & time as benchmark
				Date start = new Date();
				for (int i = 0; i < repeat; i++) {
					searcher.search(query, 100); // Finds the top 100 hits for query
				}
				Date end = new Date();
				System.out.println("Time: " + (end.getTime() - start.getTime()) + "ms");
			}

			doPagingSearch(searcher, query, line, hitsPerPage, raw, line == null, question.getSolutions()[n],
					printerResults);
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
	 * @param query
	 * @param queryString
	 * @param hitsPerPage
	 * @param raw
	 * @param interactive
	 * @param solution
	 * @param printerResults
	 * @throws IOException
	 */
	public static void doPagingSearch(IndexSearcher searcher, Query query, String queryString, int hitsPerPage,
			boolean raw, boolean interactive, String solution, PrintWriter printerResults) throws IOException {

		PrintWriter printer = new PrintWriter("results\\TFIDF\\" + queryString.replaceAll("[^a-zA-Z0-9]", "") + ".csv");
		System.out.println("Query: " + queryString);
		// Collect enough docs to show 1 pages
		TopDocs results = searcher.search(query, 1* hitsPerPage);
		
		ScoreDoc[] hits = results.scoreDocs;

		int numTotalHits = Math.toIntExact(results.totalHits.value);
		System.out.println(numTotalHits + " total matching documents");

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
				printer.println("doc=" + hits[i].doc + " score=" + hits[i].score);
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
//		printerResults.println(","+end);
		printer.close();
	}

}
