package com.nostudios.bruceapp.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.nostudios.bruceapp.data.model.BruceDevice;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class BruceDeviceDao_Impl implements BruceDeviceDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<BruceDevice> __insertionAdapterOfBruceDevice;

  private final EntityDeletionOrUpdateAdapter<BruceDevice> __deletionAdapterOfBruceDevice;

  private final SharedSQLiteStatement __preparedStmtOfDeleteDeviceById;

  public BruceDeviceDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfBruceDevice = new EntityInsertionAdapter<BruceDevice>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `bruce_devices` (`id`,`name`,`category`,`dateAdded`,`savedPin`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BruceDevice entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getCategory());
        statement.bindLong(4, entity.getDateAdded());
        if (entity.getSavedPin() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getSavedPin());
        }
      }
    };
    this.__deletionAdapterOfBruceDevice = new EntityDeletionOrUpdateAdapter<BruceDevice>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `bruce_devices` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BruceDevice entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteDeviceById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM bruce_devices WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertDevice(final BruceDevice device,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfBruceDevice.insert(device);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteDevice(final BruceDevice device,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfBruceDevice.handle(device);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteDeviceById(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteDeviceById.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteDeviceById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<BruceDevice>> getAllDevices() {
    final String _sql = "SELECT * FROM bruce_devices ORDER BY dateAdded ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"bruce_devices"}, new Callable<List<BruceDevice>>() {
      @Override
      @NonNull
      public List<BruceDevice> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfDateAdded = CursorUtil.getColumnIndexOrThrow(_cursor, "dateAdded");
          final int _cursorIndexOfSavedPin = CursorUtil.getColumnIndexOrThrow(_cursor, "savedPin");
          final List<BruceDevice> _result = new ArrayList<BruceDevice>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BruceDevice _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final long _tmpDateAdded;
            _tmpDateAdded = _cursor.getLong(_cursorIndexOfDateAdded);
            final String _tmpSavedPin;
            if (_cursor.isNull(_cursorIndexOfSavedPin)) {
              _tmpSavedPin = null;
            } else {
              _tmpSavedPin = _cursor.getString(_cursorIndexOfSavedPin);
            }
            _item = new BruceDevice(_tmpId,_tmpName,_tmpCategory,_tmpDateAdded,_tmpSavedPin);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAllDevicesList(final Continuation<? super List<BruceDevice>> $completion) {
    final String _sql = "SELECT * FROM bruce_devices ORDER BY dateAdded ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<BruceDevice>>() {
      @Override
      @NonNull
      public List<BruceDevice> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfDateAdded = CursorUtil.getColumnIndexOrThrow(_cursor, "dateAdded");
          final int _cursorIndexOfSavedPin = CursorUtil.getColumnIndexOrThrow(_cursor, "savedPin");
          final List<BruceDevice> _result = new ArrayList<BruceDevice>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BruceDevice _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final long _tmpDateAdded;
            _tmpDateAdded = _cursor.getLong(_cursorIndexOfDateAdded);
            final String _tmpSavedPin;
            if (_cursor.isNull(_cursorIndexOfSavedPin)) {
              _tmpSavedPin = null;
            } else {
              _tmpSavedPin = _cursor.getString(_cursorIndexOfSavedPin);
            }
            _item = new BruceDevice(_tmpId,_tmpName,_tmpCategory,_tmpDateAdded,_tmpSavedPin);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getDeviceById(final String id,
      final Continuation<? super BruceDevice> $completion) {
    final String _sql = "SELECT * FROM bruce_devices WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<BruceDevice>() {
      @Override
      @Nullable
      public BruceDevice call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfDateAdded = CursorUtil.getColumnIndexOrThrow(_cursor, "dateAdded");
          final int _cursorIndexOfSavedPin = CursorUtil.getColumnIndexOrThrow(_cursor, "savedPin");
          final BruceDevice _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final long _tmpDateAdded;
            _tmpDateAdded = _cursor.getLong(_cursorIndexOfDateAdded);
            final String _tmpSavedPin;
            if (_cursor.isNull(_cursorIndexOfSavedPin)) {
              _tmpSavedPin = null;
            } else {
              _tmpSavedPin = _cursor.getString(_cursorIndexOfSavedPin);
            }
            _result = new BruceDevice(_tmpId,_tmpName,_tmpCategory,_tmpDateAdded,_tmpSavedPin);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getSavedPin(final String id, final Continuation<? super String> $completion) {
    final String _sql = "SELECT savedPin FROM bruce_devices WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<String>() {
      @Override
      @Nullable
      public String call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final String _result;
          if (_cursor.moveToFirst()) {
            if (_cursor.isNull(0)) {
              _result = null;
            } else {
              _result = _cursor.getString(0);
            }
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
