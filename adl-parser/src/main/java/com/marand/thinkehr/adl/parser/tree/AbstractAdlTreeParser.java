/*
 * Copyright (C) 2014 Marand
 */

package com.marand.thinkehr.adl.parser.tree;

import com.google.common.collect.Lists;
import com.marand.thinkehr.adl.antlr.AdlParser;
import com.marand.thinkehr.adl.parser.RuntimeRecognitionException;
import com.marand.thinkehr.adl.rm.RmModel;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.Token;
import org.antlr.runtime.tree.BaseTree;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;
import org.apache.commons.lang.StringEscapeUtils;
import org.openehr.jaxb.rm.CodePhrase;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.marand.thinkehr.adl.rm.RmObjectFactory.newCodePhrase;
import static com.marand.thinkehr.adl.rm.RmObjectFactory.newTerminologyId;

/**
 * @author markopi
 */
abstract class AbstractAdlTreeParser {
    protected final CommonTokenStream tokenStream;
    protected final CommonTree adlTree;
    protected final AdlTreeParserState state;
    protected final RmModel rmModel;

    AbstractAdlTreeParser(CommonTokenStream tokenStream, CommonTree adlTree, AdlTreeParserState state, RmModel rmModel) {
        this.tokenStream = tokenStream;
        this.adlTree = adlTree;
        this.state = state;
        this.rmModel = rmModel;
    }


    protected boolean isAdlV15() {
        return state.isAdlV15();

    }

    protected AdlTreeParserState getState() {
        return state;
    }

    @Nullable
    protected Token tokenOf(Tree tree) {
        int tokenStart = tree.getTokenStartIndex();
        if (tokenStart < 0) return null;
        return tokenStream.get(tokenStart);
    }

    protected String tokenName(Tree token) {
        return token == null ? "null" : AdlParser.tokenNames[token.getType()];
    }

    protected String unescapeString(String string) {
        if (string.startsWith("\"") && string.endsWith("\"")) {
            string = string.substring(1, string.length() - 1);
        }
        return StringEscapeUtils.unescapeJava(string);
    }


    protected void require(boolean condition, Tree location, String message, Object... parameters) {
        if (!condition) {
            throw new AdlTreeParserException(String.format(message, parameters), tokenStream.get(location.getTokenStartIndex()));
        }
    }

    protected void assertTokenType(Tree tree, int type, int... otherTypes) {
        boolean match = tree.getType() == type;
        if (!match) {
            for (int otherType : otherTypes) {
                if (tree.getType() == otherType) {
                    match = true;
                    break;
                }
            }
        }
        if (!match) {
            if (otherTypes.length == 0) {
                throw new AdlTreeParserException(String.format("Invalid token type. Expected: %s, found: %s",
                        AdlParser.tokenNames[type], tokenName(tree)), tokenStream.get(tree.getTokenStartIndex()));
            } else {
                List<String> tokens = new ArrayList<>(otherTypes.length + 1);
                tokens.add(AdlParser.tokenNames[type]);
                for (int otherType : otherTypes) {
                    tokens.add(AdlParser.tokenNames[otherType]);
                }
                throw new AdlTreeParserException(String.format("Invalid token type. Expected one of: %s, found: %s",
                        tokens, tokenName(tree)), tokenStream.get(tree.getTokenStartIndex()));
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected List<Tree> children(Tree tree) {
        List<Tree> children = (List) ((BaseTree) tree).getChildren();
        return children != null ? children : Collections.<Tree>emptyList();
    }

    @Nullable
    protected Tree childMaybe(Tree tree, int index, int assertChildType) {
        Tree result = tree.getChild(index);
        if (result == null) return null;
        assertTokenType(result, assertChildType);
        return result;
    }

    protected Tree child(Tree tree, int index, int assertChildType) {
        Tree result = childMaybe(tree, index, assertChildType);
        if (result == null) {
            throw new AdlTreeParserException(
                    String.format("Required child not found: index=%d, type=%s on parent", index, AdlParser.tokenNames[assertChildType]),
                    tokenOf(tree));
        }
        return result;
    }

    @Nullable
    protected Integer parseNullableInteger(@Nullable Tree tNumber) {
        if (tNumber == null) return null;
        if (tNumber.getType() == AdlParser.AST_NULL) return null;

        return parseInteger(tNumber);
    }

    protected int parseInteger(Tree tNumber) {
        if (tNumber.getType() == AdlParser.AST_NUMBER_CONTAINER) {
            tNumber = tNumber.getChild(0);
        }

        assertTokenType(tNumber, AdlParser.INTEGER, AdlParser.AST_NUMBER);
        try {
            return Integer.parseInt(collectText(tNumber));
        } catch (NumberFormatException e) {
            throw new RuntimeRecognitionException("Invalid integer", tNumber);
        }
    }

    @Nullable
    protected Float parseNullableFloat(@Nullable Tree tNumber) {
        if (tNumber == null) return null;
        if (tNumber.getType() == AdlParser.AST_NULL) return null;

        return parseFloat(tNumber);
    }

    protected float parseFloat(Tree tNumber) {
        if (tNumber.getType() == AdlParser.AST_NUMBER_CONTAINER) {
            tNumber = tNumber.getChild(0);
        }

        assertTokenType(tNumber, AdlParser.INTEGER, AdlParser.AST_NUMBER);
        try {
            return Float.parseFloat(collectText(tNumber));
        } catch (NumberFormatException e) {
            throw new RuntimeRecognitionException("Invalid float: " + collectText(tNumber), tNumber);
        }
    }

    @Nullable
    protected String collectString(@Nullable Tree tStringList) {
        if (tStringList == null) return null;
        if (tStringList.getType() == AdlParser.STRING) {
            return unescapeString(tStringList.getText());
        }

        List<String> stringList = collectStringList(tStringList);
        return stringList.get(0);
    }

    protected List<String> collectStringList(Tree tStringList) {
        if (tStringList == null) return null;
        assertTokenType(tStringList, AdlParser.AST_STRING_LIST);
        List<String> result = Lists.newArrayList();
        for (Tree tString : children(tStringList)) {
            result.add(unescapeString(tString.getText()));
        }
        return result;
    }

    @Nullable
    protected String collectText(Tree tree) {
        if (tree.getType() == AdlParser.AST_STRING_LIST && tree.getChildCount() == 1) {
            return tree.getChild(0).getText();
        }
        if (tree.getType() == AdlParser.AST_NULL) {
            return null;
        }
        // sometimes this is required to exclude container tags, such as <> in adlValue
        if (tree.getType() == AdlParser.AST_TEXT_CONTAINER) {
            return collectText(tree.getChild(0));
        }

        // walk through all contained tokens and concatenate them
        StringBuilder result = new StringBuilder();
        for (Token token : tokenStream.get(tree.getTokenStartIndex(), tree.getTokenStopIndex())) {
            result.append(token.getText());
        }
        return result.toString();
    }

    protected CodePhrase parseCodePhraseListSingleItem(Tree tCodePhraseList) {
        List<CodePhrase> phrases = parseCodePhraseList(tCodePhraseList);
        if (phrases.size() != 1) throw new AdlTreeParserException("Expected exactly one code phrase in list", tokenOf(tCodePhraseList));
        return phrases.get(0);
    }

    protected List<CodePhrase> parseCodePhraseList(Tree tCodePhraseList) {
        assertTokenType(tCodePhraseList, AdlParser.AST_CODE_PHRASE_LIST);
        List<CodePhrase> result = new ArrayList<>();

        for (Tree tCodePhrase : children(tCodePhraseList)) {
            result.add(parseCodePhrase(tCodePhrase));
        }
        return result;

    }

    protected CodePhrase parseCodePhrase(Tree tCodePhrase) {
        assertTokenType(tCodePhrase, AdlParser.AST_CODE_PHRASE);

        return newCodePhrase(
                newTerminologyId(checkNotNull(collectText(tCodePhrase.getChild(0)))),
                checkNotNull(collectText(tCodePhrase.getChild(1))));
    }

    protected void assertAdlV15(String feature, Tree location) {
        if (!isAdlV15()) {
            throw new AdlTreeParserException("Feature requires adl 1.5: " + feature, tokenOf(location));
        }
    }

}