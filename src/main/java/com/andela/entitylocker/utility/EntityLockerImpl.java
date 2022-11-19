package com.andela.entitylocker.utility;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class EntityLockerImpl<T> implements EntityLocker<T> {

	private Map<T, Lock> entityLocks = new ConcurrentHashMap<>();
	private ReentrantLock globalReentrantLock = new ReentrantLock(true);
	private boolean isGlobalLock;

	@Override
	public void lock(T entityId) throws Exception {
		if (entityId == null) {
			throw new Exception("Entity id null");
		}

		entityLocks.putIfAbsent(entityId, new ReentrantLock(true));
		Lock lock = entityLocks.get(entityId);

		if (isGlobalLock) {
			globalReentrantLock.lock();
			try {
				// protected code
			} finally {
				globalReentrantLock.unlock();
			}
		}

		lock.lock();
	}

	@Override
	public void lock(T entityId, long timeout, TimeUnit timeUnit) throws Exception {
		if (entityId == null) {
			throw new Exception("Entity id null");
		}

		entityLocks.putIfAbsent(entityId, new ReentrantLock(true));
		Lock lock = entityLocks.get(entityId);

		if (isGlobalLock) {
			globalReentrantLock.lock();
			try {
				// protected code
			} finally {
				globalReentrantLock.unlock();
			}
		}
		if (!lock.tryLock(timeout, timeUnit)) {
			throw new Exception("Timeout");
		}
	}

	@Override
	public void unlock(T entityId) {

		ReentrantLock reentrantLock = (ReentrantLock) entityLocks.get(entityId);

		if (reentrantLock.isHeldByCurrentThread()) {
			reentrantLock.unlock();
		}
	}

	@Override
	public void globalLock() {
		globalReentrantLock.lock();
		isGlobalLock = true;
		for (T id : entityLocks.keySet()) {
			entityLocks.get(id).lock();
		}
	}

	@Override
	public void globalUnlock() {
		if (globalReentrantLock.isHeldByCurrentThread()) {
			isGlobalLock = false;
			for (Lock lock : entityLocks.values()) {
				if (((ReentrantLock) lock).isHeldByCurrentThread()) {
					lock.unlock();
				}
			}
			globalReentrantLock.unlock();
		}
	}

}