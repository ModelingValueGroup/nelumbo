/**
 * Type class representing types in the Nelumbo type system.
 * @JAVA_REF org.modelingvalue.nelumbo.Type
 */

import { List, Set, Map } from 'immutable';
import { TokenType } from './syntax/TokenType';
import type { AstElement } from './AstElement';
import { Node } from './Node';
import type { Variable } from './Variable';
import type { Functor } from './patterns/Functor';

/**
 * Type groups for parsing.
 */
export const DEFAULT_GROUP = '_';
export const TOP_GROUP = 'TOP';
export const PATTERN_GROUP = 'PATTERN';

/**
 * Sentinel object for Type equality comparison.
 * All Types use this same object as their typeForEquals(),
 * so constructor check in Node.equals differentiates Type vs Node.
 */
const EQUALS_TYPE = {
  equals(other: unknown): boolean { return this === other; },
  hashCode(): number { return 0; },
  toString(): string { return 'Type'; }
};

/**
 * Type class - represents types in Nelumbo.
 * Types form a hierarchy and can have modifiers like literal/function.
 *
 * Extends Node with data layout: [TYPE, elements, name, supers, group, element?]
 * where get(0)=name, get(1)=supers, get(2)=group, get(3)=element (for collections)
 */
export class Type extends Node {
  // Cached derived types
  private _list: Type | null = null;
  private _set: Type | null = null;
  private _literal: Type | null = null;
  private _function: Type | null = null;
  private _allSupers: List<Type> | null = null;

  // Private constructor - use factory methods
  private constructor(typeOrFunctor: Type | Functor, elements: List<AstElement>, ...args: unknown[]) {
    super(typeOrFunctor, elements, ...args);
  }

  // --- Node overrides ---

  override typeOrFunctor(): Type | Functor {
    return Type.TYPE;
  }

  protected override typeForEquals(): unknown {
    return EQUALS_TYPE;
  }

  protected override struct(data: unknown[], declaration?: Node): Type {
    const t = Object.create(Type.prototype) as Type;
    (t as unknown as { _data: unknown[] })._data = data;
    (t as unknown as { _declaration: Node })._declaration = declaration ?? this._declaration;
    return t;
  }

  override set(i: number, ...a: unknown[]): Type {
    return super.set(i, ...a) as Type;
  }

  override setFunctor(functor: Functor): Type {
    return super.setFunctor(functor) as Type;
  }

  override setAstElements(elements: List<AstElement>): Type {
    return super.setAstElements(elements) as Type;
  }

  override setBinding(vars: Map<Variable, unknown>, declaration?: Node): Type {
    if (this.isCollection()) {
      const v = this.element().variable();
      if (v !== null) {
        const elt = vars.get(v);
        if (elt instanceof Node && elt.isType()) {
          return this.setElement(elt as Type).setBinding(vars, declaration);
        }
      }
    }
    return super.setBinding(vars, declaration) as Type;
  }

  override variable(): Variable | null {
    const type = this.get(0);
    if (Set.isSet(type)) {
      for (const t of type as Set<Type>) {
        const v = t.variable();
        if (v !== null) return v;
      }
    }
    return this.isVariable(type) ? type as unknown as Variable : null;
  }

  override toString(previous?: TokenType[]): string {
    if (previous) {
      const prevType = previous[0];
      if (prevType === TokenType.NAME || prevType === TokenType.NUMBER || prevType === TokenType.DECIMAL) {
        previous[0] = TokenType.NAME;
        return ' ' + this.name();
      }
    }
    return this.name();
  }

  override isType(): boolean {
    return true;
  }

  // --- Type-specific methods ---

  private isVariable(val: unknown): val is Variable {
    return val !== null && typeof val === 'object' && 'type' in val && 'name' in val && !('_data' in val);
  }

  /**
   * Get the element type for collection types.
   */
  element(): Type {
    if (this.isCollection()) {
      return this.get(3) as Type;
    }
    return this;
  }

  /**
   * Set the element type for collection types.
   */
  setElement(element: Type): Type {
    let supers = this.supers().map(s => s.isCollection() ? s.setElement(element) : s);
    for (const s of element.supers()) {
      supers = supers.add(this.setElement(s));
    }
    return this.set(3, element).set(1, supers);
  }

  /**
   * Get the group name.
   */
  group(): string {
    return this.get(2) as string;
  }

  /**
   * Set the group name.
   */
  setGroup(group: string): Type {
    return this.set(2, group);
  }

  /**
   * Get the set of types for a "many" type.
   */
  many(): Set<Type> {
    const val = this.get(0);
    if (Set.isSet(val)) {
      return val as Set<Type>;
    }
    throw new Error('Not a many type');
  }

  /**
   * Check if this is a collection type.
   */
  isCollection(): boolean {
    return this.length() === 4;
  }

  /**
   * Check if this is a "many" type (union).
   */
  isMany(): boolean {
    return Set.isSet(this.get(0));
  }

  /**
   * Get or create the function version of this type.
   */
  function(): Type {
    if (this.isFunction()) {
      return this;
    }
    if (this._function == null) {
      this._function = this.equals(Type.OBJECT)
        ? Type.FUNCTION
        : Type.many(this, Type.FUNCTION);
    }
    return this._function;
  }

  /**
   * Get the non-function base type.
   */
  nonFunction(): Type {
    if (!this.isFunction()) {
      return this;
    }
    const first = this.supers().find(s => !s.equals(Type.FUNCTION));
    if (first === undefined) {
      throw new Error('No non-function supertype for ' + this.name());
    }
    return first;
  }

  /**
   * Check if this type is a function type.
   */
  isFunction(): boolean {
    return Type.FUNCTION.isAssignableFrom(this);
  }

  /**
   * Get the non-literal base type.
   */
  nonLiteral(): Type {
    if (!this.isLiteral()) {
      return this;
    }
    const first = this.supers().find(s => !s.equals(Type.LITERAL));
    if (first === undefined) {
      throw new Error('No non-literal supertype for ' + this.name());
    }
    return first;
  }

  /**
   * Get or create the literal version of this type.
   */
  literal(): Type {
    if (this.isLiteral()) {
      return this;
    }
    if (this._literal == null) {
      this._literal = this.equals(Type.OBJECT)
        ? Type.LITERAL
        : Type.many(this, Type.LITERAL);
    }
    return this._literal;
  }

  /**
   * Check if this type is a literal type.
   */
  isLiteral(): boolean {
    return Type.LITERAL.isAssignableFrom(this);
  }

  /**
   * Get or create the list version of this type.
   */
  list(group?: string): Type {
    const g = group ?? this.group();
    if (this._list == null) {
      this._list = Type.LIST.setElement(this);
    }
    if (g !== this.group()) {
      return this._list.setGroup(g);
    }
    return this._list;
  }

  /**
   * Get or create the set version of this type.
   */
  asSet(group?: string): Type {
    const g = group ?? this.group();
    if (this._set == null) {
      this._set = Type.SET.setElement(this);
    }
    if (g !== this.group()) {
      return this._set.setGroup(g);
    }
    return this._set;
  }

  /**
   * Get the TokenType if this type wraps one.
   */
  tokenType(): TokenType | null {
    let type = this.get(0);
    if (this.isVariable(type)) {
      type = (type as unknown as Variable).type().tokenType();
    }
    return type instanceof TokenType ? type : null;
  }

  /**
   * Get the type name.
   */
  name(): string {
    const rawName = this.rawName();
    if (this.isCollection()) {
      return rawName + '<' + this.element().name() + '>';
    }
    return rawName;
  }

  /**
   * Get the raw type name (without collection suffix).
   */
  rawName(): string {
    const type = this.get(0);
    if (Set.isSet(type)) {
      const s = type as Set<Type>;
      const names = s.map(t => t.name()).sort().join(',');
      return '(' + names + ')';
    } else if (type instanceof TokenType) {
      return type.name;
    } else if (this.isVariable(type)) {
      return (type as unknown as Variable).name();
    } else if (typeof type === 'function') {
      return '$' + (type as { name: string }).name;
    }
    return type as string;
  }

  /**
   * Get the JavaScript class if this type wraps one.
   */
  clss(): (new (...args: unknown[]) => unknown) | null {
    const type = this.get(0);
    return typeof type === 'function' ? type as new (...args: unknown[]) => unknown : null;
  }

  /**
   * Rewrite this type to another type, preserving modifiers.
   */
  rewrite(type: Type): Type {
    if (this.isLiteral()) {
      return type.literal();
    } else if (this.isFunction()) {
      return type.function();
    }
    return type;
  }

  /**
   * Get the direct supertypes.
   */
  supers(): Set<Type> {
    return this.get(1) as Set<Type>;
  }

  /**
   * Get all supertypes including transitive.
   */
  allSupers(): List<Type> {
    if (this._allSupers == null) {
      let pre = List<Type>();
      let post = List<Type>([this]);

      while (post.size > pre.size) {
        const i = pre.size;
        pre = post;
        for (let j = i; j < pre.size; j++) {
          const t = pre.get(j)!;
          for (const s of t.supers()) {
            if (!post.includes(s)) {
              post = post.push(s);
            }
          }
        }
      }

      this._allSupers = post;
    }
    return this._allSupers;
  }

  /**
   * Check if this type is assignable from another type.
   */
  isAssignableFrom(type: Type): boolean {
    if (this.isMany()) {
      return this.many().every(s => s.isAssignableFrom(type));
    }
    for (const s of type.allSupers()) {
      if (this.equals(s)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check if this type is assignable from a JavaScript class.
   */
  isAssignableFromClass(type: new (...args: unknown[]) => unknown): boolean {
    const clss = this.clss();
    return clss !== null && (clss === type || Object.prototype.isPrototypeOf.call(clss.prototype, type.prototype));
  }

  // --- Static factory methods ---

  /**
   * Create a Type from raw data array.
   * Data layout: [TYPE, elements, name, supers, group, element?]
   */
  static createType(name: unknown, supers: Set<Type>, group: string, elements?: List<AstElement>, element?: Type): Type {
    const elems = elements ?? List<AstElement>();
    const data = element !== undefined
      ? [Type.TYPE, elems, name, supers, group, element]
      : [Type.TYPE, elems, name, supers, group];
    const t = Object.create(Type.prototype) as Type;
    (t as unknown as { _data: unknown[] })._data = data;
    (t as unknown as { _declaration: Node })._declaration = t;
    if (elems.size > 0) {
      t.init(elems);
    }
    return t;
  }

  /**
   * Create a Type with null typeOrFunctor slot (for bootstrap before TYPE exists).
   */
  static createBootstrapType(name: unknown, supers: Set<Type>, group: string): Type {
    const data = [null, List<AstElement>(), name, supers, group];
    const t = Object.create(Type.prototype) as Type;
    (t as unknown as { _data: unknown[] })._data = data;
    (t as unknown as { _declaration: Node })._declaration = t;
    return t;
  }

  /**
   * Create a type from a JavaScript class.
   */
  static fromClass(clss: new (...args: unknown[]) => unknown, ...supers: Type[]): Type {
    return Type.createBootstrapType(
      clss,
      supers.length === 0 ? Set<Type>() : Set(supers),
      supers.length > 0 ? supers[0].group() : DEFAULT_GROUP,
    );
  }

  /**
   * Create a named type with optional supers.
   */
  static named(name: string, ...supers: Type[]): Type {
    const superSet = supers.length === 0
      ? Set<Type>([Type.OBJECT])
      : Set(supers);
    return Type.createType(
      name,
      superSet,
      supers.length > 0 ? supers[0].group() : DEFAULT_GROUP,
    );
  }

  /**
   * Create a named type with specific group.
   */
  static namedWithGroup(name: string, group: string, ...supers: Type[]): Type {
    const superSet = supers.length === 0
      ? Set<Type>([Type.OBJECT])
      : Set(supers);
    return Type.createType(name, superSet, group);
  }

  /**
   * Create a type from a TokenType.
   */
  static fromTokenType(tokenType: TokenType): Type {
    return Type.createType(tokenType, Set<Type>(), DEFAULT_GROUP);
  }

  /**
   * Create a type from a Variable.
   */
  static fromVariable(variable: Variable): Type {
    return Type.createType(variable, Set<Type>([Type.OBJECT]), DEFAULT_GROUP, List<AstElement>([variable as AstElement]));
  }

  /**
   * Create a type with elements (AST elements from parsing).
   */
  static withElements(elements: List<AstElement>, name: string, supers: Set<Type>, group: string, element?: Type): Type {
    return Type.createType(name, supers, group, elements, element);
  }

  /**
   * Create a "many" type (union of multiple types).
   */
  static many(type1: Type, type2: Type): Type {
    let supers = Set<Type>([type1, type2]);

    // Add cross-products of supers
    for (const s1 of type1.supers().filter(s => !s.equals(Type.OBJECT))) {
      supers = supers.add(Type.many(s1, type2));
    }
    for (const s2 of type2.supers().filter(s => !s.equals(Type.OBJECT))) {
      supers = supers.add(Type.many(type1, s2));
    }

    return Type.createType(supers, supers, type1.group());
  }

  // Predefined types (initialized after class definition)
  static readonly $OBJECT: Type = Type.fromClass(Object);
  static readonly $STRING: Type = Type.fromClass(String, Type.$OBJECT);

  static OBJECT: Type;
  static TYPE: Type;
  static FUNCTION: Type;
  static LITERAL: Type;
  static ROOT: Type;
  static BOOLEAN: Type;
  static FACT_TYPE: Type;
  static VARIABLE: Type;
  static RULE: Type;
  static FUNCTOR: Type;
  static FACT: Type;
  static PATTERN: Type;
  static QUERY: Type;
  static TRANSFORM: Type;
  static IMPORT: Type;
  static COLLECTION: Type;
  static SET: Type;
  static LIST: Type;
  static STRING: Type;
  static INTEGER: Type;
  static DECIMAL: Type;
  static NUMBER: Type;

  static predefined(): List<Type> {
    return List([
      Type.OBJECT,
      Type.TYPE,
      Type.FUNCTION,
      Type.LITERAL,
      Type.ROOT,
      Type.BOOLEAN,
      Type.FACT_TYPE,
      Type.VARIABLE,
      Type.RULE,
      Type.FUNCTOR,
      Type.FACT,
      Type.PATTERN,
      Type.QUERY,
      Type.TRANSFORM,
      Type.IMPORT,
      Type.COLLECTION,
      Type.SET,
      Type.LIST,
    ]);
  }
}

// Initialize predefined types
(function initializeTypes() {
  // Create OBJECT first - bootstrap type (TYPE doesn't exist yet)
  Type.OBJECT = Type.createBootstrapType('Object', Set<Type>([Type.$OBJECT]), DEFAULT_GROUP) as Type;

  // Create TYPE early so subsequent types can reference it
  Type.TYPE = Type.named('Type', Type.OBJECT);

  // Now create other types
  Type.FUNCTION = Type.named('Function', Type.OBJECT);
  Type.LITERAL = Type.named('Literal', Type.OBJECT);
  Type.ROOT = Type.named('Root', Type.OBJECT);
  Type.BOOLEAN = Type.named('Boolean', Type.OBJECT);
  Type.FACT_TYPE = Type.named('FactType', Type.BOOLEAN);
  Type.VARIABLE = Type.named('Variable', Type.OBJECT);
  Type.RULE = Type.named('Rule', Type.ROOT);
  Type.FUNCTOR = Type.named('Functor', Type.ROOT);
  Type.FACT = Type.named('Fact', Type.ROOT);
  Type.PATTERN = Type.namedWithGroup('Pattern', PATTERN_GROUP, Type.OBJECT);
  Type.QUERY = Type.named('Query', Type.ROOT);
  Type.TRANSFORM = Type.named('Transform', Type.ROOT);
  Type.IMPORT = Type.named('Import', Type.ROOT);

  // Primitive types
  Type.STRING = Type.named('String', Type.OBJECT);
  Type.INTEGER = Type.named('Integer', Type.OBJECT);
  Type.DECIMAL = Type.named('Decimal', Type.OBJECT);
  Type.NUMBER = Type.named('Number', Type.OBJECT);

  // Collection types with element type parameter
  Type.COLLECTION = Type.withElements(List(), 'Collection', Set([Type.OBJECT]), DEFAULT_GROUP, Type.OBJECT);
  Type.SET = Type.withElements(List(), 'Set', Set([Type.COLLECTION]), DEFAULT_GROUP, Type.OBJECT);
  Type.LIST = Type.withElements(List(), 'List', Set([Type.COLLECTION]), DEFAULT_GROUP, Type.OBJECT);

  // Register factory hook for Node.setBindingValue
  Node._typeFromVariable = (v) => Type.fromVariable(v as Variable);
})();
