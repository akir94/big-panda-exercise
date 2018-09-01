package aryehg.http;

class RequestedStatistic {
    private String type;
    private String key;

    RequestedStatistic(String type, String key) {
        this.type = type;
        this.key = key;
    }

    String getType() {
        return type;
    }

    String getKey() {
        return key;
    }
}
