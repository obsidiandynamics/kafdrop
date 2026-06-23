resource "helm_release" "kafdrop" {
  name       = "my-kafdrop"
  repository = "https://bedag.github.io/helm-charts/"
  chart      = "kafdrop"
  version    = "0.2.3"

  set {
    name  = "deployment.replicaCount"
    value = "1"
  }

  set {
    name  = "deployment.image.tag"
    value = "latest"
  }

  set {
    name  = "deployment.image.repository"
    value = "obsidiandynamics/kafdrop"
  }

  set {
    name  = "config.server.port"
    value = "9000"
  }

  set {
    name  = "config.jvm[0]"
    value = "-Xms128M"
  }

  set {
    name  = "config.jvm[1]"
    value = "-Xmx256M"
  }

  set {
    name  = "service.port"
    value = "9000"
  }
  
  set {
    name  = "service.nodePort"
    value = "9000"
  }
  
  set {
    name  =  "config.kafka.connections[0]"
    value =   "localhost:9092"
  }

  set {
    name  =  "timezone"
    value =   "Asia/Tehran"
  }
}
