/**
 * Equal - unification/equality predicate.
 * @JAVA_REF org.modelingvalue.nelumbo.logic.Equal
 */

import { List } from 'immutable';
import type { AstElement } from '../AstElement';
import { Type } from '../Type';
import { Node } from '../Node';
import type { Functor } from '../patterns/Functor';
import { Predicate } from './Predicate';
import { InferResult } from '../InferResult';
import type { InferContext } from '../InferContext';

/**
 * Equal - represents equality/unification between two terms.
 * @JAVA_REF org.modelingvalue.nelumbo.logic.Equal
 */
export class Equal extends Predicate {
  constructor(functor: Functor, elements: List<AstElement>, left: unknown, right: unknown) {
    super(functor, elements, left, right);
  }

  protected static fromDataEqual(data: unknown[], declaration?: Node): Equal {
    const eq = Object.create(Equal.prototype) as Equal;
    (eq as unknown as { _data: unknown[] })._data = data;
    (eq as unknown as { _declaration: Node })._declaration = declaration ?? eq;
    (eq as any)._binding = null;
    (eq as any)._hashCodeCached = false;
    (eq as any)._hashCode = 0;
    (eq as any)._nrOfUnbound = -1;
    return eq;
  }

  protected override struct(data: unknown[], declaration?: Node): Predicate {
    return Equal.fromDataEqual(data, declaration ?? this.declaration());
  }

  left(): Node {
    return this.get(0) as Node;
  }

  right(): Node {
    return this.get(1) as Node;
  }

  override set(i: number, ...a: unknown[]): Equal {
    return super.set(i, ...a) as Equal;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.Equal#infer(int, InferContext)
  protected override inferInternal(_nrOfUnbound: number, _context: InferContext): InferResult {
    const complete = [true];
    const eq = Equal.eqNode(this.left(), this.right(), complete);
    if (eq === null) {
      return complete[0] ? this.falsehoodCC() : this.falsehoodCI();
    } else {
      const r = this.set(0, eq).set(1, eq);
      return complete[0] ? r.factCC() : r.factCI();
    }
  }

  /**
   * Unify two Nodes.
   * @JAVA_REF org.modelingvalue.nelumbo.logic.Equal#eq(Node, Node, boolean[])
   */
  private static eqNode(left: Node, right: Node, complete: boolean[]): Node | null {
    if (left.equals(right)) {
      return left;
    } else if (!(left instanceof Type) && right instanceof Type) {
      complete[0] = false;
      return (right as Type).isAssignableFrom(left.type()) ? left : null;
    } else if (left instanceof Type && !(right instanceof Type)) {
      complete[0] = false;
      return (left as Type).isAssignableFrom(right.type()) ? right : null;
    } else if (left instanceof Type && right instanceof Type) {
      complete[0] = false;
      return left.equals(right) ? left : null;
    } else if (!left.typeOrFunctor().equals(right.typeOrFunctor())) {
      return null;
    } else if (left.length() !== right.length()) {
      return null;
    }
    let array: unknown[] | null = null;
    for (let i = 0; i < left.length(); i++) {
      const leftVal = left.get(i);
      const eq = Equal.eqValue(leftVal, right.get(i), complete);
      if (eq === null) {
        return null;
      } else if (!Equal.objEquals(eq, leftVal)) {
        if (array === null) {
          array = [...(left as any)._data];
        }
        array[i + 2] = eq; // 2 = Node.START offset
      }
    }
    return array !== null ? (left as any).struct(array) : left;
  }

  /**
   * Unify two values (may be Nodes, Types, or primitives).
   * @JAVA_REF org.modelingvalue.nelumbo.logic.Equal#eq(Object, Object, boolean[])
   */
  private static eqValue(left: unknown, right: unknown, complete: boolean[]): unknown | null {
    if (left !== right) {
      if (left instanceof Node && right instanceof Node) {
        return Equal.eqNode(left, right, complete);
      } else if (right instanceof Type) {
        complete[0] = false;
        return Equal.isAssignableFromValue(right, left) ? left : null;
      } else if (left instanceof Type) {
        complete[0] = false;
        return Equal.isAssignableFromValue(left, right) ? right : null;
      } else if (!Equal.objEquals(left, right)) {
        return null;
      }
    }
    return left;
  }

  /**
   * Check if a Type is assignable from a raw value's "class".
   * @JAVA_REF Java: ((Type) right).isAssignableFrom(left.getClass())
   */
  private static isAssignableFromValue(type: Type, value: unknown): boolean {
    if (value instanceof Node) {
      return type.isAssignableFrom(value.type());
    }
    if (typeof value === 'bigint') {
      return type.isAssignableFrom(Type.INTEGER);
    }
    if (typeof value === 'string') {
      return type.isAssignableFrom(Type.STRING);
    }
    if (typeof value === 'boolean') {
      return type.isAssignableFrom(Type.BOOLEAN);
    }
    return false;
  }

  /**
   * Object equality check matching Java's Objects.equals()
   */
  private static objEquals(a: unknown, b: unknown): boolean {
    if (a === b) return true;
    if (a === null || a === undefined || b === null || b === undefined) return false;
    if (a instanceof Node && b instanceof Node) return a.equals(b);
    if (typeof a === 'bigint' && typeof b === 'bigint') return a === b;
    return a === b;
  }
}
