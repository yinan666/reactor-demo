package com.wyn.reactor;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * rxjava中的操作符：https://rxmarbles.com/#debounceTime
 * @description
 * @author wangyinan.wang
 * @date 2021/6/7
 */
public class TestRxjava {
    public static void main(String[] args) throws Exception {
        test9();
    }

    static ExecutorService pool = Executors.newFixedThreadPool(10);
    public static void test9()throws Exception {
        long time = System.currentTimeMillis();
        System.out.println(time);
//        Observable.range(1,10)
//                .map(a -> remoteRpc(a))
//                .subscribeOn(Schedulers.io())
//                .subscribe(TestRxjava::executeResult);
        for(int i =1;i<11;i++){
            execute(i).observeOn(Schedulers.from(pool)).subscribe(TestRxjava::executeResult);
        }
        System.out.println("总共用时："+(System.currentTimeMillis() - time));
        Thread.currentThread().join();
    }

    public static Observable<String> execute(int i){
      return  Observable.fromCallable(() -> remoteRpc(i)).subscribeOn(Schedulers.from(pool));
    }

    public static void test8()throws Exception {
        long time = System.currentTimeMillis();
        List<Future> futureList = new ArrayList<>();
        for (int i = 1; i < 11; i++) {
            final int ct = i;
            Future<String> future = pool.submit(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return remoteRpc(ct);
                }
            });
            futureList.add(future);
        }

        // 处理结果
        for(Future<String> future:futureList){
            executeResult(future.get());
        }
        System.out.println("总共用时："+(System.currentTimeMillis() - time));
    }

    private static void executeResult(String result){
        try {
            Thread.sleep(10);
        }catch (Exception e){}
        System.out.println(Thread.currentThread().getName()+"处理结果:"+result+","+System.currentTimeMillis());
    }

    public static String remoteRpc(int i) {
        try {
            if(i == 1){
                Thread.sleep(1000);
            }else {
                Thread.sleep(100);
            }
        } catch (Exception e) {
        }
        System.out.println(Thread.currentThread().getName()+"第" + i + "个任务执行");
        return "task" + i;
    }


    public static void test7() {
        // 自定义操作符
//        Observable.just(1,2,4).compose(new ObservableTransformer<Integer, Object>() {
//            @Override
//            public ObservableSource<Object> apply(Observable<Integer> upstream) {
//                return upstream.
//            }
//        })

    }

    public static void test6() {
        Observable.zip(
                Observable.just("A", "B", "C"),
                Observable.just("1", "2", "3"),
                (x, y) -> x + y).forEach(System.out::println);

    }

    public static void test5() throws Exception {
        // Disposable 用于查看是否取消订阅，并提供取消订阅的方法
        Disposable disposable = Observable.interval(1, TimeUnit.SECONDS) // 每隔指定时间返回一个递增的序列号，从1开始
                .subscribe(e -> System.out.println("Received: " + e));
        // 如果删除Thread.sleep(...)(1)，那么应用程序将在不输出任何内容的情况下退出。发生这种情况是因为生成事件并进行消费的过程发生在一个单独的守护线程中。因此，为了防止主线程完成执行，我们可以调用sleep()方法或执行一些其他有用的任务
        Thread.sleep(5000);
        System.out.println("disposeable: " + disposable.isDisposed());
        Thread.sleep(3000);
        disposable.dispose();
        Thread.sleep(3000);
        System.out.println("over disposeable: " + disposable.isDisposed());

    }

    public static void test4() throws Exception {
        Observable.interval(1, TimeUnit.SECONDS) // 每隔指定时间返回一个递增的序列号，从1开始
                .subscribe(e -> System.out.println("Received: " + e));
        // 如果删除Thread.sleep(...)(1)，那么应用程序将在不输出任何内容的情况下退出。发生这种情况是因为生成事件并进行消费的过程发生在一个单独的守护线程中。因此，为了防止主线程完成执行，我们可以调用sleep()方法或执行一些其他有用的任务
        Thread.sleep(5000);
    }

    public static void test3() throws Exception {
        Observable.just("1", "2", "3").map(a -> "x" + a).subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                System.out.println("consumer get:" + s);
            }
        });

        Observable.just(new String[]{"1", "2", "3"});
        Observable.fromArray(new String[]{"1", "2", "3"});
        Observable.fromCallable(() -> {
            return "hello";
        });
        // 可以从future中获取数据源
        Future<String> future = Executors.newCachedThreadPool().submit(() -> "World");
        Observable<String> world = Observable.fromFuture(future);
    }

    public static List<String> getList() {
        List<String> list = new ArrayList<>();
        list.add("1");
        list.add("2");
        return list;
    }

    public static void test2() throws Exception {
        // 创建一个Observable并使其带有一个回调，该回调将在订阅者出现时立即被触发(1
//        Observable<String> observable = Observable.create(new ObservableOnSubscribe<String>() {
//            @Override
//            public void subscribe(ObservableEmitter<String> observableEmitter) throws Exception {
//                observableEmitter.onNext("hello, reactive world"); // Observer将产生一个字符串值
//                observableEmitter.onComplete(); // 并将流的结束信号发送给订阅者
//            }
//        });
        // 等同于上面的代码，用java8 的lambda进行了简化
        Observable<String> observable = Observable.create(sub -> {
            sub.onNext("hello, reactive world"); // Observer将产生一个字符串值
            sub.onComplete(); // 并将流的结束信号发送给订阅者
        });

        Subscriber<String> subscriber = new Subscriber<String>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                // 出发订阅时调用该方法
                System.out.println("触发了订阅");
            }

            @Override
            public void onNext(String s) {
                System.out.println("订阅者收到数据：" + s);
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("订阅者收到异常：" + throwable.getMessage());
            }

            @Override
            public void onComplete() {
                System.out.println("订阅者结束消息...............");
            }
        };

        observable.subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {

            }
        });

    }

    public static void test1() throws Exception {
        ExecutorService myPool = Executors.newFixedThreadPool(5);
        List<User> userList = new ArrayList<>();
        userList.add(new User("zhangsan", 1));
        userList.add(new User("lili", 2));
        userList.add(new User("wangwu", 2));
//        Flux.fromArray(userList.toArray(new User[0]))
//                .filter(user -> user.getAge()>0)
//                .map(user -> user.getName())
//                .subscribe(System.out::println);
        long time = System.currentTimeMillis();
//        Flowable.fromArray(userList.toArray(new User[0]))
//                .observeOn(Schedulers.io())
//                .map(user -> testRpc(user.getName()))
//                .subscribe(System.out::println);

//        Flowable.fromCallable(() ->{
//            Thread.sleep(1000);
//            return "ok";
//        })
//                .subscribeOn(Schedulers.io())
//                .observeOn(Schedulers.single())
//                .subscribe(System.out::println);

        Flowable.fromArray(userList.toArray(new User[0]))
                .observeOn(Schedulers.io())
                .flatMap(user -> Flowable.just(user)
//                                    .subscribeOn(Schedulers.io()) // 使用框架默认提供的io线程池
                        .subscribeOn(Schedulers.from(myPool)) // 使用自定义的线程池
                        .map(u -> testRpc(u.getName())))
                .blockingSubscribe(System.out::println);

        System.out.println("total cost:" + (System.currentTimeMillis() - time));
        Thread.currentThread().join();
    }

    public static String testRpc(String name) {
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
        }
        return Thread.currentThread().getName() + "__RPC" + name;
    }
}
