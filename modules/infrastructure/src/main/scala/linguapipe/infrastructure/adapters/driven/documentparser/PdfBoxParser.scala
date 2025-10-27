package linguapipe.infrastructure.adapters.driven.documentparser

import java.util.Base64

import zio.*

import linguapipe.application.ports.driven.DocumentParserPort
import linguapipe.domain.IngestPayload

final class PdfBoxParser extends DocumentParserPort {

  override def parseDocument(payload: IngestPayload.Base64Document): Task[String] =
    payload.mediaType match {
      case mt if mt.startsWith("application/pdf") =>
        parsePdf(payload.content)
      case mt if mt.contains("officedocument") || mt.contains("msword") =>
        parseOfficeDocument(payload.content, mt)
      case mt if mt.startsWith("text/") =>
        parseTextDocument(payload.content)
      case unsupported =>
        ZIO.fail(new UnsupportedOperationException(s"Unsupported media type: $unsupported"))
    }

  private def parsePdf(base64Content: String): Task[String] =
    ZIO.attempt {
      // TODO: Implement with Apache PDFBox
      // val bytes = Base64.getDecoder.decode(base64Content)
      // val document = PDDocument.load(bytes)
      // try {
      //   val stripper = new PDFTextStripper()
      //   stripper.getText(document)
      // } finally {
      //   document.close()
      // }

      // Placeholder implementation
      "[PDF text extraction not yet implemented]"
    }

  private def parseOfficeDocument(base64Content: String, mediaType: String): Task[String] =
    ZIO.attempt {
      // TODO: Implement with Apache POI for .docx, .xlsx, etc.
      s"[Office document extraction not yet implemented for $mediaType]"
    }

  private def parseTextDocument(base64Content: String): Task[String] =
    ZIO.attempt {
      val bytes = Base64.getDecoder.decode(base64Content)
      new String(bytes, "UTF-8")
    }
}
