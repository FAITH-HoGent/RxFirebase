package be.hogent.faith.database.rxfirebase3.exceptions;

import com.google.firebase.database.DatabaseError;
import io.reactivex.rxjava3.annotations.NonNull;

public class RxFirebaseDataException extends Exception {

    protected DatabaseError error;

    public RxFirebaseDataException(@NonNull DatabaseError error) {
        this.error = error;
    }

    public DatabaseError getError() {
        return error;
    }

    @Override
    public String toString() {
        return "RxFirebaseDataException{" +
            "error=" + error +
            '}';
    }
}
