/**
 * ConstructorRegistry - maps Java class names to TS NodeConstructor functions.
 * Used by @annotation handling in BaseSyntax to wire up builtin classes.
 */

import type { NodeConstructor } from './patterns/Functor';
import { Node } from './Node';
import { Equal } from './logic/Equal';
import { Not } from './logic/Not';
import { And } from './logic/And';
import { Or } from './logic/Or';
import { ExistentialQuantifier } from './logic/ExistentialQuantifier';
import { UniversalQuantifier } from './logic/UniversalQuantifier';
import { NInteger } from './integers/NInteger';
import { Add } from './integers/Add';
import { Multiply } from './integers/Multiply';
import { GreaterThan } from './integers/GreaterThan';
import { NString } from './strings/NString';
import { Concat } from './strings/Concat';
import { Length } from './strings/Length';
import { ToInteger } from './strings/ToInteger';
import { NList } from './collections/NList';
import { NSet } from './collections/NSet';

const registry = new Map<string, NodeConstructor>([
  ['org.modelingvalue.nelumbo.logic.Equal',
    (elements, args, functor) => new Equal(functor, elements, args[0], args[1])],
  // NBoolean: intentionally NOT registered - singletons lack AST elements
  // needed for token stream navigation. Default Predicate construction is
  // used instead, which preserves elements for nextToken() tracking.
  ['org.modelingvalue.nelumbo.logic.Not',
    (elements, args, functor) => new Not(functor, elements, args[0] as Node)],
  ['org.modelingvalue.nelumbo.logic.And',
    (elements, args, functor) => new And(functor, elements, args[0] as Node, args[1] as Node)],
  ['org.modelingvalue.nelumbo.logic.Or',
    (elements, args, functor) => new Or(functor, elements, args[0] as Node, args[1] as Node)],
  ['org.modelingvalue.nelumbo.logic.ExistentialQuantifier',
    (elements, args, functor) => new ExistentialQuantifier(functor, elements, args[0] as any, args[1] as any)],
  ['org.modelingvalue.nelumbo.logic.UniversalQuantifier',
    (elements, args, functor) => new UniversalQuantifier(functor, elements, args[0] as any, args[1] as any)],
  ['org.modelingvalue.nelumbo.integers.NInteger',
    (elements, args, functor) => new NInteger(functor, elements, ...args)],
  ['org.modelingvalue.nelumbo.integers.Add',
    (elements, args, functor) => new Add(functor, elements, ...args)],
  ['org.modelingvalue.nelumbo.integers.Multiply',
    (elements, args, functor) => new Multiply(functor, elements, ...args)],
  ['org.modelingvalue.nelumbo.integers.GreaterThan',
    (elements, args, functor) => new GreaterThan(functor, elements, ...args)],
  ['org.modelingvalue.nelumbo.strings.NString',
    (elements, args, functor) => new NString(functor, elements, ...args)],
  ['org.modelingvalue.nelumbo.strings.Concat',
    (elements, args, functor) => new Concat(functor, elements, ...args)],
  ['org.modelingvalue.nelumbo.strings.Length',
    (elements, args, functor) => new Length(functor, elements, ...args)],
  ['org.modelingvalue.nelumbo.strings.ToInteger',
    (elements, args, functor) => new ToInteger(functor, elements, ...args)],
  ['org.modelingvalue.nelumbo.collections.NList',
    (elements, args, functor) => new NList(functor, elements, args)],
  ['org.modelingvalue.nelumbo.collections.NSet',
    (elements, args, functor) => new NSet(functor, elements, ...args)],
  ['org.modelingvalue.nelumbo.Node',
    (elements, args, functor) => new Node(functor, elements, ...args)],
]);

export function findConstructor(className: string): NodeConstructor | null {
  return registry.get(className) ?? null;
}
