package Main;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.LinkedList;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;
import DBPedia.DBPediaQuery;
import MetaMap.MetaMap;
import SKOS.SKOS;
import SKOS.SKOSThesaurus;
import WordNet.Wordnet;

/**
 * The Class Annotator. For use as a library use its method annotate(string)
 */
public class MarvinSemAnnotator {

	/** The tokenizer. */
	public static Tokenizer tokenizer;

	/** The words. */
	public static LinkedList<Word> words;

	/** The wn. */
	Wordnet wn = new Wordnet();

	/** The db. */
	DBPediaQuery db = null;

	/** The is. */
	InputStream is;

	/** The _pos tagger. */
	POSTaggerME _posTagger = null;
	MetaMap mms;

	public boolean DBPediaAnnptate = false;
	public boolean WordNetAnnotate = false;
	public boolean MetaMapAnnotate = false;
	public String DBPediaEndpoint = "";
	public String MetaMap_host = "";
	public int MetaMap_port = 8066;
	public static String MetaMapName = "";
	public static String MetaMapVersion = "";
	public static String WordNetName = "";
	public static String WordNetVersion = "";
	public static String DBPediaName = "";
	public static String DBPediaVersion = "";
	public static String Location = "";
	public static String Environment = "";
	public static LinkedList<SKOSThesaurus> SKOSThesauri = new LinkedList<SKOSThesaurus>();
	
	/**
	 * Instantiates a new annotator.
	 */
	public MarvinSemAnnotator() {
		try {

			BufferedReader br = new BufferedReader(new FileReader("settings.cfg"));
			// StringBuilder sb = new StringBuilder();
			String line = br.readLine();
			System.out.println("settings.cfg setting:");
			System.out.println(line);
			
			while (line != null && line != "") {
				KeyValue kv = new KeyValue();
				String[] parts = line.split(";");			  
				kv.key = parts[0];
				kv.value = parts[1];
				if (kv.key.equals("use_dbpedia") && kv.value.equals("true")) {
					DBPediaAnnptate = true;
				}
				if (kv.key.equals("use_metamap") && kv.value.equals("true")) {
					MetaMapAnnotate = true;
				}
				if (kv.key.equals("use_wordnet") && kv.value.equals("true")) {
					WordNetAnnotate = true;
				}
				if (kv.key.equals("dbpedia_url")) {
					DBPediaEndpoint = kv.value;
				}
				if (kv.key.equals("metamap_host")) {
					MetaMap_host = kv.value;
				}
				if (kv.key.equals("metamap_port")) {
					MetaMap_port = Integer.parseInt(kv.value);
				}
				if (kv.key.equals("location")) {
					Location = kv.value;
				}
				if (kv.key.equals("env_desc")) {
					Environment = kv.value;
				}
				if (kv.key.equals("metamap_desc")) {
					MetaMapName = parts[1];
					MetaMapVersion = parts[2];
				}
				if (kv.key.equals("dbpedia_desc")) {
					DBPediaName = parts[1];
					DBPediaVersion = parts[2];
				}
				if (kv.key.equals("wordnet_desc")) {
					WordNetName = parts[1];
					WordNetVersion = parts[2];
				}
				if (kv.key.equals("SKOS")) {
					SKOSThesaurus skos = new SKOSThesaurus();
					skos.setFilePath(parts[1]);
					skos.setVersion(parts[2]);
					skos.setVocabularyName(parts[3]);
					if(parts[4].equals("true"))
					{
						skos.setIsUsed(true);
					}
					else
					{
						skos.setIsUsed(false);
					}
					SKOSThesauri.add(skos);
				}
				line = br.readLine();
			}
			br.close();
			if(MetaMapAnnotate)
			{
				mms = new MetaMap(MetaMap_host, MetaMap_port);
			}
			if(DBPediaAnnptate)
			{
				db = new DBPediaQuery(DBPediaEndpoint);
			}
			
			for(int i = 0;i<SKOSThesauri.size();i++)
			{
				if(SKOSThesauri.get(i).isUsed()){
					SKOSThesauri.get(i).Thesaurus = new SKOS(SKOSThesauri.get(i).getFilePath(),SKOSThesauri.get(i).getVersion());
				}
			}

			InputStream modelIn = null;
			// Loading tokenizer model
			modelIn = new FileInputStream("en-pos-maxent.bin");
			final POSModel posModel = new POSModel(modelIn);
			modelIn.close();
			_posTagger = new POSTaggerME(posModel);
			is = new FileInputStream("en-token.bin");
			TokenizerModel model = new TokenizerModel(is);
			tokenizer = new TokenizerME(model);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Annotate.
	 *
	 * @param text
	 *            the text
	 * @return the linked list of word objects
	 */
	public LinkedList<Word> annotate(String text) {
		try {
			words = new LinkedList<Word>();
			String[] tokens = tokenizer.tokenize(text);
			Span[] tokens2 = tokenizer.tokenizePos(text);
			String[] tags = _posTagger.tag(tokens);
			for (int i = 0; i < tokens.length; i++) {
				Word w = new Word();
				w.starting = tokens2[i].getStart();
				w.ending = tokens2[i].getEnd();
				w.word = tokens[i];
				if (WordNetAnnotate) {
					w.wordmeanings.addAll(wn.getSencesFromWordnet(w.word,
							tags[i], w.starting, w.ending,tokens,i));
				}
				// TODO: ADD DBPedia as local instance
				if (DBPediaAnnptate) {
					w.wordmeanings.addAll(db.queryDBPedia(w.word, w.starting,
							w.ending));
					if (i + 1 < tokens.length) {
						w.wordmeanings.addAll(db.queryDBPedia(w.word + " "
								+ tokens[i + 1], w.starting,
								tokens2[i + 1].getEnd()));
					}
					
					if (i + 2 < tokens.length) {
						w.wordmeanings.addAll(db.queryDBPedia(w.word + " "
								+ tokens[i + 1]+ " "+tokens[i + 2], w.starting,
								tokens2[i + 1].getEnd()));
					}
				}
				
				
				
				
				words.add(w);
			}
			if (MetaMapAnnotate) {
				mms.getMetaMapMeanings(text);
			}
			for(int j = 0 ;j<SKOSThesauri.size();j++)
			{
				SKOSThesaurus Thesauri = SKOSThesauri.get(j);
				if(Thesauri.isUsed())
				{
					Thesauri.Thesaurus.annotate(text,Thesauri);
				}
			}

			return words;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}

	}

	public LinkedList<Word> annotateWordNetOnly(String text) {
		try {
			words = new LinkedList<Word>();
			String[] tokens = tokenizer.tokenize(text);
			Span[] tokens2 = tokenizer.tokenizePos(text);
			String[] tags = _posTagger.tag(tokens);
			for (int i = 0; i < tokens.length; i++) {
				Word w = new Word();
				w.starting = tokens2[i].getStart();
				w.ending = tokens2[i].getEnd();
				w.word = tokens[i];
				w.wordmeanings.addAll(wn.getSencesFromWordnet(w.word, tags[i],
						w.starting, w.ending,tokens,i));

				// TODO: ADD DBPedia as local instance
				// w.wordmeanings.addAll(db.queryDBPedia(w.word, w.starting,
				// w.ending));
				// if (i + 1 < tokens.length) {
				// w.wordmeanings.addAll(db.queryDBPedia(w.word + " "
				// + tokens[i + 1], w.starting,
				// tokens2[i + 1].getEnd()));
				// }
				words.add(w);
			}
			return words;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}

	}

	public LinkedList<Word> annotateMetaMapOnly(String text) {
		try {
			words = new LinkedList<Word>();
			mms.getMetaMapMeanings(text);
			return words;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}

	}

	public LinkedList<Word> annotateDBPediaOnly(String text) {
		try {
			words = new LinkedList<Word>();
			String[] tokens = tokenizer.tokenize(text);
			Span[] tokens2 = tokenizer.tokenizePos(text);
			//String[] tags = _posTagger.tag(tokens);
			for (int i = 0; i < tokens.length; i++) {
				Word w = new Word();
				w.starting = tokens2[i].getStart();
				w.ending = tokens2[i].getEnd();
				w.word = tokens[i];

				// TODO: ADD DBPedia as local instance
				w.wordmeanings.addAll(db.queryDBPedia(w.word, w.starting,
						w.ending));
				if (i + 1 < tokens.length) {
					w.wordmeanings.addAll(db.queryDBPedia(w.word + " "
							+ tokens[i + 1], w.starting,
							tokens2[i + 1].getEnd()));
				}
				if (i + 2 < tokens.length) {
					w.wordmeanings.addAll(db.queryDBPedia(w.word + " "
							+ tokens[i + 1]+ " "+tokens[i + 2], w.starting,
							tokens2[i + 1].getEnd()));
				}
				words.add(w);
			}
			return words;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}

	}

}
