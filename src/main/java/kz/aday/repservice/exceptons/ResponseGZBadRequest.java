package kz.aday.repservice.exceptons;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ResponseGZBadRequest extends RuntimeException {
    public ResponseGZBadRequest(String message) {
        super(message);
    }
}
