package be.hogent.faith.database.rxfirebase3.exceptions;


import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.annotations.Nullable;

public class RxFirebaseNullDataException extends NullPointerException {
    private final static String DEFAULT_MESSAGE = "Task result was successfully but data was empty";

    public RxFirebaseNullDataException() {
    }

    public RxFirebaseNullDataException(@NonNull String detailMessage) {
        super(detailMessage);
    }

    public RxFirebaseNullDataException(@Nullable Exception resultException) {
        super(resultException != null ? resultException.getMessage() : DEFAULT_MESSAGE);
    }
}
