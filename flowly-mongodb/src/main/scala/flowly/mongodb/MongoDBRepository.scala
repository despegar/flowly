package flowly.mongodb

import java.lang
import java.time.Instant
import java.util.Date

import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.mongodb.MongoClient
import com.mongodb.client.model.IndexOptions
import flowly.core.repository.Repository
import flowly.core.repository.model.{Session, Status}
import flowly.core.repository.model.Session.{SessionId, Status}
import flowly.core.{ErrorOr, SessionNotFound}
import javax.persistence.{OptimisticLockException, PersistenceException}
import org.bson.Document
import org.mongojack.JacksonMongoCollection

import scala.jdk.CollectionConverters._
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

class MongoDBRepository(client: MongoClient, databaseName: String, collectionName: String, objectMapper: ObjectMapper with ScalaObjectMapper) extends Repository {

  private val collection: JacksonMongoCollection[Session] = {
    val mongoCollection = client.getDatabase(databaseName).getCollection(collectionName)

    val builder: JacksonMongoCollection.JacksonMongoCollectionBuilder[Session] = JacksonMongoCollection.builder()
    val coll = builder.withObjectMapper(objectMapper).build(mongoCollection, classOf[Session])

    // Initialize sessionId index
    coll.createIndex(Document("sessionId" -> 1.asJava), new IndexOptions().unique(true))
    coll.createIndex(Document("status" -> 1.asJava, "attempts.nextRetry" -> 1.asJava))
    coll

  }

  def getById(sessionId: SessionId): ErrorOr[Session] = {
    Try(collection.findOne(new Document("sessionId", sessionId))) match {
      case Success(null) => Left(SessionNotFound(sessionId))
      case Success(element) => Right(element)
      case Failure(throwable) => Left(new PersistenceException(s"Error getting session $sessionId", throwable))
    }
  }

  def getToRetry(): ErrorOr[List[SessionId]] = {
    Try(collection.find(Document("status" -> Status.TO_RETRY, "attempts.nextRetry" -> Document("$lte" -> Date.from(Instant.now)) ))) match {
      case Success(result) => Right(result.map(_.sessionId).asScala.toList)
      case Failure(throwable) => Left(new PersistenceException("Error getting sessions to retry", throwable))
    }
  }

  def insert(session: Session): ErrorOr[Session] = {
    Try(collection.insert(session)) match {
      case Success(_) => Right(session)
      case Failure(throwable) => Left(new PersistenceException(s"Error inserting session ${session.sessionId}", throwable))
    }
  }

  def update(session: Session): ErrorOr[Session] = {
    Try {
      // Update will replace every document field and it is going to increment in one unit its version
      val document = JacksonMongoCollection.convertToDocument(session, objectMapper, classOf[Session])
      document.remove("version")

      val update = Document("$set" -> document, "$inc" -> Document("version" -> 1.asJava))

      // Condition: there is a session with the same sessionId and version
      val query = Document("sessionId" -> session.sessionId, "version" -> session.version.asJava)

      collection.findAndModify(query, Document(), Document(), collection.serializeFields(update), true, false)

    } match {
      case Success(null) => Left(new OptimisticLockException(s"Session ${session.sessionId} was modified by another transaction"))
      case Success(elem) => Right(elem)
      case Failure(throwable) => Left(throwable)
    }
  }

  private def Document(values:(String, AnyRef)*):Document = new Document(Map(values:_*).asJava)

  private implicit class NumberOps(value:Long) {
    def asJava:lang.Long = lang.Long.valueOf(value)
  }

}