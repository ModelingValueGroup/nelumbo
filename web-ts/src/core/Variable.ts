/**
 * Variable class representing logic variables in Nelumbo.
 * Ported from Java: org.modelingvalue.nelumbo.Variable
 */

import { List } from 'immutable';
import { TokenType } from '../TokenType';
import type { Token } from '../Token';
import type { AstElement } from './AstElement';
import { Type } from './Type';
import type { Functor } from '../patterns/Functor';

/**
 * Variable class - represents logic variables with type constraints.
 */
export class Variable implements AstElement {
  // Internal storage
  private readonly _elements: List<AstElement>;
  private readonly _type: Type;
  private readonly _name: string | Variable;
  private readonly _declaration: Variable;

  constructor(elements: List<AstElement>, type: Type, name: string | Variable, declaration?: Variable) {
    this._elements = elements;
    this._type = type;
    this._name = name;
    this._declaration = declaration ?? this;
  }

  /**
   * Get the type of this variable.
   */
  type(): Type {
    return this._type;
  }

  /**
   * Get the name of this variable.
   */
  name(): string {
    if (typeof this._name === 'string') {
      return this._name;
    }
    return this._name.name();
  }

  /**
   * Get the declaration of this variable.
   */
  declaration(): Variable {
    return this._declaration;
  }

  /**
   * Get the AST elements.
   */
  astElements(): List<AstElement> {
    return this._elements;
  }

  /**
   * Create a literal version of this variable.
   */
  literal(): Variable {
    const type = this.type();
    return type.isLiteral()
      ? this
      : new Variable(this._elements, type.literal(), this.name());
  }

  /**
   * Rename this variable.
   */
  rename(name: string): Variable {
    return new Variable(this._elements, this.type(), name);
  }

  /**
   * Set the type of this variable.
   */
  setType(type: Type): Variable {
    return new Variable(this._elements, type, this._name, this._declaration);
  }

  /**
   * Set the functor (for API compatibility).
   */
  setFunctor(_functor: Functor): Variable {
    return this;
  }

  /**
   * Set the AST elements.
   */
  setAstElements(elements: List<AstElement>): Variable {
    return new Variable(elements, this._type, this._name, this._declaration);
  }

  /**
   * Get this as a variable (identity).
   */
  variable(): Variable {
    return this;
  }

  // Equality
  equals(other: unknown): boolean {
    if (this === other) return true;
    if (!(other instanceof Variable)) return false;

    return this._type.equals(other._type) &&
           this.name() === other.name();
  }

  hashCode(): number {
    return this._type.hashCode() * 31 + this.name().length;
  }

  toString(previous?: TokenType[]): string {
    if (previous) {
      const prevType = previous[0];
      if (prevType === TokenType.NAME || prevType === TokenType.NUMBER || prevType === TokenType.DECIMAL) {
        previous[0] = TokenType.NAME;
        return ' ' + this.name();
      }
      previous[0] = TokenType.NAME;
    }
    return this.name();
  }

  // AstElement implementation
  firstToken(): Token | null {
    for (const element of this._elements) {
      const first = element.firstToken();
      if (first !== null) {
        return first;
      }
    }
    return null;
  }

  lastToken(): Token | null {
    for (const element of this._elements.reverse()) {
      const last = element.lastToken();
      if (last !== null) {
        return last;
      }
    }
    return null;
  }

  /**
   * Get the next token after this variable.
   */
  nextToken(): Token | null {
    const last = this.lastToken();
    return last !== null ? last.next : null;
  }

  deparse(sb: string[]): void {
    for (const element of this._elements) {
      element.deparse(sb);
    }
  }
}
