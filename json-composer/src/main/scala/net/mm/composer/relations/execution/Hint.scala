package net.mm.composer.relations.execution

sealed trait Hint

/**
 * Hints affecting execution / execution plan optimization.
 */
trait ExecutionHint extends Hint

object ExecutionHint {

  /**
   * Non-bijective relations can't be reversed or optimized.
   */
  object NonBijective extends ExecutionHint
}

/**
 * Hints affecting serialization.
 */
trait SerializationHint extends Hint

object SerializationHint{

  /**
   * Present relation target as array.
   */
  object Array extends SerializationHint
}
