package com.horizen.storage;

import java.util.Optional;
import java.util.List;
import javafx.util.Pair;
import com.horizen.utils.ByteArrayWrapper;

public interface Storage {

    Optional<ByteArrayWrapper> get(ByteArrayWrapper key);
    ByteArrayWrapper getOrElse(ByteArrayWrapper key, ByteArrayWrapper defaultValue);
    List<Pair<ByteArrayWrapper,Optional<ByteArrayWrapper>>> get(List<ByteArrayWrapper> keys);
    List<Pair<ByteArrayWrapper,ByteArrayWrapper>> getAll();

    Optional<ByteArrayWrapper> lastVersionID();

    void update(ByteArrayWrapper version, List<ByteArrayWrapper> toRemove,
                List<Pair<ByteArrayWrapper, ByteArrayWrapper>> toUpdate);

    void rollback(ByteArrayWrapper versionID);

    List<ByteArrayWrapper> rollbackVersions();

    void close();
}