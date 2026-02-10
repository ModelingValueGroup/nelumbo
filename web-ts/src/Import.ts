/**
 * Import - KB merging.
 * Ported from Java: org.modelingvalue.nelumbo.Import
 */

import { List } from 'immutable';
import type { AstElement } from './AstElement';
import { Type } from './Type';
import { Node } from './Node';
import type { KnowledgeBase } from './KnowledgeBase';

/**
 * Import - an import statement that merges another KB.
 */
export class Import extends Node {
  constructor(elements: List<AstElement>, path: string) {
    super(Type.IMPORT, elements, path);
  }

  protected static fromDataImport(data: unknown[], declaration?: Node): Import {
    const imp = Object.create(Import.prototype) as Import;
    (imp as unknown as { _data: unknown[] })._data = data;
    (imp as unknown as { _declaration: Node | undefined })._declaration = declaration ?? imp;
    return imp;
  }

  protected override struct(data: unknown[], declaration?: Node): Node {
    return Import.fromDataImport(data, declaration ?? this.declaration());
  }

  override set(i: number, ...a: unknown[]): Node {
    return super.set(i, ...a);
  }

  /**
   * Get the import name/path.
   */
  name(): string {
    return this.get(0) as string;
  }

  /**
   * Initialize this import in a knowledge base.
   */
  initInKb(knowledgeBase: KnowledgeBase): Import {
    knowledgeBase.doImport(this.name(), this);
    return this;
  }
}
