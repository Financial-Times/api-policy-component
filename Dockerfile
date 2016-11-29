FROM coco/dropwizardbase:0.7.x-mvn333

ADD .git/ /.git/
ADD api-policy-component-service/ /api-policy-component-service/

ADD pom.xml /

RUN apk --no-cache upgrade ca-certificates openjdk8

RUN apk --no-cache --virtual .build-dependencies add git \
  && cd api-policy-component-service \
  && HASH=$(git log -1 --pretty=format:%H) \
  && mvn clean install -Dbuild.git.revision=$HASH -Djava.net.preferIPv4Stack=true \
  && rm target/api-policy-component-service-*-sources.jar \
  && mv target/api-policy-component-service-*.jar /api-policy-component-service.jar \
  && mv config-local.yml /config.yml \
  && apk del .build-dependencies \
  && mvn clean \
  && rm -rf /root/.m2/* \

EXPOSE 8080 8081

CMD exec java $JAVA_OPTS \
         -Ddw.varnish.primaryNodes=$READ_ENDPOINT \      
         -Ddw.varnish.jerseyClient.timeout=$JERSEY_TIMEOUT_DURATION \
         -Ddw.checkingVulcanHealth=true \
         -Ddw.metrics.reporters[0].host=$GRAPHITE_HOST \
         -Ddw.metrics.reporters[0].port=$GRAPHITE_PORT \
         -Ddw.metrics.reporters[0].prefix=$GRAPHITE_PREFIX \
         -jar api-policy-component-service.jar server config.yml
