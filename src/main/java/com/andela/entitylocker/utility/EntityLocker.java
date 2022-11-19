package com.andela.entitylocker.utility;

import java.util.concurrent.TimeUnit;

public interface EntityLocker<T> {

	void lock(T entityId) throws Exception;

	void lock(T entityId, long timeout, TimeUnit timeUnit) throws Exception;

	void unlock(T entityId);

	void globalLock();

	void globalUnlock();

}