package com.rreganjr.nlp.impl;

import com.rreganjr.ResourceBundleHelper;
import com.rreganjr.nlp.dictionary.GrammaticalStructureLevel;
import com.rreganjr.nlp.dictionary.NLPProcessor;
import com.rreganjr.nlp.dictionary.NLPText;
import com.rreganjr.nlp.dictionary.ParseTag;
import com.rreganjr.nlp.dictionary.impl.NLPTextImpl;
import com.rreganjr.platform.ApplicationException;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.util.Sequence;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class OpenNLPTagger extends OpenNLPTokenizer implements NLPProcessor<NLPText> {

    public static final String PROP_POSTAGGER_MODEL_FILE = "POSTaggerModelFile";
    public static final String PROP_POSTAGGER_MODEL_FILE_DEFAULT = "nlp/opennlp-tools/parser/tag.bin.gz";

    private static POSTaggerME posTagger;
    private static POSModel posModel;

    static {
        try {
            ResourceBundleHelper resourceBundleHelper = new ResourceBundleHelper(OpenNLPParser.class.getName());
            String modelFile = resourceBundleHelper.getString(PROP_POSTAGGER_MODEL_FILE, PROP_POSTAGGER_MODEL_FILE_DEFAULT);
            InputStream modelStream = OpenNLPTagger.class.getClassLoader().getResourceAsStream(modelFile);
            posModel = new POSModel(modelStream);
            posTagger = new POSTaggerME(posModel);
        } catch (Exception e) {
            posTagger = null;
            throw new ExceptionInInitializerError(e);
        }
    }

    public OpenNLPTagger() {
        init();
    }

    private synchronized void init() {
        if (posTagger == null) {
            try {
                ResourceBundleHelper resourceBundleHelper = new ResourceBundleHelper(OpenNLPParser.class.getName());
                String modelFile = resourceBundleHelper.getString(PROP_POSTAGGER_MODEL_FILE, PROP_POSTAGGER_MODEL_FILE_DEFAULT);
                InputStream modelStream = OpenNLPTagger.class.getClassLoader().getResourceAsStream(modelFile);
                posModel = new POSModel(modelStream);
                posTagger = new POSTaggerME(posModel);
            } catch (Exception e) {
                posTagger = null;
                throw ApplicationException.failedToInitializeComponent(getClass(), e);
            }
        }
    }

    @Override
    public NLPText process(NLPText text) {
        if (text.is(GrammaticalStructureLevel.SENTENCE)) {
            if (text.getLeaves().isEmpty()) {
                super.process(text);
            }
            List<String> words = new ArrayList<>();
            for (NLPText word : text.getLeaves()) {
                words.add(word.getText());
            }
            String[] tags = posTagger.tag(words.toArray(new String[0]));
            for (int i = 0; i < tags.length; i++) {
                ((NLPTextImpl) text.getLeaves().get(i)).setParseTag(ParseTag.tagOf(tags[i]));
            }
        }
        return text;
    }

    protected static POSTaggerME getTagger() {
        return posTagger;
    }

    protected static POSModel getPosModel() {
        return posModel;
    }
}
