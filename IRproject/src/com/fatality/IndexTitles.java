/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fatality;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.digester3.Digester;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * Index all text files under a directory.
 * <p>
 * This is a command-line application demonstrating simple Lucene indexing. Run
 * it with no command-line arguments for usage information.
 * 
 * @author Simone Monti and Gianluca Puleri
 *
 */
public class IndexTitles {

	private static int n_doc = 0;

	public static IndexWriter writer;

	private IndexTitles() {
	}

	/** Index all text files under a directory. */
	public static void main(String[] args) {
		String usage = "java org.apache.lucene.demo.IndexFiles" + " [-index INDEX_PATH] [-docs DOCS_PATH] [-update]\n\n"
				+ "This indexes the documents in DOCS_PATH, creating a Lucene index"
				+ "in INDEX_PATH that can be searched with SearchFiles";
		String indexPath = "indexTitles";
		String docsPath = null;
		boolean create = true;
		for (int i = 0; i < args.length; i++) {
			if ("-index".equals(args[i])) {
				indexPath = args[i + 1];
				i++;
			} else if ("-docs".equals(args[i])) {
				docsPath = args[i + 1];
				i++;
			} else if ("-update".equals(args[i])) {
				create = false;
			}
		}

		if (docsPath == null) {
			System.err.println("Usage: " + usage);
			System.exit(1);
		}

		final Path docDir = Paths.get(docsPath);
		if (!Files.isReadable(docDir)) {
			System.out.println("Document directory '" + docDir.toAbsolutePath()
					+ "' does not exist or is not readable, please check the path");
			System.exit(1);
		}

		Date start = new Date();
		try {
			System.out.println("Indexing to directory '" + indexPath + "'...");

			Directory dir = FSDirectory.open(Paths.get(indexPath));
			Analyzer analyzer = new StandardAnalyzer();
			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

			if (create) {
				// Create a new index in the directory, removing any
				// previously indexed documents:
				iwc.setOpenMode(OpenMode.CREATE);
			} else {
				// Add new documents to an existing index:
				iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
			}

			// Optional: for better indexing performance, if you
			// are indexing many documents, increase the RAM
			// buffer. But if you do this, increase the max heap
			// size to the JVM (eg add -Xmx512m or -Xmx1g):
			//
			// iwc.setRAMBufferSizeMB(256.0);

			writer = new IndexWriter(dir, iwc);

			PrintWriter printer = new PrintWriter("queries_test_titles.xml");

			printer.println("<queries>");

			indexDocs(writer, docDir, printer);

			printer.println("</queries>");
			printer.close();

			// NOTE: if you want to maximize search performance,
			// you can optionally call forceMerge here. This can be
			// a terribly costly operation, so generally it's only
			// worth it when your index is relatively static (ie
			// you're done adding documents to it):
			//
			// writer.forceMerge(1);

			writer.close();

			Date end = new Date();
			System.out.println(end.getTime() - start.getTime() + " total milliseconds");

		} catch (IOException e) {
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
		}
	}

	/**
	 * Indexes the given file using the given writer, or if a directory is given,
	 * recurses over files and directories found under the given directory.
	 *
	 * NOTE: This method indexes one document per input file. This is slow. For good
	 * throughput, put multiple documents into your input file(s). An example of
	 * this is in the benchmark module, which can create "line doc" files, one
	 * document per line, using the <a href=
	 * "../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
	 * >WriteLineDocTask</a>.
	 *
	 * @param writer Writer to the index where the given file/dir info will be
	 *               stored
	 * @param path   The file to index, or the directory to recurse into to find
	 *               files to index
	 * @throws IOException If there is a low-level I/O error
	 */
	static void indexDocs(final IndexWriter writer, Path path, PrintWriter printer) throws IOException {
		if (Files.isDirectory(path)) {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					try {
						indexDoc(writer, file, attrs.lastModifiedTime().toMillis(), printer);
					} catch (IOException ignore) {
						// don't index files that can't be read.
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} else {
			indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis(), printer);
		}
	}

	/** Indexes a single document 
	 * 
	 * @param writer
	 * @param file
	 * @param lastModified
	 * @param printer
	 * @throws IOException
	 */
	static void indexDoc(IndexWriter writer, Path file, long lastModified, PrintWriter printer) throws IOException {
		try (InputStream stream = Files.newInputStream(file)) {
			// make a new, empty document
			Document doc = new Document();

			// Add the path of the file as a field named "path". Use a
			// field that is indexed (i.e. searchable), but don't tokenize
			// the field into separate words and don't index term frequency
			// or positional information:
			Field pathField = new StringField("path", file.toString(), Field.Store.YES);
			doc.add(pathField);

			// Try to parse XML file
			Digester digester = new Digester();
			digester.setValidating(false);
			digester.addObjectCreate("root/", Question.class);
			digester.addCallMethod("root/question/Title", "setTitle", 0);
//			digester.addCallMethod("root/question/Body", "setBody_question", 0);
			digester.addCallMethod("root/question/Tags", "setTags", 0);
//			digester.addCallMethod("root/replies/answer/Body", "addAnswers", 0);

			try {
				Question question = (Question) digester.parse(file.toUri().toString());
				n_doc = n_doc + 1; //Number of total document indexed

//				Write to file
				if (n_doc % 1800 == 0) {

					printer.println("<text>" + StringEscapeUtils.escapeXml11(question.title) + "</text>" );
					printer.println( "<tags>" + StringEscapeUtils.escapeXml11(question.tags) + "</tags>" );
					printer.println("<solution> " + file.toString().substring(file.toString().lastIndexOf("\\") + 1)
							+ " </solution>");
				}

				FieldType type = new FieldType();
				type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
				type.setTokenized(true);
				type.setStored(true);
				type.setStoreTermVectors(true);

//				Add all the parts of the file to contents without the title
//				The tiltle will be the query

				Field title = new Field("title_question", question.getTitle(), type);
//				Field body_question = new Field("body_question", question.getBody_question(), type);
				Field tags = new Field("tags", question.getTags(), type);
//				Field answers = new Field("answers", question.getAnswers(), type);

				
				doc.add(title);
//				doc.add(body_question);
				doc.add(tags);
//				doc.add(answers);

			} catch (Exception e) {
				System.out.println(e + " " + file.toString());
			}

			// Add the last modified date of the file a field named "modified".
			// Use a LongPoint that is indexed (i.e. efficiently filterable with
			// PointRangeQuery). This indexes to milli-second resolution, which
			// is often too fine. You could instead create a number based on
			// year/month/day/hour/minutes/seconds, down the resolution you require.
			// For example the long value 2011021714 would mean
			// February 17, 2011, 2-3 PM.
			doc.add(new LongPoint("modified", lastModified));

			if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
				// New index, so we just add the document (no old document can be there):
				System.out.println("adding " + file);
				writer.addDocument(doc);
			} else {
				// Existing index (an old copy of this document may have been indexed) so
				// we use updateDocument instead to replace the old one matching the exact
				// path, if present:
				System.out.println("updating " + file);
				writer.updateDocument(new Term("path", file.toString()), doc);
			}

		}
	}
/**
 * 
 * @author Simone Monti and Gianluca Puleri
 *
 */
	public static class Question {
		public String title; 
		public String body_question; 
		public String tags;
		public List<String> answers = new ArrayList<String>();
		/**
		 * 
		 * @param title
		 */
		public void setTitle(String title) {
			this.title = title;
		}
		/**
		 * 
		 * @return
		 */
		public String getTitle() {
			return this.title;
		}
		/**
		 * 
		 * @param question
		 */
		public void setBody_question(String question) {
			this.body_question = question;
		}
		/**
		 * 
		 * @return
		 */
		public String getBody_question() {
			return this.body_question;
		}
		/**
		 * 
		 * @param text
		 */
		public void setTags(String text) {
			this.tags = text;
		}
		/**
		 * 
		 * @return
		 */
		public String getTags() {
			return this.tags;
		}
		/**
		 * 
		 * @param text
		 */
		public void addAnswers(String text) {
			this.answers.add(text);
		}
		/**
		 * 
		 * @return
		 */
		public String getAnswers() {
			return this.answers.toString();
		}
		/**
		 * 
		 * @return
		 */
		public int countAnswers() {
			return this.answers.size();
		}
	}
}
