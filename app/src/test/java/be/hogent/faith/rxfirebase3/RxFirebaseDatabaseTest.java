package be.hogent.faith.rxfirebase3;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import be.hogent.faith.database.rxfirebase3.DataSnapshotMapper;
import be.hogent.faith.database.rxfirebase3.RxFirebaseChildEvent;
import be.hogent.faith.database.rxfirebase3.RxFirebaseDatabase;
import be.hogent.faith.database.rxfirebase3.exceptions.RxFirebaseDataException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;

import static be.hogent.faith.rxfirebase3.RxTestUtil.ANY_KEY;
import static be.hogent.faith.rxfirebase3.RxTestUtil.PREVIOUS_CHILD_NAME;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RxFirebaseDatabaseTest {

    @Mock
    private DatabaseReference databaseReference;
    @Mock
    private DatabaseReference databaseReferenceTwo;

    @Mock
    private Query query;

    @Mock
    private DataSnapshot dataSnapshot;
    @Mock
    private DataSnapshot dataSnapshotTwo;

    @Mock
    private Task<Void> voidTask;

    private ChildData childData = new ChildData();
    private List<ChildData> childDataList = new ArrayList<>();
    private Map<String, ChildData> childDataMap = new HashMap<>();
    private Map<String, Object> updatedData = new HashMap<>();

    private RxFirebaseChildEvent<ChildData> childEventAdded;
    private RxFirebaseChildEvent<ChildData> childEventChanged;
    private RxFirebaseChildEvent<ChildData> childEventRemoved;
    private RxFirebaseChildEvent<ChildData> childEventMoved;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        childDataList.add(childData);
        childDataMap.put(ANY_KEY, childData);
        updatedData.put(databaseReference.toString(), childData);

        childEventAdded = new RxFirebaseChildEvent<>(ANY_KEY, childData, PREVIOUS_CHILD_NAME, RxFirebaseChildEvent.EventType.ADDED);
        childEventChanged = new RxFirebaseChildEvent<>(ANY_KEY, childData, PREVIOUS_CHILD_NAME, RxFirebaseChildEvent.EventType.CHANGED);
        childEventRemoved = new RxFirebaseChildEvent<>(ANY_KEY, childData, RxFirebaseChildEvent.EventType.REMOVED);
        childEventMoved = new RxFirebaseChildEvent<>(ANY_KEY, childData, PREVIOUS_CHILD_NAME, RxFirebaseChildEvent.EventType.MOVED);

        when(dataSnapshot.exists()).thenReturn(true);
        when(dataSnapshot.getValue(ChildData.class)).thenReturn(childData);
        when(dataSnapshot.getKey()).thenReturn(ANY_KEY);
        when(dataSnapshot.getChildren()).thenReturn(Arrays.asList(dataSnapshot));

        when(dataSnapshotTwo.exists()).thenReturn(true);
        when(dataSnapshotTwo.getValue(ChildData.class)).thenReturn(childData);
        when(dataSnapshotTwo.getKey()).thenReturn(ANY_KEY);
        when(dataSnapshotTwo.getChildren()).thenReturn(Arrays.asList(dataSnapshotTwo));

        when(databaseReference.updateChildren(updatedData)).thenReturn(voidTask);
    }

    @Test
    public void testObserveSingleValue() {
        TestObserver<ChildData> testObserver = RxFirebaseDatabase
                .observeSingleValueEvent(databaseReference, ChildData.class)
                .test();

        ArgumentCaptor<ValueEventListener> argument = ArgumentCaptor.forClass(ValueEventListener.class);
        verify(databaseReference).addListenerForSingleValueEvent(argument.capture());
        argument.getValue().onDataChange(dataSnapshot);

        testObserver.assertNoErrors()
                .assertValueCount(1)
                .assertValue(childData)
                .assertComplete();
    }

    @Test
    public void testObserveSingleNoData() {

        DataSnapshot mockFirebaseDataSnapshotNoData = mock(DataSnapshot.class);
        when(mockFirebaseDataSnapshotNoData.exists()).thenReturn(false);

        TestObserver<ChildData> testObserver = RxFirebaseDatabase
                .observeSingleValueEvent(databaseReference, ChildData.class)
                .test();

        ArgumentCaptor<ValueEventListener> argument = ArgumentCaptor.forClass(ValueEventListener.class);
        verify(databaseReference).addListenerForSingleValueEvent(argument.capture());
        argument.getValue().onDataChange(mockFirebaseDataSnapshotNoData);

        testObserver.assertValueCount(0)
                .assertComplete();
    }

    @Test
    public void testObserveSingleWrongType() {

        TestObserver<WrongType> testObserver = RxFirebaseDatabase
                .observeSingleValueEvent(databaseReference, WrongType.class)
                .test();

        ArgumentCaptor<ValueEventListener> argument = ArgumentCaptor.forClass(ValueEventListener.class);
        verify(databaseReference).addListenerForSingleValueEvent(argument.capture());
        argument.getValue().onDataChange(dataSnapshot);

        testObserver.assertError(RuntimeException.class)
                .assertNotComplete();
    }

    @Test
    public void testObserveSingleValueDisconnected() {

        TestObserver<ChildData> testObserver = RxFirebaseDatabase
                .observeSingleValueEvent(databaseReference, ChildData.class)
                .test();

        ArgumentCaptor<ValueEventListener> argument = ArgumentCaptor.forClass(ValueEventListener.class);
        verify(databaseReference).addListenerForSingleValueEvent(argument.capture());
        argument.getValue().onCancelled(DatabaseError.fromCode(DatabaseError.DISCONNECTED));

        testObserver.assertError(RxFirebaseDataException.class)
                .assertNotComplete();
    }

    @Test
    public void testObserveSingleValueEventFailed() {

        TestObserver<ChildData> testObserver = RxFirebaseDatabase
                .observeSingleValueEvent(databaseReference, ChildData.class)
                .test();

        ArgumentCaptor<ValueEventListener> argument = ArgumentCaptor.forClass(ValueEventListener.class);
        verify(databaseReference).addListenerForSingleValueEvent(argument.capture());
        argument.getValue().onCancelled(DatabaseError.fromCode(DatabaseError.OPERATION_FAILED));

        testObserver.assertError(RxFirebaseDataException.class)
                .assertNotComplete();
    }

    @Test
    public void testObserveValueEvent() {

        TestSubscriber<ChildData> testObserver = RxFirebaseDatabase
                .observeValueEvent(databaseReference, ChildData.class)
                .test();

        ArgumentCaptor<ValueEventListener> argument = ArgumentCaptor.forClass(ValueEventListener.class);
        verify(databaseReference).addValueEventListener(argument.capture());
        argument.getValue().onDataChange(dataSnapshot);

        testObserver.assertNoErrors()
                .assertValueCount(1)
                .assertValue(childData)
                .assertNotComplete();
    }

    @Test
    public void testMultipleSingleValueEvent() {

        TestSubscriber<DataSnapshot> testObserver = RxFirebaseDatabase
                .observeMultipleSingleValueEvent(databaseReference, databaseReferenceTwo)
                .test();

        ArgumentCaptor<ValueEventListener> argument = ArgumentCaptor.forClass(ValueEventListener.class);
        verify(databaseReference).addListenerForSingleValueEvent(argument.capture());
        argument.getValue().onDataChange(dataSnapshot);
        verify(databaseReferenceTwo).addListenerForSingleValueEvent(argument.capture());
        argument.getValue().onDataChange(dataSnapshotTwo);

        testObserver.assertNoErrors()
                .assertValueCount(2)
                .assertComplete();
    }

    @Test
    public void testSingleValueEvent() {


        TestObserver<ChildData> testObserver = RxFirebaseDatabase
                .observeSingleValueEvent(databaseReference, ChildData.class)
                .test();

        ArgumentCaptor<ValueEventListener> argument = ArgumentCaptor.forClass(ValueEventListener.class);
        verify(databaseReference).addListenerForSingleValueEvent(argument.capture());
        argument.getValue().onDataChange(dataSnapshot);

        testObserver.assertNoErrors()
                .assertValueCount(1)
                .assertValue(childData)
                .assertComplete();
    }

    @Test
    public void testObserveValueEventList() {

        TestObserver<ChildData> testObserver = RxFirebaseDatabase
                .observeSingleValueEvent(databaseReference, ChildData.class)
                .test();

        ArgumentCaptor<ValueEventListener> argument = ArgumentCaptor.forClass(ValueEventListener.class);
        verify(databaseReference).addListenerForSingleValueEvent(argument.capture());
        argument.getValue().onDataChange(dataSnapshot);

        testObserver.assertNoErrors()
                .assertValueCount(1)
                .assertComplete();
    }

    @Test
    public void testObserveValuesMap() {
        TestObserver<Map<String, ChildData>> testObserver = RxFirebaseDatabase
                .observeSingleValueEvent(databaseReference)
                .map(new Function<DataSnapshot, Map<String, ChildData>>() {
                    @Override
                    public LinkedHashMap<String, ChildData> apply(DataSnapshot dataSnapshot) {
                        LinkedHashMap<String, ChildData> map = new LinkedHashMap<>();
                        map.put(dataSnapshot.getKey(), dataSnapshot.getValue(ChildData.class));
                        return map;
                    }
                }).test();

        ArgumentCaptor<ValueEventListener> argument = ArgumentCaptor.forClass(ValueEventListener.class);
        verify(databaseReference).addListenerForSingleValueEvent(argument.capture());
        argument.getValue().onDataChange(dataSnapshot);

        testObserver.assertNoErrors()
                .assertValueCount(1)
                .assertValue(childDataMap)
                .dispose();
    }

    @Test
    public void testObserveChildEventAdded() {

        TestSubscriber<RxFirebaseChildEvent<ChildData>> testObserver = RxFirebaseDatabase
                .observeChildEvent(databaseReference, ChildData.class)
                .test();

        ArgumentCaptor<ChildEventListener> argument = ArgumentCaptor.forClass(ChildEventListener.class);
        verify(databaseReference).addChildEventListener(argument.capture());
        argument.getValue().onChildAdded(dataSnapshot, PREVIOUS_CHILD_NAME);

        testObserver.assertNoErrors()
                .assertValueCount(1)
                .assertValue(childEventAdded)
                .assertNotComplete();
    }

    @Test
    public void testObserveChildEventChanged() {

        TestSubscriber<RxFirebaseChildEvent<ChildData>> testObserver = RxFirebaseDatabase
                .observeChildEvent(databaseReference, ChildData.class)
                .test();

        ArgumentCaptor<ChildEventListener> argument = ArgumentCaptor.forClass(ChildEventListener.class);
        verify(databaseReference).addChildEventListener(argument.capture());
        argument.getValue().onChildChanged(dataSnapshot, PREVIOUS_CHILD_NAME);

        testObserver.assertNoErrors()
                .assertValueCount(1)
                .assertValue(childEventChanged)
                .assertNotComplete();
    }

    @Test
    public void testObserveChildEventRemoved() {

        TestSubscriber<RxFirebaseChildEvent<ChildData>> testObserver = RxFirebaseDatabase
                .observeChildEvent(databaseReference, ChildData.class)
                .test();

        ArgumentCaptor<ChildEventListener> argument = ArgumentCaptor.forClass(ChildEventListener.class);
        verify(databaseReference).addChildEventListener(argument.capture());
        argument.getValue().onChildRemoved(dataSnapshot);

        testObserver.assertNoErrors()
                .assertValueCount(1)
                .assertValue(childEventRemoved)
                .assertNotComplete();
    }

    @Test
    public void testObserveChildEventMoved() {

        TestSubscriber<RxFirebaseChildEvent<ChildData>> testObserver = RxFirebaseDatabase
                .observeChildEvent(databaseReference, ChildData.class)
                .test();

        ArgumentCaptor<ChildEventListener> argument = ArgumentCaptor.forClass(ChildEventListener.class);
        verify(databaseReference).addChildEventListener(argument.capture());
        argument.getValue().onChildMoved(dataSnapshot, PREVIOUS_CHILD_NAME);

        testObserver.assertNoErrors()
                .assertValueCount(1)
                .assertValue(childEventMoved)
                .assertNotComplete();
    }

    @Test
    public void testObserveChildEventCancelled() {

        TestSubscriber<RxFirebaseChildEvent<ChildData>> testObserver = RxFirebaseDatabase
                .observeChildEvent(databaseReference, ChildData.class)
                .test();

        ArgumentCaptor<ChildEventListener> argument = ArgumentCaptor.forClass(ChildEventListener.class);
        verify(databaseReference).addChildEventListener(argument.capture());
        argument.getValue().onCancelled(DatabaseError.fromCode(DatabaseError.DISCONNECTED));

        testObserver.assertError(RxFirebaseDataException.class)
                .assertNotComplete();
    }

    @Test
    public void testObserveListWithDataSnapshotMapper() {
        TestSubscriber<List<ChildData>> testObserver = RxFirebaseDatabase
                .observeValueEvent(query, DataSnapshotMapper.listOf(ChildData.class))
                .test();

        ArgumentCaptor<ValueEventListener> argument = ArgumentCaptor.forClass(ValueEventListener.class);
        verify(query).addValueEventListener(argument.capture());
        argument.getValue().onDataChange(dataSnapshot);

        testObserver.assertNoErrors()
                .assertValueCount(1)
                .assertValue(childDataList)
                .assertNotComplete();
    }

    @Test
    public void testObserveListWithDataSnapshotCustomMapper() throws Throwable {
        //noinspection unchecked
        Function<DataSnapshot, ChildData> mapper = (Function<DataSnapshot, ChildData>) mock(Function.class);
        doReturn(childData).when(mapper).apply(eq(dataSnapshot));

        TestSubscriber<List<ChildData>> testObserver = RxFirebaseDatabase
                .observeValueEvent(query, DataSnapshotMapper.listOf(ChildData.class, mapper))
                .test();

        ArgumentCaptor<ValueEventListener> argument = ArgumentCaptor.forClass(ValueEventListener.class);
        verify(query).addValueEventListener(argument.capture());
        argument.getValue().onDataChange(dataSnapshot);

        verify(mapper).apply(dataSnapshot);

        testObserver.assertNoErrors()
                .assertValueCount(1)
                .assertValue(childDataList)
                .assertNotComplete();
    }

    class ChildData {
        int id;
        String str;
    }

    class WrongType {
        String somethingWrong;
        long more;
    }
}
