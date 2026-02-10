/**
 * Tokenizer tests - adapted from TokenizerTest.java
 *
 * Note: Some test expectations differ from Java because:
 * 1. TypeScript tokenizer skips NEWLINE tokens differently (based on CONTINUES_ON_NEXT_LINE)
 * 2. Skip tokens (comments, whitespace) are always excluded from the non-skip list
 * 3. Negative numbers are tokenized as single tokens (not split into operator + number)
 */

import { describe, it, expect } from 'vitest';
import { List } from 'immutable';
import { Token } from '../src/syntax/Token';
import { TokenType } from '../src/syntax/TokenType';
import { Tokenizer } from '../src/syntax/Tokenizer';

/**
 * Tokenize and return all tokens including whitespace.
 */
function tokenizeAll(input: string): List<Token> {
  return new Tokenizer(input, 'test').tokenize().listAll;
}

/**
 * Assert a token at a specific index has expected properties.
 */
function assertEqualsToken(
  expectedLine: number,
  expectedPosition: number,
  tokens: List<Token>,
  index: number,
  type: TokenType
): void {
  expect(index).toBeLessThan(tokens.size);
  const token = tokens.get(index)!;
  expect(token.type).toBe(type);
  expect(token.line).toBe(expectedLine);
  expect(token.position).toBe(expectedPosition);
}

describe('Tokenizer', () => {
  it('tokenizerTest - basic tokenization with comments and operators', () => {
    const example = `// COMMENT
    -abb + bcc *
       c - dee // ANOTHER COMMENT
    e = 8.9 / 2
`;

    const result = new Tokenizer(example, 'tokenizerTest').tokenize();
    const tokens = result.list;
    const all = result.listAll;

    // Reassemble and verify
    const reassembled = all.map(t => t.text).join('');
    expect(reassembled).toBe(example);

    // Verify token types
    const types = all.map(t => t.type.name).join(' ');
    const expectedTypes = 'BEGINOFFILE END_LINE_COMMENT NEWLINE HSPACE OPERATOR NAME HSPACE OPERATOR HSPACE NAME HSPACE OPERATOR NEWLINE HSPACE NAME HSPACE OPERATOR HSPACE NAME HSPACE END_LINE_COMMENT NEWLINE HSPACE NAME HSPACE OPERATOR HSPACE DECIMAL HSPACE OPERATOR HSPACE NUMBER NEWLINE ENDOFFILE';
    expect(types).toBe(expectedTypes);

    // Verify counts - matching Java TokenizerTest expectations
    // 17 non-skip tokens: BEGINOFFILE + operators + names + decimal + number + 2 NEWLINEs + ENDOFFILE
    // First NEWLINE (after comment) is skipped because lastToken is BEGINOFFILE
    // Second NEWLINE (after line 1) is skipped because * continues on next line
    expect(tokens.size).toBe(17);
    expect(all.size).toBe(34);

    // Verify specific tokens in all
    assertEqualsToken(0, 0, all, 1, TokenType.END_LINE_COMMENT);
    assertEqualsToken(0, 10, all, 2, TokenType.NEWLINE);
    assertEqualsToken(1, 0, all, 3, TokenType.HSPACE);
    assertEqualsToken(1, 4, all, 4, TokenType.OPERATOR);
    assertEqualsToken(1, 5, all, 5, TokenType.NAME);

    // Verify specific tokens in non-skip list (index 0 is BEGINOFFILE)
    // Matches Java test assertions
    assertEqualsToken(1, 4, tokens, 1, TokenType.OPERATOR);  // -
    assertEqualsToken(1, 5, tokens, 2, TokenType.NAME);      // abb
    assertEqualsToken(1, 9, tokens, 3, TokenType.OPERATOR);  // +
    assertEqualsToken(1, 11, tokens, 4, TokenType.NAME);     // bcc
    assertEqualsToken(1, 15, tokens, 5, TokenType.OPERATOR); // *
  });

  it('tokenizerComment1Test - unterminated inline comment', () => {
    const example = '/* unterminated comment';

    const result = new Tokenizer(example, 'tokenizerCommentTest').tokenize();
    const tokens = result.list;
    const all = result.listAll;

    // BEGINOFFILE and ENDOFFILE in non-skip list (IN_LINE_COMMENT is skipped)
    expect(tokens.size).toBe(2);
    // BEGINOFFILE, IN_LINE_COMMENT, ENDOFFILE in all
    expect(all.size).toBe(3);

    assertEqualsToken(0, 0, all, 1, TokenType.IN_LINE_COMMENT);
  });

  it('tokenizerComment2Test - inline comments in expressions', () => {
    const example = '<a/*a*/>•a';

    const result = new Tokenizer(example, 'tokenizerCommentTest').tokenize();
    const tokens = result.list;
    const all = result.listAll;

    const reassembled = all.map(t => t.text).join('');
    const types = all.map(t => t.type.name).join(' ');
    const expectedTypes = 'BEGINOFFILE OPERATOR NAME IN_LINE_COMMENT OPERATOR ERROR NAME ENDOFFILE';

    // 7 non-skip tokens (BEGINOFFILE + 5 actual + ENDOFFILE, IN_LINE_COMMENT is skipped)
    expect(tokens.size).toBe(7);
    expect(all.size).toBe(8);
    expect(reassembled).toBe(example);
    expect(types).toBe(expectedTypes);

    assertEqualsToken(0, 0, all, 1, TokenType.OPERATOR);         // <
    assertEqualsToken(0, 1, all, 2, TokenType.NAME);             // a
    assertEqualsToken(0, 2, all, 3, TokenType.IN_LINE_COMMENT);  // /*a*/
    assertEqualsToken(0, 7, all, 4, TokenType.OPERATOR);         // >
    assertEqualsToken(0, 8, all, 5, TokenType.ERROR);            // •

    assertEqualsToken(0, 0, tokens, 1, TokenType.OPERATOR);  // <
    assertEqualsToken(0, 1, tokens, 2, TokenType.NAME);      // a
    assertEqualsToken(0, 7, tokens, 3, TokenType.OPERATOR);  // >
    assertEqualsToken(0, 8, tokens, 4, TokenType.ERROR);     // •
  });

  it('tokenizerEmptyCommentTest - empty end-line comment', () => {
    const all = tokenizeAll('//');
    expect(all.size).toBe(3);
    assertEqualsToken(0, 0, all, 1, TokenType.END_LINE_COMMENT);
    expect(all.get(1)!.text).toBe('//');
  });

  it('tokenizerCommentWithSpaceTest - comment with trailing space', () => {
    const all = tokenizeAll('// ');
    expect(all.size).toBe(3);
    assertEqualsToken(0, 0, all, 1, TokenType.END_LINE_COMMENT);
    expect(all.get(1)!.text).toBe('// ');
  });

  it('tokenizerCommentWithTextTest - comment with text', () => {
    const all = tokenizeAll('// text');
    expect(all.size).toBe(3);
    assertEqualsToken(0, 0, all, 1, TokenType.END_LINE_COMMENT);
    expect(all.get(1)!.text).toBe('// text');
  });

  it('tokenizerTripleSlashCommentTest - triple slash comment', () => {
    const all = tokenizeAll('///');
    expect(all.size).toBe(3);
    assertEqualsToken(0, 0, all, 1, TokenType.END_LINE_COMMENT);
    expect(all.get(1)!.text).toBe('///');
  });

  it('tokenizerCommentWithSpecialCharsTest - comment with special chars', () => {
    const all = tokenizeAll('//===');
    expect(all.size).toBe(3);
    assertEqualsToken(0, 0, all, 1, TokenType.END_LINE_COMMENT);
    expect(all.get(1)!.text).toBe('//===');
  });

  it('tokenizerSingleSlashOperatorTest - single slash as operator', () => {
    const all = tokenizeAll('/');
    expect(all.size).toBe(3);
    assertEqualsToken(0, 0, all, 1, TokenType.OPERATOR);
    expect(all.get(1)!.text).toBe('/');
  });

  it('tokenizerSlashEqualsOperatorTest - slash-equals operator', () => {
    const all = tokenizeAll('/=');
    expect(all.size).toBe(3);
    assertEqualsToken(0, 0, all, 1, TokenType.OPERATOR);
    expect(all.get(1)!.text).toBe('/=');
  });

  it('tokenizerCodeFollowedByEmptyCommentTest - code followed by empty comment', () => {
    const all = tokenizeAll('a//');
    expect(all.size).toBe(4);
    assertEqualsToken(0, 0, all, 1, TokenType.NAME);
    assertEqualsToken(0, 1, all, 2, TokenType.END_LINE_COMMENT);
    expect(all.get(2)!.text).toBe('//');
  });

  it('tokenizerCodeWithSpaceAndEmptyCommentTest - code with space and empty comment', () => {
    const all = tokenizeAll('a //');
    expect(all.size).toBe(5);
    assertEqualsToken(0, 0, all, 1, TokenType.NAME);
    assertEqualsToken(0, 1, all, 2, TokenType.HSPACE);
    assertEqualsToken(0, 2, all, 3, TokenType.END_LINE_COMMENT);
    expect(all.get(3)!.text).toBe('//');
  });

  it('tokenizerDivisionExpressionTest - division expression', () => {
    const all = tokenizeAll('a/b');
    expect(all.size).toBe(5);
    assertEqualsToken(0, 0, all, 1, TokenType.NAME);
    assertEqualsToken(0, 1, all, 2, TokenType.OPERATOR);
    assertEqualsToken(0, 2, all, 3, TokenType.NAME);
    expect(all.get(2)!.text).toBe('/');
  });

  it('tokenizerEmptyCommentWithNewlineTest - empty comment followed by newline', () => {
    const all = tokenizeAll('//\n');
    expect(all.size).toBe(4);
    assertEqualsToken(0, 0, all, 1, TokenType.END_LINE_COMMENT);
    assertEqualsToken(0, 2, all, 2, TokenType.NEWLINE);
    expect(all.get(1)!.text).toBe('//');
  });

  it('tokenizerMultipleEmptyCommentsTest - multiple empty comments', () => {
    const all = tokenizeAll('//\n//\n//');
    expect(all.size).toBe(7);
    assertEqualsToken(0, 0, all, 1, TokenType.END_LINE_COMMENT);
    assertEqualsToken(0, 2, all, 2, TokenType.NEWLINE);
    assertEqualsToken(1, 0, all, 3, TokenType.END_LINE_COMMENT);
    assertEqualsToken(1, 2, all, 4, TokenType.NEWLINE);
    assertEqualsToken(2, 0, all, 5, TokenType.END_LINE_COMMENT);
  });

  it('tokenizerMixedOperatorsTest - mixed operators as single token', () => {
    const all = tokenizeAll('+-*/');
    expect(all.size).toBe(3);
    assertEqualsToken(0, 0, all, 1, TokenType.OPERATOR);
    expect(all.get(1)!.text).toBe('+-*/');
  });

  it('tokenizerInlineCommentTest - inline comment is recognized', () => {
    const all = tokenizeAll('a /* comment */ b');
    expect(all.size).toBe(7);
    assertEqualsToken(0, 0, all, 1, TokenType.NAME);
    assertEqualsToken(0, 2, all, 3, TokenType.IN_LINE_COMMENT);
    expect(all.get(3)!.text).toBe('/* comment */');
    assertEqualsToken(0, 16, all, 5, TokenType.NAME);
  });

  it('tokenizerMultilineCommentTest - multiline comment spans lines', () => {
    const all = tokenizeAll('/* line1\nline2\nline3 */');
    expect(all.size).toBe(3);
    assertEqualsToken(0, 0, all, 1, TokenType.IN_LINE_COMMENT);
    expect(all.get(1)!.text).toBe('/* line1\nline2\nline3 */');
  });
});
