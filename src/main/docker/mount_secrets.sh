# Reads the deployment secrets into the environment before the application starts.
#
# A missing secret aborts the start. Falling back silently is what makes a misconfigured deployment
# look healthy: without the session key Quarkus quietly generates a random one (sessions then break
# on every restart), and without the initial administrator password a fresh database would come up
# unadministrable. Both are far cheaper to notice as a failed deployment.

fail() {
  echo "FATAL: $1" >&2
  exit 1
}

MAILER_PASSWORD_FILE=/run/secrets/international_mailer_password
if [ -f "$MAILER_PASSWORD_FILE" ]; then
    export QUARKUS_MAILER_PASSWORD=$(cat $MAILER_PASSWORD_FILE)
    echo "Mounted $MAILER_PASSWORD_FILE"
else
  fail "$MAILER_PASSWORD_FILE is missing - the contact form could not send mail. See docs/deployment.md"
fi

HTTP_SESSION_KEY_FILE=/run/secrets/international_http_session_key
if [ -f "$HTTP_SESSION_KEY_FILE" ]; then
  export QUARKUS_HTTP_AUTH_SESSION_ENCRYPTION_KEY=$(cat $HTTP_SESSION_KEY_FILE)
  echo "Mounted $HTTP_SESSION_KEY_FILE"
else
  fail "$HTTP_SESSION_KEY_FILE is missing - refusing to start with a generated session key. See docs/deployment.md"
fi

# Only used the first time the application starts against an empty database; it is ignored once an
# account exists, so the secret may stay in place across deployments.
ADMIN_PASSWORD_FILE=/run/secrets/international_admin_password
if [ -f "$ADMIN_PASSWORD_FILE" ]; then
  export APPLICATION_ADMIN_PASSWORD=$(cat $ADMIN_PASSWORD_FILE)
  echo "Mounted $ADMIN_PASSWORD_FILE"
else
  fail "$ADMIN_PASSWORD_FILE is missing - an empty database could not be given an administrator. See docs/deployment.md"
fi
