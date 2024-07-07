
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class VanillaHttpServer {
    static class HelloHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "Hello World 5!";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }


    static class WorkoutHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "Run a hard 5k!";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    static class FortuneHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "Today is your lucky day.";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    // Handler to calculate the sum of random numbers synchronously
    static class SumHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Extract the query parameter 'n' from the request URI
            String query = exchange.getRequestURI().getQuery();
            BigInteger n = new BigInteger(query.split("=")[1]);
            System.out.println("Value of n = " + n);

            // Initialize random number generator and sum variable
            Random random = new Random();
            BigInteger sum = BigInteger.ZERO;

            // Calculate the sum of 'n' random numbers between 0 and 99
            for (BigInteger i = BigInteger.ZERO; i.compareTo(n) < 0; i = i.add(BigInteger.ONE)) {
                sum = sum.add(BigInteger.valueOf(random.nextInt(100)));
            }
            System.out.println("Calculated sum = " + sum);

            // Send the calculated sum as the response
            String response = sum.toString();
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    // Handler to calculate the sum of random numbers asynchronously
    static class AsyncSumHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Extract the query parameter 'n' from the request URI
            String query = exchange.getRequestURI().getQuery();
            BigInteger n = new BigInteger(query.split("=")[1]);
            System.out.println("Value of n = " + n);

            // Perform the sum calculation asynchronously
            CompletableFuture.supplyAsync(() -> {
                Random random = new Random();
                BigInteger sum = BigInteger.ZERO;
                for (BigInteger i = BigInteger.ZERO; i.compareTo(n) < 0; i = i.add(BigInteger.ONE)) {
                    sum = sum.add(BigInteger.valueOf(random.nextInt(100)));
                }
                return sum;
            }).thenAccept(sum -> {
                try {
                    System.out.println("Calculated sum: " + sum);
                    String response = sum.toString();
                    exchange.sendResponseHeaders(200, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).exceptionally(e -> {
                e.printStackTrace();
                return null;
            });
        }
    }

    // Handler to fetch the sum from an external service synchronously
    static class FetchSumHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Extract the query parameter 'n' from the request URI
            String query = exchange.getRequestURI().getQuery();
            BigInteger n = new BigInteger(query.split("=")[1]);
            System.out.println("Value of n =  " + n);

            // Construct the URL for the external service
            String urlString = "http://localhost:3000/sum?n=" + n;
            BigInteger result = BigInteger.ZERO;

            try {
                // Create an HTTP client and send a GET request to the external service
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                                                 .uri(new URI(urlString))
                                                 .GET()
                                                 .build();

                // Get the response and parse the result
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                result = new BigInteger(response.body());
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Send the fetched sum as the response
            String response = result.toString();
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    // Handler to fetch the sum from an external service asynchronously
    static class FetchSumAsyncHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Extract the query parameter 'n' from the request URI
            String query = exchange.getRequestURI().getQuery();
            BigInteger n = new BigInteger(query.split("=")[1]);
            System.out.println("Value of n =  " + n);

            // Construct the URL for the external service
            String urlString = "http://localhost:3000/sum?n=" + n;

            // Create an HTTP client and send a GET request to the external service asynchronously
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                                             .uri(URI.create(urlString))
                                             .GET()
                                             .build();

            // Handle the response asynchronously
            CompletableFuture<HttpResponse<String>> future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            future.thenApply(HttpResponse::body)
                  .thenApply(BigInteger::new)
                  .thenAccept(result -> {
                      try {
                          String response = result.toString();
                          exchange.sendResponseHeaders(200, response.length());
                          OutputStream os = exchange.getResponseBody();
                          os.write(response.getBytes());
                          os.close();
                      } catch (IOException e) {
                          e.printStackTrace();
                      }
                  })
                  .exceptionally(e -> {
                      e.printStackTrace();
                      return null;
                  });
        }
    }

	public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new HelloHandler());
        server.createContext("/workout", new WorkoutHandler());
        server.createContext("/fortune", new FortuneHandler());
        server.createContext("/sum", new SumHandler());
		server.createContext("/async-sum", new AsyncSumHandler());
        server.createContext("/fetch-sum", new FetchSumHandler());
        server.createContext("/fetch-sum-async", new FetchSumAsyncHandler());
		// Runtime.getRuntime().addShutdownHook(new Thread(() -> {
		// 	server.stop(0); // Stop the server immediately
		// 	System.out.println("Server stopped");
		// }));
        server.setExecutor(null); // creates a default executor
        server.start();
        System.out.println("Yo Server started at 8080");
    }

}
