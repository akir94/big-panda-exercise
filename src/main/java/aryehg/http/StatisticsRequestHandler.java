package aryehg.http;

import aryehg.Statistics;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;

public class StatisticsRequestHandler implements HttpHandler {
    private static final Logger logger = LogManager.getLogger();

    private Statistics statistics;

    private StatisticsRequestParser requestParser;

    public StatisticsRequestHandler(Statistics statistics) {
        this.statistics = statistics;
        requestParser = new StatisticsRequestParser();
    }

    @Override
    public void handle(HttpExchange httpExchange) {
        try {
            RequestedStatistic requestedStatistic = requestParser.parse(httpExchange);
            int statistic = getStatistic(requestedStatistic);
            respond(httpExchange, 200, String.valueOf(statistic));
        } catch (UnsupportedRequest e) {
            logger.error("Received an unsupported request", e);
            respond(httpExchange, 400, e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Failed to handle request", e);
            respond(httpExchange, 500, e.getMessage());
        }
    }

    private int getStatistic(RequestedStatistic requestedStatistic) throws UnsupportedRequest {
        switch (requestedStatistic.getType()) {
            case "event_type":
                return statistics.getEventCount(requestedStatistic.getKey());
            case "data":
                return statistics.getWordOccurences(requestedStatistic.getKey());
            default:
                throw new UnsupportedRequest("Unknown statistic type");
        }
    }

    private void respond(HttpExchange httpExchange, int httpCode, String response) {
        try {
            byte[] responseBytes = response.getBytes();
            httpExchange.sendResponseHeaders(httpCode, responseBytes.length);
            OutputStream responseBody = httpExchange.getResponseBody();
            responseBody.write(responseBytes);
            responseBody.close();
        } catch (IOException e) {
            logger.error("Failed to send response", e);
            httpExchange.close();
        }
    }
}
