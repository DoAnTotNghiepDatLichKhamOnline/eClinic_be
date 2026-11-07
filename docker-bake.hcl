# Builds the 8 app images in one BuildKit session, so the Maven "build" stage of the Dockerfile runs once.
# CI (.github/workflows/deploy.yml) pushes them to GHCR tagged with the commit SHA:
#   TAG=<sha> docker buildx bake --push
# Local check: docker buildx bake --print   |   TAG=test docker buildx bake --load

variable "REGISTRY" {
  # GHCR names must be lowercase.
  default = "ghcr.io/doantotnghiepdatlichkhamonline"
}

variable "TAG" {
  default = "dev"
}

group "default" {
  targets = ["app"]
}

target "app" {
  name = m
  matrix = {
    m = [
      "identity-service",
      "catalog-service",
      "booking-service",
      "medical-service",
      "notification-service",
      "chatbot-service",
      "report-service",
      "api-gateway",
    ]
  }
  context    = "."
  dockerfile = "Dockerfile"
  args       = { MODULE = m }
  tags       = ["${REGISTRY}/eclinic-${m}:${TAG}"]
  labels = {
    "org.opencontainers.image.source" = "https://github.com/DoAnTotNghiepDatLichKhamOnline/eClinic_be"
  }
}
