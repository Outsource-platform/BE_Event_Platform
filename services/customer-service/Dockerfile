FROM eclipse-temurin:17-jre
WORKDIR /app
# Build the jar first: mvn -pl services/customer-service -am package
COPY target/customer-service.jar app.jar
EXPOSE 8084
# Giới hạn bộ nhớ JVM: máy chủ chỉ có vài GB dùng chung nhiều project, JVM mặc định lấy 1/4 RAM máy mỗi tiến trình.
# Ghi đè khi cần bằng biến môi trường JAVA_OPTS trong docker-compose.yml.
ENV JAVA_OPTS="-Xms64m -Xmx160m -XX:+UseSerialGC -XX:MaxMetaspaceSize=160m -XX:ReservedCodeCacheSize=64m -Xss512k -XX:TieredStopAtLevel=1 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
