package leaderboard.axis

import izumi.distage.model.definition.Axis

/**
  * Choice axis controlling whether third-party services that the application requires
  * should be provided by `distage-framework-docker` or another orchestrator when the application starts (`Scene.Managed`),
  * or whether it should try to connect to these services as if they already exist in the environment (`Scene.Provided`)
  *
  * The set of third-party services required by the application is called a `Scene`, etymology being that the running
  * third-party services that the application depends on are like a scene that is prepared for for the actor (the application)
  * to enter.
  */
object Scene extends Axis {
  case object Managed extends AxisValueDef
  case object Provided extends AxisValueDef
}
