# Mount docker secrets if available
MAILER_PASSWORD_FILE=/run/secrets/international_mailer_password
if [ -f "$MAILER_PASSWORD_FILE" ]; then
    export QUARKUS_MAILER_PASSWORD=$(cat $MAILER_PASSWORD_FILE)
    echo "Mounted $MAILER_PASSWORD_FILE"
else
  echo "No $MAILER_PASSWORD_FILE to mount"
fi

HTTP_SESSION_KEY_FILE=/run/secrets/international_http_session_key
if [ -f "$HTTP_SESSION_KEY_FILE" ]; then
  export QUARKUS_HTTP_AUTH_SESSION_ENCRYPTION_KEY=$(cat $HTTP_SESSION_KEY_FILE)
  echo "Mounted $HTTP_SESSION_KEY_FILE"
else
  echo "No $HTTP_SESSION_KEY_FILE to mount"
fi