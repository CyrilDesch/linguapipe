object DeploymentSettings {

  val overrideDockerRegistry = sys.env.get("LOCAL_DOCKER_REGISTRY").isDefined
}
