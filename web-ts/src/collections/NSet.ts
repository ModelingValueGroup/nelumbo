/**
 * NSet - Set node type.
 * Ported from Java: org.modelingvalue.nelumbo.collections.NSet
 */

import { List, Set } from 'immutable';
import type { AstElement } from '../AstElement';
import { Type } from '../Type';
import { Node } from '../Node';
import type { Functor } from '../patterns/Functor';
import { TokenType } from '../syntax/TokenType';

export class NSet extends Node {
  constructor(functor: Functor, elements: List<AstElement>, ...args: unknown[]) {
    super(functor, elements, Set(args));
  }

  protected static fromData(data: unknown[], declaration?: Node): NSet {
    const node = Object.create(NSet.prototype) as NSet;
    (node as unknown as { _data: unknown[] })._data = data;
    (node as unknown as { _declaration: Node | undefined })._declaration = declaration ?? node;
    return node;
  }

  protected override struct(data: unknown[], declaration?: Node): Node {
    return NSet.fromData(data, declaration ?? this.declaration());
  }

  override setAstElements(elements: List<AstElement>): NSet {
    return super.setAstElements(elements) as NSet;
  }

  elementType(): Type {
    return this.type().element()!;
  }

  elements<T = Node>(): Set<T> {
    return this.get(0) as Set<T>;
  }

  override args(): List<unknown> {
    return this.elements().toList();
  }

  override toString(_previous?: TokenType[]): string {
    const str = this.elements().toString();
    // Convert "Set [ ... ]" to "{ ... }"
    if (str.startsWith('Set')) {
      const inner = str.substring(4);
      return '{' + inner.substring(1, inner.length - 1) + '}';
    }
    return str;
  }
}
