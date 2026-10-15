package com.orbi.orbi_app.data;

public interface RepositoryCallback<T> {
    void onSuccess(T result);
    void onError(String message);
}
