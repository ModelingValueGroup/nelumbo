/**
 * Pattern registry - registers all pattern subclasses with the Pattern base class.
 * This file must be imported before any Pattern factory methods are used.
 */

import { Pattern } from './Pattern';
import { AlternationPattern } from './AlternationPattern';
import { NodeTypePattern } from './NodeTypePattern';
import { OptionalPattern } from './OptionalPattern';
import { RepetitionPattern } from './RepetitionPattern';
import { SequencePattern } from './SequencePattern';
import { TokenTextPattern } from './TokenTextPattern';
import { TokenTypePattern } from './TokenTypePattern';

// Register all pattern classes
Pattern.registerPatternClass('AlternationPattern', AlternationPattern);
Pattern.registerPatternClass('NodeTypePattern', NodeTypePattern);
Pattern.registerPatternClass('OptionalPattern', OptionalPattern);
Pattern.registerPatternClass('RepetitionPattern', RepetitionPattern);
Pattern.registerPatternClass('SequencePattern', SequencePattern);
Pattern.registerPatternClass('TokenTextPattern', TokenTextPattern);
Pattern.registerPatternClass('TokenTypePattern', TokenTypePattern);
