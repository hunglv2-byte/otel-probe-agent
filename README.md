Do not run OpenTelemetry’s javaagent at the same time yet — confirm this bare-bones agent works first.
Example:
```shell
java \
  -javaagent:/home/dgwo/Documents/otel-probe-agent/build/libs/otel-probe-agent-1.0-SNAPSHOT.jar \
  -Dotel.probe.agent.packages=jp.joinsure.claim.console.presentation,jp.joinsure.claim.console.usecase \
  -javaagent:/home/dgwo/Documents/joinsure-record-backend/libs/opentelemetry-javaagent.jar \
  -Dotel.javaagent.debug=true \
  -Dotel.service.name=record-backend \
  -Dotel.traces.exporter=zipkin \
  -Dotel.exporter.zipkin.endpoint=http://localhost:9411/api/v2/spans \
  -Dotel.metrics.exporter=none \
  -Dotel.logs.exporter=none \
  -Dotel.resource.attributes=deployment.environment=dev \
  -Dotel.instrumentation.methods.include.private=true \
  -Dotel.instrumentation.methods.exclude=org.springframework.security.*,org.springframework.boot.actuate.health.* \
  -jar ./console/build/libs/console-0.0.1-SNAPSHOT.jar
```