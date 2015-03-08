package net.mm.composer

package object properties {

  type Error = String

  type PropertiesParser = String => Either[Error, Seq[Property]]

  type PropertiesReader = Either[Error, Seq[Property]] => Either[Error, Seq[Property]]
}
