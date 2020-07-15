package leaderboard.axis

import izumi.distage.model.definition.Axis

/**
  * Choice axis controlling whether external services that the application requires
  * should be provided by `distage-framework-docker` when the application starts (`Services.Docker`),
  * or whether it should try to connect to external services normally, assuming the environment provides them (`Services.Prod`)
  */
object Services extends Axis {
  case object Docker extends AxisValueDef
  case object Prod extends AxisValueDef
}
