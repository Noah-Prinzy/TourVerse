FROM gradle:9.6.1-jdk17 AS build
WORKDIR /workspace
COPY . .
RUN gradle clean installDist --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 10001 tourverse
COPY --from=build /workspace/build/install/backend/ /app/
USER tourverse
EXPOSE 8081
ENV TOURVERSE_ENV=production
ENTRYPOINT ["/app/bin/backend"]
