package steps

import com.github.tomakehurst.wiremock.client.WireMock
import cucumber.api.scala.{EN, ScalaDsl}
import org.scalatest.Matchers
import play.api.libs.json.Json
import support.Http
import support.Env._

class ReleaseSteps extends ScalaDsl with EN with Matchers {

  When("""the remote release service is unavailable""") { () =>
    //nothing to do
  }

  When("""^posting JSON on the "(.*?)" endpoint:$""") { (endpoint: String, payload: String) =>
    val headers = Map("consumer_key" -> consumerKey, "consumer_token" -> consumerToken)
    val (rc, rb) = Http.postJson(endpoint, payload.stripMargin)(headers)
    responseCode = rc
    responseBody = rb
  }

  Then("""^the status received is "(.*?)"$""") { (status: String) =>
    responseCode shouldBe statusCodes(status)
  }

  sealed case class ApiResponse(status: Int, id: Option[String], message: String)

  implicit val responseReads = Json.reads[ApiResponse]

  Then("""^the response is:$""") { (expectedJson: String) =>
    val actual = Json.parse(responseBody).as[ApiResponse]
    val expected = Json.parse(expectedJson.stripMargin).as[ApiResponse]
    actual shouldBe expected
  }

  Given("""^the remote release service returns a "(.*)" response:$""") { (status: String, payload: String) =>
    import WireMock._
    stubFor(post(urlEqualTo("/release"))
      .willReturn(aResponse()
        .withBody(payload.stripMargin)
        .withStatus(statusCodes(status))
      )
    )
  }

  Then("""^the remote release service expects payload and appropriate headers:$""") { (payload: String) =>
    import WireMock._
    verify(postRequestedFor(urlEqualTo("/release"))
      .withRequestBody(equalToJson(payload.stripMargin))
      .withHeader("access_token", equalTo("default_token"))
      .withHeader("consumer", equalTo("groovy"))
    )
  }

  And("""^the remote release service expects NO posts$""") { () =>
    import WireMock._
    verify(0, postRequestedFor(urlEqualTo("/release")))
  }

}
