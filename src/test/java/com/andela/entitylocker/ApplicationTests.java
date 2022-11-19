package com.andela.entitylocker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.andela.entitylocker.utility.EntityLocker;
import com.andela.entitylocker.utility.EntityLockerImpl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationTests {

	private static List<Thread> threads;
	private static MyClass myObject;
	private static EntityLocker<Integer> entityLocker;
	private static int increments = 5;

	@BeforeAll
	public static void init() {
		threads = new ArrayList<>();
		entityLocker = new EntityLockerImpl<>();
		myObject = new MyClass(20);
	}

	@Test
	public void testLock() throws Exception {
		int value = 30;

		try {
			entityLocker.lock(myObject.getId());
			myObject.setValue(value);
		} finally {
			entityLocker.unlock(myObject.getId());
		}

		Assertions.assertEquals(value, myObject.getValue());
	}

	@Test
	public void testeConcurrentRequests() throws Exception {
		for (int i = 0; i < increments; ++i) {
			Thread thread = new Thread(() -> {
				try {
					entityLocker.lock(myObject.getId());
					myObject.plusOne();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					entityLocker.unlock(myObject.getId());
				}
			});
			thread.start();
			threads.add(thread);
		}

		for (Thread thread : threads) {
			thread.join();
		}

		Assertions.assertNotEquals(increments, myObject.getValue());
	}

	@Test
	public void testTimeout() throws Exception {
		final long numOfIncrements = 1;

		Thread thread = new Thread(() -> {
			try {
				entityLocker.lock(myObject.getId());
				Thread.sleep(1000);
				myObject.plusOne();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				entityLocker.unlock(myObject.getId());
			}
		});
		thread.start();
		threads.add(thread);

		Thread anotherThread = new Thread(() -> {
			try {
				entityLocker.lock(myObject.getId(), 1L, TimeUnit.MILLISECONDS);
				try {
					myObject.plusOne();
				} finally {
					entityLocker.unlock(myObject.getId());
				}
			} catch (Exception e) {
				e.printStackTrace();
				// time is out - do nothing
			}
		});
		anotherThread.start();
		threads.add(anotherThread);

		for (Thread t : threads) {
			t.join();
		}

		Assertions.assertEquals(numOfIncrements, myObject.getValue());
	}

	@Test
	public void testGlobalLock() throws Exception {
		final EntityLocker<Integer> entityLocker = new EntityLockerImpl<>();

		for (int i = 0; i < increments; ++i) {
			Thread thread = new Thread(() -> {
				try {
					entityLocker.lock(myObject.getId());
					myObject.plusOne();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					entityLocker.unlock(myObject.getId());
				}
			});
			thread.start();
			threads.add(thread);
		}

		Thread thread = new Thread(() -> {
			entityLocker.globalLock();

			try {
				for (int i = 0; i < increments; i++) {
					myObject.plusOne();
				}
			} finally {
				entityLocker.globalUnlock();
			}
		});
		thread.start();
		threads.add(thread);

		for (Thread t : threads) {
			t.join();
		}

		Assertions.assertEquals(increments * 2, (myObject.getValue() - 1));
	}

	private static class MyClass {
		private int id;

		private int value;

		public MyClass(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}

		public int getValue() {
			return value;
		}

		public void setValue(int value) {
			this.value = value;
		}

		public void plusOne() {
			this.value = this.value + 1;
		}
	}

}
