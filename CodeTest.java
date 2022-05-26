import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 *A program that send N http request to Baidu to query "HSBC", and summarize the result base on the http status code. (N can be 10000)
 1. generate the result with readable format
 2. performance - try to use as less time as possible
 3. unit test
 */
public class CodeTest {
    public static void main(String[] args) {
        //request number N
        int N = 500;
        //target url,keyWord= hsbc
        String targetUrl = "https://sp1.baidu.com/5b11fzupBgM18t7jm9iCKT-xh_/sensearch?wd=HSBC";
        //to summary request result
        List<String> resultList = Collections.synchronizedList(new ArrayList<>());
        //get threadPoolExecutor
        ExecutorService executorService = getExecutor();

        CountDownLatch latch = new CountDownLatch(N);
        //execute task and receive result
        for(int i=1;i<=N;i++){
            executorService.execute(()->{
                String result = sendGetRequest(targetUrl);
                resultList.add(result);
                latch.countDown();
            });
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(!executorService.isShutdown()){
            executorService.shutdownNow();
        }

        //iterate resultList
        for (String str : resultList){
            System.out.println(str);
        }
    }

    private static ExecutorService getExecutor(){
        int threadSize = Runtime.getRuntime().availableProcessors();
        int queueSize = 10000;
        ExecutorService executorService = new ThreadPoolExecutor(
                threadSize,
                threadSize,
                3,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueSize),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()   //throw exception
        );

        return executorService;
    }

    //send http request with get method
    public static String sendGetRequest(String urlParam) {

        HttpURLConnection con = null;
        BufferedReader buffer = null;
        StringBuffer resultBuffer = null;

        try {
            URL url = new URL(urlParam);
            //get connection
            con = (HttpURLConnection) url.openConnection();
            //set mothod
            con.setRequestMethod("GET");
            //data type
            con.setRequestProperty("Content-Type", "application/json;charset=GBK");
            //allow write
            con.setDoOutput(true);
            //allow read
            con.setDoInput(true);
            //no cache
            con.setUseCaches(false);
            //status code
            int responseCode = con.getResponseCode();

            if(responseCode == HttpURLConnection.HTTP_OK){
                InputStream inputStream = con.getInputStream();
                resultBuffer = new StringBuffer();
                String line;
                buffer = new BufferedReader(new InputStreamReader(inputStream, "GBK"));
                while ((line = buffer.readLine()) != null) {
                    resultBuffer.append(line);
                }
                return resultBuffer.toString();
            }

        }catch(Exception e) {
            e.printStackTrace();
        }finally {
            try {
                buffer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "";
    }
}
