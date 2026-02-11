/**
 * NodeTypePattern - matches a node of a specific type.
 * @JAVA_REF org.modelingvalue.nelumbo.patterns.NodeTypePattern
 */

import { List, Map } from 'immutable';
import { TokenType } from '../syntax/TokenType';
import type { Token } from '../syntax/Token';
import type { AstElement } from '../AstElement';
import { Type } from '../Type';
import { Variable } from '../Variable';
import { Node } from '../Node';
import { Pattern } from './Pattern';
import { ParseState } from '../syntax/ParseState';
import type { Functor } from './Functor';

export class NodeTypePattern extends Pattern {
  constructor(type: Type, elements: List<AstElement>, ...args: unknown[]) {
    super(type, elements, ...args);
  }

  protected static fromData(data: unknown[], declaration?: Node): NodeTypePattern {
    const pattern = Object.create(NodeTypePattern.prototype) as NodeTypePattern;
    (pattern as unknown as { _data: unknown[] })._data = data;
    (pattern as unknown as { _declaration: Node })._declaration = declaration ?? pattern;
    return pattern;
  }

  protected struct(data: unknown[], declaration?: Node): NodeTypePattern {
    return NodeTypePattern.fromData(data, declaration ?? this.declaration());
  }

  nodeType(): Type {
    return this.get(0) as Type;
  }

  variable(): Variable | null {
    return this.nodeType().variable();
  }

  precedence(): number | null {
    return this.get(1) as number | null;
  }

  setBinding(vars: Map<Variable, unknown>, declaration?: Node): NodeTypePattern {
    const v = this.variable();
    if (v !== null) {
      const val = vars.get(v);
      if (val instanceof Type) {
        return this.set(0, this.nodeType().rewrite(val)) as NodeTypePattern;
      }
    }
    return super.setBinding(vars, declaration) as NodeTypePattern;
  }

  // @JAVA_REF NodeTypePattern.state(ParseState next)
  parseState(next: ParseState): ParseState {
    return new ParseState(this.nodeType(), next, this.precedence());
  }

  toString(_previous?: TokenType[]): string {
    return '<' + this.nodeType().name() + '>';
  }

  setPrecedence(precedence: number): Pattern {
    if (this.precedence() !== null) {
      return this;
    }
    return this.set(1, precedence) as NodeTypePattern;
  }

  setTypes(typeFunction: (type: Type) => Type): Pattern {
    return this.set(0, typeFunction(this.nodeType())) as NodeTypePattern;
  }

  argTypes(types: List<Type>): List<Type> {
    return types.push(this.nodeType());
  }

  string(args: List<unknown>, ai: number, sb: string[], previous: TokenType[], _alt: boolean): number {
    const arg = args.get(ai);
    if (arg instanceof Node) {
      const node = arg;
      const nodeT = node.type();

      // Check if type is assignable or if it's a Variable matching VARIABLE type
      if (this.nodeType().isAssignableFrom(nodeT) ||
          (node instanceof Variable && this.nodeType().isAssignableFrom(Type.VARIABLE))) {

        let parenthetical = false;
        const nodeFunctor = node.functor();
        const post = nodeFunctor?.postStart?.() ?? null;

        if (post !== null) {
          const inner = this.precedence();
          if (inner !== null && inner > (post.leftPrecedence() ?? Number.MAX_SAFE_INTEGER)) {
            parenthetical = true;
          }
        }

        if (parenthetical) {
          sb.push('(');
        }
        sb.push(node.toString(previous));
        if (parenthetical) {
          sb.push(')');
        }
        return ai + 1;
      }
    }
    return -1;
  }

  extractArgs(
    elements: List<AstElement>,
    i: number,
    args: unknown[],
    _alt: boolean,
    _functor: Functor,
    typeArgs: Map<Variable, Type>
  ): number {
    if (i < elements.size) {
      const e = elements.get(i);
      // In Java, Variable extends Node so instanceof Node covers both.
      // In TS, Variable is separate, so we handle both explicitly.
      if (e instanceof Node || e instanceof Variable) {
        const n = e;
        const nType = e instanceof Node ? (e as Node).type() : (e as Variable).type();
        const nodeType = this.nodeType();

        if (nodeType.isAssignableFrom(nType)) {
          args.push(n);
          return i + 1;
        } else if (Type.VARIABLE.equals(nodeType) && e instanceof Variable) {
          args.push(n);
          return i + 1;
        } else {
          const v = nodeType.variable();
          if (v !== null) {
            const resolvedType = typeArgs.get(v);
            if (resolvedType !== undefined && resolvedType.isAssignableFrom(nType)) {
              args.push(n);
              return i + 1;
            }
          }
        }
      }
    }
    return -1;
  }

  tokenDeclaration(_token: Token): Pattern | null {
    return null;
  }
}
