package steps

import com.github.tomakehurst.wiremock.client.WireMock
import cucumber.api.scala.{EN, ScalaDsl}
import org.scalatest.Matchers
import play.api.libs.json.Json
import support.Env._
import support.{Db, Env, Http}
import utils.{ConsumerMarshalling, ErrorMarshalling}

class ConsumerSteps extends ScalaDsl with EN with Matchers with ConsumerMarshalling with ErrorMarshalling {

  After() { scenario =>
    Db.truncateVendorsTable()
    WireMock.reset()
  }

  val ConsumerTokenPattern = """^[a-f0-9]{64}$""".r

  Given( """^the Admin Token "(.*?)" is presented$""") { (token: String) =>
    Env.adminToken = token
  }

  When("""^the Create Consumer endpoint "(.*)" is posted a request:$""") { (endpoint: String, json: String) =>
    val (rc, rb) = Http.postJson(endpoint, json.stripMargin)(Map("admin_token" -> adminToken))
    Env.responseCode = rc
    Env.responseBody = rb
  }

  Then("""^the returned status is "(.*?)"$""") { (status: String) =>
    responseCode shouldBe statusCodes(status)
  }

  Then("""^the payload contains a consumerKey of value "(.*?)"$""") { (value: String) =>
    Json.parse(responseBody).validate[Response].asOpt match {
      case Some(actual) => actual.consumerKey shouldBe value
      case None => fail("No valid response found.")
    }
  }

  Then("""^the payload contains a valid consumerToken$""") { () =>
    val actual = Json.parse(responseBody).as[Response]
    actual.consumerToken should fullyMatch regex ConsumerTokenPattern
  }

  Then("""the payload contains a status of value (.*)""") { (status: Int) =>
    Json.parse(responseBody).validate[ErrorMessage].asOpt match {
      case Some(actual) => actual.status shouldBe status
      case None => fail("No valid status code found.")
    }
  }

  Then("""the payload contains message "(.*)"""") { (message: String) =>
    Json.parse(responseBody).validate[ErrorMessage].asOpt match {
      case Some(actual) => actual.message shouldBe message
      case None => fail("No valid message found.")
    }
  }

  Then("""the Consumer "(.*)" has been persisted""") { (consumer: String) =>
    Db.vendorExists(consumer) should be
    true
  }

  Then("""the persisted Consumer "(.*)" has consumerKey "(.*)"""") { (consumer: String, consumerKey: String) =>
    Db.vendorKey(consumer) match {
      case Some(key) => key shouldBe consumerKey
      case None => fail("no consumer found")
    }
  }

  Then("""the persisted Consumer "(.*)" has a valid consumerToken""") { (consumer: String) =>
    Db.vendorToken(consumer) match {
      case Some(token) => token should fullyMatch regex ConsumerTokenPattern
      case None => fail("no consumer found")
    }
  }

}