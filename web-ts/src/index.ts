/**
 * Nelumbo TypeScript - Main exports
 */

// Tokenizer layer
export { Token } from './syntax/Token';
export { TokenType, TokenTypeFlag } from './syntax/TokenType';
export { Tokenizer } from './syntax/Tokenizer';
export type { TokenizerResult } from './syntax/Tokenizer';

// Core layer
export type { AstElement } from './AstElement';
export { AstElementUtil } from './AstElement';
export { Type, DEFAULT_GROUP, TOP_GROUP, PATTERN_GROUP } from './Type';
export { Variable } from './Variable';
export { Node } from './Node';
export type { ReplaceFunction } from './Node';

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
export { InferResult } from './InferResult';
export { InferContext } from './InferContext';
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
export { MatchState } from './MatchState';
export { isEvaluatable } from './Evaluatable';
export type { Evaluatable } from './Evaluatable';
export { KnowledgeBase } from './KnowledgeBase';
export type { ParseExceptionHandler } from './KnowledgeBase';
export { Rule, InconsistencyException } from './Rule';
export { Fact } from './Fact';
export { Query } from './Query';
export { Transform } from './Transform';
export type { NList } from './Transform';
export { Import } from './Import';

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
