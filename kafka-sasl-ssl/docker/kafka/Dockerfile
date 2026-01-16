FROM confluentinc/cp-kafka:7.6.0

USER root

COPY server.properties /etc/kafka/server.properties

USER appuser

CMD ["kafka-server-start", "/etc/kafka/server.properties"]