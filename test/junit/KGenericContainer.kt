package junit

import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

internal fun DockerImageName.createContainer() = KGenericContainer(this)

internal class KGenericContainer(imageName: DockerImageName) : GenericContainer<KGenericContainer>(imageName)
