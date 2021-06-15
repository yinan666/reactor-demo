package com.wyn.reactor;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * reactor 相关操作符
 * @description
 * @author wangyinan.wang
 * @date 2021/6/7
 */
public class TestOneReactor_2 {
    public static void main(String[] args) throws Exception {
        test3();
    }

    static ExecutorService pool = Executors.newFixedThreadPool(10);

    /**
     * 下面介绍了timestamp,index两个操作符，可以为数据项添加时间戳和索引序号
     * @throws Exception
     */
    private static void test2()throws Exception{
        Flux.range(2012,10) // 使用range,生成从2012到2021直接的数字
                .timestamp() // 使用timestamp操作符，为每个数据项添加当前的时间戳，返回Flux<Tuple2<Long, T>>类型的数据项流，第一个值是时间戳，第二个是序列
                .index() // 使用index操作符，为每个数据项添加一个索引序号，最终得到也是Flux<Tuple2<Long, T>>类型的数据项流，不过这里面的T是上一个timetstamp的数据项，此时数据项实际上未为：Tuple2<Long, Tuple2<Long, Integer>>>
                .subscribe(o -> println("index:"+o.getT1()+",ts:"+ Instant.ofEpochMilli(o.getT2().getT1())+",value:"+o.getT2().getT2()));
        // 最终得到结果为：index:0,ts:2021-06-09T15:11:07.265Z,value:2012 一直到2021结束
    }

    /**
     * publishOn 的作用是将publishOn之后的操作从当前主线程(main)上转移交给另外一个线程(pool-1-thread-1)去执行，
     * 并不是说publishOn会开启多个线程并发执行，而是将所有数据项交给另外一个线程去串行执行后面的任务。
     * @throws Exception
     */
    private static void test3() throws Exception{
        Flux.range(1,10).map(a -> exeRpc2(a+"")).publishOn(Schedulers.fromExecutor(pool)).map(a ->exeRpc(a)).subscribe();
    }

    private static void println(Object obj){
        System.out.println(obj);
    }

    private static String exeRpc2(String obj){
        System.out.println(Thread.currentThread().getName()+" exeRpc"+obj);
        try {
            Thread.sleep(1000);
        }catch (Exception e){
        }
        return "exeRpc"+obj;
    }

    private static String exeRpc(String obj){
        System.out.println(Thread.currentThread().getName()+" exeRpc"+obj);
       try {
           Thread.sleep(10000);
       }catch (Exception e){
       }
       return "exeRpc"+obj;
    }
    /**
     * 这里用到了map操作符，用于逐个处理元素。当然，当它将元素的类型从T转变为R时，整个序列的类型将改变
     * @throws Exception
     */
    private static void test1()throws Exception{
        Flux.range(1,10).map(a -> a+"x").subscribe(System.out::println);
    }
}
