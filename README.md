# Rx2Firebase
[Rxjava 2.0](https://github.com/ReactiveX/RxJava/tree/2.x) wrapper on Google's [Android Firebase](https://firebase.google.com/docs/android/setup?hl=es) library.

This repository started as a personal usage of [Nick Moskalenko](https://github.com/nmoskalenko) RxFirebase library. You can check his work [here](https://github.com/nmoskalenko/RxFirebase).


## Download

##### Gradle:

```groovy
dependencies {
  compile 'com.github.frangsierra:rx2firebase:1.5.0'
}
```
```
allprojects {
		repositories {
			...
			maven { url "https://jitpack.io" }
		}
	}
```

## Usage
Library provides set of static methods of classes:
* RxFirebaseAuth
* RxFirebaseUser
* RxFirebaseDatabase
* RxFirebaseStorage
* RxFirestore

It also provides a custom implementation of `FirebaseRecyclerAdapter`:
* RxFirebaseRecyclerAdapter

### Authentication:
Sign in with email and password:

```java
    RxFirebaseAuth.signInWithEmailAndPassword(auth, email, password)
                .map(authResult -> authResult.getUser() != null)
                .take(1)
                .subscribe(logged -> {
                    Log.i("Rxfirebase2", "User logged " + logged);
                });
```
### Firestore:

You can observe values providing the Class of expected data like:

```java
    DocumentReference document = firestore.collection("Users").document("UserId_1");
    RxFirestore.observeDocumentRef(document)
       .subscribe( userDoc -> {
          //Do something with my snapshot
       });
```

Get and set documents on a specific reference:

```java
    DocumentReference document = firestore.collection("Users").document("UserId_1");
    User mynewUser = User("newUserName", 24);
    //Set data
    RxFirestore.setDocument(document, myNewUser).subscribe();
    //Get and map data
    RxFirestore.getDocument(document)
       .map( userDoc -> { return userDoc.toObject(User.class); })
       .subscribe( casterUser -> {
          //Do something with my already casted user
       });
```

Finally you can do sync operations on the database using `runTransaction` and if you wanna realize multiple
operations at once, you should use the method `atomicOperation` which wraps the `WriteBatch` related methods from Firestore.

### Database:

You can observe values providing the Class of expected data like:

```java
    RxFirebaseDatabase.observeSingleValueEvent(getPostsRef().child("posts"), Post.class)
                .subscribe(post -> {
           //Do something with yourpost 
        });
```

or providing your own mapper between DataSnapshot and your data type:

```java
    RxFirebaseDatabase.observeSingleValueEvent(getPostsRef().child("posts"),
                dataSnapshot -> {
                    // do your own mapping here
                    return new Author();
                })
                .subscribe(author -> {
                    // process author value
                });
```

There are some pre-defined mappers to make things easier:

##### Observing list values

```java
    RxFirebaseDatabase.observeSingleValueEvent(getPostsRef().child("posts"), DataSnapshotMapper.listOf(PostComment.class))
                .subscribe(blogPost -> {
                    // process postcomment list item
                });
```

##### Observing map values

```java
     RxFirebaseDatabase.observeSingleValueEvent(getPostsRef().child("posts"), DataSnapshotMapper.mapOf(PostComment.class))
                .subscribe(PostCommentAsMapItem -> {
                    // process blogPost as key-value pair
                });
```

### Storage:

Download file from Firebase storage

```java
    RxFirebaseStorage.getFile(getStorageRef(), targetFile)
                .subscribe(taskSnapshot -> {
                    Log.i("RxFirebaseSample", "transferred: " + snapshot.getBytesTransferred() + " bytes");
                }, throwable -> {
                    Log.e("RxFirebaseSample", throwable.toString());
            });
```

or download file as bytes array

```java
    RxFirebaseStorage.getBytes(getStorageRef(), 1024 * 100)
                .subscribe(bytes -> {
                    Log.i("RxFirebaseSample", "downloaded: " + new String(bytes));
                }, throwable -> {
                    Log.e("RxFirebaseSample", throwable.toString());
            });
```
### RxFirebaseQuery

RxFirebaseQuery is a builder class used to work together with methods from RxFirebaseDatabase that allow you to retrieve data from multiple databaseReferences. Doing this allow you to build and create dynamic queries to retrieve database objects from references retrieved from different tables easily. 
At the moment RxFirebaseQuery just allow the user to create the queries and retrieve the data. Filters should be done with the `DatabaseReference` items that you pass to the constructor. In other hand for update and delete data you should use `Firebase` method `updateChildren()`
```java
	DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
		      DatabaseReference from = reference.child("tweets");
		      Query where = reference.child("users").child(userId).child("feedReferences");
		      RxFirebaseQuery.getInstance()
			    .filterByRefs(from, where)
			    .asList()
			    .subscribe(dataSnapshots -> {
			       Log.i("RxFirebase", "Retrieved a total of " + dataSnapshots.size() + " tweets");
			       for (DataSnapshot dataSnapshot : dataSnapshots) {
				  Tweet tweet = dataSnapshot.getValue(Tweet.class);
				  Log.i("RxFirebase", "New tweet for user feed: " + tweet.getDescription());
			       }
			    });
```
### RxFirebaseRecyclerAdapter:

RxFirebaseRecyclerAdapter was created looking for a way to manage the `RxFirebaseChildEvent<DataSnapshot>`  items recieved with the `observeChildEvent` method. It is an abstract class based on FirebaseRecyclerAdapter but modifying the query and firebase call dependency. 
Doing this, now it only recieve a RxFirebaseRecyclerAdapter and using the method `manageChildItem`it will manage the `ChildEvent` doing the right task based on the `EventType` of the item:

For example:

##### Posts are emited to our disposable:

```java 
   RxFirebaseDatabase.observeChildEvent(getPostsRefFromGroup(groupId))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(post -> {
                    getView().managePost(post);
                    getView().showLoadingProgressBar(false);
                }, throwable -> {
                    getView().showLoadingProgressBar(false);
                    getView().showReloadLayout(true);
                }));    
```

##### We call `manageChildItem(item)` in our Custom adapter which extends `RxFirebaseRecyclerAdapter`:

```java
 public void managePost(RxFirebaseChildEvent<DataSnapshot> post) {
        if(postRecyclerView.getVisibility() != View.VISIBLE) {
            postRecyclerView.setVisibility(View.VISIBLE);
        }
        if (adapter == null){
            adapter = new PostAdapter(this, this);
            postRecyclerView.setAdapter(adapter);
        }
        adapter.manageChildItem(post);
    }
```

##### Custom adapter example:

```java
public class PostAdapter extends RxFirebaseRecyclerAdapter<PostViewHolder, Post> {
    private static final String TAG = "PostAdapter";

    public PostAdapter() {
        super(Post.class);
    }

    @Override
    public PostViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //Initialize your Holder
    }

    @Override
    public void onBindViewHolder(PostViewHolder holder, int position) {
        Post post = getItems().get(position);
       //bind your post
    }

    @Override
    protected void itemAdded(Post item, String key, int position) {
        //Add the refs if you need them later
        item.setRef(key);
        Log.d(TAG, "Added a new item to the adapter.");
    }

    @Override
    protected void itemChanged(Post oldItem, Post newItem, String key, int position) {
        //Add the refs if you need them later
        newItem.setRef(key);
        Log.d(TAG, "Changed an item.");
    }

    @Override
    protected void itemRemoved(Post item, String key, int position) {
        Log.d(TAG, "Removed an item.");
    }

    @Override
    protected void itemMoved(Post item, String key, int oldPosition, int newPosition) {
        Log.d(TAG, "Moved an item.");
    }

}
```

## RxJava and RxJava 2.0
One of the differences between RxJava and RxJava 2 is that RxJava 2 no longer accepts `null` values. Throwing a `NullPointerException` immediately. For this reason some of the methods of the library as been redesigned to return a `Completable` instead of a `Observable<Void>`. For example:

#### RxFirebase

```java
@NonNull
public static Observable<Void> updateEmail(@NonNull final FirebaseUser firebaseUser, @NonNull final String email) {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(final Subscriber<? super Void> subscriber) {
                RxHandler.assignOnTask(subscriber, firebaseUser.updateEmail(email));
            }
        });
}
```

#### Rx2Firebase

```java
@NonNull
public static Completable updateEmail(@NonNull final FirebaseUser firebaseUser, @NonNull final String email) {
        return Completable.create(new CompletableOnSubscribe() {
            @Override
            public void subscribe(CompletableEmitter emitter) throws Exception {
                RxCompletableHandler.assignOnTask(emitter, firebaseUser.updateEmail(email));
            }
        });
}
```

`RxCompletableHandler` manages the CompletableEmitters in the same way that `RxHandler` manages the `Subscriber`.
You can check all the differences between RxJava and RxJava 2.0 in the next [Link](https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0)

## License

	MIT License

	Copyright (c) 2016 Francisco García Sierra

	Permission is hereby granted, free of charge, to any person obtaining a 
	copy of this software and associated documentation files (the "Software"), 
	to deal in the Software without restriction, including without limitation 
	the rights to use, copy, modify, merge, publish, distribute, sublicense, 
	and/or sell copies of the Software, and to permit persons to whom the 
	Software is furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included 
	in all copies or substantial portions of the Software.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
	OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
	THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
	FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR 
	OTHER DEALINGS IN THE SOFTWARE.

