package com.haohandata.httpclient;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.haohandata.Constant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteAccess {
    private final Logger logger = LoggerFactory.getLogger(RemoteAccess.class);
    public final static String APPLICATION_JSON = "application/json";

    private List<GenericUrl> urlList;
    private HttpRequestFactory hrFactory;

    private HttpTransport httpTransport;
    private JsonFactory jsonFactory;


    public RemoteAccess(String url) throws MalformedURLException {

        urlList = new ArrayList<>();
        GenericUrl genericUrl = new GenericUrl(new URL(url));
        urlList.add(genericUrl);
    }

    public RemoteAccess(String ip, Integer port, String api) {
        urlList = new ArrayList<>();
        GenericUrl url = new GenericUrl();
        url.setScheme("http");
        url.setHost(ip);
        url.setPort(port);
        url.setRawPath(api);
        urlList.add(url);
    }

    public void build() {
        try {
            httpTransport = new NetHttpTransport.Builder().doNotValidateCertificate().build();
            jsonFactory = new GsonFactory();
            hrFactory = httpTransport.createRequestFactory(new HttpRequestInitializer() {
                @Override
                public void initialize(HttpRequest request) {
                    request.setConnectTimeout(Constant.HTTP_REQUEST_TIMEOUT);  //设置HTTP请求的超时时间：3秒
                    request.setParser(new JsonObjectParser(jsonFactory));
                    HttpHeaders httpHeaders = new HttpHeaders();
                    httpHeaders.setBasicAuthentication(Constant.ATLAS_USER, Constant.ATLAS_PASSWORD);
                    request.setHeaders(httpHeaders);
                }
            });
        } catch (GeneralSecurityException ex) {
            logger.error(ex.getMessage());
        }

    }

    private String parseResponse(HttpResponse response) {
        String jsonStr = null;
        try {
            jsonStr = response.parseAsString();
        } catch (IOException ex) {
            logger.error("", ex);
            try {
                response.disconnect();

            } catch (IOException ex1) {
                logger.error("", ex1);
            }
        }
        return jsonStr;
    }

    /**
     * get请求，返回IP列表下的所有API返回结果
     *
     * @param paramList
     * @return
     */
    public List<RemoteResponse> get(List<HttpParam> paramList) {
        List<RemoteResponse> responseList = new ArrayList<>();
        Map<String, Future<HttpResponse>> futureResponseMap = new ConcurrentHashMap<>(8, 0.9f, 1);
        for (GenericUrl url : urlList) {
            try {
                if (paramList != null) {
                    for (HttpParam param : paramList) {
                        url.put(param.getParamName(), param.getParamValue());
                    }
                }
//                System.out.println(url.build());
                HttpRequest request = hrFactory.buildGetRequest(url);
                futureResponseMap.put(url.getHost() + '|' + url.getPort(), request.executeAsync());
                logger.info("futureResponseMap size:" + futureResponseMap.size());
                logger.info("CurrentThreadCounter:" + Thread.activeCount());
            } catch (IOException ex) {
                logger.error("", ex);
            }

        }
        for (Entry<String, Future<HttpResponse>> entry : futureResponseMap.entrySet()) {
            RemoteResponse response = new RemoteResponse();
            try {
                String ipPort = entry.getKey();
                String ip = ipPort.split("\\|")[0];
                int port = Integer.parseInt(ipPort.split("\\|")[1]);
                response.setIp(ip);
                response.setPort(port);
                String responseStr = parseResponse(entry.getValue().get());
                if (null != responseStr) {
                    response.setResponse(responseStr);
                    responseList.add(response);
                } else {
                    logger.info("{} response is null!", entry.getKey());
                }
            } catch (InterruptedException ex) {
                logger.error("", ex);
                Thread.currentThread().interrupt();
            } catch (ExecutionException ex) {
                logger.error("", ex);
//                logger.error(ex.getCause().getMessage());
            }
            logger.info("To debug remote get:{}", entry.getKey());
        }
        return responseList;
    }

    /**
     * get请求，返回请求结果cynosure
     *
     * @param paramList
     * @return
     */
    public RemoteResponse getRemote(List<HttpParam> paramList) {
        RemoteResponse response = new RemoteResponse();
        GenericUrl url = new GenericUrl();
        try {
            if (paramList != null) {
                for (HttpParam param : paramList) {
                    url.put(param.getParamName(), param.getParamValue());
                }
            }
//            System.out.println(url.build());
            HttpRequest request = hrFactory.buildGetRequest(url);
            response.setIp(url.getHost());
            response.setResponse(parseResponse(request.execute()));  //这种方式是同步的，当有很多个跳板，且时延有长有短时，总的获取响应的时间会很长                
        } catch (IOException ex) {
            logger.error("", ex);
        }
        return response;
    }

    /**
     * post请求
     *
     * @param paramList
     * @param jsonContent
     * @return
     */
    public List<RemoteResponse> post(List<HttpParam> paramList, String jsonContent) {
        List<RemoteResponse> responseList = new ArrayList<>();
        Map<String, Future<HttpResponse>> futureResponseMap = new ConcurrentHashMap<>(8, 0.9f, 1);
        for (GenericUrl url : urlList) {
            try {
                if (paramList != null) {
                    for (HttpParam param : paramList) {
                        url.put(param.getParamName(), param.getParamValue());
                    }
                }
                HttpRequest request = hrFactory.buildPostRequest(url, ByteArrayContent.fromString(null, jsonContent));
                request.getHeaders().setContentType(APPLICATION_JSON);
//                System.out.println(request.getUrl());
                futureResponseMap.put(url.getHost() + '|' + url.getPort(), request.executeAsync());
            } catch (IOException ex) {
                logger.warn(ex.getMessage());
            }
        }
        for (Entry<String, Future<HttpResponse>> entry : futureResponseMap.entrySet()) {
            RemoteResponse response = new RemoteResponse();
            try {
                String ipPort = entry.getKey();
                String ip = ipPort.split("\\|")[0];
                int port = Integer.parseInt(ipPort.split("\\|")[1]);
                response.setIp(ip);
                response.setPort(port);
                response.setResponse(parseResponse(entry.getValue().get()));
                responseList.add(response);
            } catch (InterruptedException ex) {
                logger.warn(ex.getMessage());
                Thread.currentThread().interrupt();
            } catch (ExecutionException ex) {
                logger.error("", ex);
//                Logger.getLogger(RemoteAccess.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return responseList;
    }

    /**
     * put请求
     *
     * @param paramList
     * @param jsonContent
     * @return
     */
    public List<RemoteResponse> put(List<HttpParam> paramList, String jsonContent) {
        List<RemoteResponse> responseList = new ArrayList<>();
        Map<String, Future<HttpResponse>> futureResponseMap = new ConcurrentHashMap<>(8, 0.9f, 1);
        for (GenericUrl url : urlList) {
            try {
                if (paramList != null) {
                    for (HttpParam param : paramList) {
                        url.put(param.getParamName(), param.getParamValue());
                    }
                }
                HttpRequest request = hrFactory.buildPutRequest(url, ByteArrayContent.fromString(null, jsonContent));
                request.getHeaders().setContentType(APPLICATION_JSON);
                futureResponseMap.put(url.getHost() + '|' + url.getPort(), request.executeAsync());
            } catch (IOException ex) {
                logger.warn(ex.getMessage());
            }
        }
        for (Entry<String, Future<HttpResponse>> entry : futureResponseMap.entrySet()) {
            RemoteResponse response = new RemoteResponse();
            try {
                String ipPort = entry.getKey();
                String ip = ipPort.split("\\|")[0];
                int port = Integer.parseInt(ipPort.split("\\|")[1]);
                response.setIp(ip);
                response.setPort(port);
                response.setResponse(parseResponse(entry.getValue().get()));
                responseList.add(response);
            } catch (InterruptedException ex) {
                logger.warn(ex.getMessage());
                Thread.currentThread().interrupt();
            } catch (ExecutionException ex) {
                logger.error("", ex);
//                Logger.getLogger(RemoteAccess.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return responseList;
    }

    public List<RemoteResponse> delete(List<HttpParam> paramList) {
        List<RemoteResponse> responseList = new ArrayList<>();
        Map<String, Future<HttpResponse>> futureResponseMap = new ConcurrentHashMap<>(8, 0.9f, 1);
        for (GenericUrl url : urlList) {
            try {
                if (paramList != null) {
                    for (HttpParam param : paramList) {
                        url.put(param.getParamName(), param.getParamValue());
                    }
                }
                HttpRequest request = hrFactory.buildDeleteRequest(url);
                request.getHeaders().setContentType(APPLICATION_JSON);
                futureResponseMap.put(url.getHost() + '|' + url.getPort(), request.executeAsync());
            } catch (IOException ex) {
                logger.warn(ex.getMessage());
            }
        }
        for (Entry<String, Future<HttpResponse>> entry : futureResponseMap.entrySet()) {
            RemoteResponse response = new RemoteResponse();
            try {
                String ipPort = entry.getKey();
                String ip = ipPort.split("\\|")[0];
                int port = Integer.parseInt(ipPort.split("\\|")[1]);
                response.setIp(ip);
                response.setPort(port);
                response.setResponse(parseResponse(entry.getValue().get()));
                responseList.add(response);
            } catch (InterruptedException ex) {
                logger.warn(ex.getMessage());
                Thread.currentThread().interrupt();
            } catch (ExecutionException ex) {
                logger.error("", ex);
//                Logger.getLogger(RemoteAccess.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return responseList;
    }
}
