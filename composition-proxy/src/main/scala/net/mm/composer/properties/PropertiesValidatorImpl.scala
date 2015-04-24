package net.mm.composer.properties

import net.mm.composer.relations.RelationRegistry
import net.mm.composer.utils.SeqTypeFilterSupport._

import scala.annotation.tailrec
import scala.collection.mutable.Map

/**
 * Validator to identify wrong properties that are neither
 * present as fields nor registered as relation.
 */
class PropertiesValidatorImpl(clazz: Class[_])(implicit relationRegistry: RelationRegistry) extends PropertiesReader {

  @tailrec
  private def validate(propsMap: Map[Class[_],Seq[Property]]): Option[Error] = {

    val propsMapIt = propsMap.iterator
    val nextProps = Map.empty[Class[_], Seq[Property]]

    while(propsMapIt.hasNext){
      val (clazz, props) = propsMapIt.next()
      val (relations, fields) = props.typePartition[RelationProperty]

      val checkRequired = fields.map(_.name).toSet --
        clazz.getDeclaredFields.toSeq.map(_.getName) ++
        relations.map(_.name)

      val propsIt = props.iterator
      while(propsIt.hasNext){
        val property = propsIt.next()

        if(checkRequired(property.name)) {
          val relation = relationRegistry.get(clazz, property.name)

          if (relation.isEmpty)
            return Some(s"Unknown ${property.description} for ${clazz.getSimpleName}")

          property match {
            case p: RelationProperty =>
              val target = relation.get.target
              nextProps += target -> nextProps.get(target).fold(p.properties)(_ ++ p.properties)
            case _ =>
          }
        }
      }
    }

    if (nextProps.isEmpty) None else validate(nextProps)
  }

  override def apply(parsedProperties: Either[Error, Seq[Property]]): Either[Error, Seq[Property]] = {
    parsedProperties.right.flatMap{ properties =>
      validate(Map(clazz -> properties)).toLeft(properties)
    }
  }
}
