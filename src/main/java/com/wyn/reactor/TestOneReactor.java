package com.wyn.reactor;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * reactor 流的创建和订阅
 * @description
 * @author wangyinan.wang
 * @date 2021/6/7
 */
public class TestOneReactor {
    public static void main(String[] args) throws Exception {
        test12();
    }


    /**
     * 通过继承BaseSubscriber类实现自定义订阅者
     * @throws Exception
     */
    public static void test12() throws Exception {
        MySubscriber<String> mySubscriber = new MySubscriber<String>();

        Flux<String> stream = Flux.just("Hello", "world", "!");                  // (4)
        stream.subscribe(mySubscriber);                                            // (5)
    }

    /**
     * 一般不建议通过实现Subscriber接口自定义订阅者，而是通过继承BaseSubscriber类
     * 因为BaseSubscriber提供了较为完善的背压处理（默认使用完全推的模式）、异常处理等逻辑，如果自己通过实现Subscriber接口实现这些功能很容易出错
     *不仅可以重载hookOnSubscribe(Subscription)方法hookOnNext(T)方法，还可以重载hookOnError(Throwable)方法、hookOnCancel()方法、hookOnComplete()方法以及其他方法。BaseSubscriber类提供了request(long)和requestUnbounded()这些方法来对响应式流需求进行粒度控制
     * @param <T>
     */
   static class MySubscriber<T> extends BaseSubscriber<T> {
        public void hookOnSubscribe(Subscription subscription) {
            System.out.println("initial request for 1 element");
            request(1);
        }

        public void hookOnNext(T value) {
            System.out.println("onNext: "+ value);
            System.out.println("requesting 1 more element");
            request(1);
        }
    }

    /**
     * 实现自定义订阅者
     * 实现Subscriber接口
     * @throws Exception
     */
    public static void test11() throws Exception {
        Subscriber<String> subscriber = new Subscriber<String>() {
            // 我们的订阅者必须持有对Subscription的引用，而Subscription需要绑定Publisher和Subscriber。
            // 由于订阅和数据处理可能发生在不同的线程中，因此我们使用volatile关键字来确保所有线程都具有对Subscription实例的正确引用
            volatile Subscription subscription;                                   // (1)
            // 触发订阅时，通过onSubscribe回调通知Subscriber
            public void onSubscribe(Subscription s) {                             // (2)
                subscription = s;                                                  // (2.1)
                System.out.println("initial request for 1 element");                         //
                subscription.request(1);                                           // (2.2)
            }

            // 在onNext回调中，记录接收的数据并请求下一个元素。在这种情况下，我们使用简单的拉模型（subscription.request(1)）来管理背压
            public void onNext(String s) {                                        // (3)
                System.out.println("onNext: " + s);                                         //
                System.out.println("requesting 1 more element");                             //
                subscription.request(1);                                           // (3.1)
            }

            public void onComplete() {
                System.out.println("onComplete");
            }

            public void onError(Throwable t) {
                System.out.println("onError:" + t.getMessage());
            }
        };

        Flux<String> stream = Flux.just("Hello", "world", "!");                  // (4)
        stream.subscribe(subscriber);                                            // (5)
    }

    /**
     * interval工厂方法能生成具有周期定义（每50毫秒）的事件。生成的是无限流(从1开始的序列号)
     * @throws Exception
     */
    public static void test10() throws Exception {
        Disposable disposable = Flux.interval(Duration.ofMillis(50))                // (1)
                .subscribe(                                                             // (2)
                        data -> System.out.println(data)
                );
        Thread.sleep(200);
        // 内部取消订阅的dispose方法
        disposable.dispose();
    }

    /**
     * Disposable实例也可用于取消。通常，它不是由订阅者使用，而是由更上一级抽象的代码使用
     * @throws Exception
     */
    public static void test9() throws Exception {
        Flux<String> flux = Flux.range(1, 100).map(a -> {
            try {
                Thread.sleep(10);
            } catch (Exception e) {
            }
            return a + "x";
        });
        Disposable disposable = flux.subscribeOn(Schedulers.parallel()).subscribe(data -> System.out.println(data),
                err -> err.printStackTrace(),
                () -> System.out.println("complete!!!")
        );
        System.out.println(disposable.isDisposed());
        Thread.sleep(500);
        disposable.dispose();
        System.out.println(disposable.isDisposed());
        Thread.sleep(20000);
    }


    public static void test8() throws Exception {
        Flux<String> flux = Flux.range(1, 100).map(a -> a + "x");
        flux.subscribe(data -> System.out.println(data),
                err -> err.printStackTrace(),
                () -> System.out.println("complete!!!"),

                subscription -> { // 通过subscription对象控制订阅
                    subscription.request(4); // 请求4个数据项
                    subscription.cancel();  // 取消订阅
                }
        );

    }

    public static void test7() throws Exception {
        Flux<String> flux = Flux.just(1, 2, 4).map(a -> a + "x");
        flux.subscribe(data -> System.out.println(data), err -> err.printStackTrace(), () -> System.out.println("complete!!!"));
        //这是订阅流的最简单方法，因为此方法会忽略所有信号,通常，我们会首选其他变体方法。但是，触发具有副作用的流处理有时也可能很有用
//        flux.subscribe();
        //对每个值（onNext信号）调用dataConsumer。它不处理onError和onComplete信号。
//        flux.subscribe(Consumer<T> dataConsumer);
        //与上一个相同，但这里可以处理onError信号。而onComplete信号将被忽略。
//        flux.subscribe(Consumer<T> dataConsumer, Consumer<Throwable> errorConsumer);
        // 与上一个相同，但这里也可以处理onComplete信号
//        flux.subscribe(Consumer<T> dataConsumer, Consumer<Throwable> errorConsumer, Runnable completeConsumer);
        // 消费响应式流中的所有元素，包括错误处理和完成信号
//        flux.subscribe(Consumer<T> dataConsumer,  Consumer<Throwable> errorConsumer,Runnable completeConsumer,
//                  Consumer<Subscription> subscriptionConsumer);
        // 订阅序列的最通用方式,我们可以为我们,的Subscriber实现提供所需的行为。这个选项尽管非常通用，但很少被用到。
//        flux.subscribe(Subscriber<T> subscriber);


        Thread.sleep(1000);
    }

    public static void test6() throws Exception {
        requestUserData("123222222222").subscribe();
        Thread.sleep(1000);
    }

    /**
     * defer工厂方法会创建一个序列，并在订阅时决定其行为，因此可以为不同的订阅者生成不同的数据
     * 此代码可将对sessionId的验证推迟至发生实际订阅之后，也就是说，下面的代码并不会立即去执行isValidSession
     * 等逻辑，而是在触发订阅之后才会执行
     * @param sessionId
     * @return
     */
    static Mono<User> requestUserData(String sessionId) {
        return Mono.defer(() ->
                isValidSession(sessionId)
                        ? Mono.fromCallable(() -> requestUser(sessionId))
                        : Mono.error(new RuntimeException("Invalid user session")));
    }

    private static boolean isValidSession(String sessionId) {
        System.out.println("isValidSession____***************");
        return sessionId.startsWith("123");
    }

    private static User requestUser(String sessionId) {
        System.out.println("requestUser____***************");
        return new User(sessionId, 1);
    }

    /**
     * Flux和Mono都有名为empty()的工厂方法，它们分别生成Flux或Mono的空实例。
     * 类似地，never()方法会创建一个永远不会发出完成、数据或错误等信号的流
     * error(Throwable)工厂方法能创建一个序列，该序列在订阅时始终通过每个订阅者的onError(...)方法传播错误。
     * 由于错误是在Flux或Mono声明期间被创建的，因此，每个订阅者都会收到相同的Throwable实例。
     * @throws Exception
     */
    public static void test5() throws Exception {
        Flux<String> empty = Flux.empty();
        Flux<String> never = Flux.never();
        Mono<String> error = Mono.error(new RuntimeException("Unknown id"));
    }

    /**
     * Mono对于包装异步操作（如HTTP请求或数据库查询）非常有用。
     * 为此，Mono提供了fromCallable(Callable)、fromRunnable(Runnable)、fromSupplier(Supplier)、fromFuture(CompletableFuture)、fromCompletionStage(CompletionStage)等方法
     * @throws Exception
     */
    public static void test4() throws Exception {
        Mono<String> mono = Mono.fromCallable(() -> remoteRpc(1)).subscribeOn(Schedulers.parallel());
        // 不触发订阅，流不会执行
        mono.subscribe();
        Thread.sleep(2000);
    }

    public static String remoteRpc(int i) {
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
        }
        System.out.println(Thread.currentThread().getName() + "第" + i + "个任务执行");
        return "task" + i;
    }


    /**
     * Flux和Mono提供了许多工厂方法，可以根据已有的数据创建响应式流。
     */
    public static void test3() {
        // 我们可以使用对象引用或集合创建Flux，甚至可以简单地用数字范围来创建它
        Flux<String> stream1 = Flux.just("Hello", "world");
        Flux<Integer> stream2 = Flux.fromArray(new Integer[]{1, 2, 3});
        Flux<Integer> stream3 = Flux.fromIterable(Arrays.asList(9, 8, 7));
        // 使用range方法生成整数流很容易，其中2010是起点，9是序列中元素的数量：2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018
        Flux<Integer> stream4 = Flux.range(2010, 9);

        // Mono提供类似的工厂方法，但主要针对单个元素。它也经常与nullable类型和Optional类型一起使用
        Mono<String> stream5 = Mono.just("One");
        Mono<String> stream6 = Mono.justOrEmpty(null);
        Mono<String> stream7 = Mono.justOrEmpty(Optional.empty());
    }


    /**
     * 下面这个程序，会不停的重复产生1到100的数字流，这是一个无限流，而且下面的block会触发订阅阻塞正在运行的线程，最终会导致OOM
     * ，因为repeat操作符生成了一个无限流，元素会到达并增加列表的大小，所以它消耗所有内存从而导致应用程序失败，并出现java.lang.OutOfMemoryError: Java heap space错误
     */
    public static void test2() {
        // Flux定义了一个普通的响应式流，它可以产生零个、一个或多个元素，乃至无限元素
        Flux.range(1, 100) //range操作符创建从1到100的整数序列（包括100）
                .repeat()  // repeat操作符在源流完成之后一次又一次地订阅源响应式流。因此，repeat操作符订阅流操作符的结果、接收从1到100的元素以及onComplete信号，然后再次订阅、接收从1到100的元素，以此类推，不停重复。
                .collectList() //尝试将所有生成的元素收集到一个列表中
                .block();  //会触发实际订阅并阻塞正在运行的线程，直到最终结果到达，而在当前场景下不会发生这种情况，因为响应式流是无限的
    }

    public static void test1() throws Exception {
        List<User> userList = new ArrayList<>();
        userList.add(new User("zhangsan", 1));
        userList.add(new User("lili", 2));
        userList.add(new User("wangwu", 2));
//        Flux.fromArray(userList.toArray(new User[0]))
//                .filter(user -> user.getAge()>0)
//                .map(user -> user.getName())
//                .subscribe(System.out::println);
        long time = System.currentTimeMillis();
//        Flux.fromArray(userList.toArray(new User[0]))
//                .filter(user -> user.getAge()>0)
//                .map(user -> testRpc(user.getName()))
//                .subscribe(System.out::println);

        Flux.fromArray(userList.toArray(new User[0]))
                .filter(user -> user.getAge() > 0)
                .publishOn(Schedulers.parallel())
                .flatMap(user -> Flux.just(user).subscribeOn(Schedulers.parallel()).map(u -> testRpc(u.getName())))
                .subscribe(System.out::println);
        System.out.println("total cost:" + (System.currentTimeMillis() - time));

        Thread.currentThread().join();
    }

    public static String testRpc(String name) {
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
        }
        return Thread.currentThread().getName() + "____RPC" + name;
    }
}
