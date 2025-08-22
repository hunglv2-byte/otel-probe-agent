Do not run OpenTelemetry’s javaagent at the same time yet — confirm this bare-bones agent works first.
Example:
```shell
java \
  -javaagent:/home/dgwo/Documents/otel-probe-agent/build/libs/otel-probe-agent-1.1.0-SNAPSHOT.jar \
  -Dotel.probe.agent.packages.ignore=jp.joinsure.policy.console.port,jp.joinsure.console.port.adapter.config \
  -Dotel.probe.agent.packages=jp.joinsure \
  -javaagent:/home/dgwo/Documents/run-record-backend-on-branch-develop/opentelemetry-javaagent.jar \
  -Dotel.javaagent.debug=true \
  -Dotel.service.name=record-backend \
  -Dotel.traces.exporter=otlp \
  -Dotel.exporter.otlp.protocol=grpc \
  -Dotel.exporter.otlp.endpoint=http://localhost:4317 \
  -Dotel.metrics.exporter=none \
  -Dotel.logs.exporter=none \
  -Dotel.resource.attributes=deployment.environment=dev \
  -Dotel.instrumentation.methods.include.private=true \
  -jar /home/dgwo/Documents/joinsure-record-backend/console/build/libs/console-0.0.1-SNAPSHOT.jar
```