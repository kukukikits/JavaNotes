FROM java:8u202-jbrx
VOLUME /tmp
VOLUME /log
ADD /target/*.jar app.jar
LABEL maintainer="geshengbin@cetiti.com"
LABEL name="web server"
ENV SERVER_PORT=${SERVER_PORT:-80}
ENV JAVA_OPTS=${JAVA_OPTS:-'-Xmx256m -XX:+UseG1GC'}
ENV ARGS=${ARGS}
ENV MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD:-'123456'}
ENV MYSQL_DATABASE=${MYSQL_DATABASE}
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar", "${JAVA_OPTS}","/app.jar", "${ARGS}"]