package com.fatality;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.standard.ClassicTokenizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.synonym.SynonymGraphFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.synonym.WordnetSynonymParser;
//import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
//import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.*;
import java.text.ParseException;

public class QueryExpansionAnalyzer extends Analyzer {

    private static final String wn_pl_path = "D:\\Sgmon\\Documents\\Erasmus_Doc\\Corsi\\InformationRetrieval\\IRproject\\wn_s.pl";

    private SynonymMap buildSynonym() throws IOException, ParseException {
        File file = new File(wn_pl_path);
        InputStream stream = new FileInputStream(file);
        Reader rulesReader = new InputStreamReader(stream);
        WordnetSynonymParser parser = new WordnetSynonymParser(true, true, new StandardAnalyzer(CharArraySet.EMPTY_SET));
        parser.parse(rulesReader);
        return parser.build();
    }

    @Override
    protected TokenStreamComponents createComponents(String string)
    {
        Tokenizer source = new ClassicTokenizer();
        TokenStream filter = new LowerCaseFilter(source);
//        filter = new EnglishPossessiveFilter(filter);
        SynonymMap mySynonymMap = null;
        try {
            mySynonymMap = buildSynonym();
        } catch (IOException | ParseException e) { e.printStackTrace(); }

        filter = new SynonymGraphFilter(filter, mySynonymMap, false);

        return new TokenStreamComponents(source, filter);
    }
}