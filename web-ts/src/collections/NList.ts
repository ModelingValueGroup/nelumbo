/**
 * NList - List node type.
 * @JAVA_REF org.modelingvalue.nelumbo.collections.NList
 */

import { List } from 'immutable';
import type { AstElement } from '../AstElement';
import { Type } from '../Type';
import { Node } from '../Node';
import type { Functor } from '../patterns/Functor';
import { TokenType } from '../syntax/TokenType';

export class NList extends Node {
  constructor(elements: List<AstElement>, elementType: Type);
  constructor(elementType: Type, elements: List<AstElement>, args: List<Node>);
  constructor(functor: Functor, elements: List<AstElement>, args: unknown[]);
  constructor(elements: List<AstElement>, list: NList, last: Node);
  constructor(elements: List<AstElement>, elementType: Type, ...nodes: Node[]);
  constructor(
    arg1: List<AstElement> | Type | Functor,
    arg2: Type | List<AstElement> | NList,
    arg3?: List<Node> | unknown[] | Node,
    ...rest: Node[]
  ) {
    if (arg1 instanceof Type) {
      // NList(elementType, elements, args)
      const elementType = arg1;
      const elements = arg2 as List<AstElement>;
      const args = arg3 as List<Node>;
      super(elementType.list(), elements, args);
    } else if (arg2 instanceof Type && (arg3 !== undefined || rest.length > 0)) {
      // NList(elements, elementType, ...nodes)
      const elements = arg1 as List<AstElement>;
      const elementType = arg2;
      const nodes: Node[] = [];
      if (arg3 !== undefined) {
        nodes.push(arg3 as Node);
      }
      nodes.push(...rest);
      super(elementType.list(), elements, List(nodes));
    } else if (arg2 instanceof Type) {
      // NList(elements, elementType) - no additional args
      const elements = arg1 as List<AstElement>;
      const elementType = arg2;
      super(elementType.list(), elements, List<Node>());
    } else if (arg2 instanceof NList) {
      // NList(elements, list, last)
      const elements = arg1 as List<AstElement>;
      const list = arg2;
      const last = arg3 as Node;
      super(list.type(), list.astElements().concat(elements).push(last as AstElement), list.elements().push(last));
    } else if ('pattern' in arg1) {
      // NList(functor, elements, args)
      const functor = arg1 as Functor;
      const elements = arg2 as List<AstElement>;
      const args = arg3 as unknown[];
      super(functor, elements, List(args));
    } else {
      // Fallback
      const elements = arg1 as List<AstElement>;
      super(Type.ROOT.list(), elements, List<Node>());
    }
  }

  protected static fromData(data: unknown[], declaration?: Node): NList {
    const node = Object.create(NList.prototype) as NList;
    (node as unknown as { _data: unknown[] })._data = data;
    (node as unknown as { _declaration: Node | undefined })._declaration = declaration ?? node;
    return node;
  }

  protected override struct(data: unknown[], declaration?: Node): Node {
    return NList.fromData(data, declaration ?? this.declaration());
  }

  override args(): List<unknown> {
    return this.elements();
  }

  override setAstElements(elements: List<AstElement>): NList {
    return super.setAstElements(elements) as NList;
  }

  elementType(): Type {
    return this.type().element()!;
  }

  elements<T = Node>(): List<T> {
    return this.get(0) as List<T>;
  }

  elementsFlattened<T extends Node = Node>(): List<T> {
    let result = List<T>();
    for (const e of this.elements<T>()) {
      if (e instanceof NList) {
        result = result.concat(e.elementsFlattened<T>());
      } else {
        result = result.push(e);
      }
    }
    return result;
  }

  override toString(_previous?: TokenType[]): string {
    const str = this.elements().toString();
    // Remove "List" prefix from Immutable.js toString
    return str.startsWith('List') ? str.substring(4) : str;
  }
}
