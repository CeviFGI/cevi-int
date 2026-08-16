#!/usr/bin/env bash
#
# Container-based development tooling (C-016).
#
# Runs every build/test/run command inside a container so a contributor needs
# only Docker and git — no local JDK, Maven or browser installation.
#
#   tooling/docker.sh build         package the application
#   tooling/docker.sh test          unit tests only
#   tooling/docker.sh verify        unit tests + Playwright e2e tests + coverage gate
#   tooling/docker.sh dev           Quarkus dev mode on http://localhost:8080
#   tooling/docker.sh docker-image  package + build the application OCI image
#   tooling/docker.sh mvn <args>    arbitrary Maven goals via ./mvnw
#
set -euo pipefail

CMD="${1:-}"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Toolchain image built on demand from tooling/Dockerfile.maven.
IMAGE="cevi-int-tooling"
# Named volume holding the container user's home: ~/.m2/repository and the
# Playwright browser download. Deliberately NOT the host's ~/.m2 — that one
# carries a settings.xml mirroring every artifact through an internal Nexus,
# which is unreachable (and its TLS chain untrusted) outside that network. A
# private volume keeps the tooling self-contained and resolvable from Maven
# Central for everyone. Discard it with:
#   docker volume rm cevi-int-tooling-home
HOME_VOLUME="cevi-int-tooling-home"
# Application image produced by `docker-image`, matching README/CLAUDE.md.
APP_IMAGE="quarkus/international-jvm"

# Most setups can talk to the daemon directly; fall back to sudo if not.
DOCKER=(docker)
if ! docker info >/dev/null 2>&1; then
  DOCKER=(sudo docker)
fi

# Run as the invoking host user so target/ and the test reports stay editable and
# deletable by the IDE and git (NFR-033). That user has no /etc/passwd entry in
# the container, so HOME must be pointed at the volume explicitly — the Maven
# wrapper derives ~/.m2 (repository *and* its own Maven distribution) from it.
# Do not set MAVEN_CONFIG here: the mvnw script appends its value to the Maven
# command line, so a path lands where a lifecycle phase is expected.
#
# `-Duser.home` is needed on top of HOME: the JVM reads user.home from the passwd
# database, not from the environment, and uid 1000 resolves to the base image's
# `ubuntu` account. Without it the wrapper downloads its Maven distribution to
# /home/ubuntu on every run (outside the volume, NFR-034) and fails outright for
# a contributor whose uid is not 1000.
CURRENT_USER="$(id -u):$(id -g)"
MAVEN_USER_ARGS=(
  --user "${CURRENT_USER}"
  -e HOME=/home/dev
  -e MAVEN_OPTS="-Duser.home=/home/dev -Dmaven.repo.local=/home/dev/.m2/repository"
  -v "${HOME_VOLUME}:/home/dev"
)

ensure_image() {
  if ! "${DOCKER[@]}" image inspect "${IMAGE}" >/dev/null 2>&1; then
    echo "Building toolchain image ${IMAGE} (one-time)..." >&2
    "${DOCKER[@]}" build -t "${IMAGE}" \
      -f "${PROJECT_ROOT}/tooling/Dockerfile.maven" "${PROJECT_ROOT}/tooling"
  fi
}

run_mvn() {
  ensure_image
  "${DOCKER[@]}" run --rm -i \
    "${MAVEN_USER_ARGS[@]}" \
    -v "${PROJECT_ROOT}:/app" \
    -w /app \
    "${IMAGE}" \
    ./mvnw --batch-mode "$@"
}

case "$CMD" in
  build)
    run_mvn package "${@:2}"
    ;;
  test)
    run_mvn test "${@:2}"
    ;;
  verify)
    # The e2e tests drive a headless Chromium inside the same container, so no
    # host network or display is needed. The SQLite test database lives at the
    # container's /tmp and is therefore fresh on every run, which is what the
    # coverage gate expects.
    run_mvn verify "${@:2}"
    ;;
  dev)
    ensure_image
    # --network host so http://localhost:8080 and the Dev UI are reachable from
    # the host browser; -t so live reload reacts to keystrokes — but only when
    # there actually is a terminal, otherwise docker refuses to start.
    TTY_ARGS=()
    if [ -t 0 ]; then TTY_ARGS=(-t); fi
    "${DOCKER[@]}" run --rm -i "${TTY_ARGS[@]}" \
      "${MAVEN_USER_ARGS[@]}" \
      --network host \
      -v "${PROJECT_ROOT}:/app" \
      -w /app \
      "${IMAGE}" \
      ./mvnw compile quarkus:dev "${@:2}"
    ;;
  docker-image)
    run_mvn package
    echo "Building application image ${APP_IMAGE}..." >&2
    "${DOCKER[@]}" build \
      -f "${PROJECT_ROOT}/src/main/docker/Dockerfile.jvm" \
      -t "${APP_IMAGE}" \
      "${PROJECT_ROOT}"
    ;;
  mvn)
    run_mvn "${@:2}"
    ;;
  *)
    echo "Usage: tooling/docker.sh <build|test|verify|dev|docker-image|mvn ...>"
    exit 1
    ;;
esac
