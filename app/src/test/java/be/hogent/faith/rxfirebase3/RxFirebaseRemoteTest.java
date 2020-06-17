package be.hogent.faith.rxfirebase3;

import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import be.hogent.faith.database.rxfirebase3.RxFirebaseRemote;
import io.reactivex.rxjava3.observers.TestObserver;

import static be.hogent.faith.rxfirebase3.RxTestUtil.ANY_TIME;
import static be.hogent.faith.rxfirebase3.RxTestUtil.EXCEPTION;
import static be.hogent.faith.rxfirebase3.RxTestUtil.setupTask;
import static be.hogent.faith.rxfirebase3.RxTestUtil.testOnCompleteListener;
import static be.hogent.faith.rxfirebase3.RxTestUtil.testOnFailureListener;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RxFirebaseRemoteTest {

    @Mock
    private Task<Void> voidTask;

    @Mock
    private FirebaseRemoteConfig firebaseConfig;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        setupTask(voidTask);

        when(firebaseConfig.fetch(ANY_TIME)).thenReturn(voidTask);
    }

    @Test
    public void fetchRemoteConfig() {
        TestObserver fetchTestObserver = RxFirebaseRemote
                .fetch(firebaseConfig, ANY_TIME)
                .test();

        testOnCompleteListener.getValue().onComplete(voidTask);

        verify(firebaseConfig).fetch(eq(ANY_TIME));

        fetchTestObserver.assertNoErrors()
                .assertComplete();
    }

    @Test
    public void fetchRemoteFailure() {
        TestObserver fetchTestObserver = RxFirebaseRemote
                .fetch(firebaseConfig, ANY_TIME)
                .test();

        testOnFailureListener.getValue().onFailure(EXCEPTION);

        verify(firebaseConfig).fetch(eq(ANY_TIME));

        fetchTestObserver.assertError(EXCEPTION)
                .assertNotComplete();
    }
}