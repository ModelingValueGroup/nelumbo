package org.modelingvalue.nelumbo.syntax;

import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.impl.StructureImpl;
import org.modelingvalue.nelumbo.syntax.Token.TokenType;

public class Parser {

    private final LinkedList<Token>              tokens;
    private final Map<TokenType, PrefixParselet> prefixParselets;
    private final Map<TokenType, InfixParselet>  infixParselets;

    public Parser(LinkedList<Token> tokens) {
        this.tokens = tokens;
        this.prefixParselets = new HashMap<>();
        this.infixParselets = new HashMap<>();
        register(TokenType.OPERATOR, UnaryOperatorParselet.INSTANCE);
        register(TokenType.OPERATOR, BinaryOperatorParselet.INSTANCE);
    }

    public void register(TokenType token, PrefixParselet parselet) {
        prefixParselets.put(token, parselet);
    }

    public void register(TokenType token, InfixParselet parselet) {
        infixParselets.put(token, parselet);
    }

    public StructureImpl<?> parseExpression(int precedence) throws ParseException {
        Token token = tokens.poll();
        PrefixParselet prefix = prefixParselets.get(token.type());
        if (prefix == null) {
            throw new ParseException("Could not parse \"" + token.text() + "\" at position " + token.position() + ".", token.position());
        }
        StructureImpl<?> left = prefix.parse(this, token);
        while (precedence < precedence()) {
            token = tokens.poll();
            InfixParselet infix = infixParselets.get(token.type());
            left = infix.parse(this, left, token);
        }
        return left;
    }

    public List<StructureImpl<?>> parseExpression() throws ParseException {
        List<StructureImpl<?>> result = List.of();
        while (!tokens.isEmpty()) {
            while (tokens.peek() != null && tokens.peek().type() == TokenType.V) {
                tokens.poll();
            }
            if (!tokens.isEmpty()) {
                result = result.add(parseExpression(0));
                if (tokens.peek() == null && tokens.peek().type() != TokenType.V) {
                    break;
                }
            }
        }
        if (!tokens.isEmpty()) {
            Token token = tokens.peek();
            throw new ParseException("Could not parse \"" + token.text() + "\" at position " + token.position() + ".", token.position());
        }
        return result;
    }

    private int precedence() throws ParseException {
        Token token = tokens.peek();
        if (token != null) {
            InfixParselet parser = infixParselets.get(token.type());
            if (parser != null) {
                return parser.precedence(token);
            }
        }
        return 0;
    }

}
