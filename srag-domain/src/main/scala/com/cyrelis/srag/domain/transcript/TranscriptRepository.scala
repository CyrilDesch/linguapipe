package com.cyrelis.srag.domain.transcript

import java.util.UUID

import com.cyrelis.srag.domain.transcript.Transcript

trait TranscriptRepository[F[_]] {
  def persist(transcript: Transcript): F[Unit]
  def getAll(): F[List[Transcript]]
  def getById(id: UUID): F[Option[Transcript]]
}
