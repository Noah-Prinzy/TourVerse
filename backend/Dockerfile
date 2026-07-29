FROM gradle:9.6.1-jdk17 AS build
WORKDIR /workspace
COPY . .
RUN gradle clean installDist --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 tourverse
COPY --from=build /workspace/build/install/backend/ /app/
USER tourverse
EXPOSE 8081
ENV TOURVERSE_ENV=production
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl --fail --silent http://localhost:${PORT:-8081}/api/health >/dev/null || exit 1
ENTRYPOINT ["/app/bin/backend"]
