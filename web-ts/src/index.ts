/**
 * Nelumbo TypeScript - Main exports
 */

// Tokenizer layer
export { Token } from './Token';
export { TokenType, TokenTypeFlag } from './TokenType';
export { Tokenizer } from './Tokenizer';
export type { TokenizerResult } from './Tokenizer';

// Core layer
export type { AstElement } from './core/AstElement';
export { AstElementUtil } from './core/AstElement';
export { Type, DEFAULT_GROUP, TOP_GROUP, PATTERN_GROUP } from './core/Type';
export { Variable } from './core/Variable';
export { Node } from './core/Node';
export type { ReplaceFunction } from './core/Node';

// Pattern layer
export { Pattern } from './patterns/Pattern';
export { SequencePattern } from './patterns/SequencePattern';
export { AlternationPattern } from './patterns/AlternationPattern';
export { OptionalPattern, Optional, some, none } from './patterns/OptionalPattern';
export { RepetitionPattern } from './patterns/RepetitionPattern';
export { TokenTextPattern } from './patterns/TokenTextPattern';
export { TokenTypePattern } from './patterns/TokenTypePattern';
export { NodeTypePattern } from './patterns/NodeTypePattern';
export { Functor } from './patterns/Functor';
export type { NodeConstructor } from './patterns/Functor';

// Syntax layer
export { ParseException } from './syntax/ParseException';
export { createParseContext } from './syntax/ParseContext';
export type { ParseContext } from './syntax/ParseContext';
export { ParseState } from './syntax/ParseState';
export { PatternResult } from './syntax/PatternResult';
export { ParserResult } from './syntax/ParserResult';
export { Parser, parseString, parseTokenizerResult } from './syntax/Parser';

// Logic layer
export { InferResult } from './logic/InferResult';
export { InferContext } from './logic/InferContext';
export { Predicate } from './logic/Predicate';
export { NBoolean } from './logic/NBoolean';
export { Equal } from './logic/Equal';
export { CompoundPredicate } from './logic/CompoundPredicate';
export { BinaryPredicate } from './logic/BinaryPredicate';
export { And } from './logic/And';
export { Or } from './logic/Or';
export { Not } from './logic/Not';
export { Quantifier } from './logic/Quantifier';
export { ExistentialQuantifier } from './logic/ExistentialQuantifier';
export { UniversalQuantifier } from './logic/UniversalQuantifier';
export { When } from './logic/When';

// Knowledge base layer
export { MatchState } from './kb/MatchState';
export { isEvaluatable } from './kb/Evaluatable';
export type { Evaluatable } from './kb/Evaluatable';
export { KnowledgeBase } from './kb/KnowledgeBase';
export type { ParseExceptionHandler } from './kb/KnowledgeBase';
export { Rule, InconsistencyException } from './kb/Rule';
export { Fact } from './kb/Fact';
export { Query } from './kb/Query';
export { Transform } from './kb/Transform';
export type { NList } from './kb/Transform';
export { Import } from './kb/Import';

// Editor layer
export {
  Editor,
  EditorWindow,
  WindowManager,
  EditorTheme,
  SyntaxHighlighter,
  MessagePane,
  TreeViewer,
  KnowledgeBaseViewer,
  EditorImportResolver,
  editorImportResolver,
  showTreeViewerDialog,
  showKnowledgeBaseViewerDialog,
} from './editor';
export type {
  EditorConfig,
  EditorWindowState,
  EditorChangeHandler,
  WindowListChangeHandler,
  ExampleResource,
  ColorScheme,
  HighlightSpan,
  Message,
  MessageType,
  HighlightInfo,
  ImportChangeListener,
} from './editor';
