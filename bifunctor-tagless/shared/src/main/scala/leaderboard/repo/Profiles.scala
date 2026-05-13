package leaderboard.repo

import distage.Lifecycle
import izumi.functional.bio.{Applicative2, F, Primitives2}
import leaderboard.model.{QueryFailure, UserId, UserProfile}

trait Profiles[F[_, _]] {
  def setProfile(userId: UserId, profile: UserProfile): F[QueryFailure, Unit]
  def getProfile(userId: UserId): F[QueryFailure, Option[UserProfile]]
}

object Profiles {
  // In-memory dummy used by both JVM tests and the in-browser simulation.
  final class Dummy[F[+_, +_]: Applicative2: Primitives2]
    extends Lifecycle.LiftF[F[Nothing, _], Profiles[F]](for {
      state <- F.mkRef(Map.empty[UserId, UserProfile])
    } yield {
      new Profiles[F] {
        override def setProfile(userId: UserId, profile: UserProfile): F[Nothing, Unit] =
          state.update_(_ + (userId -> profile))

        override def getProfile(userId: UserId): F[Nothing, Option[UserProfile]] =
          state.get.map(_.get(userId))
      }
    })
}
