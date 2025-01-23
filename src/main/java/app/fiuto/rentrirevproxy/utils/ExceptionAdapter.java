package app.fiuto.rentrirevproxy.utils;

import lombok.Getter;

@Getter
class ExceptionAdapter {

    public String message;
    public String causeMessage;

    public ExceptionAdapter(Exception e) {
        this.message = e.getMessage();
        this.causeMessage = e.getCause().getMessage();
    }
}
