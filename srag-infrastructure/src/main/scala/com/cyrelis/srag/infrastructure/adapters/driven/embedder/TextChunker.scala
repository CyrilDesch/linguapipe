package com.cyrelis.srag.infrastructure.adapters.driven.embedder

import scala.jdk.CollectionConverters.*

import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.splitter.DocumentSplitters

object TextChunker:

  def chunkText(text: String, maxTokensPerChunk: Int): List[String] =
    if text.isEmpty then List.empty
    else
      val document = Document.from(text)
      val splitter = DocumentSplitters.recursive(maxTokensPerChunk, (maxTokensPerChunk * 0.2).toInt)
      val segments = splitter.split(document)

      segments.asScala.map(_.text()).filter(_.nonEmpty).toList
