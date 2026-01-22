terraform {
  required_providers {
    helm = {
      source  = "hashicorp/helm"
      version = "2.0.2"
    }
  }
}

provider "helm" {
  kubernetes {
    config_path    = "~/.kube/config"
  }
}

