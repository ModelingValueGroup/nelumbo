/**
 * Node class - base immutable AST node with binding/substitution.
 * @JAVA_REF org.modelingvalue.nelumbo.Node
 */

import { List, Map, Set } from 'immutable';
import { TokenType } from './syntax/TokenType';
import type { Token } from './syntax/Token';
import type { AstElement } from './AstElement';
import type { Type } from './Type';
import { Variable } from './Variable';
import type { Functor } from './patterns/Functor';
import type { MatchState } from './MatchState';

// Type for the replace function
export type ReplaceFunction = (node: Node) => Node;

/**
 * Node class - base immutable AST node.
 */
export class Node implements AstElement {
  // Storage offset for user args (0 = typeOrFunctor, 1 = elements)
  protected static readonly START = 2;

  // Internal storage
  protected readonly _data: unknown[];
  protected readonly _declaration: Node;

  // Cached values
  private _binding: Map<Variable, unknown> | null = null;
  private _hashCodeCached = false;
  private _hashCode = 0;

  // Factory hook for Type.fromVariable (avoids circular runtime dependency)
  static _typeFromVariable: ((v: Variable) => Node) | null = null;

  /**
   * Constructor for parsed nodes.
   */
  constructor(typeOrFunctor: Type | Functor, elements: List<AstElement>, ...args: unknown[]) {
    this._data = [typeOrFunctor, elements, ...args.map(a => a === undefined ? null : a)];
    this._declaration = this;
    this.init(elements);
  }

  /**
   * Constructor from raw data (for internal use).
   */
  protected static fromData(data: unknown[], declaration?: Node): Node {
    const node = Object.create(Node.prototype) as Node;
    (node as unknown as { _data: unknown[] })._data = data;
    (node as unknown as { _declaration: Node })._declaration = declaration ?? node;
    // Object.create doesn't run class field initializers, so initialize manually
    (node as any)._binding = null;
    (node as any)._hashCodeCached = false;
    (node as any)._hashCode = 0;
    return node;
  }

  /**
   * Initialize token-node links.
   */
  protected init(elements: List<AstElement>): void {
    for (const e of elements) {
      if (this.isToken(e)) {
        (e as Token).setNode(this);
      }
    }
  }

  private isToken(e: unknown): e is Token {
    return e !== null && typeof e === 'object' && 'type' in e && 'text' in e && 'setNode' in e;
  }

  /**
   * Get the declaration of this node.
   */
  declaration(): Node {
    return this._declaration;
  }

  /**
   * Reset the declaration (for pattern templates).
   */
  // @JAVA_REF org.modelingvalue.nelumbo.Node#resetDeclaration()
  // Java calls struct(array, null) where null means "use self as declaration"
  resetDeclaration(): Node {
    const newData = [...this._data];
    for (let i = Node.START; i < newData.length; i++) {
      newData[i] = this.resetDeclarationValue(newData[i]);
    }
    const result = this.struct(newData);
    // Force self-declaration (equivalent to Java's struct(data, null))
    (result as any)._declaration = result;
    (result as any)._binding = null;
    return result;
  }

  private resetDeclarationValue(from: unknown): unknown {
    if (from instanceof Node) {
      return from.resetDeclaration();
    } else if (List.isList(from)) {
      return (from as List<unknown>).map(e => this.resetDeclarationValue(e));
    }
    return from;
  }

  /**
   * Get the type or functor.
   */
  typeOrFunctor(): Type | Functor {
    return this._data[0] as Type | Functor;
  }

  /**
   * Get the type of this node.
   */
  type(): Type {
    const tf = this.typeOrFunctor();
    if (this.isFunctor(tf)) {
      return (tf as Functor).resultType();
    }
    return tf as Type;
  }

  private isFunctor(tf: unknown): tf is Functor {
    return tf !== null && typeof tf === 'object' && 'resultType' in tf;
  }

  /**
   * Get the functor of this node.
   */
  functor(): Functor | null {
    const tf = this.typeOrFunctor();
    return this.isFunctor(tf) ? tf : null;
  }

  /**
   * Get the AST elements.
   */
  astElements(): List<AstElement> {
    return this._data[1] as List<AstElement>;
  }

  /**
   * Get the number of user arguments.
   */
  length(): number {
    return this._data.length - Node.START;
  }

  /**
   * Get a user argument by index.
   */
  get(i: number): unknown {
    return this._data[i + Node.START];
  }

  /**
   * Get all user arguments as a list.
   */
  args(): List<unknown> {
    let args = List<unknown>();
    for (let i = 0; i < this.length(); i++) {
      const a = this.get(i);
      args = args.push(a === null ? undefined : a);
    }
    return args;
  }

  /**
   * Get all child nodes.
   */
  children(): List<Node> {
    let children = List<Node>();
    for (let i = 0; i < this.length(); i++) {
      children = this.collectChildren(this.get(i), children);
    }
    return children;
  }

  private collectChildren(a: unknown, children: List<Node>): List<Node> {
    if (a instanceof Node) {
      return children.push(a);
    } else if (List.isList(a) || Set.isSet(a)) {
      for (const e of a as Iterable<unknown>) {
        children = this.collectChildren(e, children);
      }
    }
    return children;
  }

  /**
   * Set the functor.
   */
  setFunctor(functor: Functor): Node {
    const newData = [...this._data];
    newData[0] = functor;
    return this.struct(newData, undefined);
  }

  /**
   * Set the AST elements.
   */
  setAstElements(elements: List<AstElement>): Node {
    const newData = [...this._data];
    newData[1] = elements;
    const node = this.struct(newData);
    node.init(elements);
    return node;
  }

  /**
   * Set a user argument by index.
   */
  set(i: number, ...values: unknown[]): Node {
    const newData = this.setArray(i, ...values);
    return newData !== null ? this.struct(newData) : this;
  }

  protected setArray(from: number, ...values: unknown[]): unknown[] | null {
    let array: unknown[] | null = null;
    for (let i = 0; i < values.length; i++) {
      const oldVal = this.get(i + from);
      if (!this.valueEquals(values[i], oldVal)) {
        if (array === null) {
          array = [...this._data];
        }
        array[i + from + Node.START] = values[i];
      }
    }
    return array;
  }

  private valueEquals(a: unknown, b: unknown): boolean {
    if (a === b) return true;
    if (a instanceof Node && b instanceof Node) return a.equals(b);
    if (a instanceof Variable && b instanceof Variable) return a.equals(b);
    if (List.isList(a) && List.isList(b)) return (a as List<unknown>).equals(b as List<unknown>);
    if (Set.isSet(a) && Set.isSet(b)) return (a as Set<unknown>).equals(b as Set<unknown>);
    if (Map.isMap(a) && Map.isMap(b)) return (a as Map<unknown, unknown>).equals(b as Map<unknown, unknown>);
    return false;
  }

  /**
   * Set a value at a nested index path.
   */
  setPath(idx: number[], val: unknown): Node {
    return this.setPathHelper(0, idx, val);
  }

  private setPathHelper(ii: number, idx: number[], val: unknown): Node {
    const newData = [...this._data];
    const i = idx[ii] + Node.START;
    if (ii < idx.length - 1) {
      const s = newData[i] as Node;
      newData[i] = s.setPathHelper(ii + 1, idx, val);
    } else {
      newData[i] = val;
    }
    return this.struct(newData);
  }

  /**
   * Get a value at a nested index path.
   */
  getVal<V>(...indices: number[]): V | null {
    let val: unknown = this;
    for (const i of indices) {
      val = (val as Node).get(i);
      if ((val instanceof Node && val.isType()) || val instanceof Variable) {
        return null;
      }
    }
    return val as V;
  }

  /**
   * Create a new node with the same structure.
   */
  protected struct(data: unknown[], declaration?: Node): Node {
    return Node.fromData(data, declaration ?? this._declaration);
  }

  /**
   * Get local variables defined by this node.
   */
  localVars(): List<Variable> {
    return List();
  }

  /**
   * Get all local variables including children.
   */
  allLocalVars(): Set<Variable> {
    let allVars = this.localVars().toSet();
    for (let i = 0; i < this.length(); i++) {
      allVars = this.collectAllLocalVars(this.get(i), allVars);
    }
    return allVars;
  }

  private collectAllLocalVars(val: unknown, allVars: Set<Variable>): Set<Variable> {
    if (val instanceof Node) {
      return allVars.union(val.allLocalVars());
    } else if (List.isList(val)) {
      for (const e of val as List<unknown>) {
        allVars = this.collectAllLocalVars(e, allVars);
      }
    }
    return allVars;
  }

  /**
   * Get variable bindings from comparing with declaration.
   */
  getBinding(declaration?: Node): Map<Variable, unknown> | null {
    if (declaration === undefined) {
      // Use == to handle both null (from constructor) and undefined (from Object.create in fromData)
      if (this._binding == null) {
        this._binding = this.computeBinding(this._declaration);
      }
      return this._binding;
    }
    return this.computeBinding(declaration);
  }

  private computeBinding(declaration: Node, vars: Map<Variable, unknown> = Map()): Map<Variable, unknown> | null {
    let result: Map<Variable, unknown> | null = vars;
    for (let i = 0; result !== null && i < this.length(); i++) {
      result = this.getBindingValue(declaration.get(i), this.get(i), result, i);
    }
    return result;
  }

  private getBindingValue(
    declVal: unknown,
    thisIn: unknown,
    vars: Map<Variable, unknown>,
    _i: number
  ): Map<Variable, unknown> | null {
    let thisVal: unknown = (thisIn instanceof Node && thisIn.isType()) || thisIn instanceof Variable ? null : thisIn;

    // Handle type with variable
    if (declVal instanceof Node && declVal.isType()) {
      const v = declVal.variable();
      if (v !== null) {
        declVal = v;
      }
    }

    if (declVal instanceof Variable) {
      const declVar = declVal;
      let varVal = vars.get(declVar);
      if (varVal instanceof Node && varVal.isType()) varVal = null;

      if (varVal !== null && varVal !== undefined) {
        if (thisVal !== null && !this.valueEquals(thisVal, varVal)) {
          return null;
        }
      } else {
        if (thisVal === null) {
          if (thisIn instanceof Variable && !thisIn.equals(declVar)) {
            thisVal = thisIn;
          } else {
            thisVal = Node.typeOf(thisIn);
          }
        }
        if (thisVal !== null && this.doGetBinding(thisVal, _i)) {
          vars = vars.set(declVar, thisVal);
          if (thisVal instanceof Node) {
            const nodeBinding = thisVal.getBinding();
            if (nodeBinding != null) {
              vars = vars.merge(nodeBinding.mapKeys(key => key.rename('$' + key.name())));
            }
          }
        }
      }
    } else if (declVal instanceof Node && thisVal instanceof Node) {
      return thisVal.computeBinding(declVal, vars);
    } else if (List.isList(declVal) && List.isList(thisVal)) {
      const declList = declVal as List<unknown>;
      const thisList = thisVal as List<unknown>;
      if (declList.size === thisList.size) {
        for (let ii = 0; ii < declList.size; ii++) {
          const result = this.getBindingValue(declList.get(ii), thisList.get(ii), vars, _i);
          if (result === null) return null;
          vars = result;
        }
      }
    }

    return vars;
  }

  /**
   * Get the type of a value.
   */
  // @JAVA_REF org.modelingvalue.nelumbo.Node#typeOf(Object)
  // In Java, Variable extends Node so instanceof Node covers it.
  // In TS, Variable does not extend Node, so we handle it separately.
  static typeOf(v: unknown): Type | null {
    if (v instanceof Node) {
      return v.isType() ? v as unknown as Type : v.type();
    }
    if (v instanceof Variable) {
      return v.type();
    }
    return null;
  }

  /**
   * Set a single variable binding.
   */
  protected setVar(variable: Variable, val: unknown): Node {
    return this.setBinding(Map([[variable, val]]));
  }

  /**
   * Apply variable bindings to this node.
   */
  setBinding(vars: Map<Variable, unknown>, declaration?: Node): Node {
    const decl = declaration ?? this._declaration;
    let newData: unknown[] | null = null;

    for (let i = 0; i < this.length(); i++) {
      const thisVal = this.get(i);
      const bound = this.setBindingValue(decl.get(i), thisVal, vars, i);
      if (!this.valueEquals(bound, thisVal)) {
        if (newData === null) {
          newData = [...this._data];
        }
        newData[i + Node.START] = bound;
      }
    }

    return newData !== null ? this.struct(newData, decl) : this;
  }

  protected setBindingValue(
    declVal: unknown,
    thisVal: unknown,
    vars: Map<Variable, unknown>,
    _i: number
  ): unknown {
    if (declVal instanceof Variable) {
      const varVal = vars.get(declVal);
      if (varVal !== null && varVal !== undefined && this.doSetBinding(varVal, _i)) {
        // When thisVal is a non-Type Node (e.g. NBoolean wrapping a Variable) and varVal
        // is a primitive (not a Node/Variable), recurse into thisVal's setBinding to
        // preserve the Node wrapper. In Java, StructImpl interning implicitly preserves
        // the wrapper by returning cached nodes whose declarations track the Node structure.
        // Without interning in TS, we must handle this explicitly.
        if (thisVal instanceof Node && !thisVal.isType() && !(varVal instanceof Node) && !(varVal instanceof Variable)) {
          return thisVal.setBinding(vars);
        }
        return varVal;
      }
      if (thisVal instanceof Variable) {
        const from = thisVal.type();
        const v = from.variable();
        if (v !== null) {
          const to = vars.get(v);
          if (to instanceof Node && to.isType()) {
            return thisVal.setType(from.rewrite(to as unknown as Type));
          }
        }
      }
    } else if (declVal instanceof Node && !declVal.isType() &&
               thisVal instanceof Node && !thisVal.isType()) {
      return thisVal.setBinding(vars, declVal);
    } else if (List.isList(declVal) && List.isList(thisVal)) {
      const declList = declVal as List<unknown>;
      const thisList = thisVal as List<unknown>;
      if (declList.size === thisList.size) {
        let newList = thisList;
        for (let ii = 0; ii < declList.size; ii++) {
          const bound = this.setBindingValue(declList.get(ii), thisList.get(ii), vars, _i);
          if (!this.valueEquals(bound, thisList.get(ii))) {
            newList = newList.set(ii, bound);
          }
        }
        return newList;
      }
    } else if (declVal instanceof Node && declVal.isType() && thisVal instanceof Node && thisVal.isType()) {
      const declVar = declVal.variable();
      if (declVar !== null) {
        const varVal = vars.get(declVar);
        if (varVal instanceof Node && varVal.isType()) {
          return (thisVal as unknown as { rewrite(t: Node): Node }).rewrite(varVal);
        } else if (varVal instanceof Variable) {
          return Node._typeFromVariable!(varVal);
        }
      } else {
        return thisVal.setBinding(vars, declVal);
      }
    }

    return thisVal;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.Node#doGetBinding(Object, int)
  protected doGetBinding(_varVal: unknown, _i: number): boolean {
    return true;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.Node#doSetBinding(Object, int)
  protected doSetBinding(_varVal: unknown, _i: number): boolean {
    return true;
  }

  /**
   * Replace nodes using a replacer function.
   */
  replace(replacer: ReplaceFunction): Node {
    const replaced = replacer(this);
    if (replaced !== this) {
      return replaced;
    }

    let newData: unknown[] | null = null;
    for (let i = 0; i < this.length(); i++) {
      const fromVal = this.get(i);
      const toVal = this.replaceValue(fromVal, replacer);
      if (toVal !== fromVal) {
        if (newData === null) {
          newData = [...this._data];
        }
        newData[i + Node.START] = toVal;
      }
    }

    return newData !== null ? this.struct(newData) : this;
  }

  private replaceValue(from: unknown, replacer: ReplaceFunction): unknown {
    if (from instanceof Node) {
      return from.replace(replacer);
    } else if (List.isList(from)) {
      let result = from as List<unknown>;
      let changed = false;
      for (let i = 0; i < result.size; i++) {
        const replaced = this.replaceValue(result.get(i), replacer);
        if (replaced !== result.get(i)) {
          result = result.set(i, replaced);
          changed = true;
        }
      }
      return changed ? result : from;
    }
    return from;
  }

  /**
   * Set type at index.
   */
  setType(i: number, type: Type): Node {
    return this.set(i, type);
  }

  /**
   * Set typed node at index.
   */
  protected setTyped(i: number, typed: Node): Node {
    return this.set(i, typed);
  }

  /**
   * Get variable for this node.
   */
  variable(): Variable | null {
    return null;
  }

  /**
   * Check if this node is a Type.
   */
  isType(): boolean {
    return false;
  }

  // Token access
  firstToken(): Token | null {
    const elements = this.astElements();
    for (const element of elements) {
      const first = element.firstToken();
      if (first !== null) {
        return first;
      }
    }
    return null;
  }

  lastToken(): Token | null {
    const elements = this.astElements();
    for (const element of elements.reverse()) {
      const last = element.lastToken();
      if (last !== null) {
        return last;
      }
    }
    return null;
  }

  nextToken(): Token | null {
    const last = this.lastToken();
    return last !== null ? last.next : null;
  }

  tokens(): Token[] {
    const first = this.firstToken();
    const last = this.lastToken();
    return first !== null ? first.list(last) : [];
  }

  /**
   * Build a MatchState for this node (for rule matching).
   */
  state<E>(matchState: MatchState<E>): MatchState<E> {
    const args = this.args().reverse();
    let state = matchState;

    for (const arg of args) {
      if (arg instanceof Node && arg.isType()) {
        const typeArg = arg as unknown as { tokenType(): TokenType | null; variable(): Variable | null };
        const tt = typeArg.tokenType();
        if (tt !== null) {
          state = state.withTokenType(tt);
        } else {
          const v = typeArg.variable();
          if (v !== null) {
            state = state.withType(v.type());
          } else {
            state = state.withType(arg as unknown as Type);
          }
        }
      } else if (arg instanceof Variable) {
        const type = arg.type();
        const tt = type.tokenType();
        if (tt !== null) {
          state = state.withTokenType(tt);
        } else {
          state = state.withType(type);
        }
      } else if (arg instanceof Node) {
        state = arg.state(state);
      } else if (arg !== null && arg !== undefined) {
        state = state.withClass(arg.constructor);
      }
    }

    const functor = this.functor();
    if (functor !== null) {
      state = state.withFunctor(functor);
    }

    return state;
  }

  // Equality and hashing
  equals(obj: unknown): boolean {
    if (this === obj) return true;
    if (obj === null || obj === undefined) return false;
    if (!(obj instanceof Node)) return false;
    if (obj.constructor !== this.constructor) return false;
    if (obj.hashCode() !== this.hashCode()) return false;

    if (!this.valueEquals(this.typeForEquals(), obj.typeForEquals())) {
      return false;
    }
    if (this.length() !== obj.length()) {
      return false;
    }
    for (let i = 0; i < this.length(); i++) {
      if (!this.valueEquals(this.get(i), obj.get(i))) {
        return false;
      }
    }
    return true;
  }

  protected typeForEquals(): unknown {
    return this.typeOrFunctor();
  }

  hashCode(): number {
    if (!this._hashCodeCached) {
      let r = 1;
      for (let i = 0; i < this.length(); i++) {
        const e = this.get(i);
        r = 31 * r + this.hashValue(e);
      }
      r = 31 * r + this.hashValue(this.typeForEquals());
      this._hashCode = r === 0 ? 1 : r;
      this._hashCodeCached = true;
    }
    return this._hashCode;
  }

  private hashValue(e: unknown): number {
    if (e === null || e === undefined) return 0;
    if (e instanceof Node) return e.hashCode();
    if (e instanceof Variable) return e.hashCode();
    if (typeof e === 'string') return e.length;
    if (typeof e === 'number') return e;
    if (List.isList(e) || Set.isSet(e) || Map.isMap(e)) {
      return (e as { hashCode(): number }).hashCode();
    }
    return 0;
  }

  // String representation
  toString(previous?: TokenType[]): string {
    const prev = previous ?? [TokenType.BEGINOFFILE];
    const functor = this.functor();

    if (functor !== null) {
      const str = functor.string(this.args(), prev);
      if (str !== null) {
        return str;
      }
    }

    const sb: string[] = [];
    if (functor !== null) {
      sb.push(functor.name());
    } else {
      sb.push(this.type().name());
    }
    sb.push('(');

    let sep = '';
    for (let i = 0; i < this.length(); i++) {
      sb.push(sep);
      sb.push(this.argToString(i));
      sep = ',';
    }
    sb.push(')');

    return sb.join('');
  }

  argToString(i: number): string {
    const val = this.get(i);
    if (val === null || val === undefined) return 'null';
    if (val instanceof Node) return val.toString();
    if (val instanceof Variable) return val.name();
    if (typeof val === 'string') return '"' + val + '"';
    return String(val);
  }

  // AstElement implementation
  deparse(sb: string[]): void {
    for (const e of this.astElements()) {
      e.deparse(sb);
    }
  }
}
