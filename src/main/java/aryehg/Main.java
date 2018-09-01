package aryehg;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ActorAttributes;
import akka.stream.ActorMaterializer;
import akka.stream.Supervision;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import aryehg.http.StatisticsRequestHandler;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class Main {
    private static final Logger logger = LogManager.getLogger();

    public static void main(String[] args) {
        Process generator = null;
        ActorSystem actorSystem = null;
        HttpServer httpServer = null;
        Statistics statistics = new Statistics();
        try {
            generator = launchGenerator();
            actorSystem = initializeWriteStream(generator, statistics);
            httpServer = initializeHttpServer(statistics);
            waitUntilInterrupted();
        } catch (IOException e) {
            logger.fatal("Failed to start generator process, exiting", e);
        } catch (WriteStreamInitializationFailed e) {
            logger.fatal("Write stream initialization failed, exiting", e);
        } catch (HttpServerInitializationFailed e) {
            logger.fatal("Http server initialization failed, exiting", e);
        } finally {
            if (generator != null) {
                generator.destroy();
            }
            if (actorSystem != null) {
                actorSystem.terminate();
            }
            if (httpServer != null) {
                httpServer.stop(1);
            }
        }
    }

    private static Process launchGenerator() throws IOException {
        String generatorFile = System.getenv().getOrDefault("GENERATOR_FILE", "generator-windows-amd64.exe");
        ProcessBuilder processBuilder = new ProcessBuilder(generatorFile);
        return processBuilder.start();
    }

    private static ActorSystem initializeWriteStream(Process generator, Statistics statistics)
            throws WriteStreamInitializationFailed {
        try {
            ActorSystem actorSystem = ActorSystem.create();
            ActorMaterializer actorMaterializer = ActorMaterializer.create(actorSystem);

            getSource(generator)
                    .alsoTo(Sink.foreach(line -> logger.debug("Received new line: {}", line)))  // for debugging
                    .via(getParserFlow())
                    .to(getStatisticsSink(statistics))
                    .run(actorMaterializer);

            return actorSystem;
        } catch (RuntimeException e) {
            throw new WriteStreamInitializationFailed(e);
        }
    }

    private static Source<String, NotUsed> getSource(Process process) {
        BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        return Source.unfoldResource(() -> stdoutReader,
                reader -> Optional.of(reader.readLine()),
                BufferedReader::close);
    }

    private static Flow<String, JsonObject, NotUsed> getParserFlow() {
        JsonParser jsonParser = new JsonParser();
        return Flow.<String, JsonObject>fromFunction(line -> jsonParser.parse(line).getAsJsonObject())
                .withAttributes(ActorAttributes.withSupervisionStrategy(Supervision.getResumingDecider()));
    }

    private static Sink<JsonObject, CompletionStage<Done>> getStatisticsSink(Statistics statistics) {
        return Sink.foreach(statistics::updateForEvent);
    }

    private static HttpServer initializeHttpServer(Statistics statistics) throws HttpServerInitializationFailed {
        try {
            int port = getHttpServerPort();
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            HttpContext context = server.createContext("/");
            context.setHandler(new StatisticsRequestHandler(statistics));
            server.start();
            return server;
        } catch (RuntimeException | IOException e) {
            throw new HttpServerInitializationFailed(e);
        }
    }

    private static int getHttpServerPort() {
        String value = System.getenv("HTTP_SERVER_PORT");
        if (value != null) {
            return Integer.parseInt(value);
        } else {
            return 8500;
        }
    }

    private static void waitUntilInterrupted() {
        try {
            synchronized (logger) {
                logger.wait();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
