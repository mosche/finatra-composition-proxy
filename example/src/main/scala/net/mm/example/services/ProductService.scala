package net.mm.example.services

import net.mm.composer.relations.Relation._

class ProductService extends FakeService {
  private val products = Seq(
    Product(1, "Apple iBook", "computer", "laptop"), Product(2, "WD My Passport External", "computer", "hardware", "storage"),
    Product(3, "AudioStar Audio Speaker Pair", "audio", "speaker"), Product(4, "Boss Audio In-Dash", "audio", "car"), Product(5, "Stereo Audio Cable", "audio"),
    Product(6, "Case Logic Laptop", "bag", "laptop")
  ).map(p => (p.id, p)).toMap

  val allProducts = products.values.toSeq
  val getProducts: Executor[Int, Product] = products.filterKeys(_).asFuture
  val getProductsByCategories: Executor[String, Seq[Product]] = _.map(cat => (cat, allProducts.filter(_.categoryIds.contains(cat)))).filterNot(_._2.isEmpty).toMap.asFuture
  val getCategories: Executor[String, Category] = _.map(c => (c, Category(c))).toMap.asFuture
  val getCategoriesByProduct: Executor[Int, Seq[Category]] = products.filterKeys(_).mapValues(p => p.categoryIds.map(Category)).asFuture
  val getCategorySize: Executor[String, Int] = getProductsByCategories(_).map(_.mapValues(_.size))
}

case class Product(id: Int, title: String, categoryIds: String*)

case class Category(id: String)
