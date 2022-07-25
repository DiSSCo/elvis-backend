FROM jboss/keycloak:10.0.1

COPY ./dependency/keycloak-bcrypt-1.5.0.jar /opt/jboss/keycloak/standalone/deployments/
COPY ./exports/elvis-realm.json /tmp/exports/
COPY ./themes/messages_en.properties /opt/jboss/keycloak/themes/keycloak/login/messages/

USER jboss
RUN sed -i -e 's/<web-context>auth<\/web-context>/<web-context>keycloak\/auth<\/web-context>/' $JBOSS_HOME/standalone/configuration/standalone.xml

CMD ["-b", "0.0.0.0", "-Dkeycloak.profile.feature.upload_scripts=enabled"]