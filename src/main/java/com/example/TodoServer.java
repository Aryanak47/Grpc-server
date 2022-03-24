package com.example;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public class TodoServer {
    private static Logger logger = Logger.getLogger(TodoServer.class.getName());

    private static  Server server;

    private  void start() throws IOException {
        int port = 50052;
        server = ServerBuilder.forPort(port).addService(new TodoService()).build().start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            System.out.println("Received shutdown request");
            server.shutdown();
            System.out.println("Server is shutting down");
        }));


    }

    private void awaitServer() throws InterruptedException {
        if(server != null){
            server.awaitTermination();
        }

    }


    public static void main(String[] args) throws IOException,InterruptedException  {
        final TodoServer todoServer =  new TodoServer();
        todoServer.start();
        todoServer.awaitServer();


    }

    static class TodoService extends TodoAppServiceGrpc.TodoAppServiceImplBase {
        static TodoItems.Builder items = TodoItems.newBuilder();
        private  int  count = 0;
        @Override
        public void createTodo(TodoItem request, StreamObserver<TodoItem> responseObserver) {
            String task = request.getTask();
            int id = count++;
            TodoItem item = TodoItem.newBuilder().setId(count).setTask(task).build();
            items.addItems(item);
            responseObserver.onNext(item);
            responseObserver.onCompleted();
        }

        @Override
        public void readTodos(noParam request, StreamObserver<TodoItems> responseObserver) {
            System.out.println(items.getItemsList());
//             send the response
            responseObserver.onNext(items.build());
//            Complete the rpc call
            responseObserver.onCompleted();

        }

        @Override
        public void readTodoStream(noParam request, StreamObserver<TodoItem> responseObserver) {

                try {
                    for (int i = 0; i < items.getItemsCount() ; i++) {
                        TodoItem item = items.getItems(i);
                        responseObserver.onNext(item);
                        Thread.sleep(1000L);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }finally {
                    responseObserver.onCompleted();
                }


        }

        @Override
        public StreamObserver<calculateRequest> readTodoBiStream(StreamObserver<calculateResponse> responseObserver) {
            System.out.println("Got data from client ");
            StreamObserver<calculateRequest> requestStreamObserver = new StreamObserver<calculateRequest>() {
                @Override
                public void onNext(calculateRequest value) {
                    int a = value.getNum1();
                    int b = value.getNum2();
                    System.out.println("Items received from client are : "+a+" "+b);
                    char operator =  value.getOperator().charAt(0);
                    int result =0;
                    switch (operator){
                        case '+':
                            result = a +b;
                            break;
                        case '-':
                            result = a - b;
                            break;
                        case '*' :
                            result = a * b;
                            break;
                    }

                    calculateResponse res =  calculateResponse.newBuilder().setNum(result).build();
                    responseObserver.onNext(res);
                }

                @Override
                public void onError(Throwable t) {
                    System.out.println("error");
                }

                @Override
                public void onCompleted() {
                    responseObserver.onCompleted();

                }
            };
            return requestStreamObserver;
        }
    }
}
