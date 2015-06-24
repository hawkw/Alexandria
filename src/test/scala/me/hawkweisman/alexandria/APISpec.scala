import com.mchange.v2.c3p0.ComboPooledDataSource

import me.hawkweisman.alexandria.controllers.APIController
import me.hawkweisman.alexandria.controllers.swagger.AlexandriaSwagger

import org.scalatest.{Inside, BeforeAndAfter, Matchers,OptionValues}
import org.scalatra.test.scalatest._

import org.json4s._
import org.json4s.native.JsonMethods._

import me.hawkweisman.alexandria.model.Book
import me.hawkweisman.alexandria.model.Tables._

import slick.driver.H2Driver.api._

import scala.concurrent.Await
import scala.concurrent.duration._

class APISpec extends ScalatraWordSpec
  with Matchers
  with Inside
  with BeforeAndAfter
  with OptionValues {

  protected implicit lazy val jsonFormats: Formats = DefaultFormats

  implicit val swagger = new AlexandriaSwagger
  val cpds = new ComboPooledDataSource
  val db = Database.forDataSource(cpds)
  addServlet(new APIController(db), "/*")

  before {
    // block on creating fresh DB before running test
    Await.ready(db run createSchemaAction, Duration.Inf)
  }

  after {
    // block on dropping tables from DB before running next test
    Await.ready(db run dropTablesAction, Duration.Inf)
  }

  "The GET /book/{isbn} route" when {
    "the requested book is not in the database" should {
      "add the book to the database and return it" in {
        // TODO: assume that the internet is available here
        get("/book/ISBN:9780980200447") {
          //info(body) //uncomment this if you need to look at the books that are happening
          status should equal (201)
          val parsedBook = parse(body).extract[Book]
          inside (parsedBook) {
            case Book(isbn, title,subtitle,byline,pages,publishedDate,publisher,weight) =>
              isbn shouldEqual "ISBN:9780980200447"
              title shouldEqual "Slow reading"
              subtitle should not be 'defined
              byline shouldEqual "by John Miedema."
              pages shouldEqual 92
              publisher shouldEqual "Litwin Books"
              publishedDate shouldEqual "March 2009"
              weight.value shouldEqual "1 grams"
          }
        }
      }
    }
    "the requested book is already in the database" should {
      "return the requested book" in {
        Await.ready(db.run(books += Book(
          isbn          = "ISBN:9780980200447",
          title         = "Slow reading",
          subtitle      = None,
          byline        = "by John Miedema.",
          pages         = 92,
          publishedDate = "March 2009",
          publisher     = "Litwin Books",
          weight        = Some("1 grams")
        )), Duration.Inf)
        get("/book/ISBN:9780980200447") {
          //info(body) //uncomment this if you need to look at the books that are happening
          status should equal (200)
          val parsedBook = parse(body).extract[Book]
          inside (parsedBook) {
            case Book(isbn, title,subtitle,byline,pages,publishedDate,publisher,weight) =>
              isbn shouldEqual "ISBN:9780980200447"
              title shouldEqual "Slow reading"
              subtitle should not be 'defined
              byline shouldEqual "by John Miedema."
              pages shouldEqual 92
              publisher shouldEqual "Litwin Books"
              publishedDate shouldEqual "March 2009"
              weight.value shouldEqual "1 grams"
          }
        }
      }
    }
  }
  "The GET /books/ route" when {
    "the requested number of books is greater than the number of books in the database" should {
      "return all the books" in {
        Await.ready(db.run(
          books += Book(
              isbn          = "ISBN:9780980200447",
              title         = "Slow reading",
              subtitle      = None,
              byline        = "by John Miedema.",
              pages         = 92,
              publishedDate = "March 2009",
              publisher     = "Litwin Books",
              weight        = Some("1 grams")
            )
          ), Duration.Inf)
          Await.ready(db.run(
            books += Book(
              isbn          = "ISBN:0201558025",
              title         = "Concrete mathematics",
              subtitle      = Some("a foundation for computer science"),
              byline        = "Ronald L. Graham, Donald E. Knuth, Oren Patashnik.",
              pages         = 657,
              publishedDate = "1994",
              publisher     = "Addison-Wesley",
              weight        = None
            )
        ), Duration.Inf)

        get("/books/") {
          status should equal (200)
          val books = parse(body).extract[Seq[Book]]
          books should have length 2
          books should contain (
            Book(
              isbn          = "ISBN:9780980200447",
              title         = "Slow reading",
              subtitle      = None,
              byline        = "by John Miedema.",
              pages         = 92,
              publishedDate = "March 2009",
              publisher     = "Litwin Books",
              weight        = Some("1 grams")
            )
          )
          books should contain (
            Book(
              isbn          = "ISBN:0201558025",
              title         = "Concrete mathematics",
              subtitle      = Some("a foundation for computer science"),
              byline        = "Ronald L. Graham, Donald E. Knuth, Oren Patashnik.",
              pages         = 657,
              publishedDate = "1994",
              publisher     = "Addison-Wesley",
              weight        = None
            )
          )
        }
      }
    }
    "the requested number of books is less than the number of books in the database" should {
      "return only the requested amount" in {
        Await.ready(db.run(
          books += Book(
              isbn          = "ISBN:9780980200447",
              title         = "Slow reading",
              subtitle      = None,
              byline        = "by John Miedema.",
              pages         = 92,
              publishedDate = "March 2009",
              publisher     = "Litwin Books",
              weight        = Some("1 grams")
            )
          ), Duration.Inf)
          Await.ready(db.run(
            books += Book(
              isbn          = "ISBN:0201558025",
              title         = "Concrete mathematics",
              subtitle      = Some("a foundation for computer science"),
              byline        = "Ronald L. Graham, Donald E. Knuth, Oren Patashnik.",
              pages         = 657,
              publishedDate = "1994",
              publisher     = "Addison-Wesley",
              weight        = None
            )
        ), Duration.Inf)
        Await.ready(db.run(
          books += Book(
            isbn          = "ISBN:9780201896831",
            title         = "The Art of Computer Programming, Vol. 1",
            subtitle      = Some("Fundamental Algorithms"),
            byline        = "by Donald E. Knuth.",
            pages         = 672,
            publishedDate = "1997",
            publisher     = "Addison-Wesley",
            weight        = Some("2.5 pounds")
          )
      ), Duration.Inf)
      get("/books?offset=0&count=2") {
        status should equal (200)
        val books = parse(body).extract[Seq[Book]]
        books should have length 2
        books should contain (
          Book(
            isbn          = "ISBN:9780980200447",
            title         = "Slow reading",
            subtitle      = None,
            byline        = "by John Miedema.",
            pages         = 92,
            publishedDate = "March 2009",
            publisher     = "Litwin Books",
            weight        = Some("1 grams")
          )
        )
        books should contain (
          Book(
            isbn          = "ISBN:0201558025",
            title         = "Concrete mathematics",
            subtitle      = Some("a foundation for computer science"),
            byline        = "Ronald L. Graham, Donald E. Knuth, Oren Patashnik.",
            pages         = 657,
            publishedDate = "1994",
            publisher     = "Addison-Wesley",
            weight        = None
          )
        )
        }
      }
      "return the requested amount, starting at an offset" in {
        Await.ready(db.run(
          books += Book(
              isbn          = "ISBN:9780980200447",
              title         = "Slow reading",
              subtitle      = None,
              byline        = "by John Miedema.",
              pages         = 92,
              publishedDate = "March 2009",
              publisher     = "Litwin Books",
              weight        = Some("1 grams")
            )
          ), Duration.Inf)
          Await.ready(db.run(
            books += Book(
              isbn          = "ISBN:0201558025",
              title         = "Concrete mathematics",
              subtitle      = Some("a foundation for computer science"),
              byline        = "Ronald L. Graham, Donald E. Knuth, Oren Patashnik.",
              pages         = 657,
              publishedDate = "1994",
              publisher     = "Addison-Wesley",
              weight        = None
            )
        ), Duration.Inf)
        Await.ready(db.run(
          books += Book(
            isbn          = "ISBN:9780201896831",
            title         = "The Art of Computer Programming, Vol. 1",
            subtitle      = Some("Fundamental Algorithms"),
            byline        = "by Donald E. Knuth.",
            pages         = 672,
            publishedDate = "1997",
            publisher     = "Addison-Wesley",
            weight        = Some("2.5 pounds")
          )
      ), Duration.Inf)
      get("/books?offset=1&count=2") {
        status should equal (200)
        val books = parse(body).extract[Seq[Book]]
        books should have length 2
        books should contain (
          Book(
            isbn          = "ISBN:9780201896831",
            title         = "The Art of Computer Programming, Vol. 1",
            subtitle      = Some("Fundamental Algorithms"),
            byline        = "by Donald E. Knuth.",
            pages         = 672,
            publishedDate = "1997",
            publisher     = "Addison-Wesley",
            weight        = Some("2.5 pounds")
          )
        )
        books should contain (
          Book(
            isbn          = "ISBN:0201558025",
            title         = "Concrete mathematics",
            subtitle      = Some("a foundation for computer science"),
            byline        = "Ronald L. Graham, Donald E. Knuth, Oren Patashnik.",
            pages         = 657,
            publishedDate = "1994",
            publisher     = "Addison-Wesley",
            weight        = None
          )
        )
        }
      }
    }
    "the requested amount is negative" should {
      "return all the books" in {
        Await.ready(db.run(
          books += Book(
              isbn          = "ISBN:9780980200447",
              title         = "Slow reading",
              subtitle      = None,
              byline        = "by John Miedema.",
              pages         = 92,
              publishedDate = "March 2009",
              publisher     = "Litwin Books",
              weight        = Some("1 grams")
            )
          ), Duration.Inf)
          Await.ready(db.run(
            books += Book(
              isbn          = "ISBN:0201558025",
              title         = "Concrete mathematics",
              subtitle      = Some("a foundation for computer science"),
              byline        = "Ronald L. Graham, Donald E. Knuth, Oren Patashnik.",
              pages         = 657,
              publishedDate = "1994",
              publisher     = "Addison-Wesley",
              weight        = None
            )
        ), Duration.Inf)
        Await.ready(db.run(
          books += Book(
            isbn          = "ISBN:9780201896831",
            title         = "The Art of Computer Programming, Vol. 1",
            subtitle      = Some("Fundamental Algorithms"),
            byline        = "by Donald E. Knuth.",
            pages         = 672,
            publishedDate = "1997",
            publisher     = "Addison-Wesley",
            weight        = Some("2.5 pounds")
          )
      ), Duration.Inf)

        get("/books?offset=0&count=-1") {
          status should equal (200)
          val books = parse(body).extract[Seq[Book]]
          books should have length 3
          books should contain (
            Book(
              isbn          = "ISBN:9780980200447",
              title         = "Slow reading",
              subtitle      = None,
              byline        = "by John Miedema.",
              pages         = 92,
              publishedDate = "March 2009",
              publisher     = "Litwin Books",
              weight        = Some("1 grams")
            )
          )
          books should contain (
            Book(
              isbn          = "ISBN:0201558025",
              title         = "Concrete mathematics",
              subtitle      = Some("a foundation for computer science"),
              byline        = "Ronald L. Graham, Donald E. Knuth, Oren Patashnik.",
              pages         = 657,
              publishedDate = "1994",
              publisher     = "Addison-Wesley",
              weight        = None
            )
          )
          books should contain (
            Book(
              isbn          = "ISBN:9780201896831",
              title         = "The Art of Computer Programming, Vol. 1",
              subtitle      = Some("Fundamental Algorithms"),
              byline        = "by Donald E. Knuth.",
              pages         = 672,
              publishedDate = "1997",
              publisher     = "Addison-Wesley",
              weight        = Some("2.5 pounds")
            )
          )
        }
      }
      "return all the books, starting at an offset" in {
        Await.ready(db.run(
          books += Book(
              isbn          = "ISBN:9780980200447",
              title         = "Slow reading",
              subtitle      = None,
              byline        = "by John Miedema.",
              pages         = 92,
              publishedDate = "March 2009",
              publisher     = "Litwin Books",
              weight        = Some("1 grams")
            )
          ), Duration.Inf)
          Await.ready(db.run(
            books += Book(
              isbn          = "ISBN:0201558025",
              title         = "Concrete mathematics",
              subtitle      = Some("a foundation for computer science"),
              byline        = "Ronald L. Graham, Donald E. Knuth, Oren Patashnik.",
              pages         = 657,
              publishedDate = "1994",
              publisher     = "Addison-Wesley",
              weight        = None
            )
        ), Duration.Inf)
        Await.ready(db.run(
          books += Book(
            isbn          = "ISBN:9780201896831",
            title         = "The Art of Computer Programming, Vol. 1",
            subtitle      = Some("Fundamental Algorithms"),
            byline        = "by Donald E. Knuth.",
            pages         = 672,
            publishedDate = "1997",
            publisher     = "Addison-Wesley",
            weight        = Some("2.5 pounds")
          )
      ), Duration.Inf)

        get("/books?offset=1&count=-1") {
          status should equal (200)
          val books = parse(body).extract[Seq[Book]]
          books should have length 2
          books should contain (
            Book(
              isbn          = "ISBN:0201558025",
              title         = "Concrete mathematics",
              subtitle      = Some("a foundation for computer science"),
              byline        = "Ronald L. Graham, Donald E. Knuth, Oren Patashnik.",
              pages         = 657,
              publishedDate = "1994",
              publisher     = "Addison-Wesley",
              weight        = None
            )
          )
          books should contain (
            Book(
              isbn          = "ISBN:9780201896831",
              title         = "The Art of Computer Programming, Vol. 1",
              subtitle      = Some("Fundamental Algorithms"),
              byline        = "by Donald E. Knuth.",
              pages         = 672,
              publishedDate = "1997",
              publisher     = "Addison-Wesley",
              weight        = Some("2.5 pounds")
            )
          )
        }
      }
    }
  }

}
