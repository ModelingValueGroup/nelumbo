/**
 * ConstructorRegistry - maps Java class names to TS NodeConstructor functions.
 * Used by @annotation handling in BaseSyntax to wire up builtin classes.
 *
 * Also implements the functor setter mechanism matching Java's
 * KnowledgeBase.registerFunctorSetter() pattern.
 * @JAVA_REF org.modelingvalue.nelumbo.KnowledgeBase#FUNCTOR_REGISTRATION
 */

import type { NodeConstructor, Functor } from './patterns/Functor';
import { Node } from './Node';
import { Equal } from './logic/Equal';
import { NBoolean } from './logic/NBoolean';
import { Not } from './logic/Not';
import { And } from './logic/And';
import { Or } from './logic/Or';
import { ExistentialQuantifier } from './logic/ExistentialQuantifier';
import { UniversalQuantifier } from './logic/UniversalQuantifier';
import { NInteger, setNIntegerFunctor } from './integers/NInteger';
import { Add } from './integers/Add';
import { Multiply } from './integers/Multiply';
import { GreaterThan } from './integers/GreaterThan';
import { NString, setNStringFunctor } from './strings/NString';
import { Concat } from './strings/Concat';
import { Length } from './strings/Length';
import { ToInteger } from './strings/ToInteger';
import { NList } from './collections/NList';
import { NSet } from './collections/NSet';

const registry = new Map<string, NodeConstructor>([
  ['org.modelingvalue.nelumbo.logic.Equal',
    (elements, args, functor) => new Equal(functor, elements, args[0], args[1])],
  // @JAVA_REF org.modelingvalue.nelumbo.logic.NBoolean (registered via @NelumboConstructor)
  ['org.modelingvalue.nelumbo.logic.NBoolean',
    (elements, _args, functor) => new NBoolean(functor, elements)],
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

/**
 * Functor setters - maps class names to callbacks that set static FUNCTOR fields.
 * @JAVA_REF org.modelingvalue.nelumbo.KnowledgeBase#FUNCTOR_REGISTRATION
 */
const functorSetters = new Map<string, (f: Functor) => void>([
  ['org.modelingvalue.nelumbo.logic.And', (f) => { And.FUNCTOR = f; }],
  ['org.modelingvalue.nelumbo.logic.Or', (f) => { Or.FUNCTOR = f; }],
  ['org.modelingvalue.nelumbo.logic.Not', (f) => { Not.FUNCTOR = f; }],
  ['org.modelingvalue.nelumbo.logic.ExistentialQuantifier', (f) => { ExistentialQuantifier.FUNCTOR = f; }],
  ['org.modelingvalue.nelumbo.logic.UniversalQuantifier', (f) => { UniversalQuantifier.FUNCTOR = f; }],
  ['org.modelingvalue.nelumbo.integers.NInteger', (f) => { setNIntegerFunctor(f); }],
  ['org.modelingvalue.nelumbo.strings.NString', (f) => { setNStringFunctor(f); }],
]);

// Reverse lookup: NodeConstructor function reference -> class name
const reverseRegistry = new Map<NodeConstructor, string>();
for (const [className, ctor] of registry) {
  reverseRegistry.set(ctor, className);
}

export function findConstructor(className: string): NodeConstructor | null {
  return registry.get(className) ?? null;
}

/**
 * Check if a functor's constructor has a registered functor setter, and call it.
 * Called from KnowledgeBase.register() to match Java's init() behavior.
 * @JAVA_REF org.modelingvalue.nelumbo.KnowledgeBase#init (FUNCTOR_REGISTRATION check)
 */
export function checkFunctorSetter(constructorFn: NodeConstructor, functor: Functor): void {
  const className = reverseRegistry.get(constructorFn);
  if (className) {
    const setter = functorSetters.get(className);
    if (setter) {
      setter(functor);
      functorSetters.delete(className);
    }
  }
}
