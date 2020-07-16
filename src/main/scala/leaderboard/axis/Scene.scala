package leaderboard.axis

import izumi.distage.model.definition.Axis

/**
  * Choice axis controlling whether third-party services that the application requires
  * should be provided by `distage-framework-docker` when the application starts (`Scene.Managed`),
  * or whether it should try to connect to these services normally, assuming they already exist in the environment (`Scene.Provided`)
  *
  * The set of third-party services required by the application is called a `Scene`, etymology being that the running
  * third-party services that the application depends on are like a set-up scene which an actor (the application) then enters.
  */
object Scene extends Axis {
  case object Managed extends AxisValueDef
  case object Provided extends AxisValueDef
}
