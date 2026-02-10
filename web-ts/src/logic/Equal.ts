/**
 * Equal - unification/equality predicate.
 * Ported from Java: org.modelingvalue.nelumbo.logic.Equal
 */

import { List, Map } from 'immutable';
import type { AstElement } from '../AstElement';
import { Type } from '../Type';
import { Variable } from '../Variable';
import { Node } from '../Node';
import type { Functor } from '../patterns/Functor';
import { Predicate } from './Predicate';
import { InferResult } from '../InferResult';
import type { InferContext } from '../InferContext';

/**
 * Equal - represents equality/unification between two terms.
 */
export class Equal extends Predicate {
  constructor(functor: Functor, elements: List<AstElement>, left: unknown, right: unknown) {
    super(functor, elements, left, right);
  }

  protected static fromDataEqual(data: unknown[], declaration?: Node): Equal {
    const eq = Object.create(Equal.prototype) as Equal;
    (eq as unknown as { _data: unknown[] })._data = data;
    (eq as unknown as { _declaration: Node })._declaration = declaration ?? eq;
    return eq;
  }

  protected override struct(data: unknown[], declaration?: Node): Predicate {
    return Equal.fromDataEqual(data, declaration ?? this.declaration());
  }

  /**
   * Get the left-hand side.
   */
  left(): unknown {
    return this.get(0);
  }

  /**
   * Get the right-hand side.
   */
  right(): unknown {
    return this.get(1);
  }

  override infer(context: InferContext): InferResult {
    const result = this.eq(this.left(), this.right(), true);
    if (result === null) {
      return context.reduce() ? this.falsehoodCC() : this.falsehoodCI();
    }
    const equal = result[0];
    const complete = result[1];
    if (complete) {
      return equal.factCC();
    }
    return equal.factCI();
  }

  /**
   * Unify two values.
   * Returns [equal, complete] or null if not unifiable.
   */
  eq(left: unknown, right: unknown, complete: boolean): [Equal, boolean] | null {
    const result = this.eqInternal(left, right, complete);
    if (result === null) {
      return null;
    }
    const newEqual = this.set(0, result[0], result[1]) as Equal;
    return [newEqual, result[2]];
  }

  private eqInternal(left: unknown, right: unknown, complete: boolean): [unknown, unknown, boolean] | null {
    // Handle null/undefined
    if (left === null || left === undefined) {
      return right === null || right === undefined ? [left, right, complete] : null;
    }
    if (right === null || right === undefined) {
      return null;
    }

    // Handle Types
    if (left instanceof Type && right instanceof Type) {
      return this.eqTypes(left, right, complete);
    }

    // Handle Variables
    if (left instanceof Variable && right instanceof Variable) {
      return this.eqVariables(left, right, complete);
    }
    if (left instanceof Variable) {
      return this.eqVariableValue(left, right, complete);
    }
    if (right instanceof Variable) {
      return this.eqVariableValue(right, left, complete);
    }

    // Handle Type with value
    if (left instanceof Type) {
      return this.eqTypeValue(left, right, complete);
    }
    if (right instanceof Type) {
      return this.eqTypeValue(right, left, complete);
    }

    // Handle Nodes
    if (left instanceof Node && right instanceof Node) {
      return this.eqNodes(left, right, complete);
    }

    // Handle Lists
    if (List.isList(left) && List.isList(right)) {
      return this.eqLists(left as List<unknown>, right as List<unknown>, complete);
    }

    // Handle primitives
    if (this.valuesEqual(left, right)) {
      return [left, right, complete];
    }

    return null;
  }

  private eqTypes(left: Type, right: Type, complete: boolean): [Type, Type, boolean] | null {
    // Check assignability
    if (left.isAssignableFrom(right)) {
      return [right, right, complete];
    }
    if (right.isAssignableFrom(left)) {
      return [left, left, complete];
    }
    return null;
  }

  private eqVariables(left: Variable, right: Variable, _complete: boolean): [Variable, Variable, boolean] | null {
    // Same variable
    if (left.equals(right)) {
      return [left, right, true];
    }
    // Compatible types
    const leftType = left.type();
    const rightType = right.type();
    if (leftType.isAssignableFrom(rightType)) {
      return [right, right, false];
    }
    if (rightType.isAssignableFrom(leftType)) {
      return [left, left, false];
    }
    return null;
  }

  private eqVariableValue(variable: Variable, value: unknown, _complete: boolean): [unknown, unknown, boolean] | null {
    const varType = variable.type();
    const valType = this.typeOfValue(value);
    if (valType !== null && varType.isAssignableFrom(valType)) {
      return [value, value, false];
    }
    if (valType !== null && valType.isAssignableFrom(varType)) {
      return [variable, variable, false];
    }
    return null;
  }

  private eqTypeValue(type: Type, value: unknown, _complete: boolean): [unknown, unknown, boolean] | null {
    const valType = this.typeOfValue(value);
    if (valType !== null && type.isAssignableFrom(valType)) {
      return [value, value, false];
    }
    return null;
  }

  private eqNodes(left: Node, right: Node, complete: boolean): [Node, Node, boolean] | null {
    // Must have same functor
    const leftFunctor = left.functor();
    const rightFunctor = right.functor();
    if (leftFunctor !== rightFunctor) {
      if (leftFunctor === null || rightFunctor === null) return null;
      if (!leftFunctor.equals(rightFunctor)) return null;
    }

    // Must have same length
    if (left.length() !== right.length()) {
      return null;
    }

    // Unify each argument
    let newLeft = left;
    let newRight = right;
    let newComplete = complete;

    for (let i = 0; i < left.length(); i++) {
      const result = this.eqInternal(left.get(i), right.get(i), newComplete);
      if (result === null) {
        return null;
      }
      if (result[0] !== left.get(i)) {
        newLeft = newLeft.set(i, result[0]);
      }
      if (result[1] !== right.get(i)) {
        newRight = newRight.set(i, result[1]);
      }
      newComplete = result[2];
    }

    return [newLeft, newRight, newComplete];
  }

  private eqLists(left: List<unknown>, right: List<unknown>, complete: boolean): [List<unknown>, List<unknown>, boolean] | null {
    if (left.size !== right.size) {
      return null;
    }

    let newLeft = left;
    let newRight = right;
    let newComplete = complete;

    for (let i = 0; i < left.size; i++) {
      const result = this.eqInternal(left.get(i), right.get(i), newComplete);
      if (result === null) {
        return null;
      }
      if (result[0] !== left.get(i)) {
        newLeft = newLeft.set(i, result[0]);
      }
      if (result[1] !== right.get(i)) {
        newRight = newRight.set(i, result[1]);
      }
      newComplete = result[2];
    }

    return [newLeft, newRight, newComplete];
  }

  private typeOfValue(value: unknown): Type | null {
    if (value instanceof Type) return value;
    if (value instanceof Variable) return value.type();
    if (value instanceof Node) return value.type();
    if (typeof value === 'string') return Type.STRING;
    if (typeof value === 'number') {
      return Number.isInteger(value) ? Type.INTEGER : Type.DECIMAL;
    }
    if (typeof value === 'boolean') return Type.BOOLEAN;
    if (List.isList(value)) return Type.LIST;
    return Type.OBJECT;
  }

  private valuesEqual(a: unknown, b: unknown): boolean {
    if (a === b) return true;
    if (a instanceof Node && b instanceof Node) return a.equals(b);
    if (a instanceof Type && b instanceof Type) return a.equals(b);
    if (a instanceof Variable && b instanceof Variable) return a.equals(b);
    if (List.isList(a) && List.isList(b)) return (a as List<unknown>).equals(b as List<unknown>);
    if (Map.isMap(a) && Map.isMap(b)) return (a as Map<unknown, unknown>).equals(b as Map<unknown, unknown>);
    return false;
  }
}
