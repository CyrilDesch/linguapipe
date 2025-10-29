package com.cyrelis.linguapipe.infrastructure.adapters.driven.documentparser

import java.util.Base64

import com.cyrelis.linguapipe.application.errors.PipelineError
import com.cyrelis.linguapipe.application.ports.driven.DocumentParserPort
import com.cyrelis.linguapipe.infrastructure.resilience.ErrorMapper
import zio.*

final class PdfBoxParser extends DocumentParserPort {

  override def parseDocument(documentContent: String, mediaType: String): ZIO[Any, PipelineError, String] =
    ErrorMapper.mapDocumentParserError {
      mediaType match {
        case mt if mt.startsWith("application/pdf") =>
          parsePdf(documentContent)
        case mt if mt.contains("officedocument") || mt.contains("msword") =>
          parseOfficeDocument(documentContent, mt)
        case mt if mt.startsWith("text/") =>
          parseTextDocument(documentContent)
        case unsupported =>
          ZIO.fail(new UnsupportedOperationException(s"Unsupported media type: $unsupported"))
      }
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
