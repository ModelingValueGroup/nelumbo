/**
 * Token types for Nelumbo lexer.
 * Ported from Java: org.modelingvalue.nelumbo.syntax.TokenType
 */

export enum TokenTypeFlag {
  SKIP = 1,                    // Non-semantic token (comments, whitespace)
  CONTINUES_ON_NEXT_LINE = 2,  // Newlines after these tokens are ignored
  VARIABLE_CONTENT = 4,        // Token content is variable/meaningful
  LAYOUT = 8,                  // Layout/formatting token
  NOT_MATCHED = 16,            // Not matched by lexer (synthetic)
}

export interface TokenTypeDef {
  name: string;
  pattern: RegExp | null;
  flags: number;
}

// Token type definitions
const tokenTypeDefs: TokenTypeDef[] = [
  // Matched tokens (in priority order - comments must come before OPERATOR)
  { name: 'SINGLEQUOTE', pattern: /^'/, flags: 0 },
  { name: 'SEMICOLON', pattern: /^;/, flags: 0 },
  { name: 'COMMA', pattern: /^,/, flags: TokenTypeFlag.CONTINUES_ON_NEXT_LINE },
  { name: 'LEFT', pattern: /^[(\[{]/, flags: TokenTypeFlag.CONTINUES_ON_NEXT_LINE | TokenTypeFlag.VARIABLE_CONTENT },
  { name: 'RIGHT', pattern: /^[)\]}]/, flags: TokenTypeFlag.VARIABLE_CONTENT },
  { name: 'STRING', pattern: /^"([^"\\]|\\[\s\S])*"/, flags: TokenTypeFlag.VARIABLE_CONTENT },
  { name: 'DECIMAL', pattern: /^-?[0-9]+\.[0-9]+/, flags: TokenTypeFlag.VARIABLE_CONTENT },
  { name: 'NUMBER', pattern: /^-?[0-9]+(#[0-9a-zA-Z]+)?/, flags: TokenTypeFlag.VARIABLE_CONTENT },
  { name: 'NAME', pattern: /^[a-zA-Z_][0-9a-zA-Z_]*/, flags: TokenTypeFlag.VARIABLE_CONTENT },
  { name: 'NEWLINE', pattern: /^(\r\n|\r|\n)/, flags: TokenTypeFlag.CONTINUES_ON_NEXT_LINE | TokenTypeFlag.LAYOUT },
  { name: 'HSPACE', pattern: /^[ \t]+/, flags: TokenTypeFlag.SKIP },
  { name: 'END_LINE_COMMENT', pattern: /^\/\/[^\r\n]*/, flags: TokenTypeFlag.SKIP | TokenTypeFlag.VARIABLE_CONTENT },
  { name: 'IN_LINE_COMMENT', pattern: /^\/\*[\s\S]*?(\*\/|$)/, flags: TokenTypeFlag.SKIP | TokenTypeFlag.VARIABLE_CONTENT },
  { name: 'OPERATOR', pattern: /^(?!\/\/|\/\*)[~!@#$%^&*=+|:<>.?/-]+/, flags: TokenTypeFlag.CONTINUES_ON_NEXT_LINE | TokenTypeFlag.VARIABLE_CONTENT },
  { name: 'ERROR', pattern: /^./, flags: 0 },

  // Synthetic tokens (not matched by lexer)
  { name: 'BEGINOFFILE', pattern: null, flags: TokenTypeFlag.NOT_MATCHED | TokenTypeFlag.LAYOUT },
  { name: 'ENDOFFILE', pattern: null, flags: TokenTypeFlag.NOT_MATCHED | TokenTypeFlag.LAYOUT },
  { name: 'ENDOFLINE', pattern: null, flags: TokenTypeFlag.NOT_MATCHED | TokenTypeFlag.LAYOUT },

  // Semantic tokens (for syntax highlighting, not matched by lexer)
  { name: 'VARIABLE', pattern: null, flags: TokenTypeFlag.NOT_MATCHED | TokenTypeFlag.VARIABLE_CONTENT },
  { name: 'KEYWORD', pattern: null, flags: TokenTypeFlag.NOT_MATCHED },
  { name: 'TYPE', pattern: null, flags: TokenTypeFlag.NOT_MATCHED },
  { name: 'META_OPERATOR', pattern: null, flags: TokenTypeFlag.NOT_MATCHED },
];

export class TokenType {
  private static readonly types: Map<string, TokenType> = new Map();
  private static readonly matchedTypes: TokenType[] = [];

  readonly name: string;
  readonly pattern: RegExp | null;
  readonly flags: number;

  private constructor(def: TokenTypeDef) {
    this.name = def.name;
    this.pattern = def.pattern;
    this.flags = def.flags;
  }

  static initialize(): void {
    for (const def of tokenTypeDefs) {
      const type = new TokenType(def);
      TokenType.types.set(def.name, type);
      if (def.pattern !== null) {
        TokenType.matchedTypes.push(type);
      }
    }
  }

  static get(name: string): TokenType {
    const type = TokenType.types.get(name);
    if (!type) {
      throw new Error(`Unknown token type: ${name}`);
    }
    return type;
  }

  static getMatchedTypes(): TokenType[] {
    return TokenType.matchedTypes;
  }

  // Common token types as static properties
  static SINGLEQUOTE: TokenType;
  static SEMICOLON: TokenType;
  static COMMA: TokenType;
  static LEFT: TokenType;
  static RIGHT: TokenType;
  static STRING: TokenType;
  static DECIMAL: TokenType;
  static NUMBER: TokenType;
  static NAME: TokenType;
  static OPERATOR: TokenType;
  static NEWLINE: TokenType;
  static HSPACE: TokenType;
  static END_LINE_COMMENT: TokenType;
  static IN_LINE_COMMENT: TokenType;
  static ERROR: TokenType;
  static BEGINOFFILE: TokenType;
  static ENDOFFILE: TokenType;
  static ENDOFLINE: TokenType;
  static VARIABLE: TokenType;
  static KEYWORD: TokenType;
  static TYPE: TokenType;
  static META_OPERATOR: TokenType;

  isSkip(): boolean {
    return (this.flags & TokenTypeFlag.SKIP) !== 0;
  }

  isContinuesOnNextLine(): boolean {
    return (this.flags & TokenTypeFlag.CONTINUES_ON_NEXT_LINE) !== 0;
  }

  isLayout(): boolean {
    return (this.flags & TokenTypeFlag.LAYOUT) !== 0;
  }

  isVariableContent(): boolean {
    return (this.flags & TokenTypeFlag.VARIABLE_CONTENT) !== 0;
  }

  isNotMatched(): boolean {
    return (this.flags & TokenTypeFlag.NOT_MATCHED) !== 0;
  }

  /**
   * Determine the token type for a given text string.
   */
  static of(text: string): TokenType {
    for (const type of TokenType.matchedTypes) {
      if (type.pattern && type.pattern.test(text)) {
        // Verify it matches the whole string
        const match = text.match(type.pattern);
        if (match && match[0] === text) {
          return type;
        }
      }
    }
    return TokenType.ERROR;
  }
}

// Initialize token types
TokenType.initialize();

// Set static properties
TokenType.SINGLEQUOTE = TokenType.get('SINGLEQUOTE');
TokenType.SEMICOLON = TokenType.get('SEMICOLON');
TokenType.COMMA = TokenType.get('COMMA');
TokenType.LEFT = TokenType.get('LEFT');
TokenType.RIGHT = TokenType.get('RIGHT');
TokenType.STRING = TokenType.get('STRING');
TokenType.DECIMAL = TokenType.get('DECIMAL');
TokenType.NUMBER = TokenType.get('NUMBER');
TokenType.NAME = TokenType.get('NAME');
TokenType.OPERATOR = TokenType.get('OPERATOR');
TokenType.NEWLINE = TokenType.get('NEWLINE');
TokenType.HSPACE = TokenType.get('HSPACE');
TokenType.END_LINE_COMMENT = TokenType.get('END_LINE_COMMENT');
TokenType.IN_LINE_COMMENT = TokenType.get('IN_LINE_COMMENT');
TokenType.ERROR = TokenType.get('ERROR');
TokenType.BEGINOFFILE = TokenType.get('BEGINOFFILE');
TokenType.ENDOFFILE = TokenType.get('ENDOFFILE');
TokenType.ENDOFLINE = TokenType.get('ENDOFLINE');
TokenType.VARIABLE = TokenType.get('VARIABLE');
TokenType.KEYWORD = TokenType.get('KEYWORD');
TokenType.TYPE = TokenType.get('TYPE');
TokenType.META_OPERATOR = TokenType.get('META_OPERATOR');
