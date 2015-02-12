package net.mm.example.services

import net.mm.composer.relations.Relation._

class UserService extends FakeService {
  val getUsers: RelationSource[String, User] = _.map(u => (u, User(u))).toMap.asFuture
}

case class User(username: String)