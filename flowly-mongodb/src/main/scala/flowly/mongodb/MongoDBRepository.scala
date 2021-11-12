package flowly.mongodb

import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import com.mongodb.client.model.{Filters, IndexOptions}
import com.mongodb.client.{MongoClient, MongoCursor}
import flowly.core.compat.CompatUtils
import flowly.core.repository.Repository
import flowly.core.repository.model.Session.SessionId
import flowly.core.repository.model.{Session, Status}
import flowly.core.{ErrorOr, RepositoryError, SessionNotFound}
import org.bson.{Document, UuidRepresentation}
import org.mongojack.JacksonMongoCollection

import java.lang
import java.time.Instant
import java.util.Date
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

class MongoDBRepository(client: MongoClient, databaseName: String, collectionName: String, objectMapper: ObjectMapper with ScalaObjectMapper) extends Repository {

  protected val collection: JacksonMongoCollection[Session] = {
    val mongoCollection = client.getDatabase(databaseName).getCollection(collectionName, classOf[Session])
    val builder: JacksonMongoCollection.JacksonMongoCollectionBuilder = JacksonMongoCollection.builder()
    //TODO ver si va STANDARD o UNSPECIFIED...
    val coll = builder.withObjectMapper(objectMapper).build(mongoCollection, classOf[Session], UuidRepresentation.STANDARD)

    // Initialize sessionId index
    coll.createIndex(Document("sessionId" -> 1.asJava), new IndexOptions().unique(true))
    coll.createIndex(Document("status" -> 1.asJava, "attempts.nextRetry" -> 1.asJava))
    coll

  }

  def getById(sessionId: SessionId): ErrorOr[Session] = {
    Try(collection.findOne(new Document("sessionId", sessionId))) match {
      case Success(null)      => Left(SessionNotFound(sessionId))
      case Success(element)   => Right(element)
      case Failure(throwable) => Left(RepositoryError(s"Error getting session $sessionId", throwable))
    }
  }

  def getToRetry: ErrorOr[Iterator[SessionId]] = {
    Try(collection.find(Document("status" -> Status.TO_RETRY, "attempts.nextRetry" -> Document("$lte" -> Date.from(Instant.now)) ))) match {
      case Success(result)    => Right(result.sort(Document("attempts.nextRetry" -> 1.asJava)).map(_.sessionId).iterator())
      case Failure(throwable) => Left(RepositoryError("Error getting sessions to retry", throwable))
    }
  }

  private[flowly] def insert(session: Session): ErrorOr[Session] = {
    Try(collection.insert(session)) match {
      case Success(_)         => Right(session)
      case Failure(throwable) => Left(RepositoryError(s"Error inserting session ${session.sessionId}", throwable))
    }
  }

  private[flowly] def update(session: Session): ErrorOr[Session] = {
    val sessionWithVersionInc = session.copy(version = session.version + 1)
    Try {
      import com.mongodb.client.model.{FindOneAndReplaceOptions, ReturnDocument}
      val returnDocAfterReplace = new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER).upsert(false)
      // Update will replace every document field and it is going to increment in one unit its version
      collection.findOneAndReplace(Filters.and(Filters.eq("sessionId", session.sessionId), Filters.eq("version", session.version.asJava)), sessionWithVersionInc, returnDocAfterReplace)

    } match {
      //TODO: ver si esto es posible...
      //case Success(null) => Left(new OptimisticLockException(s"Session ${session.sessionId} was modified by another transaction"))
      case Success(elem)      => Right(elem)
      case Failure(throwable) => Left(throwable)
    }
  }

  protected def Document(values:(String, AnyRef)*):Document = new Document(CompatUtils.asJavaMap(values:_*))

  protected  implicit class NumberOps(value:Long) {
    def asJava:lang.Long = lang.Long.valueOf(value)
  }

  protected implicit def cursor2iterator[T](cursor:MongoCursor[T]):Iterator[T] = new Iterator[T] {
    override def hasNext: Boolean = cursor.hasNext
    override def next(): T = cursor.next()
  }

}