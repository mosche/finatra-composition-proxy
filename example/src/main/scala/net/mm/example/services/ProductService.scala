package net.mm.example.services

import net.mm.composer.relations.Relation._

class ProductService extends FakeService {
  private val products = Seq(
    Product(1, "computer", "laptop"), Product(2, "computer", "hardware", "storage"),
    Product(3, "audio", "speaker"), Product(4, "audio", "car"), Product(5, "audio"),
    Product(6, "bag", "laptop")
  ).map(p => (p.id, p)).toMap

  val allProducts = products.values.toSeq
  val getProducts: Executor[Int, Product] = products.filterKeys(_).asFuture
  val getProductsByCategories: Executor[String, Seq[Product]] = _.map(cat => (cat, allProducts.filter(_.categoryIds.contains(cat)))).filterNot(_._2.isEmpty).toMap.asFuture
  val getCategories: Executor[String, Category] = _.map(c => (c, Category(c))).toMap.asFuture
  val getCategoriesByProduct: Executor[Int, Seq[Category]] = products.filterKeys(_).mapValues(p => p.categoryIds.map(Category)).asFuture
  val getCategorySize: Executor[String, Int] = getProductsByCategories(_).map(_.mapValues(_.size))
}

case class Product(id: Int, categoryIds: String*)

case class Category(id: String)
