package socialnet.errors;

import java.util.function.Supplier;

public class NotFoundException extends Exception implements Supplier<Exception> {

    public NotFoundException(String message) {
        super(message);
    }

    @Override
    public Exception get() {
        return this;
    }
}
