package edu.cmu.lti.f14.hw3.hw3_zhiyuel.annotators;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import edu.cmu.lti.f14.hw3.hw3_zhiyuel.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_zhiyuel.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_zhiyuel.utils.StanfordLemmatizer;
import edu.cmu.lti.f14.hw3.hw3_zhiyuel.utils.Utils;

public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

  private BufferedReader br = null;

  private HashSet<String> strset = new HashSet<String>();

  public static void main(String[] args) {
    String text = "      a   a";
    text = text.trim();
    String[] strs = text.split(" ");
    System.out.println("?" + Arrays.toString(strs) + "?");
  }

  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
    String samplefile = (String) aContext.getConfigParameterValue("STOPWORDS_FILE");
    InputStream is = DocumentVectorAnnotator.class.getClassLoader().getResourceAsStream(samplefile);
    System.out.println(is == null);

    try {
      br = new BufferedReader(new InputStreamReader(is, "utf-8"));
      String str = null;
      while ((str = br.readLine()) != null) {
        strset.add(str);
        // System.out.println(str);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void process(JCas jcas) throws AnalysisEngineProcessException {

    FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
    if (iter.isValid()) {
      iter.moveToNext();
      Document doc = (Document) iter.get();
      // every line in meta data is a document
      // among them rel=99 is a query document
      createTermFreqVector(jcas, doc);

    }

  }

  /**
   * A basic white-space tokenizer, it deliberately does not split on punctuation!
   * 
   * @param doc
   *          input text
   * @return a list of tokens.
   */

  List<String> tokenize0(String doc) {
    List<String> res = new ArrayList<String>();

    for (String s : doc.split("\\s+"))
      res.add(s);
    return res;
  }

  /**
   * 
   * @param jcas
   * @param doc
   * @param aToken
   */
  private String removePunctuation(String text) {
    text = text.toLowerCase();

    if (text.endsWith("'s") || text.endsWith("s'") || text.endsWith("--")) {
      text = text.substring(0, text.length() - 2);
      // System.out.println(text);
    }
    if (text.endsWith("\"") || text.endsWith(",") || text.endsWith(";") || text.endsWith(".")
            || text.endsWith("?") || text.endsWith("!") || text.endsWith("-")) {
      // System.out.println("?????????");
      text = text.substring(0, text.length() - 1);
      if (text.endsWith("."))
        text = text.substring(0, text.length() - 1);
    }

    if (text.startsWith("\"") || text.startsWith(",") || text.startsWith(";")
            || text.startsWith("."))
      text = text.substring(1);
    return text;
  }

  private void createTermFreqVector(JCas jcas, Document doc) {

    String docText = doc.getText();
    int queryid = doc.getQueryID();
    int rel = doc.getRelevanceValue();
    
//    String att = docText.replaceAll("[\\p{Punct}]+", " ");“，”
    String att = docText.replaceAll("[,;.?\"]+", "");

    
    String stemmedText = StanfordLemmatizer.stemText(att).replaceAll("[']+", "");
    stemmedText=stemmedText.replaceAll("[-]+", " ");
    System.out.println(stemmedText);
    // TO DO: construct a vector of tokens and update the tokenList in CAS
    // TO DO: use tokenize0 from above
    List<String> arr = tokenize0(stemmedText);
    int cnt = 0;
    boolean flag = false;
    if (rel == 99) {
      for (int i = 0; i < arr.size(); i++) {
        // String text=arr.get(i).replaceAll("[\\p{Punct}\\p{Space}]+", "");
        // System.out.println(text);
//        String text = removePunctuation(arr.get(i));
        String text = arr.get(i);
        text = text.trim();
        if (text.equals("-"))
          continue;
//        if (text.length() == 0) {
//          System.out.println("???????");
//          continue;
//        }
        // this is query
        if (strset.contains(text)) {
          // stopwords
          cnt++;
        }
      }
      if (cnt > arr.size() / 2) {
        flag = true;
        // System.out.println(stemmedText);
      }
    }

    // String[] spam = docText.split(" ");
    Map<String, Integer> freq = new HashMap<String, Integer>();
    for (int i = 0; i < arr.size(); i++) {
      String text = arr.get(i);
//      String text = removePunctuation(arr.get(i));
      // more than one white space stuff
      text = text.trim();
//      if (text.equals("-"))// other symbol cause errrors, mis spelling
//        continue;
//      if (text.length() == 0) {
//        // System.out.println("???????");
//        continue;
//      }
      if (!freq.containsKey(text)) {

        if (rel == 99) {
          // this is query
          if (!strset.contains(text) || flag) {
            // stopwords
            freq.put(text, 1);
          }
        } else {
          if (text.contains("--")) {
            // deal with compound, to split up into two words
            String[] compounds = text.split("--");
            for (int j = 0; i < compounds.length; j++) {
              freq.put(compounds[j], 1);
            }
          } else if (text.contains("-")) {
            // deal with compound, to split up into two words
            String[] compounds = text.split("-");
            // System.out.println(Arrays.toString(compounds)+"|"+compounds.length);
            if (compounds.length > 0) {
              for (int j = 0; j < compounds.length; j++) {
                freq.put(compounds[j], 1);
              }
            }
          } else if (text.contains("_")) {
            // deal with compound, to split up into two words
            // System.out.println(text);
            String[] compounds = text.split("_");
            for (int j = 0; j < compounds.length; j++) {
              freq.put(compounds[j], 1);
            }
          } else {
            freq.put(text, 1);
          }
        }
      } else {
        for (Map.Entry<String, Integer> m : freq.entrySet()) {
          if (text.equals(m.getKey())) {
            // System.out.println(text);
            freq.put(m.getKey(), (int) m.getValue() + 1);
          }
        }
      }

      // original
      /*
       * if (!freq.containsKey(arr.get(i))) { freq.put(arr.get(i), 1); } else { for
       * (Map.Entry<String, Integer> m : freq.entrySet()) { if (arr.get(i).equals(m.getKey())) {
       * freq.put(m.getKey(), (int) m.getValue() + 1); } } }
       */
    }
    List<Token> aToken = new ArrayList<Token>();

    for (Map.Entry<String, Integer> m : freq.entrySet()) {
      // System.out.println(id+"|"+rel+"|"+m.getKey() + "," + m.getValue());
      Token t = new Token(jcas);
      t.setText(m.getKey());
      t.setFrequency(m.getValue());
      aToken.add(t);
      t.addToIndexes();
    }

    FSList fsTokenList = Utils.fromCollectionToFSList(jcas, aToken);
    // update the token list in cas??????
    doc.setTokenList(fsTokenList);

  }
}
