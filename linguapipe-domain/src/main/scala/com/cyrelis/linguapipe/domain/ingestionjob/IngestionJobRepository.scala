package com.cyrelis.linguapipe.domain.ingestionjob

import java.time.Instant
import java.util.UUID

trait IngestionJobRepository[F[_]] {
  def create(job: IngestionJob): F[IngestionJob]
  def update(job: IngestionJob): F[IngestionJob]
  def findById(jobId: UUID): F[Option[IngestionJob]]
  def listRunnable(now: Instant, limit: Int): F[List[IngestionJob]]
}
