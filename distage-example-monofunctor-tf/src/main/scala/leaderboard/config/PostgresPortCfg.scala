package leaderboard.config

final case class PostgresPortCfg(
  host: String,
  port: Int,
) {
  def substitute(s: String): String = {
    s.replace("{host}", host).replace("{port}", port.toString)
  }
}
