package leaderboard.model

import io.circe.Codec
import io.circe.generic.semiauto

final case class RankedProfile(
  name: String,
  description: String,
  rank: Int,
  score: Score,
)

object RankedProfile {
  implicit val codec: Codec.AsObject[RankedProfile] = semiauto.deriveCodec
}
