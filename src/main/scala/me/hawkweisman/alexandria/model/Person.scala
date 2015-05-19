package me.hawkweisman.alexandria
package model

import scala.util.Sorting

trait Person {
  def firstName: String
  def middleName: String
  def lastName: String
}
object FirstNameOrdering extends Ordering[Person] {
  def compare(a: Person, b: Person) = a.firstName compare b.firstName
}

object LastNameOrdering extends Ordering[Person] {
  def compare(a: Person, b: Person) = a.firstName compare b.firstName
}

case class Author(
  id: Integer,
  firstName: String,
  middleName: String,
  lastName: String
  ) extends Person

case class User(
  id: Integer,
  firstName: String,
  middleName: String,
  lastName: String,
  username: String,
  hash: String
  ) extends Person
