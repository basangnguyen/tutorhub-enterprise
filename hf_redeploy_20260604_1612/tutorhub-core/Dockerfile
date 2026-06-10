FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY TutorServer.jar /app/TutorServer.jar

ENV PORT=7860
EXPOSE 7860

CMD ["java", "-jar", "TutorServer.jar"]
