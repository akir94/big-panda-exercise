package aryehg.http;

class UnsupportedRequest extends Exception {
    UnsupportedRequest(String message) {
        super(message);
    }

    UnsupportedRequest(String message, Throwable cause) {
        super(message, cause);
    }
}
