/*
 * Copyright (c) 2011-2017 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactor.core.publisher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.Exceptions;
import reactor.core.Fuseable;
import reactor.core.Scannable;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.test.subscriber.AssertSubscriber;
import reactor.util.concurrent.QueueSupplier;

import static java.lang.Thread.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;
import static reactor.core.scheduler.Schedulers.parallel;

public class FluxPeekFuseableTest {

	@Test(expected = NullPointerException.class)
	public void nullSource() {
		new FluxPeekFuseable<>(null, null, null, null, null, null, null, null);
	}

	@Test
	public void normal() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create();

		AtomicReference<Subscription> onSubscribe = new AtomicReference<>();
		AtomicReference<Integer> onNext = new AtomicReference<>();
		AtomicReference<Throwable> onError = new AtomicReference<>();
		AtomicBoolean onComplete = new AtomicBoolean();
		AtomicLong onRequest = new AtomicLong();
		AtomicBoolean onAfterComplete = new AtomicBoolean();
		AtomicBoolean onCancel = new AtomicBoolean();

		new FluxPeekFuseable<>(Flux.just(1),
				onSubscribe::set,
				onNext::set,
				onError::set,
				() -> onComplete.set(true),
				() -> onAfterComplete.set(true),
				onRequest::set,
				() -> onCancel.set(true)).subscribe(ts);

		Assert.assertNotNull(onSubscribe.get());
		Assert.assertEquals((Integer) 1, onNext.get());
		Assert.assertNull(onError.get());
		Assert.assertTrue(onComplete.get());
		Assert.assertTrue(onAfterComplete.get());
		Assert.assertEquals(Long.MAX_VALUE, onRequest.get());
		Assert.assertFalse(onCancel.get());
	}

	@Test
	public void error() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create();

		AtomicReference<Subscription> onSubscribe = new AtomicReference<>();
		AtomicReference<Integer> onNext = new AtomicReference<>();
		AtomicReference<Throwable> onError = new AtomicReference<>();
		AtomicBoolean onComplete = new AtomicBoolean();
		AtomicLong onRequest = new AtomicLong();
		AtomicBoolean onAfterComplete = new AtomicBoolean();
		AtomicBoolean onCancel = new AtomicBoolean();

		new FluxPeekFuseable<>(Flux.error(new RuntimeException("forced failure")),
				onSubscribe::set,
				onNext::set,
				onError::set,
				() -> onComplete.set(true),
				() -> onAfterComplete.set(true),
				onRequest::set,
				() -> onCancel.set(true)).subscribe(ts);

		Assert.assertNotNull(onSubscribe.get());
		Assert.assertNull(onNext.get());
		Assert.assertTrue(onError.get() instanceof RuntimeException);
		Assert.assertFalse(onComplete.get());
		Assert.assertTrue(onAfterComplete.get());
		Assert.assertEquals(Long.MAX_VALUE, onRequest.get());
		Assert.assertFalse(onCancel.get());
	}

	@Test
	public void empty() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create();

		AtomicReference<Subscription> onSubscribe = new AtomicReference<>();
		AtomicReference<Integer> onNext = new AtomicReference<>();
		AtomicReference<Throwable> onError = new AtomicReference<>();
		AtomicBoolean onComplete = new AtomicBoolean();
		AtomicLong onRequest = new AtomicLong();
		AtomicBoolean onAfterComplete = new AtomicBoolean();
		AtomicBoolean onCancel = new AtomicBoolean();

		new FluxPeekFuseable<>(Flux.empty(),
				onSubscribe::set,
				onNext::set,
				onError::set,
				() -> onComplete.set(true),
				() -> onAfterComplete.set(true),
				onRequest::set,
				() -> onCancel.set(true)).subscribe(ts);

		Assert.assertNotNull(onSubscribe.get());
		Assert.assertNull(onNext.get());
		Assert.assertNull(onError.get());
		Assert.assertTrue(onComplete.get());
		Assert.assertTrue(onAfterComplete.get());
		Assert.assertEquals(Long.MAX_VALUE, onRequest.get());
		Assert.assertFalse(onCancel.get());
	}

	@Test
	public void never() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create();

		AtomicReference<Subscription> onSubscribe = new AtomicReference<>();
		AtomicReference<Integer> onNext = new AtomicReference<>();
		AtomicReference<Throwable> onError = new AtomicReference<>();
		AtomicBoolean onComplete = new AtomicBoolean();
		AtomicLong onRequest = new AtomicLong();
		AtomicBoolean onAfterComplete = new AtomicBoolean();
		AtomicBoolean onCancel = new AtomicBoolean();

		new FluxPeekFuseable<>(Flux.never(),
				onSubscribe::set,
				onNext::set,
				onError::set,
				() -> onComplete.set(true),
				() -> onAfterComplete.set(true),
				onRequest::set,
				() -> onCancel.set(true)).subscribe(ts);

		Assert.assertNotNull(onSubscribe.get());
		Assert.assertNull(onNext.get());
		Assert.assertNull(onError.get());
		Assert.assertFalse(onComplete.get());
		Assert.assertFalse(onAfterComplete.get());
		Assert.assertEquals(Long.MAX_VALUE, onRequest.get());
		Assert.assertFalse(onCancel.get());
	}

	@Test
	public void neverCancel() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create();

		AtomicReference<Subscription> onSubscribe = new AtomicReference<>();
		AtomicReference<Integer> onNext = new AtomicReference<>();
		AtomicReference<Throwable> onError = new AtomicReference<>();
		AtomicBoolean onComplete = new AtomicBoolean();
		AtomicLong onRequest = new AtomicLong();
		AtomicBoolean onAfterComplete = new AtomicBoolean();
		AtomicBoolean onCancel = new AtomicBoolean();

		new FluxPeekFuseable<>(Flux.never(),
				onSubscribe::set,
				onNext::set,
				onError::set,
				() -> onComplete.set(true),
				() -> onAfterComplete.set(true),
				onRequest::set,
				() -> onCancel.set(true)).subscribe(ts);

		Assert.assertNotNull(onSubscribe.get());
		Assert.assertNull(onNext.get());
		Assert.assertNull(onError.get());
		Assert.assertFalse(onComplete.get());
		Assert.assertFalse(onAfterComplete.get());
		Assert.assertEquals(Long.MAX_VALUE, onRequest.get());
		Assert.assertFalse(onCancel.get());

		ts.cancel();

		Assert.assertTrue(onCancel.get());
	}

	@Test
	public void callbackError() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create();

		Throwable err = new Exception("test");

		Flux.just(1)
		    .doOnNext(d -> {
			    throw Exceptions.propagate(err);
		    })
		    .subscribe(ts);

		//nominal error path (DownstreamException)
		ts.assertErrorMessage("test");

		ts = AssertSubscriber.create();

		try {
			Flux.just(1)
			    .doOnNext(d -> {
				    throw Exceptions.bubble(err);
			    })
			    .subscribe(ts);

			Assert.fail();
		}
		catch (Exception e) {
			Assert.assertTrue(Exceptions.unwrap(e) == err);
		}
	}

	@Test
	public void completeCallbackError() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create();

		Throwable err = new Exception("test");

		Flux.just(1)
		    .doOnComplete(() -> {
			    throw Exceptions.propagate(err);
		    })
		    .subscribe(ts);

		//nominal error path (DownstreamException)
		ts.assertErrorMessage("test");

		ts = AssertSubscriber.create();

		try {
			Flux.just(1)
			    .doOnComplete(() -> {
				    throw Exceptions.bubble(err);
			    })
			    .subscribe(ts);

			Assert.fail();
		}
		catch (Exception e) {
			Assert.assertTrue(Exceptions.unwrap(e) == err);
		}
	}

	@Test
	public void errorCallbackError() {
		IllegalStateException err = new IllegalStateException("test");

		FluxPeekFuseable<String> flux = new FluxPeekFuseable<>(
				Flux.error(new IllegalArgumentException("bar")), null, null,
				e -> { throw err; },
				null, null, null, null);

		AssertSubscriber<String> ts = AssertSubscriber.create();
		flux.subscribe(ts);

		ts.assertNoValues();
		ts.assertError(IllegalStateException.class);
		ts.assertErrorWith(e -> e.getSuppressed()[0].getMessage().equals("bar"));
	}

	//See https://github.com/reactor/reactor-core/issues/272
	@Test
	public void errorCallbackError2() {
		//test with alternate / wrapped error types

		AssertSubscriber<Integer> ts = AssertSubscriber.create();
		Throwable err = new Exception("test");

		Flux.just(1)
		    .doOnNext(d -> {
			    throw new RuntimeException();
		    })
		    .doOnError(e -> {
			    throw Exceptions.propagate(err);
		    })
		    .subscribe(ts);

		//nominal error path (DownstreamException)
		ts.assertErrorMessage("test");

		ts = AssertSubscriber.create();
		try {
			Flux.just(1)
			    .doOnNext(d -> {
				    throw new RuntimeException();
			    })
			    .doOnError(d -> {
				    throw Exceptions.bubble(err);
			    })
			    .subscribe(ts);

			Assert.fail();
		}
		catch (Exception e) {
			Assert.assertTrue(Exceptions.unwrap(e) == err);
		}
	}

	//See https://github.com/reactor/reactor-core/issues/253
	@Test
	public void errorCallbackErrorWithParallel() {
		AssertSubscriber<Integer> assertSubscriber = new AssertSubscriber<>();

		Mono.just(1)
		    .publishOn(parallel())
		    .doOnNext(i -> {
			    throw new IllegalArgumentException();
		    })
		    .doOnError(e -> {
			    throw new IllegalStateException(e);
		    })
		    .subscribe(assertSubscriber);

		assertSubscriber
				.await()
				.assertError(IllegalStateException.class)
				.assertNotComplete();
	}

	@Test
	public void afterTerminateCallbackErrorDoesNotInvokeOnError() {
		IllegalStateException err = new IllegalStateException("test");
		AtomicReference<Throwable> errorCallbackCapture = new AtomicReference<>();

		FluxPeekFuseable<String> flux = new FluxPeekFuseable<>(
				Flux.empty(), null, null, errorCallbackCapture::set, null,
				() -> { throw err; }, null, null);

		AssertSubscriber<String> ts = AssertSubscriber.create();

		try {
			flux.subscribe(ts);
			fail("expected thrown exception");
		}
		catch (Exception e) {
			assertThat(e).hasCause(err);
		}
		ts.assertNoValues();
		ts.assertComplete();

		//the onError wasn't invoked:
		assertThat(errorCallbackCapture.get()).isNull();
	}

	@Test
	public void afterTerminateCallbackFatalIsThrownDirectly() {
		AtomicReference<Throwable> errorCallbackCapture = new AtomicReference<>();
		Error fatal = new LinkageError();
		FluxPeekFuseable<String> flux = new FluxPeekFuseable<>(
				Flux.empty(), null, null, errorCallbackCapture::set, null,
				() -> { throw fatal; }, null, null);

		AssertSubscriber<String> ts = AssertSubscriber.create();

		try {
			flux.subscribe(ts);
			fail("expected thrown exception");
		}
		catch (Throwable e) {
			assertSame(fatal, e);
		}
		ts.assertNoValues();
		ts.assertComplete();

		Assert.assertThat(errorCallbackCapture.get(), is(nullValue()));


		//same with after error
		errorCallbackCapture.set(null);
		flux = new FluxPeekFuseable<>(
				Flux.error(new NullPointerException()), null, null, errorCallbackCapture::set, null,
				() -> { throw fatal; }, null, null);

		ts = AssertSubscriber.create();

		try {
			flux.subscribe(ts);
			fail("expected thrown exception");
		}
		catch (Throwable e) {
			assertSame(fatal, e);
		}
		ts.assertNoValues();
		ts.assertError(NullPointerException.class);

		Assert.assertThat(errorCallbackCapture.get(), is(instanceOf(NullPointerException.class)));
	}

	@Test
	public void afterTerminateCallbackErrorAndErrorCallbackError() {
		IllegalStateException err = new IllegalStateException("afterTerminate");
		IllegalArgumentException err2 = new IllegalArgumentException("error");

		FluxPeekFuseable<String> flux = new FluxPeekFuseable<>(
				Flux.empty(), null, null, e -> { throw err2; },
				null,
				() -> { throw err; }, null, null);

		AssertSubscriber<String> ts = AssertSubscriber.create();

		try {
			flux.subscribe(ts);
			fail("expected thrown exception");
		}
		catch (Exception e) {
			e.printStackTrace();
			assertSame(e.toString(), err, e.getCause());
			assertEquals(0, err2.getSuppressed().length);
			//err2 is never thrown
		}
		ts.assertNoValues();
		ts.assertComplete();
	}

	@Test
	public void afterTerminateCallbackErrorAndErrorCallbackError2() {
		IllegalStateException afterTerminate = new IllegalStateException("afterTerminate");
		IllegalArgumentException error = new IllegalArgumentException("error");
		NullPointerException err = new NullPointerException();

		FluxPeekFuseable<String> flux = new FluxPeekFuseable<>(
				Flux.error(err),
				null, null,
				e -> { throw error; }, null, () -> { throw afterTerminate; },
				null, null);

		AssertSubscriber<String> ts = AssertSubscriber.create();

		try {
			flux.subscribe(ts);
			fail("expected thrown exception");
		}
		catch (Exception e) {
			assertSame(afterTerminate, e.getCause());
			//afterTerminate suppressed error which itself suppressed original err
			assertEquals(1, afterTerminate.getSuppressed().length);
			assertEquals(error, afterTerminate.getSuppressed()[0]);

			assertEquals(1, error.getSuppressed().length);
			assertEquals(err, error.getSuppressed()[0]);
		}
		ts.assertNoValues();
		//the subscriber still sees the 'error' message since actual.onError is called before the afterTerminate callback
		ts.assertErrorMessage("error");
	}


	@Test
	public void syncFusionAvailable() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create();

		Flux.range(1, 2)
		    .doOnNext(v -> {
		    })
		    .subscribe(ts);

		Subscription s = ts.upstream();
		Assert.assertTrue("Non-fuseable upstream: " + s,
				s instanceof Fuseable.QueueSubscription);
	}

	@Test
	public void asyncFusionAvailable() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create();

		UnicastProcessor.<Integer>builder().queue(QueueSupplier.<Integer>get(2).get()).build()
		                .doOnNext(v -> {
		                })
		                .subscribe(ts);

		Subscription s = ts.upstream();
		Assert.assertTrue("Non-fuseable upstream" + s,
				s instanceof Fuseable.QueueSubscription);
	}

	@Test
	public void conditionalFusionAvailable() {
		AssertSubscriber<Object> ts = AssertSubscriber.create();

		FluxSource.wrap(u -> {
			if (!(u instanceof Fuseable.ConditionalSubscriber)) {
				Operators.error(u,
						new IllegalArgumentException("The subscriber is not conditional: " + u));
			}
			else {
				Operators.complete(u);
			}
		})
		          .doOnNext(v -> {
		          })
		          .filter(v -> true)
		          .subscribe(ts);

		ts.assertNoError()
		  .assertNoValues()
		  .assertComplete();
	}

	@Test
	public void conditionalFusionAvailableWithFuseable() {
		AssertSubscriber<Object> ts = AssertSubscriber.create();

		FluxSource.wrap(u -> {
			if (!(u instanceof Fuseable.ConditionalSubscriber)) {
				Operators.error(u,
						new IllegalArgumentException("The subscriber is not conditional: " + u));
			}
			else {
				Operators.complete(u);
			}
		})
		          .doOnNext(v -> {
		          })
		          .filter(v -> true)
		          .subscribe(ts);

		ts.assertNoError()
		  .assertNoValues()
		  .assertComplete();
	}

	//TODO was these 2 tests supposed to trigger sync fusion and go through poll() ?
	@Test
	public void noFusionCompleteCalled() {
		AtomicBoolean onComplete = new AtomicBoolean();

		AssertSubscriber<Object> ts = AssertSubscriber.create();

		Flux.range(1, 2)
		    .doOnComplete(() -> onComplete.set(true))
		    .subscribe(ts);

		ts.assertNoError()
		  .assertValues(1, 2)
		  .assertComplete();

		Assert.assertTrue("onComplete not called back", onComplete.get());
	}

	@Test
	public void noFusionAfterTerminateCalled() {
		AtomicBoolean onTerminate = new AtomicBoolean();

		AssertSubscriber<Object> ts = AssertSubscriber.create();

		Flux.range(1, 2)
		    .doAfterTerminate(() -> onTerminate.set(true))
		    .subscribe(ts);

		ts.assertNoError()
		  .assertValues(1, 2)
		  .assertComplete();

		Assert.assertTrue("onComplete not called back", onTerminate.get());
	}

	@Test
	public void syncPollCompleteCalled() {
		AtomicBoolean onComplete = new AtomicBoolean();
		ConnectableFlux<Integer> f = Flux.just(1)
		                                .doOnComplete(() -> onComplete.set(true))
		                                .publish();
		StepVerifier.create(f)
		            .then(f::connect)
		            .expectNext(1)
		            .verifyComplete();

		assertThat(onComplete.get()).withFailMessage("onComplete not called back").isTrue();
	}

	@Test
	public void syncPollConditionalCompleteCalled() {
		AtomicBoolean onComplete = new AtomicBoolean();
		ConnectableFlux<Integer> f = Flux.just(1)
		                                .doOnComplete(() -> onComplete.set(true))
		                                .filter(v -> true)
		                                .publish();
		StepVerifier.create(f)
		            .then(f::connect)
		            .expectNext(1)
		            .verifyComplete();

		assertThat(onComplete.get()).withFailMessage("onComplete not called back").isTrue();
	}

	@Test
	public void syncPollAfterTerminateCalled() {
		AtomicBoolean onAfterTerminate = new AtomicBoolean();
		ConnectableFlux<Integer> f = Flux.just(1)
		                                .doAfterTerminate(() -> onAfterTerminate.set(true))
		                                .publish();
		StepVerifier.create(f)
		            .then(f::connect)
		            .expectNext(1)
		            .verifyComplete();

		assertThat(onAfterTerminate.get()).withFailMessage("onAfterTerminate not called back").isTrue();
	}

	@Test
	public void syncPollConditionalAfterTerminateCalled() {
		AtomicBoolean onAfterTerminate = new AtomicBoolean();
		ConnectableFlux<Integer> f = Flux.just(1)
		                                .doAfterTerminate(() -> onAfterTerminate.set(true))
		                                .filter(v -> true)
		                                .publish();
		StepVerifier.create(f)
		            .then(f::connect)
		            .expectNext(1)
		            .verifyComplete();

		assertThat(onAfterTerminate.get()).withFailMessage("onAfterTerminate not called back").isTrue();
	}

	@Test
	public void fusedDoOnNextOnErrorBothFailing() {
		ConnectableFlux<Integer> f = Flux.just(1)
		                                 .doOnNext(i -> {
			                                 throw new IllegalArgumentException("fromOnNext");
		                                 })
		                                 .doOnError(e -> {
			                                 throw new IllegalStateException("fromOnError", e);
		                                 })
		                                 .publish();

		StepVerifier.create(f)
		            .then(f::connect)
		            .verifyErrorMatches(e -> e instanceof IllegalStateException
				            && "fromOnError".equals(e.getMessage())
				            && e.getCause() instanceof IllegalArgumentException
				            && "fromOnNext".equals(e.getCause().getMessage()));
	}

	@Test
	public void fusedDoOnNextOnErrorDoOnErrorAllFailing() {
		ConnectableFlux<Integer> f = Flux.just(1)
		                                 .doOnNext(i -> {
			                                 throw new IllegalArgumentException("fromOnNext");
		                                 })
		                                 .doOnError(e -> {
			                                 throw new IllegalStateException("fromOnError", e);
		                                 })
		                                 .doOnError(e -> {
			                                 throw new IllegalStateException("fromOnError2", e);
		                                 })
		                                 .publish();

		StepVerifier.create(f)
		            .then(f::connect)
		            .verifyErrorSatisfies(e -> {
					            assertThat(e)
					                      .isInstanceOf(IllegalStateException.class)
					                      .hasMessage("fromOnError2")
					                      .hasCauseInstanceOf(IllegalStateException.class);
					            assertThat(e.getCause())
					                      .hasMessage("fromOnError")
					                      .hasCauseInstanceOf(IllegalArgumentException.class);
					            assertThat(e.getCause().getCause())
					                      .hasMessage("fromOnNext");
				            });
	}

	@Test
	public void fusedDoOnNextCallsOnErrorWhenFailing() {
		AtomicBoolean passedOnError = new AtomicBoolean();

		ConnectableFlux<Integer> f = Flux.just(1)
		                                 .doOnNext(i -> {
			                                 throw new IllegalArgumentException("fromOnNext");
		                                 })
		                                 .doOnError(e -> passedOnError.set(true))
		                                 .publish();

		StepVerifier.create(f)
		            .then(f::connect)
		            .verifyErrorMatches(e -> e instanceof IllegalArgumentException
				            && "fromOnNext".equals(e.getMessage()));

		assertThat(passedOnError.get()).isTrue();
	}

	@Test
	public void conditionalFusedDoOnNextOnErrorBothFailing() {
		ConnectableFlux<Integer> f = Flux.just(1)
		                                 .doOnNext(i -> {
			                                 throw new IllegalArgumentException("fromOnNext");
		                                 })
		                                 .doOnError(e -> {
			                                 throw new IllegalStateException("fromOnError", e);
		                                 })
		                                 .filter(v -> true)
		                                 .publish();

		StepVerifier.create(f)
		            .then(f::connect)
		            .verifyErrorMatches(e -> e instanceof IllegalStateException
				            && "fromOnError".equals(e.getMessage())
				            && e.getCause() instanceof IllegalArgumentException
				            && "fromOnNext".equals(e.getCause().getMessage()));
	}

	@Test
	public void conditionalFusedDoOnNextOnErrorDoOnErrorAllFailing() {
		ConnectableFlux<Integer> f = Flux.just(1)
		                                 .doOnNext(i -> {
			                                 throw new IllegalArgumentException("fromOnNext");
		                                 })
		                                 .doOnError(e -> {
			                                 throw new IllegalStateException("fromOnError", e);
		                                 })
		                                 .doOnError(e -> {
			                                 throw new IllegalStateException("fromOnError2", e);
		                                 })
		                                 .filter(v -> true)
		                                 .publish();

		StepVerifier.create(f)
		            .then(f::connect)
		            .verifyErrorSatisfies(e -> {
					            assertThat(e)
					                      .isInstanceOf(IllegalStateException.class)
					                      .hasMessage("fromOnError2")
					                      .hasCauseInstanceOf(IllegalStateException.class);
					            assertThat(e.getCause())
					                      .hasMessage("fromOnError")
					                      .hasCauseInstanceOf(IllegalArgumentException.class);
					            assertThat(e.getCause().getCause())
					                      .hasMessage("fromOnNext");
				            });
	}

	@Test
	public void conditionalFusedDoOnNextCallsOnErrorWhenFailing() {
		AtomicBoolean passedOnError = new AtomicBoolean();

		ConnectableFlux<Integer> f = Flux.just(1)
		                                 .doOnNext(i -> {
			                                 throw new IllegalArgumentException("fromOnNext");
		                                 })
		                                 .doOnError(e -> passedOnError.set(true))
		                                 .filter(v -> true)
		                                 .publish();

		StepVerifier.create(f)
		            .then(f::connect)
		            .verifyErrorMatches(e -> e instanceof IllegalArgumentException
				            && "fromOnNext".equals(e.getMessage()));

		assertThat(passedOnError.get()).isTrue();
	}

	@Test
	public void should_reduce_to_10_events() {
		for (int i = 0; i < 20; i++) {
			int n = i;
			List<Integer> rs = Collections.synchronizedList(new ArrayList<>());
			AtomicInteger count = new AtomicInteger();
			Flux.range(0, 10)
			    .flatMap(x -> Flux.range(0, 2)
			                      .doOnNext(rs::add)
			                      .map(y -> blockingOp(x, y))
			                      .subscribeOn(Schedulers.parallel())
			                      .reduce((l, r) -> l + "_" + r +" ("+x+", it:"+n+")")
			    )
			    .doOnNext(s -> {
				    count.incrementAndGet();
			    })
			    .blockLast();

			System.out.println(rs);
			assertEquals(10, count.get());
		}
	}

	@Test
	public void should_reduce_to_10_events_conditional() {
		for (int i = 0; i < 20; i++) {
			AtomicInteger count = new AtomicInteger();
			Flux.range(0, 10)
			    .flatMap(x -> Flux.range(0, 2)
			                      .map(y -> blockingOp(x, y))
			                      .subscribeOn(Schedulers.parallel())
			                      .reduce((l, r) -> l + "_" + r)
			                      .doOnSuccess(s -> {
				                      count.incrementAndGet();
			                      })
			                      .filter(v -> true))
			    .blockLast();

			assertEquals(10, count.get());
		}
	}

	static String blockingOp(Integer x, Integer y) {
		try {
			sleep(10);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		return "x" + x + "y" + y;
	}


	@Test
    public void scanFuseableSubscriber() {
        Subscriber<Integer> actual = new LambdaSubscriber<>(null, e -> {}, null, null);
        FluxPeek<Integer> peek = new FluxPeek<>(Flux.just(1), s -> {}, s -> {},
        		e -> {}, () -> {}, () -> {}, r -> {}, () -> {});
        FluxPeekFuseable.PeekFuseableSubscriber<Integer> test = new FluxPeekFuseable.PeekFuseableSubscriber<>(actual, peek);
        Subscription parent = Operators.emptySubscription();
        test.onSubscribe(parent);

        assertThat(test.scan(Scannable.ScannableAttr.PARENT)).isSameAs(parent);
        assertThat(test.scan(Scannable.ScannableAttr.ACTUAL)).isSameAs(actual);

        assertThat(test.scan(Scannable.BooleanAttr.TERMINATED)).isFalse();
        test.onError(new IllegalStateException("boom"));
        assertThat(test.scan(Scannable.BooleanAttr.TERMINATED)).isTrue();
    }

    @Test
    public void scanFuseableConditionalSubscriber() {
	    @SuppressWarnings("unchecked")
	    Fuseable.ConditionalSubscriber<Integer> actual = Mockito.mock(Fuseable.ConditionalSubscriber.class);
        FluxPeek<Integer> peek = new FluxPeek<>(Flux.just(1), s -> {}, s -> {},
        		e -> {}, () -> {}, () -> {}, r -> {}, () -> {});
        FluxPeekFuseable.PeekFuseableConditionalSubscriber<Integer> test =
        		new FluxPeekFuseable.PeekFuseableConditionalSubscriber<>(actual, peek);
        Subscription parent = Operators.emptySubscription();
        test.onSubscribe(parent);

        assertThat(test.scan(Scannable.ScannableAttr.PARENT)).isSameAs(parent);
        assertThat(test.scan(Scannable.ScannableAttr.ACTUAL)).isSameAs(actual);

        assertThat(test.scan(Scannable.BooleanAttr.TERMINATED)).isFalse();
        test.onError(new IllegalStateException("boom"));
        assertThat(test.scan(Scannable.BooleanAttr.TERMINATED)).isTrue();
    }
}
