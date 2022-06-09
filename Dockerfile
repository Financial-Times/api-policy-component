FROM openjdk:8u212-jdk-alpine3.9

ADD .git/ /.git/
ADD api-policy-component-service/ /api-policy-component-service/
ADD pom.xml /

ARG SONATYPE_USER
ARG SONATYPE_PASSWORD
ARG GIT_TAG

ENV MAVEN_HOME=/root/.m2
ENV TAG=$GIT_TAG

RUN apk --update add git maven curl \
  # Set Nexus credentials in settings.xml file
  && mkdir $MAVEN_HOME \
  && curl -v -o $MAVEN_HOME/settings.xml "https://raw.githubusercontent.com/Financial-Times/nexus-settings/master/public-settings.xml" \
  # Generate docker tag
  && cd api-policy-component-service \
  && HASH=$(git log -1 --pretty=format:%H) \
  && TAG=$(git tag --sort=committerdate | tail -1) \
  && VERSION=${TAG:-untagged} \
  && sed -i "s/<parent>//; s/<\/parent>//; s/<artifactId>api-policy-component<\/artifactId>//" ./pom.xml \
  # Set Maven artifact version
  && mvn versions:set -DnewVersion=$VERSION \
  && mvn clean install -Dbuild.git.revision=$HASH -Djava.net.preferIPv4Stack=true \
  # Remove version from executable jar name
  && rm target/api-policy-component-service-*-sources.jar \
  && mv target/api-policy-component-service-*.jar /api-policy-component-service.jar \
  # Move resources to root directory in docker container
  && mv config-local.yml /config.yml \
  # Clean up unnecessary dependencies and binaries
  && apk del git maven curl \
  && rm -rf /var/cache/apk/* \
  && rm -rf $MAVEN_HOME/*

FROM openjdk:8u332-jre-slim
COPY --from=0 /api-policy-component-service.jar /api-policy-component-service.jar
COPY --from=0 /config.yml /config.yml

EXPOSE 8080 8081

CMD exec java $JAVA_OPTS \
         -Ddw.server.applicationConnectors[0].port=8080 \
         -Ddw.server.adminConnectors[0].port=8081 \
         -Dsun.net.http.allowRestrictedHeaders=true \
         -Ddw.varnish.primaryNodes=$READ_ENDPOINT \      
         -Ddw.varnish.timeout=$JERSEY_TIMEOUT_DURATION \
         -Ddw.checkingVulcanHealth=$CHECKING_VULCAN_HEALTH \
         -Ddw.logging.appenders[0].logFormat="%m%n" \
         -DgitTag=$TAG \
         -jar api-policy-component-service.jar server config.yml
