package com.example;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.micronaut.runtime.Micronaut;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TodoClient {
    private static Logger logger = Logger.getLogger(TodoClient.class.getName());
    private final  TodoAppServiceGrpc.TodoAppServiceBlockingStub blockingStub;
    private TodoAppServiceGrpc.TodoAppServiceStub asyncClient;
    public TodoClient(Channel channel){
        blockingStub = TodoAppServiceGrpc.newBlockingStub(channel);
        asyncClient = TodoAppServiceGrpc.newStub(channel);
    }
    private void addTask(String task){
        logger.info("-----------------Adding task-------------------");
        TodoItem request = TodoItem.newBuilder().setTask(task).build();
        TodoItem response;
        try {
            response = blockingStub.createTodo(request);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }
        System.out.println("\n New item ---> "+response.getTask() +" has been added !");

    }

    private void readTasks(){
        System.out.println("\n -----------------Reading Tasks-------------------\n");
        noParam request = noParam.newBuilder().build();
        TodoItems response;

        try {

            response = blockingStub.readTodos(request);
            System.out.println(response);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }

    }

    private void readStreamingTasks(){
        System.out.println("\n -----------------Reading Tasks-------------------\n");
        noParam request = noParam.newBuilder().build();
        Iterator<TodoItem> items;

        try {

            blockingStub.readTodoStream(request)
                    .forEachRemaining(todoItem -> {
                        System.out.println(todoItem.getTask());
                    });
//            for (int i = 1; items.hasNext(); i++) {
//                TodoItem item = items.next();
//                System.out.println("RESPONSE - Task #" + i + ":"+ item.getTask());
//            }

        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }

    }
    public void calculateNumber() {
        System.out.println("\n -----------------Calculating-------------------\n");
        final CountDownLatch latch = new CountDownLatch(1);

        StreamObserver<calculateResponse> responseStreamObserver = new StreamObserver<calculateResponse>() {
            @Override
            public void onNext(calculateResponse value) {
                System.out.println("Getting response from the server");
                System.out.println("Result is "+ value.getNum());
            }

            @Override
            public void onError(Throwable t) {

                logger.info("bidirectionalStreamingGetListsStockQuotes Failed: "+ Status.fromThrowable(t));
                latch.countDown();

            }

            @Override
            public void onCompleted() {
                System.out.println("Sever is done sending data !");
                latch.countDown();

            }
        };
        StreamObserver<calculateRequest> requestStreamObserver = asyncClient.readTodoBiStream(responseStreamObserver);
        try {
                    Arrays.asList(1,2,3,4,5,6,7,8,9,10).forEach(
                num -> {
                    requestStreamObserver.onNext(calculateRequest.newBuilder()
                        .setNum1(num).setNum2(num+1).setOperator("+")
                        .build());
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
        );
        } catch (RuntimeException e) {
            requestStreamObserver.onError(e);
            throw e;
        }
        requestStreamObserver.onCompleted();

        try {
            if (!latch.await(1, TimeUnit.MINUTES)) {
                logger.info("bidirectionalStreamingGetListsStockQuotes can not finish within 1 minute");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

    public static void main(String[] args) {
        String task ;
        task = String.join(" ",args);
        String target = "localhost:50052";
        TodoClient client = new TodoClient(ManagedChannelBuilder.forTarget(target).usePlaintext().build());
//        client.readStreamingTasks();
//        client.readTasks();

//        if(args.length==0){
//            logger.info("Please enter a task name to add !");
//            return;
//        }

//
//        client.addTask(task);
//        client.readStreamingTasks();
//        client.readTasks();

        // Bi-Directional Streaming

        client.calculateNumber();


    }



}
