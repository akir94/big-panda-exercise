package aryehg.http;

import com.sun.net.httpserver.HttpExchange;

class StatisticsRequestParser {
    private static final String SUPPORTED_PATH_REGEX = "^/\\w+/\\w+$";

    RequestedStatistic parse(HttpExchange httpExchange) throws UnsupportedRequest {
        try {
            String path = httpExchange.getRequestURI().getPath();
            if (!path.matches(SUPPORTED_PATH_REGEX)) {
                throw new UnsupportedRequest("Unsupported request path");
            } else {
                return createRequestedStatistic(path);
            }
        } catch (RuntimeException e) {
            throw new UnsupportedRequest("Failed to parse request", e);
        }
    }

    private RequestedStatistic createRequestedStatistic(String path) {
        String[] pathParts = path.split("/");
        String statisticType = pathParts[1];
        String keyType = pathParts[2];
        return new RequestedStatistic(statisticType, keyType);
    }
}
